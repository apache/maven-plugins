package org.apache.maven.report.projectinfo.dependencies.renderer;

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

/**
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
public class PluginManagementRenderer
    extends AbstractMavenReportRenderer
{
    private List plugins;

    private final Locale locale;

    private I18N i18n;

    private final MavenProject project;

    private final MavenProjectBuilder mavenProjectBuilder;

    private final ArtifactFactory artifactFactory;

    private final ArtifactRepository localRepository;

    private Log log;

    public PluginManagementRenderer( Sink sink, Locale locale, I18N i18n, List plugins, MavenProject project,
                                     MavenProjectBuilder mavenProjectBuilder, ArtifactFactory artifactFactory,
                                     ArtifactRepository localRepository )
    {
        super( sink );

        this.locale = locale;

        this.plugins = plugins;

        this.i18n = i18n;

        this.project = project;

        this.mavenProjectBuilder = mavenProjectBuilder;

        this.artifactFactory = artifactFactory;

        this.localRepository = localRepository;

    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public void setLog( Log log )
    {
        this.log = log;
    }

    /** {@inheritDoc} */
    public String getTitle()
    {
        return getReportString( "report.pluginManagement.title" );
    }

    /** {@inheritDoc} */
    public void renderBody()
    {
        // Dependencies report

        if ( plugins.isEmpty() )
        {
            startSection( getTitle() );

            // TODO: should the report just be excluded?
            paragraph( getReportString( "report.pluginManagement.nolist" ) );

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionPluginManagement();
    }

    private void renderSectionPluginManagement()
    {
        String[] tableHeader = getPluginTableHeader();

        startSection( getTitle() );

        if ( plugins != null )
        {
            // can't use straight artifact comparison because we want optional last
            Collections.sort( plugins, getPluginComparator() );

            startTable();
            tableHeader( tableHeader );

            for ( Iterator iterator = plugins.iterator(); iterator.hasNext(); )
            {
                Plugin plugin = (Plugin) iterator.next();
                VersionRange versionRange;
                if ( plugin.getVersion() == null || "".equals( plugin.getVersion() ) )
                {
                    versionRange = VersionRange.createFromVersion( Artifact.RELEASE_VERSION );
                }
                else
                {
                    versionRange = VersionRange.createFromVersion( plugin.getVersion() );
                }

                Artifact pluginArtifact =
                    artifactFactory.createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(), versionRange );
                List artifactRepositories = project.getPluginArtifactRepositories();
                if ( artifactRepositories == null )
                {
                    artifactRepositories = new ArrayList();
                }
                try
                {
                    MavenProject pluginProject =
                        mavenProjectBuilder.buildFromRepository( pluginArtifact, artifactRepositories, localRepository );
                    tableRow( getPluginRow( plugin, pluginProject.getUrl() ) );
                }
                catch ( ProjectBuildingException e )
                {
                    log.info( "Could not build project for: " + plugin.getArtifactId() + ":" + e.getMessage(), e );
                    tableRow( getPluginRow( plugin, null ) );
                }

            }
            endTable();

        }

        endSection();
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private String[] getPluginTableHeader()
    {
        String groupId = getReportString( "report.dependencyManagement.column.groupId" );
        String artifactId = getReportString( "report.dependencyManagement.column.artifactId" );
        String version = getReportString( "report.dependencyManagement.column.version" );
        return new String[] { groupId, artifactId, version };
    }

    private String[] getPluginRow( Plugin plugin, String link )
    {
        String artifactId = getPluginCell( plugin.getArtifactId(), link );
        return new String[] { plugin.getGroupId(), artifactId, plugin.getVersion() };
    }

    private String getPluginCell( String text, String link )
    {
        if ( link == null || "".equals( link ) )
        {
            return text;
        }
        return "{" + text + "," + link + "}";
    }

    private Comparator getPluginComparator()
    {
        return new Comparator()
        {
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
