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
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Generates the Project Team report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal project-team
 */
public class TeamListReport
    extends AbstractProjectInfoReport
{
    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.team-list.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.team-list.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        TeamListRenderer r = new TeamListRenderer( getSink(), project.getModel(), i18n, locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "team-list";
    }

    private static class TeamListRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        TeamListRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink );

            this.model = model;

            this.i18n = i18n;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.team-list.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( i18n.getString( "project-info-report", locale, "report.team-list.intro.title" ) );

            // To handle JS
            StringBuffer javascript = new StringBuffer( "function offsetDate(id, offset) {\n" );
            javascript.append( "    var now = new Date();\n" );
            javascript.append( "    var nowTime = now.getTime();\n" );
            javascript.append( "    var localOffset = now.getTimezoneOffset();\n" );
            javascript.append(
                "    var developerTime = nowTime + ( offset * 60 * 60 * 1000 ) + ( localOffset * 60 * 1000 );\n" );
            javascript.append( "    var developerDate = new Date(developerTime);\n" );
            javascript.append( "\n" );
            javascript.append( "    document.getElementById(id).innerHTML = developerDate;\n" );
            javascript.append( "}\n" );
            javascript.append( "\n" );
            javascript.append( "function init(){\n" );

            // Intoduction
            paragraph( i18n.getString( "project-info-report", locale, "report.team-list.intro.description1" ) );
            paragraph( i18n.getString( "project-info-report", locale, "report.team-list.intro.description2" ) );

            // Developer section
            List developers = model.getDevelopers();

            startSection( i18n.getString( "project-info-report", locale, "report.team-list.developers.title" ) );

            if ( developers == null || developers.isEmpty() )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.team-list.nodeveloper" ) );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.team-list.developers.intro" ) );

                startTable();

                String id = i18n.getString( "project-info-report", locale, "report.team-list.developers.id" );
                String name = i18n.getString( "project-info-report", locale, "report.team-list.developers.name" );
                String email = i18n.getString( "project-info-report", locale, "report.team-list.developers.email" );
                String url = i18n.getString( "project-info-report", locale, "report.team-list.developers.url" );
                String organization =
                    i18n.getString( "project-info-report", locale, "report.team-list.developers.organization" );
                String organizationUrl =
                    i18n.getString( "project-info-report", locale, "report.team-list.developers.organizationurl" );
                String roles = i18n.getString( "project-info-report", locale, "report.team-list.developers.roles" );
                String timeZone =
                    i18n.getString( "project-info-report", locale, "report.team-list.developers.timezone" );
                String actualTime =
                    i18n.getString( "project-info-report", locale, "report.team-list.developers.actualtime" );
                String properties =
                    i18n.getString( "project-info-report", locale, "report.team-list.developers.properties" );

                tableHeader( new String[]{id, name, email, url, organization, organizationUrl, roles, timeZone,
                    actualTime, properties} );

                // To handle JS
                int developersRows = 0;
                for ( Iterator i = developers.iterator(); i.hasNext(); )
                {
                    Developer developer = (Developer) i.next();

                    // To handle JS
                    sink.tableRow();

                    tableCell( "<a name=\"" + developer.getId() + "\"></a>" + developer.getId(), true );

                    tableCell( developer.getName() );

                    tableCell( createLinkPatternedText( developer.getEmail(), developer.getEmail() ) );

                    tableCell( createLinkPatternedText( developer.getUrl(), developer.getUrl() ) );

                    tableCell( developer.getOrganization() );

                    tableCell(
                        createLinkPatternedText( developer.getOrganizationUrl(), developer.getOrganizationUrl() ) );

                    if ( developer.getRoles() != null )
                    {
                        // Comma separated roles
                        tableCell( StringUtils.join( developer.getRoles().toArray( EMPTY_STRING_ARRAY ), ", " ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    tableCell( developer.getTimezone() );

                    // To handle JS
                    sink.tableCell();
                    sink.rawText( "<span id=\"developer-" + developersRows + "\">" );
                    text( developer.getTimezone() );
                    if ( !StringUtils.isEmpty( developer.getTimezone() ) )
                    {
                        javascript.append( "    offsetDate('developer-" ).append( developersRows ).append( "', '" );
                        javascript.append( developer.getTimezone() );
                        javascript.append( "');\n" );
                    }
                    sink.rawText( "</span>" );
                    sink.tableCell_();

                    Properties props = developer.getProperties();
                    if ( props != null )
                    {
                        tableCell( propertiesToString( props ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    sink.tableRow_();

                    developersRows++;
                }

                endTable();
            }

            endSection();

            // contributors section
            List contributors = model.getContributors();

            startSection( i18n.getString( "project-info-report", locale, "report.team-list.contributors.title" ) );

            if ( contributors == null || contributors.isEmpty() )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.team-list.nocontributor" ) );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.team-list.contributors.intro" ) );

                startTable();

                String name = i18n.getString( "project-info-report", locale, "report.team-list.contributors.name" );
                String email = i18n.getString( "project-info-report", locale, "report.team-list.contributors.email" );
                String url = i18n.getString( "project-info-report", locale, "report.team-list.contributors.url" );
                String organization =
                    i18n.getString( "project-info-report", locale, "report.team-list.contributors.organization" );
                String organizationUrl =
                    i18n.getString( "project-info-report", locale, "report.team-list.contributors.organizationurl" );
                String roles = i18n.getString( "project-info-report", locale, "report.team-list.contributors.roles" );
                String timeZone =
                    i18n.getString( "project-info-report", locale, "report.team-list.contributors.timezone" );
                String actualTime =
                    i18n.getString( "project-info-report", locale, "report.team-list.contributors.actualtime" );
                String properties =
                    i18n.getString( "project-info-report", locale, "report.team-list.contributors.properties" );

                tableHeader( new String[]{name, email, url, organization, organizationUrl, roles, timeZone, actualTime,
                    properties} );

                // To handle JS
                int contributorsRows = 0;
                for ( Iterator i = contributors.iterator(); i.hasNext(); )
                {
                    Contributor contributor = (Contributor) i.next();

                    sink.tableRow();

                    tableCell( contributor.getName() );

                    tableCell( createLinkPatternedText( contributor.getEmail(), contributor.getEmail() ) );

                    tableCell( createLinkPatternedText( contributor.getUrl(), contributor.getUrl() ) );

                    tableCell( contributor.getOrganization() );

                    tableCell(
                        createLinkPatternedText( contributor.getOrganizationUrl(), contributor.getOrganizationUrl() ) );

                    if ( contributor.getRoles() != null )
                    {
                        // Comma separated roles
                        tableCell( StringUtils.join( contributor.getRoles().toArray( EMPTY_STRING_ARRAY ), ", " ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    tableCell( contributor.getTimezone() );

                    // To handle JS
                    sink.tableCell();
                    sink.rawText( "<span id=\"contributor-" + contributorsRows + "\">" );
                    text( contributor.getTimezone() );
                    if ( !StringUtils.isEmpty( contributor.getTimezone() ) )
                    {
                        javascript.append( "    offsetDate('contributor-" ).append( contributorsRows ).append( "', '" );
                        javascript.append( contributor.getTimezone() );
                        javascript.append( "');\n" );
                    }
                    sink.rawText( "</span>" );
                    sink.tableCell_();

                    Properties props = contributor.getProperties();
                    if ( props != null )
                    {
                        tableCell( propertiesToString( props ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    sink.tableRow_();

                    contributorsRows++;
                }

                endTable();
            }

            endSection();

            endSection();

            // To handle JS
            javascript.append( "}\n" ).append( "\n" ).append( "window.onLoad = init();\n" );
            javaScript( javascript.toString() );
        }
    }
}
