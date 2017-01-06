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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Team report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "project-team" )
public class TeamReport
    extends AbstractProjectInfoReport
{
    /**
     * Shows avatar images for team members that have a) properties/picUrl set b) An avatar at gravatar.com for their
     * email address
     * <p/>
     * Future versions of this plugin may choose to implement different strategies for resolving avatar images, possibly
     * using different providers.
     *<p>
     *<strong>Note</strong>: This property will be renamed to {@code tteam.showAvatarImages} in 3.0.
     * @since 2.6
     */
    @Parameter( property = "teamlist.showAvatarImages", defaultValue = "true" )
    private boolean showAvatarImages;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = !isEmpty( getProject().getModel().getDevelopers() )
                    || !isEmpty( getProject().getModel().getContributors() );
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        // CHECKSTYLE_OFF: LineLength
        ProjectTeamRenderer r =
            new ProjectTeamRenderer( getSink(), project.getModel(), getI18N( locale ), locale, getLog(), showAvatarImages );
        // CHECKSTYLE_ON: LineLength

        r.render();
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "team-list";
    }

    @Override
    protected String getI18Nsection()
    {
        return "team";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class ProjectTeamRenderer
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

        private static final String IMAGE = "image";

        private static final String ID = "id";

        private final Model model;

        private final Log log;

        private final boolean showAvatarImages;

        private final String protocol;

        ProjectTeamRenderer( Sink sink, Model model, I18N i18n, Locale locale, Log log, boolean showAvatarImages )
        {
            super( sink, i18n, locale );

            this.model = model;
            this.log = log;
            this.showAvatarImages = showAvatarImages;

            // prepare protocol for gravatar
            if ( model.getUrl() != null && model.getUrl().startsWith( "https://" ) )
            {
                this.protocol = "https";
        }
            else
            {
                this.protocol = "http";
            }
        }

        @Override
        protected String getI18Nsection()
        {
            return "team";
        }

        @Override
        public void renderBody()
        {
            startSection( getI18nString( "intro.title" ) );

            // To handle JS
            StringBuilder javascript = new StringBuilder();

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

            endSection();

            endSection();
        }

        private void renderTeamMember( Contributor member, int rowId, Map<String, Boolean> headersMap,
                                       StringBuilder javascript )
        {
            sink.tableRow();

            if ( headersMap.get( IMAGE ) == Boolean.TRUE && showAvatarImages )
            {
                Properties properties = member.getProperties();
                String picUrl = properties.getProperty( "picUrl" );
                if ( StringUtils.isEmpty( picUrl ) )
                {
                    picUrl = getGravatarUrl( member.getEmail() );
                }
                if ( StringUtils.isEmpty( picUrl ) )
                {
                    picUrl = getSpacerGravatarUrl();
                }
                sink.tableCell();
                sink.figure();
                sink.figureGraphics( picUrl );
                sink.figure_();
                sink.tableCell_();
            }
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
                    List<String> var = member.getRoles();
                    tableCell( StringUtils.join( var.toArray( new String[var.size()] ), ", " ) );
                }
                else
                {
                    tableCell( null );
                }
            }
            if ( headersMap.get( TIME_ZONE ) == Boolean.TRUE )
            {
                tableCell( member.getTimezone() );
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

        private static final String AVATAR_SIZE = "s=60";

        private String getSpacerGravatarUrl()
        {
            return protocol + "://www.gravatar.com/avatar/00000000000000000000000000000000?d=blank&f=y&" + AVATAR_SIZE;
        }

        private String getGravatarUrl( String email )
        {
            if ( email == null )
            {
                return null;
            }
            email = StringUtils.trim( email );
            email = email.toLowerCase();
            MessageDigest md;
            try
            {
                md = MessageDigest.getInstance( "MD5" );
                md.update( email.getBytes() );
                byte byteData[] = md.digest();
                StringBuilder sb = new StringBuilder();
                final int lowerEightBitsOnly = 0xff;
                for ( byte aByteData : byteData )
                {
                    sb.append( Integer.toString( ( aByteData & lowerEightBitsOnly ) + 0x100, 16 ).substring( 1 ) );
                }
                return protocol + "://www.gravatar.com/avatar/" + sb.toString() + "?d=mm&" + AVATAR_SIZE;
            }
            catch ( NoSuchAlgorithmException e )
            {
                return null;
            }
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredContrHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();
            String image = getI18nString( "contributors.image" );
            String name = getI18nString( "contributors.name" );
            String email = getI18nString( "contributors.email" );
            String url = getI18nString( "contributors.url" );
            String organization = getI18nString( "contributors.organization" );
            String organizationUrl = getI18nString( "contributors.organizationurl" );
            String roles = getI18nString( "contributors.roles" );
            String timeZone = getI18nString( "contributors.timezone" );
            String actualTime = getI18nString( "contributors.actualtime" );
            String properties = getI18nString( "contributors.properties" );
            if ( requiredHeaders.get( IMAGE ) == Boolean.TRUE && showAvatarImages )
            {
                requiredArray.add( image );
            }
            setRequiredArray( requiredHeaders, requiredArray, image, name, email, url, organization, organizationUrl,
                              roles, timeZone, actualTime, properties );

            return requiredArray.toArray( new String[requiredArray.size()] );
        }

        /**
         * @param requiredHeaders
         * @return
         */
        private String[] getRequiredDevHeaderArray( Map<String, Boolean> requiredHeaders )
        {
            List<String> requiredArray = new ArrayList<String>();

            String image = getI18nString( "developers.image" );
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

            if ( requiredHeaders.get( IMAGE ) == Boolean.TRUE && showAvatarImages )
            {
                requiredArray.add( image );
            }
            if ( requiredHeaders.get( ID ) == Boolean.TRUE )
            {
                requiredArray.add( id );
            }

            setRequiredArray( requiredHeaders, requiredArray, image, name, email, url, organization, organizationUrl,
                              roles, timeZone, actualTime, properties );

            return requiredArray.toArray( new String[requiredArray.size()] );
        }

        /**
         * @param requiredHeaders
         * @param requiredArray
         * @param image
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
        private void setRequiredArray( Map<String, Boolean> requiredHeaders, List<String> requiredArray, String image,
                                       String name, String email, String url, String organization,
                                       String organizationUrl, String roles, String timeZone, String actualTime,
                                       String properties )
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

            requiredHeaders.put( IMAGE, Boolean.FALSE );
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
                    requiredHeaders.put( IMAGE, Boolean.TRUE );
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
                boolean hasPicUrl = properties.containsKey( "picUrl" );
                if ( hasPicUrl )
                {
                    requiredHeaders.put( IMAGE, Boolean.TRUE );
                }
                boolean isJustAnImageProperty = properties.size() == 1 && hasPicUrl;
                if ( !isJustAnImageProperty && !properties.isEmpty() )
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
