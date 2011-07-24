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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.MojoLogWrapper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Renders a Maven report in a doxia site.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReportDocumentRenderer
    implements DocumentRenderer
{
    private final MavenReport report;

    private final RenderingContext renderingContext;

    private final String pluginInfo;

    private final Log log;

    public ReportDocumentRenderer( MavenReport report, RenderingContext renderingContext, Log log )
    {
        this.report = report;

        pluginInfo = getPluginInfo( report );

        this.renderingContext = renderingContext;

        this.log = log;
    }

    /**
     * Get plugin information from report's Manifest.
     * 
     * @param report the Maven report
     * @return plugin information as Specification Title followed by Specification Version if set in Manifest and
     *         supported by JVM
     */
    private String getPluginInfo( MavenReport report )
    {
        Package pkg = report.getClass().getPackage();

        if ( pkg != null )
        {
            String title = pkg.getSpecificationTitle();
            String version = pkg.getSpecificationVersion();
            
            if ( title == null )
            {
                return version;
            }
            else if ( version == null )
            {
                return title;
            }
            else
            {
                return title + ' ' + version;
            }
        }

        return null;
    }

    private static class MySink
        extends SiteRendererSink
    {
        private File outputDir;

        private String outputName;

        public MySink( File outputDir, String outputName, RenderingContext ctx )
        {
            super( ctx );
            this.outputName = outputName;
            this.outputDir = outputDir;
        }

        public String getOutputName()
        {
            return outputName;
        }

        public File getOutputDir()
        {
            return outputDir;
        }

    }

    private static class MySinkFactory
        implements SinkFactory
    {
        private RenderingContext context;

        private List<MySink> sinks = new ArrayList<MySink>();

        public MySinkFactory( RenderingContext ctx )
        {
            this.context = ctx;
        }

        public Sink createSink( File outputDir, String outputName )
        {
            MySink sink = new MySink( outputDir, outputName, context );
            sinks.add( sink );
            return sink;
        }

        public Sink createSink( File arg0, String arg1, String arg2 )
            throws IOException
        {
            // Not used
            return null;
        }

        public Sink createSink( OutputStream arg0 )
            throws IOException
        {
            // Not used
            return null;
        }

        public Sink createSink( OutputStream arg0, String arg1 )
            throws IOException
        {
            // Not used
            return null;
        }

        public List<MySink> sinks()
        {
            return sinks;
        }
    }

    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException
    {
        Locale locale = siteRenderingContext.getLocale();
        String localReportName = report.getName( locale );

        log.info( "Generating \"" + localReportName + "\" report"
                  + ( pluginInfo == null ? "." : ( "    --- " + pluginInfo ) ) );

        MySinkFactory sf = new MySinkFactory( renderingContext );

        SiteRendererSink sink = new SiteRendererSink( renderingContext );
        sink.enableLogging( new MojoLogWrapper( log ) );

        try
        {
            // try extended multi-page API
            if ( !generateMultiPage( locale, sf, sink ) )
            {
                // fallback to old single-page-only API
                try
                {
                    report.generate( sink, locale );
                }
                catch ( NoSuchMethodError e )
                {
                    throw new RendererException( "No method on " + report.getClass(), e );
                }
            }
        }
        catch ( MavenReportException e )
        {
            throw new RendererException( "Error rendering Maven report: " + e.getMessage(), e );
        }
        finally
        {
            sink.close();
        }

        if ( !report.isExternalReport() )
        {
            try
            {
                List<MySink> sinks = sf.sinks();

                log.debug( "Multipage report: " + sinks.size() + " subreports" );

                for ( MySink mySink : sinks )
                {
                    mySink.enableLogging( new MojoLogWrapper( log ) );

                    log.debug( "  Rendering " + mySink.getOutputName() );

                    File outputFile = new File( mySink.getOutputDir(), mySink.getOutputName() );

                    Writer out = null;
                    try
                    {
                        out = WriterFactory.newWriter( outputFile, siteRenderingContext.getOutputEncoding() );
                        renderer.generateDocument( out, mySink, siteRenderingContext );
                    }
                    finally
                    {
                        mySink.close();
                        IOUtil.close( out );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RendererException( "Cannot create writer", e );
            }

            renderer.generateDocument( writer, sink, siteRenderingContext );
        }
    }

    /**
     * Try to generate report with extended multi-page API.
     * 
     * @return <code>true</code> if the report was compatible with the extended API
     */
    private boolean generateMultiPage( Locale locale, SinkFactory sf, Sink sink )
        throws MavenReportException
    {
        try
        {
            // MavenMultiPageReport is not in Maven Core, then the class is different in site plugin and in each report
            // plugin: only reflection can let us invoke its method
            Method generate =
                report.getClass().getMethod( "generate", Sink.class, SinkFactory.class, Locale.class );

            generate.invoke( report, sink, sf, locale );

            return true;
        }
        catch ( SecurityException se )
        {
            return false;
        }
        catch ( NoSuchMethodException nsme )
        {
            return false;
        }
        catch ( IllegalArgumentException iae )
        {
            throw new MavenReportException( "error while invoking generate", iae );
        }
        catch ( IllegalAccessException iae )
        {
            throw new MavenReportException( "error while invoking generate", iae );
        }
        catch ( InvocationTargetException ite )
        {
            throw new MavenReportException( "error while invoking generate", ite );
        }
    }

    public String getOutputName()
    {
        return renderingContext.getOutputName();
    }

    public RenderingContext getRenderingContext()
    {
        return renderingContext;
    }

    public boolean isOverwrite()
    {
        // TODO: would be nice to query the report to see if it is modified
        return true;
    }

    /**
     * @return true if the current report is external, false otherwise
     */
    public boolean isExternalReport()
    {
        return report.isExternalReport();
    }
}
