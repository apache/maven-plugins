package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates the project site.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal site
 * @requiresDependencyResolution test
 */
public class SiteMojo
    extends AbstractSiteRenderingMojo
{

    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * Convenience parameter that allows you to disable report generation.
     *
     * @parameter expression="${generateReports}" default-value="true"
     */
    private boolean generateReports;

    /**
     * Generate the project site
     * <p/>
     * throws MojoExecutionException if any
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        List filteredReports;
        if ( generateReports )
        {
            filteredReports = filterReports( reports );
        }
        else
        {
            filteredReports = Collections.EMPTY_LIST;
        }

        try
        {
            List localesList = getAvailableLocales();

            // Default is first in the list
            Locale defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                Locale locale = (Locale) iterator.next();

                renderLocale( locale, filteredReports );
            }
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "Error during report generation", e );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "Error during page generation", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }

    private void renderLocale( Locale locale, List reports )
        throws IOException, RendererException, MavenReportException, MojoFailureException, MojoExecutionException
    {
        File outputDirectory = getOutputDirectory( locale );

        SiteRenderingContext context = createSiteRenderingContext( locale );

        Map documents = siteRenderer.locateDocumentFiles( context );

        Map reportsByOutputName = new HashMap();
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            reportsByOutputName.put( report.getOutputName(), report );

            String outputName = report.getOutputName() + ".html";
            if ( documents.containsKey( outputName ) )
            {
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                getLog().info( "Skipped \"" + report.getName( locale ) + "\" report, file \"" + outputName +
                    "\" already exists for the " + displayLanguage + " version." );
                i.remove();
            }
        }

        // TODO: I want to get rid of categories eventually. There's no way to add your own in a fully i18n manner
        Map categories = categoriseReports( reportsByOutputName.values() );

        populateReportsMenu( context.getDecoration(), locale, categories );
        populateReportItems( context.getDecoration(), locale, reportsByOutputName );

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_INFORMATION ) )
        {
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );
            writeSummaryPage( categoryReports, locale, "project-info.html", context, outputDirectory,
                              i18n.getString( "site-plugin", locale, "report.information.title" ),
                              i18n.getString( "site-plugin", locale, "report.information.description1" ),
                              i18n.getString( "site-plugin", locale, "report.information.description2" ) );
        }

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_REPORTS ) )
        {
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
            writeSummaryPage( categoryReports, locale, "project-reports.html", context, outputDirectory,
                              i18n.getString( "site-plugin", locale, "report.project.title" ),
                              i18n.getString( "site-plugin", locale, "report.project.description1" ),
                              i18n.getString( "site-plugin", locale, "report.project.description2" ) );
        }

        renderDocuments( documents, reportsByOutputName.values(), outputDirectory, locale, context );
    }

    private void renderDocuments( Map documents, Collection reports, File outputDirectory, Locale locale,
                                  SiteRenderingContext context )
        throws RendererException, IOException, MavenReportException
    {
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            String outputName = report.getOutputName() + ".html";

            String localReportName = report.getName( locale );
            getLog().info( "Generate \"" + localReportName + "\" report." );

            report.setReportOutputDirectory( outputDirectory );

            SiteRendererSink sink = siteRenderer.createSink( new RenderingContext( siteDirectory, outputName ) );

            report.generate( sink, locale );

            if ( !report.isExternalReport() )
            {
                File outputFile = new File( outputDirectory, outputName );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                siteRenderer.generateDocument(
                    new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ), sink, context );
            }
        }

        siteRenderer.render( documents.values(), context, outputDirectory, outputEncoding );
    }

    private void writeSummaryPage( List categoryReports, Locale locale, String outputName, SiteRenderingContext context,
                                   File outputDirectory, String title, String desc1, String desc2 )
        throws RendererException, IOException
    {
        SiteRendererSink sink = siteRenderer.createSink( new RenderingContext( siteDirectory, outputName ) );

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

        File outputFile = new File( outputDirectory, outputName );

        if ( !outputFile.getParentFile().exists() )
        {
            outputFile.getParentFile().mkdirs();
        }

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       sink, context );
    }

    private void populateReportsMenu( DecorationModel decorationModel, Locale locale, Map categories )
    {
        Menu menu = decorationModel.getMenuRef( "reports" );

        if ( menu != null )
        {
            if ( menu.getName() == null )
            {
                menu.setName( i18n.getString( "site-plugin", locale, "report.menu.projectdocumentation" ) );
            }

            boolean found = false;
            if ( menu.getItems().isEmpty() )
            {
                List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );
                if ( !categoryReports.isEmpty() )
                {
                    MenuItem item = createCategoryMenu(
                        i18n.getString( "site-plugin", locale, "report.menu.projectinformation" ), "/project-info.html",
                        categoryReports, locale );
                    menu.getItems().add( item );
                    found = true;
                }

                categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
                if ( !categoryReports.isEmpty() )
                {
                    MenuItem item = createCategoryMenu(
                        i18n.getString( "site-plugin", locale, "report.menu.projectreports" ), "/project-reports.html",
                        categoryReports, locale );
                    menu.getItems().add( item );
                    found = true;
                }
            }
            if ( !found )
            {
                decorationModel.removeMenuRef( "reports" );
            }
        }
    }

    private MenuItem createCategoryMenu( String name, String href, List categoryReports, Locale locale )
    {
        MenuItem item = new MenuItem();
        item.setName( name );
        item.setCollapse( true );
        item.setHref( href );

        Collections.sort( categoryReports, new ReportComparator( locale ) );

        for ( Iterator k = categoryReports.iterator(); k.hasNext(); )
        {
            MavenReport report = (MavenReport) k.next();

            MenuItem subitem = new MenuItem();
            subitem.setName( report.getName( locale ) );
            subitem.setHref( report.getOutputName() + ".html" );
            item.getItems().add( subitem );
        }

        return item;
    }

    private Map categoriseReports( Collection reports )
    {
        Map categories = new HashMap();
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();
            List categoryReports = (List) categories.get( report.getCategoryName() );
            if ( categoryReports == null )
            {
                categoryReports = new ArrayList();
                categories.put( report.getCategoryName(), categoryReports );
            }
            categoryReports.add( report );
        }
        return categories;
    }

    private void populateReportItems( DecorationModel decorationModel, Locale locale, Map reportsByOutputName )
    {
        for ( Iterator i = decorationModel.getMenus().iterator(); i.hasNext(); )
        {
            Menu menu = (Menu) i.next();

            populateItemRefs( menu.getItems(), locale, reportsByOutputName );
        }
    }

    private void populateItemRefs( List items, Locale locale, Map reportsByOutputName )
    {
        for ( Iterator i = items.iterator(); i.hasNext(); )
        {
            MenuItem item = (MenuItem) i.next();

            if ( item.getRef() != null )
            {
                if ( reportsByOutputName.containsKey( item.getRef() ) )
                {
                    MavenReport report = (MavenReport) reportsByOutputName.get( item.getRef() );

                    if ( item.getName() == null )
                    {
                        item.setName( report.getName( locale ) );
                    }

                    if ( item.getHref() == null || item.getHref().length() == 0 )
                    {
                        item.setHref( report.getOutputName() + ".html" );
                    }
                }
                else
                {
                    getLog().warn( "Unrecognised reference: '" + item.getRef() + "'" );
                    i.remove();
                }
            }
            populateItemRefs( item.getItems(), locale, reportsByOutputName );
        }
    }

    private File getOutputDirectory( Locale locale )
    {
        File file;
        if ( locale.getLanguage().equals( Locale.getDefault().getLanguage() ) )
        {
            file = outputDirectory;
        }
        else
        {
            file = new File( outputDirectory, locale.getLanguage() );
        }

        // Safety
        if ( !file.exists() )
        {
            file.mkdirs();
        }

        return file;
    }

}
