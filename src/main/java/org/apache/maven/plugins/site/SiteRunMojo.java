package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.NotFoundHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Start the site up, rendering documents as requested for fast editing.
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

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Server server = new Server();
        server.setStopAtShutdown( true );

        Connector defaultConnector = getDefaultConnector();
        server.setConnectors( new Connector[]{defaultConnector} );

        WebAppContext webapp = createWebApplication();
        webapp.setServer( server );

        NotFoundHandler notFoundHandler = new NotFoundHandler();
        notFoundHandler.setServer( server );

        Handler[] handlers = new Handler[2];
        handlers[0] = webapp;
        handlers[1] = notFoundHandler;
        server.setHandlers( handlers );

        getLog().info( "Starting Jetty on http://localhost:8080/" );
        try
        {
            server.start();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing Jetty", e );
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
        webapp.setAttribute( "siteRenderer", siteRenderer );

        List filteredReports = filterReports( reports );

        List localesList = getAvailableLocales();

        // Default is first in the list
        Locale defaultLocale = (Locale) localesList.get( 0 );
        Locale.setDefault( defaultLocale );

        try
        {
            // TODO: better i18n handling
            Locale locale = Locale.getDefault();
            SiteRenderingContext context = createSiteRenderingContext( locale );
            webapp.setAttribute( "context", context );

            Map documents = locateDocuments( context, filteredReports, locale );
            webapp.setAttribute( "documents", documents );

            webapp.setAttribute( "generatedSiteDirectory", generatedSiteDirectory );

            siteRenderer.copyResources( context, new File( siteDirectory, "resources" ), tempWebappDirectory );
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
