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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Plugins report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 * @goal plugins
 * @requiresDependencyResolution test
 */
public class PluginsReport
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

    @Override
    public void executeReport( Locale locale )
    {
        @SuppressWarnings( "unchecked" )
        PluginsRenderer r = new PluginsRenderer( getLog(), getSink(), locale, getI18N( locale ), project.getPluginArtifacts(),
                                                 project.getReportArtifacts(), project, mavenProjectBuilder,
                                                 artifactFactory, localRepository );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "plugins";
    }

    @Override
    protected String getI18Nsection()
    {
        return "plugins";
    }

    @Override
    public boolean canGenerateReport()
    {
        return ( project.getPluginArtifacts() != null && !project.getPluginArtifacts().isEmpty() )
            || ( project.getReportArtifacts() != null && !project.getReportArtifacts().isEmpty() );
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    protected static class PluginsRenderer
        extends AbstractProjectInfoRenderer
    {
        private final Log log;

        private final List<Artifact> plugins;

        private final List<Artifact> reports;

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
         * @param reports
         * @param project
         * @param mavenProjectBuilder
         * @param artifactFactory
         * @param localRepository
         */
        public PluginsRenderer( Log log, Sink sink, Locale locale, I18N i18n, Set<Artifact> plugins,
                                Set<Artifact> reports, MavenProject project, MavenProjectBuilder mavenProjectBuilder,
                                ArtifactFactory artifactFactory, ArtifactRepository localRepository )
        {
            super( sink, i18n, locale );

            this.log = log;

            this.plugins = new ArrayList<Artifact>( plugins );

            this.reports = new ArrayList<Artifact>( reports );

            this.project = project;

            this.mavenProjectBuilder = mavenProjectBuilder;

            this.artifactFactory = artifactFactory;

            this.localRepository = localRepository;
        }

        @Override
        protected String getI18Nsection()
        {
            return "plugins";
        }

        @Override
        public void renderBody()
        {
            // === Section: Project Plugins.
            renderSectionPlugins( true );

            // === Section: Project Reports.
            renderSectionPlugins( false );
        }

        /**
         * @param isPlugins <code>true</code> to use <code>plugins</code> variable, <code>false</code> to use
         * <code>reports</code> variable.
         */
        private void renderSectionPlugins( boolean isPlugins )
        {
            List<Artifact> list = ( isPlugins ? plugins : reports );
            String[] tableHeader = getPluginTableHeader();

            startSection( ( isPlugins ? getI18nString( "title" )
                                     : getI18nString( "report.title" ) ) );

            if ( list == null || list.isEmpty() )
            {

                paragraph(  ( isPlugins ? getI18nString( "nolist" )
                                        : getI18nString( "report.nolist" ) ) );

                endSection();

                return;
            }

            Collections.sort( list, getArtifactComparator() );

            startTable();
            tableHeader( tableHeader );

            for ( Artifact artifact : list )
            {
                VersionRange versionRange;
                if ( StringUtils.isEmpty( artifact.getVersion() ) )
                {
                    versionRange = VersionRange.createFromVersion( Artifact.RELEASE_VERSION );
                }
                else
                {
                    versionRange = VersionRange.createFromVersion( artifact.getVersion() );
                }

                Artifact pluginArtifact = artifactFactory.createParentArtifact( artifact.getGroupId(), artifact
                    .getArtifactId(), versionRange.toString() );
                @SuppressWarnings( "unchecked" )
                List<ArtifactRepository> artifactRepositories = project.getPluginArtifactRepositories();
                if ( artifactRepositories == null )
                {
                    artifactRepositories = new ArrayList<ArtifactRepository>();
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
                    log.info( "Could not build project for: " + artifact.getArtifactId() + ":" + e.getMessage(), e );
                    tableRow( getPluginRow( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                            null ) );
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
            String groupId = getI18nString( "dependencyManagement", "column.groupId" );
            String artifactId = getI18nString( "dependencyManagement", "column.artifactId" );
            String version = getI18nString( "dependencyManagement", "column.version" );
            return new String[] { groupId, artifactId, version };
        }

        private String[] getPluginRow( String groupId, String artifactId, String version, String link )
        {
            artifactId = ProjectInfoReportUtils.getArtifactIdCell( artifactId, link );
            return new String[] { groupId, artifactId, version };
        }

        private Comparator<Artifact> getArtifactComparator()
        {
            return new Comparator<Artifact>()
            {
                /** {@inheritDoc} */
                public int compare( Artifact a1, Artifact a2 )
                {
                    int result = a1.getGroupId().compareTo( a2.getGroupId() );
                    if ( result == 0 )
                    {
                        result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                    }
                    return result;
                }
            };
        }
    }
}
