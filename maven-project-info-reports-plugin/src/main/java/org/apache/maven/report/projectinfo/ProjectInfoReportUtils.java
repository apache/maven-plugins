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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.validator.UrlValidator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
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

    /**
     * Get the input stream using ISO-8859-1 as charset from an URL.
     *
     * @param url not null
     * @param settings not null to handle proxy settings
     * @return the ISO-8859-1 inputstream found.
     * @throws IOException if any
     * @see #getInputStream(URL, Settings, String)
     */
    public static String getInputStream( URL url, Settings settings )
        throws IOException
    {
        return getInputStream( url, settings, "ISO-8859-1" );
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
    public static String getInputStream( URL url, Settings settings, String encoding )
        throws IOException
    {
        String scheme = url.getProtocol();
        if ( !"file".equals( scheme ) )
        {
            Proxy proxy = settings.getActiveProxy();
            if ( proxy != null )
            {
                if ( "http".equals( scheme ) || "https".equals( scheme ) )
                {
                    scheme = "http.";
                }
                else if ( "ftp".equals( scheme ) )
                {
                    scheme = "ftp.";
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
        }

        InputStream in = null;
        try
        {
            in = url.openStream();

            if ( encoding == null )
            {
                return IOUtil.toString( in, "ISO-8859-1" );
            }
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
            copyArtifact = factory.createProjectArtifact( copyArtifact.getGroupId(), copyArtifact.getArtifactId(),
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
}

