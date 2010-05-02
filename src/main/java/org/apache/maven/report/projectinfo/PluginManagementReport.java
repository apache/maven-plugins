package org.apache.maven.report.projectinfo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Plugin Management report.
 *
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 * @goal plugin-management
 * @requiresDependencyResolution test
 */
public class PluginManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven Project Builder component.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Maven Artifact Factory component.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        PluginManagementRenderer r = new PluginManagementRenderer( getLog(), getSink(), locale, i18n, project
            .getPluginManagement().getPlugins(), project, mavenProjectBuilder, artifactFactory, localRepository );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "plugin-management";
    }

    protected String getI18Nsection()
    {
        return "pluginManagement";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        return project.getPluginManagement() != null && project.getPluginManagement().getPlugins() != null
            && !project.getPluginManagement().getPlugins().isEmpty();
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     *
     * @author Nick Stolwijk
     */
    protected static class PluginManagementRenderer
        extends AbstractMavenReportRenderer
    {
        private final Log log;

        private final List pluginManagement;

        private final Locale locale;

        private final I18N i18n;

        private final MavenProject project;

        private final MavenProjectBuilder mavenProjectBuilder;

        private final ArtifactFactory artifactFactory;

        private final ArtifactRepository localRepository;

        /**
         * @param log
         * @param sink
         * @param locale
         * @param i18n
         * @param plugins
         * @param project
         * @param mavenProjectBuilder
         * @param artifactFactory
         * @param localRepository
         */
        public PluginManagementRenderer( Log log, Sink sink, Locale locale, I18N i18n, List plugins,
                                         MavenProject project, MavenProjectBuilder mavenProjectBuilder,
                                         ArtifactFactory artifactFactory, ArtifactRepository localRepository )
        {
            super( sink );

            this.log = log;

            this.locale = locale;

            this.pluginManagement = plugins;

            this.i18n = i18n;

            this.project = project;

            this.mavenProjectBuilder = mavenProjectBuilder;

            this.artifactFactory = artifactFactory;

            this.localRepository = localRepository;
        }


        /** {@inheritDoc} */
        public String getTitle()
        {
            return getReportString( "report.pluginManagement.title" );
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            // === Section: Project PluginManagement.
            renderSectionPluginManagement();
        }

        private void renderSectionPluginManagement()
        {
            String[] tableHeader = getPluginTableHeader();

            startSection( getTitle() );

            // can't use straight artifact comparison because we want optional last
            Collections.sort( pluginManagement, getPluginComparator() );

            startTable();
            tableHeader( tableHeader );

            for ( Iterator iterator = pluginManagement.iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();
                VersionRange versionRange;
                if ( StringUtils.isEmpty( plugin.getVersion() ) )
                {
                    versionRange = VersionRange.createFromVersion( Artifact.RELEASE_VERSION );
                }
                else
                {
                    versionRange = VersionRange.createFromVersion( plugin.getVersion() );
                }

                Artifact pluginArtifact = artifactFactory.createParentArtifact( plugin.getGroupId(), plugin
                    .getArtifactId(), versionRange.toString() );
                List artifactRepositories = project.getPluginArtifactRepositories();
                if ( artifactRepositories == null )
                {
                    artifactRepositories = new ArrayList();
                }
                try
                {
                    MavenProject pluginProject = mavenProjectBuilder.buildFromRepository( pluginArtifact,
                                                                                          artifactRepositories,
                                                                                          localRepository );
                    tableRow( getPluginRow( pluginProject.getGroupId(), pluginProject.getArtifactId(), pluginProject
                        .getVersion(), pluginProject.getUrl() ) );
                }
                catch ( ProjectBuildingException e )
                {
                    log.info( "Could not build project for: " + plugin.getArtifactId() + ":" + e.getMessage(), e );
                    tableRow( getPluginRow( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null ) );
                }

            }
            endTable();

            endSection();
        }

        // ----------------------------------------------------------------------
        // Private methods
        // ----------------------------------------------------------------------

        private String[] getPluginTableHeader()
        {
            // reused key...
            String groupId = getReportString( "report.dependencyManagement.column.groupId" );
            String artifactId = getReportString( "report.dependencyManagement.column.artifactId" );
            String version = getReportString( "report.dependencyManagement.column.version" );
            return new String[] { groupId, artifactId, version };
        }

        private String[] getPluginRow( String groupId, String artifactId, String version, String link )
        {
            artifactId = ProjectInfoReportUtils.getArtifactIdCell( artifactId, link );
            return new String[] { groupId, artifactId, version };
        }

        private Comparator getPluginComparator()
        {
            return new Comparator()
            {
                /** {@inheritDoc} */
                public int compare( Object o1, Object o2 )
                {
                    Plugin a1 = (Plugin) o1;
                    Plugin a2 = (Plugin) o2;

                    int result = a1.getGroupId().compareTo( a2.getGroupId() );
                    if ( result == 0 )
                    {
                        result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                    }
                    return result;
                }
            };
        }

        private String getReportString( String key )
        {
            return i18n.getString( "project-info-report", locale, key );
        }
    }
}
