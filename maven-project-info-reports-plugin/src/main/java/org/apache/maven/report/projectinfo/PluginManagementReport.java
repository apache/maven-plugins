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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Plugin Management report.
 *
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
@Mojo( name = "plugin-management", requiresDependencyResolution = ResolutionScope.TEST )
public class PluginManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven Project Builder component.
     */
    @Component
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Maven Artifact Factory component.
     */
    @Component
    private ArtifactFactory artifactFactory;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        PluginManagementRenderer r =
            new PluginManagementRenderer( getLog(), getSink(), locale, getI18N( locale ),
                                          project.getPluginManagement().getPlugins(), project, mavenProjectBuilder,
                                          artifactFactory, localRepository );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "plugin-management";
    }

    @Override
    protected String getI18Nsection()
    {
        return "pluginManagement";
    }

    @Override
    public boolean canGenerateReport()
    {
          boolean result = super.canGenerateReport();
          if ( result && skipEmptyReport )
          {
              result = getProject().getPluginManagement() != null
                      && !isEmpty( project.getPluginManagement().getPlugins() );
          }

          return result;
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
        extends AbstractProjectInfoRenderer
    {
        private final Log log;

        private final List<Plugin> pluginManagement;

        private final MavenProject project;

        private final MavenProjectBuilder mavenProjectBuilder;

        private final ArtifactFactory artifactFactory;

        private final ArtifactRepository localRepository;

        /**
         * @param log {@link #log}
         * @param sink {@link Sink}
         * @param locale {@link Locale}
         * @param i18n {@link I18N}
         * @param plugins {@link Plugin}
         * @param project {@link MavenProject}
         * @param mavenProjectBuilder {@link MavenProjectBuilder}
         * @param artifactFactory {@link ArtifactFactory}
         * @param localRepository {@link ArtifactRepository}
         */
        public PluginManagementRenderer( Log log, Sink sink, Locale locale, I18N i18n, List<Plugin> plugins,
                                         MavenProject project, MavenProjectBuilder mavenProjectBuilder,
                                         ArtifactFactory artifactFactory, ArtifactRepository localRepository )
        {
            super( sink, i18n, locale );

            this.log = log;

            this.pluginManagement = plugins;

            this.project = project;

            this.mavenProjectBuilder = mavenProjectBuilder;

            this.artifactFactory = artifactFactory;

            this.localRepository = localRepository;
        }

        @Override
        protected String getI18Nsection()
        {
            return "pluginManagement";
        }

        @Override
        public void renderBody()
        {
            PluginManagement pluginManagement = project.getPluginManagement();

            if ( pluginManagement == null || pluginManagement.getPlugins() == null
                || pluginManagement.getPlugins().isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nolist" ) );

                endSection();

                return;
            }

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

            for ( Plugin plugin : pluginManagement )
            {
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

        private Comparator<Plugin> getPluginComparator()
        {
            return new Comparator<Plugin>()
            {
                /** {@inheritDoc} */
                public int compare( Plugin a1, Plugin a2 )
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
