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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Generates the project information reports summary.
 *
 * @author Edwin Punzalan
 * @version $Id$
 * @since 2.0
 * @goal summary
 * @plexus.component
 */
public class ProjectSummaryReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        new ProjectSummaryRenderer( getSink(), locale ).render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "project-summary";
    }

    @Override
    protected String getI18Nsection()
    {
        return "summary";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private class ProjectSummaryRenderer
        extends AbstractProjectInfoRenderer
    {
        ProjectSummaryRenderer( Sink sink, Locale locale )
        {
            super( sink, getI18N( locale ), locale );
        }

        @Override
        protected String getI18Nsection()
        {
            return "summary";
        }

        @Override
        protected void renderBody()
        {
            startSection( getTitle() );

            // general information sub-section
            startSection( getI18nString( "general.title" ) );
            startTable();
            tableHeader( new String[] { getI18nString( "field" ), getI18nString( "value" ) } );
            tableRow( new String[] { getI18nString( "general.name" ), project.getName() } );
            tableRow( new String[] { getI18nString( "general.description" ), project.getDescription() } );
            tableRowWithLink( new String[] { getI18nString( "general.homepage" ), project.getUrl() } );
            endTable();
            endSection();

            // organization sub-section
            startSection( getI18nString( "organization.title" ) );
            Organization organization = project.getOrganization();
            if ( organization == null )
            {
                paragraph( getI18nString( "noorganization" ) );
            }
            else
            {
                startTable();
                tableHeader( new String[] { getI18nString( "field" ), getI18nString( "value" ) } );
                tableRow( new String[] { getI18nString( "organization.name" ), organization.getName() } );
                tableRowWithLink( new String[] { getI18nString( "organization.url" ), organization.getUrl() } );
                endTable();
            }
            endSection();

            // build section
            startSection( getI18nString( "build.title" ) );
            startTable();
            tableHeader( new String[] { getI18nString( "field" ), getI18nString( "value" ) } );
            tableRow( new String[] { getI18nString( "build.groupid" ), project.getGroupId() } );
            tableRow( new String[] { getI18nString( "build.artifactid" ), project.getArtifactId() } );
            tableRow( new String[] { getI18nString( "build.version" ), project.getVersion() } );
            tableRow( new String[] { getI18nString( "build.type" ), project.getPackaging() } );
            if ( isJavaProject( project ) )
            {
                tableRow( new String[] { getI18nString( "build.jdk" ), getMinimumJavaVersion() } );
            }
            endTable();
            endSection();

            // download section
            DistributionManagement distributionManagement = project.getDistributionManagement();
            if ( distributionManagement != null )
            {
                if ( StringUtils.isNotEmpty( distributionManagement.getDownloadUrl() ) )
                {
                    startSection( getI18nString( "download" ) );
                    link( distributionManagement.getDownloadUrl(), distributionManagement.getDownloadUrl() );
                    endSection();
                }
                else if ( distributionManagement.getRepository() != null
                    && StringUtils.isNotEmpty( distributionManagement.getRepository().getUrl() ) )
                {
                    startSection( getI18nString( "download" ) );
                    String link = distributionManagement.getRepository().getUrl();
                    if ( !link.endsWith( "/" ) )
                    {
                        link += "/";
                    }
                    link += StringUtils.replace( project.getGroupId(), ".", "/" );
                    link += "/";
                    link += project.getArtifactId();
                    link( link, link );
                    endSection();
                }
            }

            endSection();
        }

        private String getMinimumJavaVersion()
        {
            Xpp3Dom pluginConfig =
                project.getGoalConfiguration( "org.apache.maven.plugins", "maven-compiler-plugin", null, null );

            String source = null;
            String target = null;
            String compilerVersion = null;

            if ( pluginConfig != null )
            {
                source = getChildValue( pluginConfig, "source" );
                target = getChildValue( pluginConfig, "target" );

                String fork = getChildValue( pluginConfig, "fork" );
                if ( "true".equalsIgnoreCase( fork ) )
                {
                    compilerVersion = getChildValue( pluginConfig, "compilerVersion" );
                }
            }

            String minimumJavaVersion = compilerVersion;
            if ( target != null )
            {
                minimumJavaVersion = target;
            }
            else if ( source != null )
            {
                minimumJavaVersion = source;
            }
            else if ( compilerVersion != null )
            {
                minimumJavaVersion = compilerVersion;
            }
            else
            {
                // no source, target, compilerVersion: toolchain? default target attribute of current
                // maven-compiler-plugin's version? analyze packaged jar (like dependencies)?
            }

            return minimumJavaVersion;
        }

        private String getChildValue( Xpp3Dom parent, String childName )
        {
            if ( parent == null )
            {
                return null;
            }

            Xpp3Dom child = parent.getChild( childName );

            if ( child == null )
            {
                return null;
            }

            String value = child.getValue();

            if ( value == null || value.trim().length() == 0 )
            {
                return null;
            }

            return value.trim();
        }

        private void tableRowWithLink( String[] content )
        {
            sink.tableRow();

            for ( int ctr = 0; ctr < content.length; ctr++ )
            {
                String cell = content[ctr];

                sink.tableCell();

                if ( StringUtils.isEmpty( cell ) )
                {
                    sink.text( "-" );
                }
                else if ( ctr == content.length - 1 && cell.length() > 0 )
                {
                    sink.link( cell );
                    sink.text( cell );
                    sink.link_();
                }
                else
                {
                    sink.text( cell );
                }
                sink.tableCell_();
            }

            sink.tableRow_();
        }

        /**
         * @param project not null
         * @return return <code>true</code> if the Maven project sounds like a Java Project, i.e. has a java type
         *         packaging (like jar, war...) or java files in the source directory, <code>false</code> otherwise.
         * @since 2.3
         */
        private boolean isJavaProject( MavenProject project )
        {
            String packaging = project.getPackaging().trim().toLowerCase( Locale.ENGLISH );
            if ( packaging.equals( "pom" ) )
            {
                return false;
            }

            // some commons java packaging
            if ( packaging.equals( "jar" ) || packaging.equals( "ear" ) || packaging.equals( "war" )
                || packaging.equals( "rar" ) || packaging.equals( "sar" ) || packaging.equals( "har" ) )
            {
                return true;
            }

            // java files in the source directory?
            try
            {
                if ( FileUtils.getFileNames( new File( project.getBuild().getSourceDirectory() ), "**/*.java", null,
                                             false ).size() > 0 )
                {
                    return true;
                }
            }
            catch ( IOException e )
            {
                // ignored
            }

            // maven-compiler-plugin ?
            Xpp3Dom pluginConfig =
                project.getGoalConfiguration( "org.apache.maven.plugins", "maven-compiler-plugin", null, null );
            if ( pluginConfig != null )
            {
                return true;
            }

            return false;
        }
    }
}
