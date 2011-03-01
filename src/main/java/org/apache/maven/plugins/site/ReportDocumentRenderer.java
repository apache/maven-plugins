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
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.reporting.exec.MavenReportExecution;
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

    private final ClassLoader classLoader;
    
    private final Log log;

    public ReportDocumentRenderer( MavenReportExecution mavenReportExecution, RenderingContext renderingContext, Log log )
    {
        this.report = mavenReportExecution.getMavenReport();

        this.renderingContext = renderingContext;

        this.pluginInfo =
            mavenReportExecution.getPlugin().getArtifactId() + ':' + mavenReportExecution.getPlugin().getVersion();

        this.classLoader = mavenReportExecution.getClassLoader();

        this.log = log;
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
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( classLoader );
        try
        {
            if ( report instanceof MavenMultiPageReport )
            {
                // extended multi-page API
                ( (MavenMultiPageReport) report ).generate( sink, sf, locale );
            }
            else
            {
                // old single-page-only API
                report.generate( sink, locale );
            }
        }
        catch ( MavenReportException e )
        {
            throw new RendererException( "Error rendering Maven report: " + e.getMessage(), e );
        }
        catch ( LinkageError e )
        {
            log.warn( "An issue has occurred with report " + report.getClass().getName() + ", skip LinkageError "
                          + e.getMessage() + ", please report an issue to Maven dev team.", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
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
