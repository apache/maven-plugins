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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Iterator;

/**
 * Renders a Maven report.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReportDocumentRenderer
    implements DocumentRenderer
{
    private MavenReport report;

    private RenderingContext renderingContext;

    private Log log;

    public ReportDocumentRenderer( MavenReport report, RenderingContext renderingContext, Log log )
    {
        this.report = report;

        this.renderingContext = renderingContext;

        this.log = log;
    }

    private static class MySink extends SiteRendererSink
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

    private static class MySinkFactory implements SinkFactory
    {
        private RenderingContext context;

        private List sinks = new ArrayList();

        public MySinkFactory( RenderingContext ctx )
        {
            this.context = ctx;
        }

        public Sink createSink( File outputDir, String outputName )
        {
            SiteRendererSink sink = new MySink( outputDir, outputName, context );
            sinks.add( sink );
            return sink;
        }

        public List sinks()
        {
            return sinks;
        }
    }


    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException
    {
        Locale locale = siteRenderingContext.getLocale();
        String localReportName = report.getName( locale );
        log.info( "Generating \"" + localReportName + "\" report." );

        MySinkFactory sf = new MySinkFactory( renderingContext );

        SiteRendererSink sink = new SiteRendererSink( renderingContext );

        try
        {
            if ( report instanceof MavenMultiPageReport )
            {
                ( (MavenMultiPageReport) report ).generate( sink, sf, locale );
            }
            else
            {
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

        if ( !report.isExternalReport() )
        {
            try
            {
                List sinks = sf.sinks();

                log.debug( "Multipage report: " + sinks.size() + " subreports");

                for ( Iterator it = sinks.iterator(); it.hasNext(); )
                {
                    MySink mySink = (MySink) it.next();

                    log.debug( "  Rendering " +  mySink.getOutputName() );

                    Writer out = new FileWriter( new File( mySink.getOutputDir(), mySink.getOutputName() ) );

                    renderer.generateDocument( out, mySink, siteRenderingContext );
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
