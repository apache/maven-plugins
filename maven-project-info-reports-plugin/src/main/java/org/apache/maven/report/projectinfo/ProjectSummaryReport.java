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
import org.apache.maven.reporting.AbstractMavenReportRenderer;
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
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.summary.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.summary.description" );
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "project-summary";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private class ProjectSummaryRenderer
        extends AbstractMavenReportRenderer
    {
        private final Locale locale;

        ProjectSummaryRenderer( Sink sink, Locale locale )
        {
            super( sink );

            this.locale = locale;
        }

        /** {@inheritDoc} */
        public String getTitle()
        {
            return getReportString( "report.summary.title" );
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

            startSection( getReportString( "report.summary.general.title" ) );
            startTable();
            tableHeader(
                new String[]{getReportString( "report.summary.field" ), getReportString( "report.summary.value" )} );
            tableRow( new String[]{getReportString( "report.summary.general.name" ), name} );
            tableRow( new String[]{getReportString( "report.summary.general.description" ), description} );
            tableRowWithLink( new String[]{getReportString( "report.summary.general.homepage" ), homepage} );
            endTable();
            endSection();

            //organization sub-section
            startSection( getReportString( "report.summary.organization.title" ) );
            Organization organization = project.getOrganization();
            if ( organization == null )
            {
                paragraph( getReportString( "report.summary.noorganization" ) );
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
                tableHeader( new String[]{getReportString( "report.summary.field" ),
                    getReportString( "report.summary.value" )} );
                tableRow( new String[] { getReportString( "report.summary.organization.name" ),
                    organization.getName() } );
                tableRowWithLink(
                    new String[]{getReportString( "report.summary.organization.url" ), organization.getUrl()} );
                endTable();
            }
            endSection();

            //build section
            startSection( getReportString( "report.summary.build.title" ) );
            startTable();
            tableHeader(
                new String[]{getReportString( "report.summary.field" ), getReportString( "report.summary.value" )} );
            tableRow( new String[]{getReportString( "report.summary.build.groupid" ), project.getGroupId()} );
            tableRow( new String[]{getReportString( "report.summary.build.artifactid" ), project.getArtifactId()} );
            tableRow( new String[]{getReportString( "report.summary.build.version" ), project.getVersion()} );
            tableRow( new String[]{getReportString( "report.summary.build.type" ), project.getPackaging()} );
            endTable();
            endSection();

            endSection();
        }

        private String getReportString( String key )
        {
            return i18n.getString( "project-info-report", locale, key );
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
