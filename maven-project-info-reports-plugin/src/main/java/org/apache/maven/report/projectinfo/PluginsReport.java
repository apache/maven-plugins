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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
 */
@Mojo( name = "plugins", requiresDependencyResolution = ResolutionScope.TEST )
public class PluginsReport
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
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = !isEmpty( getProject().getBuildPlugins() ) || !isEmpty( getProject().getReportPlugins() );
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        @SuppressWarnings( "unchecked" )
        PluginsRenderer r =
            new PluginsRenderer( getLog(), getSink(), locale, getI18N( locale ), project.getBuildPlugins(),
                                 project.getReportPlugins(), project, mavenProjectBuilder, artifactFactory,
                                 localRepository );
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

        private final List<Plugin> plugins;

        private final List<ReportPlugin> reports;

        private final MavenProject project;

        private final MavenProjectBuilder mavenProjectBuilder;

        private final ArtifactFactory artifactFactory;

        private final ArtifactRepository localRepository;

        /**
         * @param log {@link #log}
         * @param sink {@link Sink}
         * @param locale {@link Locale}
         * @param i18n {@link I18N}
         * @param plugins {@link Artifact}
         * @param reports {@link Artifact}
         * @param project {@link MavenProject}
         * @param mavenProjectBuilder {@link MavenProjectBuilder}
         * @param artifactFactory {@link ArtifactFactory}
         * @param localRepository {@link ArtifactRepository}
         *
         */
        public PluginsRenderer( Log log, Sink sink, Locale locale, I18N i18n, List<Plugin> plugins,
                                List<ReportPlugin> reports, MavenProject project,
                                MavenProjectBuilder mavenProjectBuilder, ArtifactFactory artifactFactory,
                                ArtifactRepository localRepository )
        {
            super( sink, i18n, locale );

            this.log = log;

            this.plugins = new ArrayList<Plugin>( plugins );

            this.reports = new ArrayList<ReportPlugin>( reports );

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
            List<GAV> list = isPlugins ? GAV.pluginsToGAV( plugins ) : GAV.reportPluginsToGAV( reports, project );
            String[] tableHeader = getPluginTableHeader();

            startSection( getI18nString( isPlugins ? "build.title" : "report.title" ) );

            if ( list == null || list.isEmpty() )
            {

                paragraph( getI18nString( isPlugins ? "nolist" : "report.nolist" ) ) ;
                endSection();
                return;
            }

            Collections.sort( list, getPluginComparator() );

            startTable();
            tableHeader( tableHeader );

            for ( GAV plugin : list )
            {
                VersionRange versionRange = VersionRange.createFromVersion( plugin.getVersion() );

                Artifact pluginArtifact =
                    artifactFactory.createParentArtifact( plugin.getGroupId(), plugin.getArtifactId(),
                                                          versionRange.toString() );
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
                    log.info( "Could not build project for " + plugin.getArtifactId() + ": " + e.getMessage(), e );
                    tableRow( getPluginRow( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(),
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
            String groupId = getI18nString( "dependency-management", "column.groupId" );
            String artifactId = getI18nString( "dependency-management", "column.artifactId" );
            String version = getI18nString( "dependency-management", "column.version" );
            return new String[] { groupId, artifactId, version };
        }

        private String[] getPluginRow( String groupId, String artifactId, String version, String link )
        {
            artifactId = ProjectInfoReportUtils.getArtifactIdCell( artifactId, link );
            return new String[] { groupId, artifactId, version };
        }

        private static class GAV
        {
            private final String groupId;
            private final String artifactId;
            private final String version;

            private GAV( Plugin plugin )
            {
                groupId = plugin.getGroupId();
                artifactId = plugin.getArtifactId();
                version = StringUtils.isEmpty( plugin.getVersion() ) ? Artifact.RELEASE_VERSION : plugin.getVersion();
            }

            private GAV( ReportPlugin reportPlugin, MavenProject project )
            {
                groupId = reportPlugin.getGroupId();
                artifactId = reportPlugin.getArtifactId();
                version = resolveReportPluginVersion( reportPlugin, project );
            }

            public String getGroupId()
            {
                return groupId;
            }

            public String getArtifactId()
            {
                return artifactId;
            }

            public String getVersion()
            {
                return version;
            }

            public static List<GAV> pluginsToGAV( List<Plugin> plugins )
            {
                List<GAV> result = new ArrayList<GAV>( plugins.size() );
                for ( Plugin plugin : plugins )
                {
                    result.add( new GAV( plugin ) );
                }
                return result;
            }

            public static List<GAV> reportPluginsToGAV( List<ReportPlugin> reportPlugins, MavenProject project )
            {
                List<GAV> result = new ArrayList<GAV>( reportPlugins.size() );
                for ( ReportPlugin reportPlugin : reportPlugins )
                {
                    result.add( new GAV( reportPlugin, project ) );
                }
                return result;
            }
        }

        private Comparator<GAV> getPluginComparator()
        {
            return new Comparator<GAV>()
            {
                /** {@inheritDoc} */
                public int compare( GAV a1, GAV a2 )
                {
                    int result = a1.groupId.compareTo( a2.groupId );
                    if ( result == 0 )
                    {
                        result = a1.artifactId.compareTo( a2.artifactId );
                    }
                    return result;
                }
            };
        }

        /**
         * Resolve report plugin version. Steps to find a plugin version stop after each step if a non <code>null</code>
         * value has been found:
         * <ol>
         * <li>use the one defined in the reportPlugin configuration,</li>
         * <li>search similar (same groupId and artifactId) mojo in the build/plugins section of the pom,</li>
         * <li>search similar (same groupId and artifactId) mojo in the build/pluginManagement section of the pom,</li>
         * <li>default value is RELEASE.</li>
         * </ol>
         *
         * @param reportPlugin the report plugin to resolve the version
         * @param project the current project
         * @return the report plugin version
         */
        protected static String resolveReportPluginVersion( ReportPlugin reportPlugin, MavenProject project )
        {
            // look for version defined in the reportPlugin configuration
            if ( reportPlugin.getVersion() != null )
            {
                return reportPlugin.getVersion();
            }

            // search in the build section
            if ( project.getBuild() != null )
            {
                Plugin plugin = find( reportPlugin, project.getBuild().getPlugins() );

                if ( plugin != null && plugin.getVersion() != null )
                {
                    return plugin.getVersion();
                }
            }

            // search in pluginManagement section
            if ( project.getBuild() != null && project.getBuild().getPluginManagement() != null )
            {
                Plugin plugin = find( reportPlugin, project.getBuild().getPluginManagement().getPlugins() );

                if ( plugin != null && plugin.getVersion() != null )
                {
                    return plugin.getVersion();
                }
            }

            // empty version
            return Artifact.RELEASE_VERSION;
        }

        /**
         * Search similar (same groupId and artifactId) plugin as a given report plugin.
         *
         * @param reportPlugin the report plugin to search for a similar plugin
         * @param plugins the candidate plugins
         * @return the first similar plugin
         */
        private static Plugin find( ReportPlugin reportPlugin, List<Plugin> plugins )
        {
            if ( plugins == null )
            {
                return null;
            }
            for ( Plugin plugin : plugins )
            {
                if ( StringUtils.equals( plugin.getArtifactId(), reportPlugin.getArtifactId() )
                    && StringUtils.equals( plugin.getGroupId(), reportPlugin.getGroupId() ) )
                {
                    return plugin;
                }
            }
            return null;
        }
    }
}
