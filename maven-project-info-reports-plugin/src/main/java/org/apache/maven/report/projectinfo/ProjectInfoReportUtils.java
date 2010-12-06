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

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.validator.UrlValidator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utilities methods.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
public class ProjectInfoReportUtils
{
    private static final UrlValidator URL_VALIDATOR = new UrlValidator( new String[] { "http", "https" } );

    /** The timeout when getting the url input stream */
    private static final int TIMEOUT = 1000 * 5;

    /**
     * Get the input stream using ISO-8859-1 as charset from an URL.
     *
     * @param url not null
     * @param settings not null to handle proxy settings
     * @return the ISO-8859-1 inputstream found.
     * @throws IOException if any
     * @see #getContent(URL, Settings, String)
     */
    public static String getContent( URL url, Settings settings )
        throws IOException
    {
        return getContent( url, settings, "ISO-8859-1" );
    }

    /**
     * Get the input stream from an URL.
     *
     * @param url not null
     * @param settings not null to handle proxy settings
     * @param encoding the wanted encoding for the inputstream. If null, encoding will be "ISO-8859-1".
     * @return the inputstream found depending the wanted encoding.
     * @throws IOException if any
     */
    public static String getContent( URL url, Settings settings, String encoding )
        throws IOException
    {
        return getContent( url, null, settings, encoding );
    }

    /**
     * Get the input stream from an URL.
     *
     * @param url not null
     * @param project could be null
     * @param settings not null to handle proxy settings
     * @param encoding the wanted encoding for the inputstream. If empty, encoding will be "ISO-8859-1".
     * @return the inputstream found depending the wanted encoding.
     * @throws IOException if any
     * @since 2.3
     */
    public static String getContent( URL url, MavenProject project, Settings settings, String encoding )
        throws IOException
    {
        String scheme = url.getProtocol();

        if ( StringUtils.isEmpty( encoding ) )
        {
            encoding = "ISO-8859-1";
        }

        if ( "file".equals( scheme ) )
        {
            InputStream in = null;
            try
            {
                URLConnection conn = url.openConnection();
                in = conn.getInputStream();

                return IOUtil.toString( in, encoding );
            }
            finally
            {
                IOUtil.close( in );
            }
        }

        Proxy proxy = settings.getActiveProxy();
        if ( proxy != null )
        {
            if ( "http".equals( scheme ) || "https".equals( scheme ) || "ftp".equals( scheme ) )
            {
                scheme += ".";
            }
            else
            {
                scheme = "";
            }

            String host = proxy.getHost();
            if ( !StringUtils.isEmpty( host ) )
            {
                Properties p = System.getProperties();
                p.setProperty( scheme + "proxySet", "true" );
                p.setProperty( scheme + "proxyHost", host );
                p.setProperty( scheme + "proxyPort", String.valueOf( proxy.getPort() ) );
                if ( !StringUtils.isEmpty( proxy.getNonProxyHosts() ) )
                {
                    p.setProperty( scheme + "nonProxyHosts", proxy.getNonProxyHosts() );
                }

                final String userName = proxy.getUsername();
                if ( !StringUtils.isEmpty( userName ) )
                {
                    final String pwd = StringUtils.isEmpty( proxy.getPassword() ) ? "" : proxy.getPassword();
                    Authenticator.setDefault( new Authenticator()
                    {
                        /** {@inheritDoc} */
                        protected PasswordAuthentication getPasswordAuthentication()
                        {
                            return new PasswordAuthentication( userName, pwd.toCharArray() );
                        }
                    } );
                }
            }
        }

        InputStream in = null;
        try
        {
            URLConnection conn = getURLConnection( url, project, settings );
            in = conn.getInputStream();

            return IOUtil.toString( in, encoding );
        }
        finally
        {
            IOUtil.close( in );
        }
    }

    /**
     * @param factory not null
     * @param artifact not null
     * @param mavenProjectBuilder not null
     * @param remoteRepositories not null
     * @param localRepository not null
     * @return the artifact url or null if an error occurred.
     */
    public static String getArtifactUrl( ArtifactFactory factory, Artifact artifact,
                                         MavenProjectBuilder mavenProjectBuilder,
                                         List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
    {
        if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            return null;
        }

        Artifact copyArtifact = ArtifactUtils.copyArtifact( artifact );
        if ( !"pom".equals( copyArtifact.getType() ) )
        {
            copyArtifact =
                factory.createProjectArtifact( copyArtifact.getGroupId(), copyArtifact.getArtifactId(),
                                               copyArtifact.getVersion(), copyArtifact.getScope() );
        }
        try
        {
            MavenProject pluginProject =
                mavenProjectBuilder.buildFromRepository( copyArtifact,
                                                         remoteRepositories == null ? Collections.EMPTY_LIST
                                                                         : remoteRepositories, localRepository );

            if ( isArtifactUrlValid( pluginProject.getUrl() ) )
            {
                return pluginProject.getUrl();
            }

            return null;
        }
        catch ( ProjectBuildingException e )
        {
            return null;
        }
    }

    /**
     * @param artifactId not null
     * @param link could be null
     * @return the artifactId cell with or without a link pattern
     * @see AbstractMavenReportRenderer#linkPatternedText(String)
     */
    public static String getArtifactIdCell( String artifactId, String link )
    {
        if ( StringUtils.isEmpty( link ) )
        {
            return artifactId;
        }

        return "{" + artifactId + "," + link + "}";
    }

    /**
     * @param url not null
     * @return <code>true</code> if the url is valid, <code>false</code> otherwise.
     */
    public static boolean isArtifactUrlValid( String url )
    {
        if ( StringUtils.isEmpty( url ) )
        {
            return false;
        }

        return URL_VALIDATOR.isValid( url );
    }

    /**
     * @param url not null
     * @param project not null
     * @param settings not null
     * @return the url connection with auth if required. Don't check the certificate if SSL scheme.
     * @throws IOException if any
     */
    private static URLConnection getURLConnection( URL url, MavenProject project, Settings settings )
        throws IOException
    {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout( TIMEOUT );
        conn.setReadTimeout( TIMEOUT );

        // conn authorization
        if ( settings.getServers() != null
            && !settings.getServers().isEmpty()
            && project != null
            && project.getDistributionManagement() != null
            && ( project.getDistributionManagement().getRepository() != null || project.getDistributionManagement().getSnapshotRepository() != null )
            && ( StringUtils.isNotEmpty( project.getDistributionManagement().getRepository().getUrl() ) || StringUtils.isNotEmpty( project.getDistributionManagement().getSnapshotRepository().getUrl() ) ) )
        {
            Server server = null;
            if ( url.toString().contains( project.getDistributionManagement().getRepository().getUrl() ) )
            {
                server = settings.getServer( project.getDistributionManagement().getRepository().getId() );
            }
            if ( server == null
                && url.toString().contains( project.getDistributionManagement().getSnapshotRepository().getUrl() ) )
            {
                server = settings.getServer( project.getDistributionManagement().getSnapshotRepository().getId() );
            }

            if ( server != null && StringUtils.isNotEmpty( server.getUsername() )
                && StringUtils.isNotEmpty( server.getPassword() ) )
            {
                String up = server.getUsername().trim() + ":" + server.getPassword().trim();
                String upEncoded = new String( Base64.encodeBase64Chunked( up.getBytes() ) ).trim();

                conn.setRequestProperty( "Authorization", "Basic " + upEncoded );
            }
        }

        if ( conn instanceof HttpsURLConnection )
        {
            HostnameVerifier hostnameverifier = new HostnameVerifier()
            {
                /** {@inheritDoc} */
                public boolean verify( String urlHostName, SSLSession session )
                {
                    return true;
                }
            };
            ( (HttpsURLConnection) conn ).setHostnameVerifier( hostnameverifier );

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
            {
                /** {@inheritDoc} */
                public void checkClientTrusted( final X509Certificate[] chain, final String authType )
                {
                }

                /** {@inheritDoc} */
                public void checkServerTrusted( final X509Certificate[] chain, final String authType )
                {
                }

                /** {@inheritDoc} */
                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
            } };

            try
            {
                SSLContext sslContext = SSLContext.getInstance( "SSL" );
                sslContext.init( null, trustAllCerts, new SecureRandom() );

                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                ( (HttpsURLConnection) conn ).setSSLSocketFactory( sslSocketFactory );
            }
            catch ( NoSuchAlgorithmException e1 )
            {
                // ignore
            }
            catch ( KeyManagementException e )
            {
                // ignore
            }
        }

        return conn;
    }

    public static boolean isNumber( String str )
    {
        if ( str.startsWith( "+" ) )
        {
            str = str.substring( 1 );
        }
        return NumberUtils.isNumber( str );
    }

    public static int toInt( String str, int defaultValue )
    {
        if ( str.startsWith( "+" ) )
        {
            str = str.substring( 1 );
        }
        return NumberUtils.toInt( str, defaultValue );
    }
}
