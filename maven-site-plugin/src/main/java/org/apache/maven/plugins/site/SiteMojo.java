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

import org.apache.maven.doxia.module.xdoc.XdocSiteModule;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
     * Alternative directory for xdoc source, useful for m1 to m2 migration
     *
     * @parameter default-value="${basedir}/xdocs"
     * @deprecated
     */
    private File xdocDirectory;

    /**
     * Directory containing generated documentation.
     *
     * @parameter alias="workingDirectory" expression="${project.build.directory}/generated-site"
     * @required
     */
    private File generatedSiteDirectory;

    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * @parameter expression="${reports}"
     * @required
     * @readonly
     */
    private List reports;

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
        // TODO: use in :run as well? better way to call it?
        setDefaultAttributes();

        List reports = filterReports( this.reports );

        Map categories = categorizeReports( reports );

        try
        {
            // TODO [IMPORTANT] map out renderers in advance, accounting for duplicates, to make site:run easier (eg index -> this report, project-info -> project info summary report, foo -> src/site/apt/foo.apt), then push to site renderer
            List localesList = initLocalesList();

            // Default is first in the list
            Locale defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                Locale locale = (Locale) iterator.next();

                // Sort projectInfos and projectReports
                Comparator reportComparator = new ReportComparator( locale );
                for ( Iterator i = categories.values().iterator(); i.hasNext(); )
                {
                    List reportSet = (List) i.next();
                    Collections.sort( reportSet, reportComparator );
                }

                File outputDirectory = getOutputDirectory( locale, defaultLocale );

                // Generate static site
                File siteDirectoryFile;
                File xdocDirectoryFile;
                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );

                    xdocDirectoryFile = new File( xdocDirectory, locale.getLanguage() );
                }
                else
                {
                    siteDirectoryFile = siteDirectory;
                    xdocDirectoryFile = xdocDirectory;
                }

                // Try to find duplicate files
                Map duplicate = new LinkedHashMap();
                String defaultExcludes = StringUtils.join( FileUtils.getDefaultExcludes(), "," );
                if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    for ( Iterator it = localesList.iterator(); it.hasNext(); )
                    {
                        Locale l = (Locale) it.next();
                        defaultExcludes += "," + l.getLanguage() + "/**";
                    }
                }

                if ( siteDirectoryFile.exists() )
                {
                    // TODO: avoid this hardcoding - the resources dir might be elsewhere. We should really test for duplicate targets, not guess at what source files will be html.
                    // add the site's 'resources' directory to the default exclude list
                    String actualExcludes = defaultExcludes + "," + "resources/**";

                    tryToFindDuplicates( siteDirectoryFile, actualExcludes, duplicate );
                }

                // Handle the GeneratedSite Directory
                if ( generatedSiteDirectory.exists() )
                {
                    tryToFindDuplicates( generatedSiteDirectory, defaultExcludes, duplicate );
                }

                // Exception if a file is duplicate
                checkDuplicates( duplicate, locale );

                DecorationModel decoration = getDecorationModel( categories, locale );

                SiteRenderingContext context = createSiteRenderingContext( locale, decoration, siteRenderer );

                //Generate reports
                List generatedReportsFileName = Collections.EMPTY_LIST;
                if ( reports != null )
                {
                    generatedReportsFileName = generateReportsPages( reports, outputDirectory, defaultLocale, context );
                }

                // Try to generate the index.html
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                // TODO: [IMPORTANT] Be good to generate a module's summary page thats referenced off the Modules menu item.

                // Log if a user override a report file
                for ( Iterator it = generatedReportsFileName.iterator(); it.hasNext(); )
                {
                    String reportFileName = (String) it.next();

                    if ( duplicate.get( reportFileName ) != null )
                    {
                        getLog().info( "Override the generated file \"" + reportFileName + "\" for the " +
                            displayLanguage + " version." );
                    }
                }

                siteRenderer.render( siteDirectoryFile, outputDirectory, context );

                // Check if ${basedir}/xdocs is existing
                if ( xdocDirectory.exists() )
                {
                    File[] fileNames = xdocDirectoryFile.listFiles();

                    if ( fileNames.length > 0 )
                    {
                        XdocSiteModule xdoc = new XdocSiteModule();

                        siteRenderer.render( xdocDirectoryFile, outputDirectory, xdoc.getSourceDirectory(),
                                             xdoc.getExtension(), xdoc.getParserId(), context, outputEncoding );
                    }
                }

                if ( generatedSiteDirectory.exists() )
                {
                    siteRenderer.render( generatedSiteDirectory, outputDirectory, context );
                }
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

    private DecorationModel getDecorationModel( Map categories, Locale locale )
        throws MojoExecutionException
    {
        Map props = new HashMap();

        // This is to support the deprecated ${reports} and ${modules} tags.
        String menus = "";
        for ( Iterator i = categories.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            menus += "<menu ref=\"" + key + "\"/>\n";
        }
        props.put( "reports", menus );
        props.put( "modules", "<menu ref=\"modules\"/>\n" );

        DecorationModel decorationModel = getDecorationModel( project, locale, props );
        populateReportsMenus( decorationModel, locale, categories );
        populateModules( decorationModel, locale );

        if ( project.getUrl() != null )
        {
            assembler.resolvePaths( decorationModel, project.getUrl() );
        }
        else
        {
            getLog().warn( "No URL defined for the project - decoration links will not be resolved" );
        }

        return decorationModel;
    }

    private void populateReportsMenus( DecorationModel decorationModel, Locale locale, Map categories )
    {
        for ( Iterator i = categories.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            Menu menu = decorationModel.getMenuRef( key );
            if ( menu != null )
            {
                List reports = (List) categories.get( key );

                if ( reports.size() > 0 )
                {
                    menu.setName( i18n.getString( "site-plugin", locale, "report.menu.projectdocumentation" ) );

                    // TODO: [IMPORTANT] this is a hack, change to a different class - Category instead of MavenReport
                    MavenReport summary = null;
                    for ( Iterator j = reports.iterator(); j.hasNext() && summary == null; )
                    {
                        MavenReport report = (MavenReport) j.next();
                        if ( "".equals( report.getDescription( locale ) ) )
                        {
                            summary = report;
                            try
                            {
                                Field f = summary.getClass().getDeclaredField( "reports" );
                                boolean acc = f.isAccessible();
                                f.setAccessible( true );
                                List newReports = new ArrayList( reports );
                                newReports.remove( summary );
                                f.set( summary, newReports );
                                f.setAccessible( acc );
                            }
                            catch ( NoSuchFieldException e )
                            {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                            catch ( IllegalAccessException e )
                            {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    }

                    String name;
                    String href = null;
                    if ( summary == null )
                    {
                        name = key;
                    }
                    else
                    {
                        name = summary.getName( locale );
                        href = summary.getOutputName() + ".html";
                    }

                    MenuItem item = new MenuItem();
                    item.setName( name );
                    if ( href != null )
                    {
                        item.setHref( href );
                        item.setCollapse( true );
                    }

                    for ( Iterator j = reports.iterator(); j.hasNext(); )
                    {
                        MavenReport report = (MavenReport) j.next();

                        MenuItem subitem = new MenuItem();
                        subitem.setName( report.getName( locale ) );
                        subitem.setHref( report.getOutputName() + ".html" );

                        item.addItem( subitem );
                    }
                }
                else
                {
                    decorationModel.removeMenuRef( key );
                }
            }
        }
    }

    private void populateModules( DecorationModel decorationModel, Locale locale )
        throws MojoExecutionException
    {
        Menu menu = decorationModel.getMenuRef( "modules" );

        if ( menu != null )
        {
            // we require child modules and reactors to process module menu
            if ( project.getModules().size() > 0 )
            {
                List projects = this.reactorProjects;

                menu.setName( i18n.getString( "site-plugin", locale, "report.menu.projectmodules" ) );

                if ( projects.size() == 1 )
                {
                    // Not running reactor - search for the projects manually
                    List models = new ArrayList( project.getModules().size() );
                    for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
                    {
                        String module = (String) i.next();
                        Model model;
                        File f = new File( project.getBasedir(), module + "/pom.xml" );
                        if ( f.exists() )
                        {
                            MavenXpp3Reader reader = new MavenXpp3Reader();
                            FileReader fileReader = null;
                            try
                            {
                                fileReader = new FileReader( f );
                                model = reader.read( fileReader );
                            }
                            catch ( IOException e )
                            {
                                throw new MojoExecutionException( "Unable to read POM", e );
                            }
                            catch ( XmlPullParserException e )
                            {
                                throw new MojoExecutionException( "Unable to read POM", e );
                            }
                            finally
                            {
                                IOUtil.close( fileReader );
                            }
                        }
                        else
                        {
                            model = new Model();
                            model.setName( module );
                            model.setUrl( module );
                        }
                        models.add( model );
                    }
                    populateModulesMenuItemsFromModels( models, menu );
                }
                else
                {
                    populateModulesMenuItemsFromReactorProjects( menu );
                }
            }
            else
            {
                decorationModel.removeMenuRef( "modules" );
            }
        }
    }

    private List filterReports( List reports )
    {
        List filteredReports = new ArrayList( reports.size() );
        if ( generateReports )
        {
            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                MavenReport report = (MavenReport) i.next();
                //noinspection ErrorNotRethrown,UnusedCatchParameter
                try
                {
                    if ( report.canGenerateReport() )
                    {
                        filteredReports.add( report );
                    }
                }
                catch ( AbstractMethodError e )
                {
                    // the canGenerateReport() has been added just before the 2.0 release and will cause all the reporting
                    // plugins with an earlier version to fail (most of the codehaus mojo now fails)
                    // be nice with them, output a warning and don't let them break anything

                    getLog().warn( "Error loading report " + report.getClass().getName() +
                        " - AbstractMethodError: canGenerateReport()" );
                    filteredReports.add( report );
                }
            }
        }
        return filteredReports;
    }

    /**
     * Categorize reports by category name.
     *
     * @param reports list of reports
     * @return the categorised reports
     */
    private Map categorizeReports( List reports )
    {
        Map categories = new HashMap();

        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            List category = (List) categories.get( report.getCategoryName() );

            if ( category == null )
            {
                category = new ArrayList();
                categories.put( report.getCategoryName(), category );
            }
            category.add( report );
        }
        return categories;
    }

    private void populateModulesMenuItemsFromReactorProjects( Menu menu )
    {
        if ( reactorProjects != null && reactorProjects.size() > 1 )
        {
            Iterator reactorItr = reactorProjects.iterator();

            while ( reactorItr.hasNext() )
            {
                MavenProject reactorProject = (MavenProject) reactorItr.next();

                if ( reactorProject != null && reactorProject.getParent() != null &&
                    project.getArtifactId().equals( reactorProject.getParent().getArtifactId() ) )
                {
                    String reactorUrl = reactorProject.getUrl();
                    String name = reactorProject.getName();

                    appendMenuItem( menu, name, reactorUrl );
                }
            }
        }
    }

    private void populateModulesMenuItemsFromModels( List models, Menu menu )
    {
        if ( models != null && models.size() > 1 )
        {
            Iterator reactorItr = models.iterator();

            while ( reactorItr.hasNext() )
            {
                Model model = (Model) reactorItr.next();

                String reactorUrl = model.getUrl();
                String name = model.getName();

                appendMenuItem( menu, name, reactorUrl );
            }
        }
    }

    private static void appendMenuItem( Menu menu, String name, String href )
    {
        if ( href != null )
        {
            MenuItem item = new MenuItem();
            item.setName( name );
            if ( href.endsWith( "/" ) )
            {
                item.setHref( href + "index.html" );
            }
            else
            {
                item.setHref( href + "/index.html" );
            }
            menu.addItem( item );
        }
    }

    // Generate specific pages

    /**
     * Generate reports pages
     *
     * @param reports
     * @param localeOutputDirectory
     */
    private List generateReportsPages( List reports, File localeOutputDirectory, Locale defaultLocale,
                                       SiteRenderingContext context )
        throws RendererException, IOException, MavenReportException
    {
        List generatedReportsFileName = new ArrayList();

        for ( Iterator j = reports.iterator(); j.hasNext(); )
        {
            MavenReport report = (MavenReport) j.next();

            Locale locale = context.getLocale();
            getLog().info( "Generate \"" + report.getName( locale ) + "\" report." );

            report.setReportOutputDirectory( localeOutputDirectory );

            String reportFileName = report.getOutputName();

            if ( locale.equals( defaultLocale ) )
            {
                generatedReportsFileName.add( reportFileName );
            }
            else
            {
                generatedReportsFileName.add( locale.getLanguage() + File.separator + reportFileName );
            }

            String outputFileName = reportFileName + ".html";

            SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName );

            report.generate( sink, locale );

            if ( !report.isExternalReport() )
            {
                File outputFile = new File( localeOutputDirectory, outputFileName );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                // TODO: outputDirectory should be in rendering context
                siteRenderer.generateDocument(
                    new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ), sink, context );
            }
        }
        return generatedReportsFileName;
    }

    private File getOutputDirectory( Locale locale, Locale defaultLocale )
    {
        File file;
        if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
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

    /**
     * Convenience method that try to find duplicate files in sub-directories of a given directory.
     * <p>The scan is case sensitive.</p>
     *
     * @param directory       the directory to scan
     * @param defaultExcludes files patterns to be exclude from the search
     * @param duplicate       the map to update
     * @throws IOException if any
     */
    private static void tryToFindDuplicates( File directory, String defaultExcludes, Map duplicate )
        throws IOException
    {
        List siteFiles = FileUtils.getFileNames( directory, null, defaultExcludes, false );
        for ( Iterator it = siteFiles.iterator(); it.hasNext(); )
        {
            String currentFile = (String) it.next();

            int endIndex = currentFile.lastIndexOf( "." );
            if ( currentFile.lastIndexOf( File.separator ) == -1 )
            {
                // ignore files directly in the directory
            }
            else if ( endIndex == -1 || currentFile.startsWith( "." ) )
            {
                // ignore files without extension
            }
            else
            {

                int beginIndex = currentFile.indexOf( File.separator ) + 1;
                String key = currentFile.substring( beginIndex, endIndex ).toLowerCase( Locale.getDefault() );

                List tmp = (List) duplicate.get( key );
                if ( tmp == null )
                {
                    tmp = new ArrayList();
                    duplicate.put( key, tmp );
                }
                tmp.add( currentFile );
            }
        }
    }

    /**
     * Throw an exception if a file is duplicate.
     *
     * @param duplicate a map of duplicate files
     * @param locale    the current locale
     * @todo [IMPORTANT] move to site renderer
     */
    private void checkDuplicates( Map duplicate, Locale locale )
        throws MojoFailureException
    {
        if ( duplicate.size() > 0 )
        {
            StringBuffer sb = null;

            for ( Iterator it = duplicate.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                Collection values = (Collection) entry.getValue();

                if ( values.size() > 1 )
                {
                    if ( sb == null )
                    {
                        sb = new StringBuffer(
                            "Some files are duplicates in the site directory or in the generated-site directory. " );
                        sb.append( "\n" );
                        sb.append( "Review the following files for the \"" );
                        sb.append( locale.getDisplayLanguage( Locale.ENGLISH ) );
                        sb.append( "\" version:" );
                    }

                    sb.append( "\n" );
                    sb.append( entry.getKey() );
                    sb.append( "\n" );

                    for ( Iterator it2 = values.iterator(); it2.hasNext(); )
                    {
                        String current = (String) it2.next();

                        sb.append( "\t" );
                        sb.append( current );

                        if ( it2.hasNext() )
                        {
                            sb.append( "\n" );
                        }
                    }
                }
            }

            if ( sb != null )
            {
                throw new MojoFailureException( sb.toString() );
            }
        }
    }

}
