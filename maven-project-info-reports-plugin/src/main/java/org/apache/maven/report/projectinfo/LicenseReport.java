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

import org.apache.commons.validator.UrlValidator;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the Project License report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal license
 */
public class LicenseReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Settings.
     *
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Whether the system is currently offline.
     *
     * @parameter expression="${settings.offline}"
     */
    private boolean offline;

    /**
     * Whether the only render links to the license documents instead of inlining them.
     * <br/>
     * If the system is in {@link #offline} mode, the linkOnly parameter will be always <code>true</code>.
     *
     * @parameter default-value="false"
     * @since 2.3
     */
    private boolean linkOnly;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        LicenseRenderer r = new LicenseRenderer( getSink(), getProject(), getI18N( locale ), locale, settings, linkOnly );

        r.render();
    }

    @Override
    public boolean canGenerateReport()
    {
        if ( !offline )
        {
            return true;
        }

        for ( License license : project.getModel().getLicenses() )
        {
            String url = license.getUrl();

            URL licenseUrl = null;
            try
            {
                licenseUrl = getLicenseURL( project, url );
            }
            catch ( MalformedURLException e )
            {
                getLog().error( e.getMessage() );
            }
            catch ( IOException e )
            {
                getLog().error( e.getMessage() );
            }

            if ( licenseUrl != null && licenseUrl.getProtocol().equals( "file" ) )
            {
                return true;
            }

            if ( licenseUrl != null
                && ( licenseUrl.getProtocol().equals( "http" ) || licenseUrl.getProtocol().equals( "https" ) ) )
            {
                linkOnly = true;
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "license";
    }

    @Override
    protected String getI18Nsection()
    {
        return "license";
    }

    /**
     * @param project not null
     * @param url not null
     * @return a valid URL object from the url string
     * @throws IOException if any
     */
    protected static URL getLicenseURL( MavenProject project, String url )
        throws IOException
    {
        URL licenseUrl = null;
        UrlValidator urlValidator = new UrlValidator( UrlValidator.ALLOW_ALL_SCHEMES );
        // UrlValidator does not accept file URLs because the file
        // URLs do not contain a valid authority (no hostname).
        // As a workaround accept license URLs that start with the
        // file scheme.
        if ( urlValidator.isValid( url ) || StringUtils.defaultString( url ).startsWith( "file://" ) )
        {
            try
            {
                licenseUrl = new URL( url );
            }
            catch ( MalformedURLException e )
            {
                throw new MalformedURLException( "The license url '" + url + "' seems to be invalid: "
                    + e.getMessage() );
            }
        }
        else
        {
            File licenseFile = new File( project.getBasedir(), url );
            if ( !licenseFile.exists() )
            {
                // Workaround to allow absolute path names while
                // staying compatible with the way it was...
                licenseFile = new File( url );
            }
            if ( !licenseFile.exists() )
            {
                throw new IOException( "Maven can't find the file '" + licenseFile + "' on the system." );
            }
            try
            {
                licenseUrl = licenseFile.toURI().toURL();
            }
            catch ( MalformedURLException e )
            {
                throw new MalformedURLException( "The license url '" + url + "' seems to be invalid: "
                    + e.getMessage() );
            }
        }

        return licenseUrl;
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class LicenseRenderer
        extends AbstractProjectInfoRenderer
    {
        private final MavenProject project;

        private final Settings settings;

        private final boolean linkOnly;

        LicenseRenderer( Sink sink, MavenProject project, I18N i18n, Locale locale, Settings settings, boolean linkOnly )
        {
            super( sink, i18n, locale );

            this.project = project;

            this.settings = settings;

            this.linkOnly = linkOnly;
        }

        @Override
        protected String getI18Nsection()
        {
            return "license";
        }

        @Override
        public void renderBody()
        {
            List<License> licenses = project.getModel().getLicenses();

            if ( licenses.isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nolicense" ) );

                endSection();

                return;
            }

            // Overview
            startSection( getI18nString( "overview.title" ) );

            paragraph( getI18nString( "overview.intro" ) );

            endSection();

            // License
            startSection( getI18nString( "title" ) );

            for ( License license : licenses )
            {
                String name = license.getName();
                String url = license.getUrl();
                String comments = license.getComments();

                startSection( name );

                if ( !StringUtils.isEmpty( comments ) )
                {
                    paragraph( comments );
                }

                if ( url != null )
                {
                    URL licenseUrl = null;
                    try
                    {
                        licenseUrl = getLicenseURL( project, url );
                    }
                    catch ( MalformedURLException e )
                    {
                        // I18N message
                        paragraph( e.getMessage() );
                    }
                    catch ( IOException e )
                    {
                        // I18N message
                        paragraph( e.getMessage() );
                    }

                    if ( licenseUrl != null && !linkOnly)
                    {
                        String licenseContent = null;
                        try
                        {
                            // All licenses are supposed in English...
                            licenseContent = ProjectInfoReportUtils.getInputStream( licenseUrl, settings );
                        }
                        catch ( IOException e )
                        {
                            paragraph( "Can't read the url [" + licenseUrl + "] : " + e.getMessage() );
                        }

                        if ( licenseContent != null )
                        {
                            // TODO: we should check for a text/html mime type instead, and possibly use a html parser to do this a bit more cleanly/reliably.
                            String licenseContentLC = licenseContent.toLowerCase( Locale.ENGLISH );
                            int bodyStart = licenseContentLC.indexOf( "<body" );
                            int bodyEnd = licenseContentLC.indexOf( "</body>" );
                            if ( ( licenseContentLC.startsWith( "<!doctype html" )
                                || licenseContentLC.startsWith( "<html>" ) ) && bodyStart >= 0 && bodyEnd >= 0 )
                            {
                                bodyStart = licenseContentLC.indexOf( ">", bodyStart ) + 1;
                                String body = licenseContent.substring( bodyStart, bodyEnd );

                                link( licenseUrl.toExternalForm(), "[Original text]" );
                                paragraph( "Copy of the license follows." );

                                body = replaceRelativeLinks( body, baseURL( licenseUrl ).toExternalForm() );
                                sink.rawText( body );
                            }
                            else
                            {
                                verbatimText( licenseContent );
                            }
                        }
                    }
                    else if ( licenseUrl != null && linkOnly )
                    {
                        link( licenseUrl.toExternalForm(), licenseUrl.toExternalForm() );
                    }

                }

                endSection();
            }

            endSection();
        }

        private static URL baseURL( URL aUrl )
        {
            String urlTxt = aUrl.toExternalForm();
            int lastSlash = urlTxt.lastIndexOf( '/' );
            if ( lastSlash > -1 )
            {
                try
                {
                    return new URL( urlTxt.substring( 0, lastSlash + 1 ) );
                }
                catch ( MalformedURLException e )
                {
                    throw new AssertionError( e );
                }
            }

            return aUrl;
        }

        private static String replaceRelativeLinks( String html, String baseURL )
        {
            String url = baseURL;
            if ( !url.endsWith( "/" ) )
            {
                url += "/";
            }

            String serverURL = url.substring( 0, url.indexOf( '/', url.indexOf( "//" ) + 2 ) );

            String content = replaceParts( html, url, serverURL, "[aA]", "[hH][rR][eE][fF]" );
            content = replaceParts( content, url, serverURL, "[iI][mM][gG]", "[sS][rR][cC]" );
            return content;
        }

        private static String replaceParts( String html, String baseURL, String serverURL, String tagPattern,
                                            String attributePattern )
        {
            Pattern anchor = Pattern.compile(
                "(<\\s*" + tagPattern + "\\s+[^>]*" + attributePattern + "\\s*=\\s*\")([^\"]*)\"([^>]*>)" );
            StringBuffer sb = new StringBuffer( html );

            int indx = 0;
            boolean done = false;
            while ( !done )
            {
                Matcher mAnchor = anchor.matcher( sb );
                if ( mAnchor.find( indx ) )
                {
                    indx = mAnchor.end( 3 );

                    if ( mAnchor.group( 2 ).startsWith( "#" ) )
                    {
                        // relative link - don't want to alter this one!
                    }
                    if ( mAnchor.group( 2 ).startsWith( "/" ) )
                    {
                        // root link
                        sb.insert( mAnchor.start( 2 ), serverURL );
                        indx += serverURL.length();
                    }
                    else if ( mAnchor.group( 2 ).indexOf( ':' ) < 0 )
                    {
                        // relative link
                        sb.insert( mAnchor.start( 2 ), baseURL );
                        indx += baseURL.length();
                    }
                }
                else
                {
                    done = true;
                }
            }
            return sb.toString();
        }
    }
}
