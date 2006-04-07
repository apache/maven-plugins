package org.apache.maven.plugins.site;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.Locale;

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

    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException
    {
        Locale locale = siteRenderingContext.getLocale();
        String localReportName = report.getName( locale );
        log.info( "Generate \"" + localReportName + "\" report." );

        SiteRendererSink sink = new SiteRendererSink( renderingContext );

        try
        {
            report.generate( sink, locale );
        }
        catch ( MavenReportException e )
        {
            throw new RendererException( "Error rendering Maven report: " + e.getMessage(), e );
        }

        if ( !report.isExternalReport() )
        {
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
}
