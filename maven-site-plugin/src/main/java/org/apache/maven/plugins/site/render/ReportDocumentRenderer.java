package org.apache.maven.plugins.site.render;

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

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

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
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.MojoLogWrapper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.reporting.exec.MavenReportExecution;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Renders a Maven report in a Doxia site.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReportDocumentRenderer
    implements DocumentRenderer
{
    private final MavenReport report;

    private final RenderingContext renderingContext;

    private final String reportMojoInfo;

    private final ClassLoader classLoader;
    
    private final Log log;

    public ReportDocumentRenderer( MavenReportExecution mavenReportExecution, RenderingContext renderingContext,
                                   Log log )
    {
        this.report = mavenReportExecution.getMavenReport();

        this.renderingContext = renderingContext;

        if ( mavenReportExecution.getPlugin() == null )
        {
            // Maven 2: report has been prepared in Maven Core, MavenReportExecution contains only the report
            this.reportMojoInfo = getPluginInfo( report );
        }
        else
        {
            // Maven 3: full MavenReportExecution prepared by maven-reporting-impl
            this.reportMojoInfo =
                mavenReportExecution.getPlugin().getArtifactId() + ':' + mavenReportExecution.getPlugin().getVersion()
                    + ':' + mavenReportExecution.getGoal();
        }

        this.classLoader = mavenReportExecution.getClassLoader();

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

    private static class MultiPageSubSink
        extends SiteRendererSink
    {
        private File outputDir;

        private String outputName;

        public MultiPageSubSink( File outputDir, String outputName, RenderingContext ctx )
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

    private static class MultiPageSinkFactory
        implements SinkFactory
    {
        private RenderingContext context;

        private List<MultiPageSubSink> sinks = new ArrayList<MultiPageSubSink>();

        public MultiPageSinkFactory( RenderingContext ctx )
        {
            this.context = ctx;
        }

        public Sink createSink( File outputDir, String outputName )
        {
            MultiPageSubSink sink = new MultiPageSubSink( outputDir, outputName, context );
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

        public List<MultiPageSubSink> sinks()
        {
            return sinks;
        }
    }

    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException
    {
        Locale locale = siteRenderingContext.getLocale();
        String localReportName = report.getName( locale );

        String msg = "Generating \"" + buffer().strong( localReportName ) + "\" report";
        // CHECKSTYLE_OFF: MagicNumber
        log.info( reportMojoInfo == null ? ( msg + '.' )
                        : ( StringUtils.rightPad( msg, 40 ) + buffer().strong( " --- " ).mojo( reportMojoInfo ) ) );
        // CHECKSTYLE_ON: MagicNumber

        // main sink
        SiteRendererSink mainSink = new SiteRendererSink( renderingContext );
        // sink factory, for multi-page reports that need sub-sinks
        MultiPageSinkFactory multiPageSinkFactory = new MultiPageSinkFactory( renderingContext );

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            if ( classLoader != null )
            {
                Thread.currentThread().setContextClassLoader( classLoader );
            }

            if ( report instanceof MavenMultiPageReport )
            {
                // extended multi-page API
                ( (MavenMultiPageReport) report ).generate( mainSink, multiPageSinkFactory, locale );
            }
            else if ( generateMultiPage( locale, multiPageSinkFactory, mainSink ) )
            {
                // extended multi-page API for Maven 2.2, only accessible by reflection API
            }
            else
            {
                // old single-page-only API
                report.generate( mainSink, locale );
            }
        }
        catch ( MavenReportException e )
        {
            String report = ( reportMojoInfo == null ) ? ( '"' + localReportName + "\" report" ) : reportMojoInfo;
            throw new RendererException( "Error generating " + report + ": " + e.getMessage(), e );
        }
        catch ( LinkageError e )
        {
            String report = ( reportMojoInfo == null ) ? ( '"' + localReportName + "\" report" ) : reportMojoInfo;
            log.warn( "An issue has occurred with " + report + ", skipping LinkageError "
                          + e.getMessage() + ", please report an issue to Maven dev team.", e );
        }
        finally
        {
            if ( classLoader != null )
            {
                Thread.currentThread().setContextClassLoader( originalClassLoader );
            }
            mainSink.close();
        }

        if ( report.isExternalReport() )
        {
            // external reports are rendered from their own: no Doxia site rendering needed
            return;
        }

        // render main sink
        renderer.generateDocument( writer, mainSink, siteRenderingContext );

        // render sub-sinks, eventually created by multi-page reports
        try
        {
            List<MultiPageSubSink> sinks = multiPageSinkFactory.sinks();

            log.debug( "Multipage report: " + sinks.size() + " subreports" );

            for ( MultiPageSubSink mySink : sinks )
            {
                mySink.enableLogging( new MojoLogWrapper( log ) );

                log.debug( "  Rendering " + mySink.getOutputName() );

                File outputFile = new File( mySink.getOutputDir(), mySink.getOutputName() );

                Writer out = null;
                try
                {
                    out = WriterFactory.newWriter( outputFile, siteRenderingContext.getOutputEncoding() );
                    renderer.generateDocument( out, mySink, siteRenderingContext );
                    mySink.close();
                    mySink = null;
                    out.close();
                    out = null;
                }
                finally
                {
                    IOUtil.close( out );

                    if ( mySink != null )
                    {
                        mySink.close();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RendererException( "Cannot create writer", e );
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
