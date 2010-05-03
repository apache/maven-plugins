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
import org.apache.maven.model.Organization;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

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

    /** {@inheritDoc} */
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
            super( sink, i18n, locale );
        }

        protected String getI18Nsection()
        {
            return "summary";
        }

        /** {@inheritDoc} */
        protected void renderBody()
        {
            startSection( getTitle() );

            //general information sub-section
            String name = project.getName();
            if ( name == null )
            {
                name = "";
            }
            String description = project.getDescription();
            if ( description == null )
            {
                description = "";
            }
            String homepage = project.getUrl();
            if ( homepage == null )
            {
                homepage = "";
            }

            startSection( getI18nString( "general.title" ) );
            startTable();
            tableHeader( new String[]{getI18nString( "field" ), getI18nString( "value" )} );
            tableRow( new String[]{getI18nString( "general.name" ), name} );
            tableRow( new String[]{getI18nString( "general.description" ), description} );
            tableRowWithLink( new String[]{getI18nString( "general.homepage" ), homepage} );
            endTable();
            endSection();

            //organization sub-section
            startSection( getI18nString( "organization.title" ) );
            Organization organization = project.getOrganization();
            if ( organization == null )
            {
                paragraph( getI18nString( "noorganization" ) );
            }
            else
            {
                if ( organization.getName() == null )
                {
                    organization.setName( "" );
                }
                if ( organization.getUrl() == null )
                {
                    organization.setUrl( "" );
                }

                startTable();
                tableHeader( new String[]{getI18nString( "field" ), getI18nString( "value" )} );
                tableRow( new String[] { getI18nString( "organization.name" ), organization.getName() } );
                tableRowWithLink( new String[]{getI18nString( "organization.url" ), organization.getUrl()} );
                endTable();
            }
            endSection();

            //build section
            startSection( getI18nString( "build.title" ) );
            startTable();
            tableHeader( new String[]{getI18nString( "field" ), getI18nString( "value" )} );
            tableRow( new String[]{getI18nString( "build.groupid" ), project.getGroupId()} );
            tableRow( new String[]{getI18nString( "build.artifactid" ), project.getArtifactId()} );
            tableRow( new String[]{getI18nString( "build.version" ), project.getVersion()} );
            tableRow( new String[]{getI18nString( "build.type" ), project.getPackaging()} );
            endTable();
            endSection();

            endSection();
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
                else
                {
                    if ( ctr == content.length - 1 && cell.length() > 0 )
                    {
                        sink.link( cell );
                        sink.text( cell );
                        sink.link_();
                    }
                    else
                    {
                        sink.text( cell );
                    }
                }
                sink.tableCell_();
            }

            sink.tableRow_();
        }
    }
}
