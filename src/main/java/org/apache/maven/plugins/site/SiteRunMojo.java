package org.apache.maven.plugins.site;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.site.webapp.DoxiaBean;
import org.apache.maven.plugins.site.webapp.DoxiaFilter;
import org.apache.maven.reporting.exec.MavenReportExecution;
import org.codehaus.plexus.util.IOUtil;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Starts the site up, rendering documents as requested for faster editing.
 * It uses Jetty as the web server.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal run
 * @aggregator
 */
public class SiteRunMojo
    extends AbstractSiteRenderingMojo
{
    /**
     * Where to create the dummy web application.
     *
     * @parameter expression="${project.build.directory}/site-webapp"
     */
    private File tempWebappDirectory;

    /**
     * The port to execute the HTTP server on.
     *
     * @parameter expression="${port}" default-value="8080"
     */
    private int port;

    private static final int MAX_IDLE_TIME = 30000;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Server server = new Server();
        server.setStopAtShutdown( true );

        Connector defaultConnector = getDefaultConnector();
        server.setConnectors( new Connector[] { defaultConnector } );

        WebAppContext webapp = createWebApplication();
        webapp.setServer( server );

        DefaultHandler defaultHandler = new DefaultHandler();
        defaultHandler.setServer( server );

        Handler[] handlers = new Handler[2];
        handlers[0] = webapp;
        handlers[1] = defaultHandler;
        server.setHandlers( handlers );

        getLog().info( "Starting Jetty on http://localhost:" + port + "/" );
        try
        {
            server.start();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing Jetty: " + e.getMessage(), e );
        }

        // Watch it
        try
        {
            server.getThreadPool().join();
        }
        catch ( InterruptedException e )
        {
            getLog().warn( "Jetty was interrupted", e );
        }
    }

    private WebAppContext createWebApplication()
        throws MojoExecutionException
    {
        File webXml = new File( tempWebappDirectory, "WEB-INF/web.xml" );
        webXml.getParentFile().mkdirs();

        InputStream inStream = null;
        FileOutputStream outStream = null;
        try
        {
            inStream = getClass().getResourceAsStream( "/webapp/web.xml" );
            outStream = new FileOutputStream( webXml );
            IOUtil.copy( inStream, outStream );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to construct temporary webapp for running site", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to construct temporary webapp for running site", e );
        }
        finally
        {
            IOUtil.close( outStream );
            IOUtil.close( inStream );
        }

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath( "/" );
        webapp.setResourceBase( tempWebappDirectory.getAbsolutePath() );
        webapp.setAttribute( DoxiaFilter.SITE_RENDERER_KEY, siteRenderer );
        webapp.getInitParams().put( "org.mortbay.jetty.servlet.Default.useFileMappedBuffer", "false" );

        // For external reports
        project.getReporting().setOutputDirectory( tempWebappDirectory.getAbsolutePath() );
        for ( MavenReportExecution mavenReportExecution : getReports() )
        {
            mavenReportExecution.getMavenReport().setReportOutputDirectory( tempWebappDirectory );
        }

        List<MavenReportExecution> reports = getReports();

        List<Locale> localesList = siteTool.getAvailableLocales( locales );
        webapp.setAttribute( DoxiaFilter.LOCALES_LIST_KEY, localesList );

        // Default is first in the list
        Locale defaultLocale = localesList.get( 0 );
        Locale.setDefault( defaultLocale );

        try
        {
            Map<String, DoxiaBean> i18nDoxiaContexts = new HashMap<String, DoxiaBean>();

            for ( Locale locale : localesList )
            {
                SiteRenderingContext i18nContext = createSiteRenderingContext( locale );
                i18nContext.setInputEncoding( getInputEncoding() );
                i18nContext.setOutputEncoding( getOutputEncoding() );

                Map<String, DocumentRenderer> i18nDocuments = locateDocuments( i18nContext, reports, locale );
                DoxiaBean doxiaBean;
                if ( defaultLocale.equals( locale ) )
                {
                    doxiaBean = new DoxiaBean( i18nContext, i18nDocuments, generatedSiteDirectory );
                }
                else
                {
                    doxiaBean =
                        new DoxiaBean( i18nContext, i18nDocuments, new File( generatedSiteDirectory,
                                                                             locale.getLanguage() ) );
                }

                i18nDoxiaContexts.put( locale.getLanguage(), doxiaBean );
                if ( defaultLocale.equals( locale ) )
                {
                    i18nDoxiaContexts.put( "default", doxiaBean );
                }

                if ( defaultLocale.equals( locale ) )
                {
                    siteRenderer.copyResources( i18nContext, new File( siteDirectory, "resources" ),
                                                tempWebappDirectory );
                }
                else
                {
                    siteRenderer.copyResources( i18nContext, new File( siteDirectory, "resources" ),
                                                new File( tempWebappDirectory, locale.getLanguage() ) );
                }
            }

            webapp.setAttribute( DoxiaFilter.I18N_DOXIA_CONTEXTS_KEY, i18nDoxiaContexts );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to set up webapp", e );
        }
        return webapp;
    }

    private Connector getDefaultConnector()
    {
        Connector connector = new SelectChannelConnector();
        connector.setPort( port );
        connector.setMaxIdleTime( MAX_IDLE_TIME );
        return connector;
    }

    public void setTempWebappDirectory( File tempWebappDirectory )
    {
        this.tempWebappDirectory = tempWebappDirectory;
    }

    public void setPort( int port )
    {
        this.port = port;
    }
}
