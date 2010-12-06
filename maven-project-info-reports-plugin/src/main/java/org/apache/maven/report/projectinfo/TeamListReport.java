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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Generates the Project Team report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal project-team
 */
public class TeamListReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        TeamListRenderer r = new TeamListRenderer( getSink(), project.getModel(), getI18N( locale ), locale, getLog() );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "team-list";
    }

    @Override
    protected String getI18Nsection()
    {
        return "team-list";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class TeamListRenderer
        extends AbstractProjectInfoRenderer
    {
        private static final String PROPERTIES = "properties";

        private static final String TIME_ZONE = "timeZone";

        private static final String ROLES = "roles";

        private static final String ORGANIZATION_URL = "organizationUrl";

        private static final String ORGANIZATION = "organization";

        private static final String URL = "url";

        private static final String EMAIL = "email";

        private static final String NAME = "name";

        private static final String ID = "id";

        private final Model model;

        private final Log log;

        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        TeamListRenderer( Sink sink, Model model, I18N i18n, Locale locale, Log log )
        {
            super( sink, i18n, locale );

            this.model = model;
            this.log = log;
        }

        @Override
        protected String getI18Nsection()
        {
            return "team-list";
        }

        @Override
        public void renderBody()
        {
            startSection( getI18nString( "intro.title" ) );

            // To handle JS
            StringBuffer javascript = new StringBuffer( "function offsetDate(id, offset) {" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var now = new Date();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var nowTime = now.getTime();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var localOffset = now.getTimezoneOffset();" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var developerTime = nowTime + ( offset * 60 * 60 * 1000 )"
                               + "+ ( localOffset * 60 * 1000 );" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    var developerDate = new Date(developerTime);" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "    document.getElementById(id).innerHTML = developerDate;" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( "}" ).append( SystemUtils.LINE_SEPARATOR );
            javascript.append( SystemUtils.LINE_SEPARATOR);
            javascript.append( "function init(){" ).append( SystemUtils.LINE_SEPARATOR );

            // Introduction
            paragraph( getI18nString( "intro.description1" ) );
            paragraph( getI18nString( "intro.description2" ) );

            // Developer section
            List<Developer> developers = model.getDevelopers();

            startSection( getI18nString( "developers.title" ) );

            if ( isEmpty( developers ) )
            {
                paragraph( getI18nString( "nodeveloper" ) );
            }
            else
            {
                paragraph( getI18nString( "developers.intro" ) );

                startTable();

                // By default we think that all headers not required: set true for headers that are required
                Map<String, Boolean> headersMap = checkRequiredHeaders( developers );
                String[] requiredHeaders = getRequiredDevHeaderArray( headersMap );

                tableHeader( requiredHeaders );

                // To handle JS
                int developersRowId = 0;
                for ( Developer developer : developers )
                {
                    renderTeamMember( developer, developersRowId, headersMap, javascript );

                    developersRowId++;
                }

                endTable();
            }

            endSection();

            // contributors section
            List<Contributor> contributors = model.getContributors();

            startSection( getI18nString( "contributors.title" ) );

            if ( isEmpty( contributors ) )
            {
                paragraph( getI18nString( "nocontributor" ) );
            }
            else
            {
                paragraph( getI18nString( "contributors.intro" ) );

                startTable();

                Map<String, Boolean> headersMap = checkRequiredHeaders( contributors );
                String[] requiredHeaders = getRequiredContrHeaderArray( headersMap );

                tableHeader( requiredHeaders );

                // To handle JS
                int contributorsRowId = 0;
                for ( Contributor contributor : contributors )
                {
                    renderTeamMember( contributor, contributorsRowId, headersMap, javascript );

                    contributorsRowId++;
                }

                endTable();
            }

            // To handle JS
            javascript.append( "}" ).append( SystemUtils.LINE_SEPARATOR ).append( SystemUtils.LINE_SEPARATOR )
                .append( "window.onLoad = init();" ).append( SystemUtils.LINE_SEPARATOR );
            javaScript( javascript.toString() );

            endSection();

            endSection();
        }

        private void renderTeamMember( Contributor member, int rowId, Map<String, Boolean> headersMap,
                                       StringBuffer javascript )
        {
            sink.tableRow();

            String type = "contributor";
            if ( member instanceof Developer )
            {
                type = "developer";
                if ( headersMap.get( ID ) == Boolean.TRUE )
                {
                    String id = ( (Developer) member ).getId();
                    if ( id == null )
                    {
                        tableCell( null );
                    }
                    else
                    {
                        tableCell( "<a name=\"" + id + "\"></a>" + id, true );
                    }
                }
            }
            if ( headersMap.get( NAME ) == Boolean.TRUE )
            {
                tableCell( member.getName() );
            }
            if ( headersMap.get( EMAIL ) == Boolean.TRUE )
            {
                tableCell( createLinkPatternedText( member.getEmail(), member.getEmail() ) );
            }
            if ( headersMap.get( URL ) == Boolean.TRUE )
            {
                tableCellForUrl( member.getUrl() );
            }
            if ( headersMap.get( ORGANIZATION ) == Boolean.TRUE )
            {
                tableCell( member.getOrganization() );
            }
            if ( headersMap.get( ORGANIZATION_URL ) == Boolean.TRUE )
            {
                tableCellForUrl( member.getOrganizationUrl() );
            }
            if ( headersMap.get( ROLES ) == Boolean.TRUE )
            {
                if ( member.getRoles() != null )
                {
                    // Comma separated roles
                    tableCell( StringUtils.join( member.getRoles().toArray( EMPTY_STRING_ARRAY ), ", " ) );
                }
                else
                {
                    tableCell( null );
                }
            }
            if ( headersMap.get( TIME_ZONE ) == Boolean.TRUE )
            {
                tableCell( member.getTimezone() );

                if ( !NumberUtils.isNumber( member.getTimezone() ) && StringUtils.isNotEmpty( member.getTimezone() ) )
                {
                    String tz = member.getTimezone().trim();
                    try
                    {
                        // check if it is a valid timeZone
                        DateTimeZone.forID( tz );

                        sink.tableCell();
                        sink.rawText( "<span id=\"" + type + "-" + rowId + "\">" );
                        text( tz );
                        String offSet = String.valueOf( TimeZone.getTimeZone( tz ).getRawOffset() / 3600000 );
                        javascript.append( "    offsetDate('" ).append( type ).append( "-" ).append( rowId ).append( "', '" );
                        javascript.append( offSet ).append( "');" ).append( SystemUtils.LINE_SEPARATOR );
                        sink.rawText( "</span>" );
                        sink.tableCell_();
                    }
                    catch ( IllegalArgumentException e )
                    {
                        log.warn( "The time zone '" + tz + "' for the " + type + " '" + member.getName()
                            + "' is not a recognised time zone, use a number in the range -12 and +14 instead of." );

                        sink.tableCell();
                        sink.rawText( "<span id=\"" + type + "-" + rowId + "\">" );
                        text( null );
                        sink.rawText( "</span>" );
                        sink.tableCell_();
                    }
                }
                else
                {
                    // To handle JS
                    sink.tableCell();
                    sink.rawText( "<span id=\"" + type + "-" + rowId + "\">" );
                    if ( StringUtils.isEmpty( member.getTimezone() ) )
                    {
                        text( null );
                    }
                    else
                    {
                        // check if number is between -12 and +14
                        int tz = NumberUtils.toInt( member.getTimezone().trim(), Integer.MIN_VALUE );
                        if ( tz == Integer.MIN_VALUE || !( tz >= -12 && tz <= 14 ) )
                        {
                            text( null );
                            log.warn( "The time zone '" + tz + "' for the " + type + " '" + member.getName()
                                + "' is not a recognised time zone, use a number in the range -12 to +14 instead of." );
                        }
                        else
                        {
                            text( member.getTimezone() );
                            javascript.append( "    offsetDate('" ).append( type ).append( "-" ).append( rowId ).append( "', '" );
                            javascript.append( member.getTimezone() ).append( "');" ).append( SystemUtils.LINE_SEPARATOR );
                        }
                    }
                    sink.rawText( "</span>" );
                    sink.tableCell_();
                }
            }

            if ( headersMap.get( PROPERTIES ) == Boolean.TRUE )
            {
                Properties props = member.getProperties();
                if ( props != null )
                {
                    tableCell( propertiesToString( props ) );
                }
                else
                {
                    tableCell( null );
                }
            }

            sink.tableRow_();
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredContrHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();
            String name = getI18nString( "contributors.name" );
            String email = getI18nString( "contributors.email" );
            String url = getI18nString( "contributors.url" );
            String organization = getI18nString( "contributors.organization" );
            String organizationUrl = getI18nString( "contributors.organizationurl" );
            String roles = getI18nString( "contributors.roles" );
            String timeZone = getI18nString( "contributors.timezone" );
            String actualTime = getI18nString( "contributors.actualtime" );
            String properties = getI18nString( "contributors.properties" );

            setRequiredArray( requiredHeaders, requiredArray, name, email, url, organization, organizationUrl, roles,
                              timeZone, actualTime, properties );

            return requiredArray.toArray( new String[requiredArray.size()] );
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredDevHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();

            String id = getI18nString( "developers.id" );
            String name = getI18nString( "developers.name" );
            String email = getI18nString( "developers.email" );
            String url = getI18nString( "developers.url" );
            String organization = getI18nString( "developers.organization" );
            String organizationUrl = getI18nString( "developers.organizationurl" );
            String roles = getI18nString( "developers.roles" );
            String timeZone = getI18nString( "developers.timezone" );
            String actualTime = getI18nString( "developers.actualtime" );
            String properties = getI18nString( "developers.properties" );

            if ( requiredHeaders.get( ID ) == Boolean.TRUE )
            {
                requiredArray.add( id );
            }

            setRequiredArray( requiredHeaders, requiredArray, name, email, url, organization, organizationUrl, roles,
                              timeZone, actualTime, properties );

            return requiredArray.toArray( new String[requiredArray.size()] );
        }

        /**
         * @param requiredHeaders
         * @param requiredArray
         * @param name
         * @param email
         * @param url
         * @param organization
         * @param organizationUrl
         * @param roles
         * @param timeZone
         * @param actualTime
         * @param properties
         */
        private void setRequiredArray( Map<String, Boolean> requiredHeaders, List<String> requiredArray, String name,
                                       String email, String url, String organization, String organizationUrl,
                                       String roles, String timeZone, String actualTime, String properties )
        {
            if ( requiredHeaders.get( NAME ) == Boolean.TRUE )
            {
                requiredArray.add( name );
            }
            if ( requiredHeaders.get( EMAIL ) == Boolean.TRUE )
            {
                requiredArray.add( email );
            }
            if ( requiredHeaders.get( URL ) == Boolean.TRUE )
            {
                requiredArray.add( url );
            }
            if ( requiredHeaders.get( ORGANIZATION ) == Boolean.TRUE )
            {
                requiredArray.add( organization );
            }
            if ( requiredHeaders.get( ORGANIZATION_URL ) == Boolean.TRUE )
            {
                requiredArray.add( organizationUrl );
            }
            if ( requiredHeaders.get( ROLES ) == Boolean.TRUE )
            {
                requiredArray.add( roles );
            }
            if ( requiredHeaders.get( TIME_ZONE ) == Boolean.TRUE )
            {
                requiredArray.add( timeZone );
                requiredArray.add( actualTime );
            }

            if ( requiredHeaders.get( PROPERTIES ) == Boolean.TRUE )
            {
                requiredArray.add( properties );
            }
        }

        /**
         * @param units contributors and developers to check
         * @return required headers
         */
        private Map<String, Boolean> checkRequiredHeaders( List<? extends Contributor> units )
        {
            Map<String, Boolean> requiredHeaders = new HashMap<String, Boolean>();

            requiredHeaders.put( ID, Boolean.FALSE );
            requiredHeaders.put( NAME, Boolean.FALSE );
            requiredHeaders.put( EMAIL, Boolean.FALSE );
            requiredHeaders.put( URL, Boolean.FALSE );
            requiredHeaders.put( ORGANIZATION, Boolean.FALSE );
            requiredHeaders.put( ORGANIZATION_URL, Boolean.FALSE );
            requiredHeaders.put( ROLES, Boolean.FALSE );
            requiredHeaders.put( TIME_ZONE, Boolean.FALSE );
            requiredHeaders.put( PROPERTIES, Boolean.FALSE );

            for ( Contributor unit : units )
            {
                if ( unit instanceof Developer )
                {
                    Developer developer = (Developer) unit;
                    if ( StringUtils.isNotEmpty( developer.getId() ) )
                    {
                        requiredHeaders.put( ID, Boolean.TRUE );
                    }
                }
                if ( StringUtils.isNotEmpty( unit.getName() ) )
                {
                    requiredHeaders.put( NAME, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( unit.getEmail() ) )
                {
                    requiredHeaders.put( EMAIL, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( unit.getUrl() ) )
                {
                    requiredHeaders.put( URL, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( unit.getOrganization() ) )
                {
                    requiredHeaders.put( ORGANIZATION, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( unit.getOrganizationUrl() ) )
                {
                    requiredHeaders.put( ORGANIZATION_URL, Boolean.TRUE );
                }
                if ( !isEmpty( unit.getRoles() ) )
                {
                    requiredHeaders.put( ROLES, Boolean.TRUE );
                }
                if ( StringUtils.isNotEmpty( unit.getTimezone() ) )
                {
                    requiredHeaders.put( TIME_ZONE, Boolean.TRUE );
                }
                Properties properties = unit.getProperties();
                if ( null != properties && !properties.isEmpty() )
                {
                    requiredHeaders.put( PROPERTIES, Boolean.TRUE );
                }
            }
            return requiredHeaders;
        }

        /**
         * Create a table cell with a link to the given url. The url is not validated.
         *
         * @param url
         */
        private void tableCellForUrl( String url )
        {
            sink.tableCell();

            if ( StringUtils.isEmpty( url ) )
            {
                text( url );
            }
            else
            {
                link( url, url );
            }

            sink.tableCell_();
        }

        private boolean isEmpty( List<?> list )
        {
            return ( list == null ) || list.isEmpty();
        }
    }
}
