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
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.i18n.I18N;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Renders a Maven report.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CategorySummaryDocumentRenderer
    implements DocumentRenderer
{
    private RenderingContext renderingContext;

    private String title;

    private String desc1;

    private String desc2;

    private I18N i18n;

    private List categoryReports;

    public CategorySummaryDocumentRenderer( RenderingContext renderingContext, String title, String desc1, String desc2,
                                            I18N i18n, List categoryReports )
    {
        this.renderingContext = renderingContext;
        this.title = title;
        this.desc1 = desc1;
        this.desc2 = desc2;
        this.i18n = i18n;
        this.categoryReports = Collections.unmodifiableList( categoryReports );
    }

    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException
    {
        SiteRendererSink sink = new SiteRendererSink( renderingContext );

        sink.head();

        sink.title();

        sink.text( title );

        sink.title_();

        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( title );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( desc1 + " " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( " " + desc2 );
        sink.paragraph_();

        sink.section2();
        sink.sectionTitle2();
        Locale locale = siteRenderingContext.getLocale();
        sink.text( i18n.getString( "site-plugin", locale, "report.category.sectionTitle" ) );
        sink.sectionTitle2_();

        sink.table();

        String name = i18n.getString( "site-plugin", locale, "report.category.column.document" );
        String description = i18n.getString( "site-plugin", locale, "report.category.column.description" );

        sink.tableRow();

        sink.tableHeaderCell();

        sink.text( name );

        sink.tableHeaderCell_();

        sink.tableHeaderCell();

        sink.text( description );

        sink.tableHeaderCell_();

        sink.tableRow_();

        if ( categoryReports != null )
        {
            for ( Iterator i1 = categoryReports.iterator(); i1.hasNext(); )
            {
                MavenReport report = (MavenReport) i1.next();

                sink.tableRow();
                sink.tableCell();
                sink.link( report.getOutputName() + ".html" );
                sink.text( report.getName( locale ) );
                sink.link_();
                sink.tableCell_();
                sink.tableCell();
                sink.text( report.getDescription( locale ) );
                sink.tableCell_();
                sink.tableRow_();
            }
        }

        sink.table_();

        sink.section2_();

        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();

        renderer.generateDocument( writer, sink, siteRenderingContext );
    }

    public String getOutputName()
    {
        return renderingContext.getOutputName();
    }
}
