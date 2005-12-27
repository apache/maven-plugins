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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.module.xdoc.XdocSiteModule;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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
    extends AbstractSiteMojo
{
    private static final String RESOURCE_DIR = "org/apache/maven/plugins/site";

    private static final String DEFAULT_TEMPLATE = RESOURCE_DIR + "/maven-site.vm";

    /**
     * Alternative directory for xdoc source, useful for m1 to m2 migration
     *
     * @parameter expression="${basedir}/xdocs"
     * @required
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
     * Directory which contains the resources for the site.
     *
     * @parameter expression="${basedir}/src/site/resources"
     * @required
     */
    private File resourcesDirectory;

    /**
     * Directory containing the template page.
     *
     * @parameter expression="${templateDirectory}"
     */
    private File templateDirectory;

    /**
     * Default template page.
     *
     * @parameter expression="${template}"
     */
    private String template = DEFAULT_TEMPLATE;

    /**
     * @parameter expression="${attributes}"
     */
    private Map attributes;

    /**
     * Specifies the output encoding.
     *
     * @parameter expression="${outputEncoding}"
     * default-value="ISO-8859-1"
     */
    private String outputEncoding;

    /**
     * Site Renderer
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

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
     * The component for assembling inheritance.
     *
     * @component
     */
    private DecorationModelInheritanceAssembler assembler;

    /**
     * The component that is used to resolve additional artifacts required.
     *
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}
     */
    private ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @todo this is used for site descriptor resolution - it should relate to the actual project but for some reason they are not always filled in
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List repositories;

    /**
     * The component used for creating artifact instances.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

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
        ClassLoader templateClassLoader;

        if ( templateDirectory == null )
        {
            templateClassLoader = SiteMojo.class.getClassLoader();
        }
        else
        {
            try
            {
                if ( !templateDirectory.exists() )
                {
                    throw new MojoExecutionException(
                        "This templateDirectory=[" + templateDirectory + "] doesn't exist." );
                }

                templateClassLoader = new URLClassLoader( new URL[]{templateDirectory.toURL()} );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( templateDirectory + " isn't a valid URL.", e );
            }
        }

        if ( attributes == null )
        {
            attributes = new HashMap();
        }

        if ( attributes.get( "project" ) == null )
        {
            attributes.put( "project", project );
        }

        if ( attributes.get( "outputEncoding" ) == null )
        {
            attributes.put( "outputEncoding", outputEncoding );
        }

        List reports = filterReports( this.reports );

        Map categories = categorizeReports( reports );
        List projectInfos = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );
        List projectReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );

        if ( projectInfos == null )
        {
            projectInfos = Collections.EMPTY_LIST;
        }

        if ( projectReports == null )
        {
            projectReports = Collections.EMPTY_LIST;
        }

        try
        {
            List localesList = initLocalesList();

            // Default is first in the list
            Locale defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            // Sort projectInfos and projectReports with the default locale setted
            // TODO Beautify the output by sorting with each current locale
            Comparator reportComparator = new ReportComparator();
            Collections.sort( projectInfos, reportComparator );
            Collections.sort( projectReports, reportComparator );

            for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                Locale locale = (Locale) iterator.next();

                File outputDirectory = getOutputDirectory( locale, defaultLocale );

                // Safety
                if ( !outputDirectory.exists() )
                {
                    outputDirectory.mkdirs();
                }

                // Generate static site
                File siteDirectoryFile = siteDirectory;
                File xdocDirectoryFile = xdocDirectory;
                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );

                    xdocDirectoryFile = new File( xdocDirectory, locale.getLanguage() );
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

                DecorationModel decoration = getDecorationModel( reports, locale, projectInfos, projectReports );

                SiteRenderingContext context = new SiteRenderingContext();
                context.setTemplate( template );
                context.setTemplateProperties( attributes );
                context.setLocale( locale );
                context.setTemplateClassLoader( templateClassLoader );
                context.setDecoration( decoration );

                //Generate reports
                List generatedReportsFileName = Collections.EMPTY_LIST;
                if ( reports != null )
                {
                    generatedReportsFileName = generateReportsPages( reports, outputDirectory, defaultLocale, context );
                }

                //Generate overview pages
                if ( projectInfos.size() > 0 )
                {
                    generateProjectInfoPage( projectInfos, outputDirectory, context );
                }

                if ( projectReports.size() > 0 )
                {
                    generateProjectReportsPage( projectReports, outputDirectory, context );
                }

                // Try to generate the index.html
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );
                if ( duplicate.get( "index" ) != null )
                {
                    getLog().info( "Ignoring the index file generation for the " + displayLanguage + " version." );
                }
                else
                {
                    getLog().info( "Generate an index file for the " + displayLanguage + " version." );
                    generateIndexPage( outputDirectory, context );
                }

                // TODO: Be good to generate a module's summary page thats referenced off the
                // Modules menu item.

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

                copyResources( outputDirectory );

                // Copy site resources
                if ( resourcesDirectory != null && resourcesDirectory.exists() )
                {
                    copyDirectory( resourcesDirectory, outputDirectory );
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

    private DecorationModel getDecorationModel( List reports, Locale locale, List projectInfos, List projectReports )
        throws MojoExecutionException
    {
        Map props = new HashMap();

        // TODO: can we replace these with an XML tag?
        if ( reports != null )
        {
            props.put( "reports", getReportsMenu( locale, projectInfos, projectReports ) );
        }
        else
        {
            props.put( "reports", "" );
        }

        // we require child modules and reactors to process module menu

        if ( reactorProjects.size() > 1 && project.getModules().size() > 0 )
        {
            props.put( "modules", getModulesMenu( locale ) );
        }
        else
        {
            props.put( "modules", "" );
        }

        return getDecorationModel( project, locale, props );
    }

    private DecorationModel getDecorationModel( MavenProject project, Locale locale, Map origProps )
        throws MojoExecutionException
    {
        Map props = new HashMap( origProps );

        // TODO: we should use a workspace API that would know if it was in the repository already or not
        File siteDescriptor = getSiteDescriptorFile( project.getBasedir(), locale );

        String siteDescriptorContent;

        try
        {
            if ( !siteDescriptor.exists() )
            {
                // try the repository
                siteDescriptor = getSiteDescriptorFromRepository( project, locale );
            }

            if ( siteDescriptor != null && siteDescriptor.exists() )
            {
                siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
            }
            else
            {
                siteDescriptorContent = IOUtil.toString( getClass().getResourceAsStream( "/default-site.xml" ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException(
                "The site descriptor cannot be resolved from the repository: " + e.getMessage(), e );
        }

        props.put( "outputEncoding", outputEncoding );

        // TODO: interpolate ${project.*} in general

        if ( project.getName() != null )
        {
            props.put( "project.name", project.getName() );
        }
        else
        {
            props.put( "project.name", "NO_PROJECT_NAME_SET" );
        }

        if ( project.getUrl() != null )
        {
            props.put( "project.url", project.getUrl() );
        }
        else
        {
            props.put( "project.url", "NO_PROJECT_URL_SET" );
        }

        siteDescriptorContent = StringUtils.interpolate( siteDescriptorContent, props );

        DecorationModel decoration;
        try
        {
            decoration = new DecorationXpp3Reader().read( new StringReader( siteDescriptorContent ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing site descriptor", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading site descriptor", e );
        }

        MavenProject parentProject = project.getParent();
        if ( ( parentProject != null ) && ( project.getUrl() != null ) && ( parentProject.getUrl() != null ) )
        {
            props.put( "parentProject", getProjectParentMenu( locale ) );

            DecorationModel parent = getDecorationModel( parentProject, locale, props );

            assembler.assembleModelInheritance( decoration, parent, project.getUrl(), parentProject.getUrl() );
        }
        else
        {
            props.put( "parentProject", "" );
        }

        return decoration;
    }

    private File getSiteDescriptorFromRepository( MavenProject project, Locale locale )
        throws ArtifactResolutionException
    {
        File result = null;

        try
        {
            result = resolveSiteDescriptor( project, locale );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().debug( "Unable to locate site descriptor: " + e );
        }

        return result;
    }

    private File resolveSiteDescriptor( MavenProject project, Locale locale )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        File result;

        try
        {
            // TODO: this is a bit crude - proper type, or proper handling as metadata rather than an artifact in 2.1?
            Artifact artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(),
                                                                              project.getArtifactId(),
                                                                              project.getVersion(), "xml",
                                                                              "site_" + locale.getLanguage() );
            artifactResolver.resolve( artifact, repositories, localRepository );

            result = artifact.getFile();
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().debug( "Unable to locate site descriptor: " + e );

            Artifact artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(),
                                                                              project.getArtifactId(),
                                                                              project.getVersion(), "xml", "site" );
            artifactResolver.resolve( artifact, repositories, localRepository );

            result = artifact.getFile();
        }
        return result;
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

    /**
     * Retrieve the reports menu
     *
     * @param locale         the locale used
     * @param projectInfos   list of project infos
     * @param projectReports list of project reports
     * @return a XML for reports menu
     */
    private String getReportsMenu( Locale locale, List projectInfos, List projectReports )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "<menu name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.projectdocumentation" ) );
        buffer.append( "\">\n" );
        buffer.append( "    <item name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.about" ) );
        buffer.append( " " );
        buffer.append( project.getName() );
        buffer.append( "\" href=\"/index.html\"/>\n" );

        writeReportSubMenu( projectInfos, buffer, locale, "report.menu.projectinformation", "project-info.html" );
        writeReportSubMenu( projectReports, buffer, locale, "report.menu.projectreports", "maven-reports.html" );

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    /**
     * Create a report sub menu
     *
     * @param reports       list of reports specified in pom
     * @param buffer        string to be appended
     * @param locale        the locale used
     * @param key
     * @param indexFilename index page filename
     */
    private void writeReportSubMenu( List reports, StringBuffer buffer, Locale locale, String key,
                                     String indexFilename )
    {
        if ( reports.size() > 0 )
        {
            buffer.append( "    <item name=\"" );
            buffer.append( i18n.getString( "site-plugin", locale, key ) );
            buffer.append( "\" href=\"/" );
            buffer.append( indexFilename );
            buffer.append( "\" collapse=\"true\">\n" );

            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                MavenReport report = (MavenReport) i.next();

                buffer.append( "        <item name=\"" );
                buffer.append( report.getName( locale ) );
                buffer.append( "\" href=\"/" );
                buffer.append( report.getOutputName() );
                buffer.append( ".html\"/>\n" );
            }

            buffer.append( "    </item>\n" );
        }
    }

    /**
     * Generate a menu for modules
     *
     * @param locale the locale wanted
     * @return a XML menu for modules
     */
    private String getModulesMenu( Locale locale )
    {

        StringBuffer buffer = new StringBuffer();

        buffer.append( "<menu name=\"" );
        buffer.append( i18n.getString( "site-plugin", locale, "report.menu.projectmodules" ) );
        buffer.append( "\">\n" );

        if ( reactorProjects != null && reactorProjects.size() > 1 )
        {
            Iterator reactorItr = reactorProjects.iterator();

            while ( reactorItr.hasNext() )
            {
                MavenProject reactorProject = (MavenProject) reactorItr.next();

                // dont't use modules as they address file system locations and we need projects
                //
                // Note, we could try and parse the module's pom based upon its directory location
                // which would remove our reliance on reactorProjects but its more complicated.
                // The side effect of using reactorProjects is that to generate module links
                // you must do a recursive build (no mvn -N)

                if ( reactorProject != null && reactorProject.getParent() != null &&
                    project.getArtifactId().equals( reactorProject.getParent().getArtifactId() ) )
                {
                    String reactorUrl = reactorProject.getUrl();

                    if ( reactorUrl != null )
                    {
                        buffer.append( "    <item name=\"" );
                        buffer.append( reactorProject.getName() );
                        buffer.append( "\" href=\"" );
                        buffer.append( reactorUrl );
                        if ( reactorUrl.endsWith( "/" ) )
                        {
                            buffer.append( "index.html\"/>\n" );
                        }
                        else
                        {
                            buffer.append( "/index.html\"/>\n" );
                        }
                    }
                }
            }
        }

        buffer.append( "</menu>\n" );

        return buffer.toString();
    }

    /**
     * Generate a menu for the parent project
     *
     * @param locale the locale wanted
     * @return a XML menu for the parent project
     */
    private String getProjectParentMenu( Locale locale )
    {
        StringBuffer buffer = new StringBuffer();

        String parentUrl = project.getParent().getUrl();
        if ( parentUrl != null )
        {
            if ( parentUrl.endsWith( "/" ) )
            {
                parentUrl += "index.html";
            }
            else
            {
                parentUrl += "/index.html";
            }

            buffer.append( "<menu name=\"" );
            buffer.append( i18n.getString( "site-plugin", locale, "report.menu.parentproject" ) );
            buffer.append( "\">\n" );

            buffer.append( "    <item name=\"" );
            buffer.append( project.getParent().getName() );
            buffer.append( "\" href=\"" );
            buffer.append( parentUrl );
            buffer.append( "\"/>\n" );

            buffer.append( "</menu>\n" );

        }

        return buffer.toString();
    }

    /**
     * Generate an index page.
     *
     * @param outputDirectory
     */
    private void generateIndexPage( File outputDirectory, SiteRenderingContext context )
        throws RendererException, IOException
    {
        String outputFileName = "index.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName );

        Locale locale = context.getLocale();
        String title = i18n.getString( "site-plugin", locale, "report.index.title" ).trim() + " " + project.getName();

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
        if ( project.getDescription() != null )
        {
            // TODO How to handle i18n?
            sink.text( project.getDescription() );
        }
        else
        {
            sink.text( i18n.getString( "site-plugin", locale, "report.index.nodescription" ) );
        }
        sink.paragraph_();

        sink.body_();

        sink.flush();

        sink.close();

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       sink, context );
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

                siteRenderer.generateDocument(
                    new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ), sink, context );
            }
        }
        return generatedReportsFileName;
    }

    /**
     * Generates Project Info Page
     *
     * @param projectInfos    list of projectInfos
     * @param outputDirectory directory that will contain the generated project info page
     */
    private void generateProjectInfoPage( List projectInfos, File outputDirectory, SiteRenderingContext context )
        throws RendererException, IOException
    {
        String outputFileName = "project-info.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName );

        Locale locale = context.getLocale();
        String title = i18n.getString( "site-plugin", locale, "report.information.title" );

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
        sink.text( i18n.getString( "site-plugin", locale, "report.information.description1" ) + " " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( " " + i18n.getString( "site-plugin", locale, "report.information.description2" ) );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.sectionTitle" ) );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.column.document" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.information.column.description" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectInfos.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

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

        sink.table_();

        sink.section2_();

        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       sink, context );
    }

    /**
     * Generates the Project Report Pages
     *
     * @param projectReports  list of project reports
     * @param outputDirectory directory that will contain the generated project report pages
     */
    private void generateProjectReportsPage( List projectReports, File outputDirectory, SiteRenderingContext context )
        throws RendererException, IOException
    {
        String outputFileName = "maven-reports.html";

        SiteRendererSink sink = siteRenderer.createSink( siteDirectory, outputFileName );

        Locale locale = context.getLocale();
        String title = i18n.getString( "site-plugin", locale, "report.project.title" );

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
        sink.text( i18n.getString( "site-plugin", locale, "report.project.description1" ) + " " );
        sink.link( "http://maven.apache.org" );
        sink.text( "Maven" );
        sink.link_();
        sink.text( ". " + i18n.getString( "site-plugin", locale, "report.project.description2" ) );
        sink.paragraph_();

        sink.section2();

        sink.sectionTitle2();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.sectionTitle" ) );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.column.document" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "site-plugin", locale, "report.project.column.description" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator i = projectReports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

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

        sink.table_();

        sink.section2_();

        sink.section1_();

        sink.body_();

        File outputFile = new File( outputDirectory, outputFileName );

        siteRenderer.generateDocument( new OutputStreamWriter( new FileOutputStream( outputFile ), outputEncoding ),
                                       sink, context );
    }

    /**
     * Copy Resources
     *
     * @param outputDir the output directory
     * @throws IOException if any
     * @todo move to skin functionality in site renderer
     */
    private void copyResources( File outputDir )
        throws IOException
    {
        InputStream resourceList = getStream( RESOURCE_DIR + "/resources.txt" );

        if ( resourceList != null )
        {
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( resourceList ) );

            String line = reader.readLine();

            while ( line != null )
            {
                InputStream is = getStream( RESOURCE_DIR + "/" + line );

                if ( is == null )
                {
                    throw new IOException(
                        "The resource " + line + " doesn't exists in " + DEFAULT_TEMPLATE + " template." );
                }

                File outputFile = new File( outputDir, line );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream w = new FileOutputStream( outputFile );

                IOUtil.copy( is, w );

                IOUtil.close( is );

                IOUtil.close( w );

                line = reader.readLine();
            }
        }
    }

    /**
     * Get the resource as stream
     *
     * @param name
     * @return the inputstream
     */
    private InputStream getStream( String name )
    {
        return SiteMojo.class.getClassLoader().getResourceAsStream( name );
    }

    /**
     * Copy the directory
     *
     * @param source      source file to be copied
     * @param destination destination file
     * @throws IOException if any
     */
    private void copyDirectory( File source, File destination )
        throws IOException
    {
        if ( source.exists() )
        {
            DirectoryScanner scanner = new DirectoryScanner();

            String[] includedResources = {"**/**"};

            scanner.setIncludes( includedResources );

            scanner.addDefaultExcludes();

            scanner.setBasedir( source );

            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                File sourceFile = new File( source, name );

                File destinationFile = new File( destination, name );

                FileUtils.copyFile( sourceFile, destinationFile );
            }
        }
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
