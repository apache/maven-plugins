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
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Base class for site rendering mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractSiteRenderingMojo
    extends AbstractSiteMojo
{
    /**
     * module type exclusion mappings
     * ex: fml -> **\/*-m1.fml  (excludes -m1.fml files)
     *
     * @parameter
     */
    protected Map moduleExcludes;

    /**
     * Specifies the output encoding.
     *
     * @parameter expression="${outputEncoding}" default-value="ISO-8859-1"
     */
    protected String outputEncoding;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The component for assembling inheritance.
     *
     * @component
     */
    protected DecorationModelInheritanceAssembler assembler;

    /**
     * The component that is used to resolve additional artifacts required.
     *
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     */
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @todo this is used for site descriptor resolution - it should relate to the actual project but for some reason they are not always filled in
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    protected List repositories;

    /**
     * The component used for creating artifact instances.
     *
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Directory containing the template page.
     *
     * @parameter expression="${templateDirectory}" default-value="src/site"
     * @deprecated use templateFile or skinning instead
     */
    private File templateDirectory;

    /**
     * Default template page.
     *
     * @parameter expression="${template}"
     * @deprecated use templateFile or skinning instead
     */
    private String template;

    /**
     * The location of a Velocity template file to use. When used, skins and the default templates, CSS and images
     * are disabled. It is highly recommended that you package this as a skin instead.
     *
     * @parameter expression="${templateFile}"
     */
    private File templateFile;

    /**
     * @parameter expression="${attributes}"
     */
    protected Map attributes;

    /**
     * Site renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;

    /**
     * @parameter expression="${reports}"
     * @required
     * @readonly
     */
    protected List reports;

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
     * @todo should we deprecate in favour of reports?
     */
    private File generatedSiteDirectory;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Project builder
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    protected DecorationModel getDecorationModel( MavenProject project, Locale locale, Map origProps )
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

        // Legacy for the old ${parentProject} syntax
        props.put( "parentProject", "<menu ref=\"parentProject\"/>" );

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

        MavenProject parentProject = getParentProject( project );
        if ( parentProject != null && project.getUrl() != null && parentProject.getUrl() != null )
        {
            populateProjectParentMenu( decoration, locale, parentProject );
        }

        if ( parentProject != null && project.getUrl() != null && parentProject.getUrl() != null )
        {
            DecorationModel parent = getDecorationModel( parentProject, locale, props );

            assembler.assembleModelInheritance( project.getName(), decoration, parent, project.getUrl(),
                                                parentProject.getUrl() );
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

    private void populateProjectParentMenu( DecorationModel decorationModel, Locale locale, MavenProject parentProject )
    {
        Menu menu = decorationModel.getMenuRef( "parentProject" );

        if ( menu != null )
        {
            String parentUrl = parentProject.getUrl();

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

                parentUrl = getRelativePath( parentUrl, project.getUrl() );

                menu.setName( i18n.getString( "site-plugin", locale, "report.menu.parentproject" ) );

                MenuItem item = new MenuItem();
                item.setName( parentProject.getName() );
                item.setHref( parentUrl );
                menu.addItem( item );
            }
        }
    }

    /**
     * Returns the parent POM URL. Attempts to source this value from the reactor env
     * if available (reactor env model attributes are interpolated), or if the
     * reactor is unavailable (-N) resorts to the project.getParent().getUrl() value
     * which will NOT have be interpolated.
     * <p/>
     * TODO: once bug is fixed in Maven proper, remove this
     *
     * @param project
     * @return parent project URL.
     */
    protected MavenProject getParentProject( MavenProject project )
    {
        MavenProject parentProject = null;

        MavenProject origParent = project.getParent();
        if ( origParent != null )
        {
            if ( origParent.getArtifactId() != null )
            {
                String parentArtifactId = origParent.getArtifactId();

                Iterator reactorItr = reactorProjects.iterator();

                while ( reactorItr.hasNext() )
                {
                    MavenProject reactorProject = (MavenProject) reactorItr.next();

                    String reactorArtifactId = reactorProject.getArtifactId();

                    if ( parentArtifactId.equals( reactorArtifactId ) )
                    {
                        parentProject = reactorProject;
                        break;
                    }
                }
            }

            if ( parentProject == null )
            {
                try
                {
                    MavenProject mavenProject = mavenProjectBuilder.build(
                        new File( project.getBasedir(), project.getModel().getParent().getRelativePath() ),
                        localRepository, null );
                    if ( mavenProject.getGroupId().equals( origParent.getGroupId() ) &&
                        mavenProject.getArtifactId().equals( origParent.getArtifactId() ) &&
                        mavenProject.getVersion().equals( origParent.getVersion() ) )
                    {
                        parentProject = mavenProject;
                    }
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().warn( "Unable to load parent project from repository: " + e.getMessage() );
                }
            }

            if ( parentProject == null )
            {
                try
                {
                    parentProject = mavenProjectBuilder.buildFromRepository( project.getParentArtifact(),
                                                                             project.getRemoteArtifactRepositories(),
                                                                             localRepository );
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().warn( "Unable to load parent project from repository: " + e.getMessage() );
                }
            }

            if ( parentProject == null )
            {
                // fallback to uninterpolated value

                parentProject = origParent;
            }
        }
        return parentProject;
    }

    private File getSkinArtifactFile( DecorationModel decoration )
        throws MojoFailureException, MojoExecutionException
    {
        Skin skin = decoration.getSkin();

        if ( skin == null )
        {
            skin = Skin.getDefaultSkin();
        }

        String version = skin.getVersion();
        Artifact artifact;
        try
        {
            if ( version == null )
            {
                version = Artifact.RELEASE_VERSION;
            }
            VersionRange versionSpec = VersionRange.createFromVersionSpec( version );
            artifact = artifactFactory.createDependencyArtifact( skin.getGroupId(), skin.getArtifactId(), versionSpec,
                                                                 "jar", null, null );

            artifactResolver.resolve( artifact, repositories, localRepository );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoFailureException( "The skin version '" + version + "' is not valid: " + e.getMessage() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to find skin", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoFailureException( "The skin does not exist: " + e.getMessage() );
        }

        return artifact.getFile();
    }

    protected List filterReports( List reports )
    {
        List filteredReports = new ArrayList( reports.size() );
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
        return filteredReports;
    }

    protected SiteRenderingContext createSiteRenderingContext( Locale locale )
        throws MojoExecutionException, IOException, MojoFailureException
    {
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

        DecorationModel decorationModel = getDecorationModel( locale );
        if ( template != null )
        {
            if ( templateFile != null )
            {
                getLog().warn( "'template' configuration is ignored when 'templateFile' is set" );
            }
            else
            {
                templateFile = new File( templateDirectory, template );
            }
        }

        File skinFile = getSkinArtifactFile( decorationModel );
        SiteRenderingContext context;
        if ( templateFile != null )
        {
            if ( !templateFile.exists() )
            {
                throw new MojoFailureException( "Template file '" + templateFile + "' does not exist" );
            }
            context = siteRenderer.createContextForTemplate( templateFile, skinFile, attributes, decorationModel,
                                                             project.getName(), locale );
        }
        else
        {
            context =
                siteRenderer.createContextForSkin( skinFile, attributes, decorationModel, project.getName(), locale );
        }

        // Generate static site
        if ( !locale.getLanguage().equals( Locale.getDefault().getLanguage() ) )
        {
            context.addSiteDirectory( new File( siteDirectory, locale.getLanguage() ) );
            context.addModuleDirectory( new File( xdocDirectory, locale.getLanguage() ), "xdoc" );
            context.addModuleDirectory( new File( xdocDirectory, locale.getLanguage() ), "fml" );
        }
        else
        {
            context.addSiteDirectory( siteDirectory );
            context.addModuleDirectory( xdocDirectory, "xdoc" );
            context.addModuleDirectory( xdocDirectory, "fml" );
        }

        if ( moduleExcludes != null )
        {
            context.setModuleExcludes( moduleExcludes );
        }

        context.addSiteDirectory( generatedSiteDirectory );
        return context;
    }

    private DecorationModel getDecorationModel( Locale locale )
        throws MojoExecutionException
    {
        Map props = new HashMap();

        // This is to support the deprecated ${reports} and ${modules} tags.
        props.put( "reports", "<menu ref=\"reports\"/>\n" );
        props.put( "modules", "<menu ref=\"modules\"/>\n" );

        DecorationModel decorationModel = getDecorationModel( project, locale, props );
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
                    getLog().debug( "Attempting to source module information from local filesystem" );

                    // Not running reactor - search for the projects manually
                    List models = new ArrayList( project.getModules().size() );
                    for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
                    {
                        String module = (String) i.next();
                        Model model;
                        File f = new File( project.getBasedir(), module + "/pom.xml" );
                        if ( f.exists() )
                        {
                            try
                            {
                                model = mavenProjectBuilder.build( f, localRepository, null ).getModel();
                            }
                            catch ( ProjectBuildingException e )
                            {
                                throw new MojoExecutionException( "Unable to read local module-POM", e );
                            }
                        }
                        else
                        {
                            getLog().warn( "No filesystem module-POM available" );

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

    private void appendMenuItem( Menu menu, String name, String href )
    {
        if ( href != null )
        {
            MenuItem item = new MenuItem();
            item.setName( name );

            String baseUrl = project.getUrl();
            href = getRelativePath( href, baseUrl );

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

    protected Map locateReports( List reports, Map documents, Locale locale )
    {
        Map reportsByOutputName = new HashMap();
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            String outputName = report.getOutputName() + ".html";
            if ( documents.containsKey( outputName ) )
            {
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                getLog().info( "Skipped \"" + report.getName( locale ) + "\" report, file \"" + outputName +
                    "\" already exists for the " + displayLanguage + " version." );
                i.remove();
            }
            else
            {
                reportsByOutputName.put( report.getOutputName(), report );

                RenderingContext renderingContext = new RenderingContext( siteDirectory, outputName );
                ReportDocumentRenderer renderer = new ReportDocumentRenderer( report, renderingContext, getLog() );
                documents.put( outputName, renderer );
            }
        }
        return reportsByOutputName;
    }

    protected void populateReportsMenu( DecorationModel decorationModel, Locale locale, Map categories )
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
                if ( !isEmptyList( categoryReports ) )
                {
                    MenuItem item = createCategoryMenu(
                        i18n.getString( "site-plugin", locale, "report.menu.projectinformation" ), "/project-info.html",
                        categoryReports, locale );
                    menu.getItems().add( item );
                    found = true;
                }

                categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
                if ( !isEmptyList( categoryReports ) )
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

    protected void populateReportItems( DecorationModel decorationModel, Locale locale, Map reportsByOutputName )
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

    protected Map categoriseReports( Collection reports )
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

    protected Map locateDocuments( SiteRenderingContext context, List reports, Locale locale )
        throws IOException, RendererException
    {
        Map documents = siteRenderer.locateDocumentFiles( context );

        Map reportsByOutputName = locateReports( reports, documents, locale );

        // TODO: I want to get rid of categories eventually. There's no way to add your own in a fully i18n manner
        Map categories = categoriseReports( reportsByOutputName.values() );

        populateReportsMenu( context.getDecoration(), locale, categories );
        populateReportItems( context.getDecoration(), locale, reportsByOutputName );

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_INFORMATION ) )
        {
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );

            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-info.html" );
            String title = i18n.getString( "site-plugin", locale, "report.information.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.information.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.information.description2" );
            DocumentRenderer renderer =
                new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2, i18n, categoryReports );

            if ( !documents.containsKey( renderer.getOutputName() ) )
            {
                documents.put( renderer.getOutputName(), renderer );
            }
            else
            {
                getLog().info( "Category summary '" + renderer.getOutputName() + "' skipped; already exists" );
            }
        }

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_REPORTS ) )
        {
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-reports.html" );
            String title = i18n.getString( "site-plugin", locale, "report.project.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.project.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.project.description2" );
            DocumentRenderer renderer =
                new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2, i18n, categoryReports );

            if ( !documents.containsKey( renderer.getOutputName() ) )
            {
                documents.put( renderer.getOutputName(), renderer );
            }
            else
            {
                getLog().info( "Category summary '" + renderer.getOutputName() + "' skipped; already exists" );
            }
        }
        return documents;
    }

    private static boolean isEmptyList( List list )
    {
        return list == null || list.isEmpty();
    }

    private String getRelativePath( String to, String from )
    {
        URL toUrl = null;
        URL fromUrl = null;

        String toPath = to;
        String fromPath = from;

        try
        {
            toUrl = new URL( to );
        }
        catch ( MalformedURLException e )
        {
        }

        try
        {
            fromUrl = new URL( from );
        }
        catch ( MalformedURLException e )
        {
        }

        if ( toUrl != null && fromUrl != null )
        {
            // URLs, determine if they share protocol and domain info

            if ( ( toUrl.getProtocol().equalsIgnoreCase( fromUrl.getProtocol() ) ) &&
                ( toUrl.getHost().equalsIgnoreCase( fromUrl.getHost() ) ) && ( toUrl.getPort() == fromUrl.getPort() ) )
            {
                // shared URL domain details, use URI to determine relative path

                toPath = toUrl.getFile();
                fromPath = fromUrl.getFile();
            }
            else
            {
                // dont share basic URL infomation, no relative available

                return to;
            }
        }
        else if ( ( toUrl != null && fromUrl == null ) || ( toUrl == null && fromUrl != null ) )
        {
            // one is a URL and the other isnt, no relative available.

            return to;
        }

        // either the two locations are not URLs or if they are they
        // share the common protocol and domain info and we are left
        // with their URI information

        // normalise the path delimters

        toPath = new File( toPath ).getPath();
        fromPath = new File( fromPath ).getPath();

        // strip any leading slashes if its a windows path
        if ( toPath.matches( "^\\[a-zA-Z]:" ) )
        {
            toPath = toPath.substring( 1 );
        }
        if ( fromPath.matches( "^\\[a-zA-Z]:" ) )
        {
            fromPath = fromPath.substring( 1 );
        }

        // lowercase windows drive letters.

        if ( toPath.startsWith( ":", 1 ) )
        {
            toPath = toPath.substring( 0, 1 ).toLowerCase() + toPath.substring( 1 );
        }
        if ( fromPath.startsWith( ":", 1 ) )
        {
            fromPath = fromPath.substring( 0, 1 ).toLowerCase() + fromPath.substring( 1 );
        }

        // check for the presence of windows drives. No relative way of
        // traversing from one to the other.

        if ( ( toPath.startsWith( ":", 1 ) && fromPath.startsWith( ":", 1 ) ) &&
            ( !toPath.substring( 0, 1 ).equals( fromPath.substring( 0, 1 ) ) ) )
        {
            // they both have drive path element but they dont match, no
            // relative path

            return to;
        }

        if ( ( toPath.startsWith( ":", 1 ) && !fromPath.startsWith( ":", 1 ) ) ||
            ( !toPath.startsWith( ":", 1 ) && fromPath.startsWith( ":", 1 ) ) )
        {

            // one has a drive path element and the other doesnt, no relative
            // path.

            return to;

        }

        // use tokeniser to traverse paths and for lazy checking
        StringTokenizer toTokeniser = new StringTokenizer( toPath, File.separator );
        StringTokenizer fromTokeniser = new StringTokenizer( fromPath, File.separator );

        int count = 0;

        // walk along the to path looking for divergence from the from path
        while ( toTokeniser.hasMoreTokens() && fromTokeniser.hasMoreTokens() )
        {
            if ( File.separatorChar == '\\' )
            {
                if ( !fromTokeniser.nextToken().equalsIgnoreCase( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }
            else
            {
                if ( !fromTokeniser.nextToken().equals( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }

            count++;
        }

        // reinitialise the tokenisers to count positions to retrieve the
        // gobbled token

        toTokeniser = new StringTokenizer( toPath, File.separator );
        fromTokeniser = new StringTokenizer( fromPath, File.separator );

        while ( count-- > 0 )
        {
            fromTokeniser.nextToken();
            toTokeniser.nextToken();
        }

        String relativePath = "";

        // add back refs for the rest of from location.
        while ( fromTokeniser.hasMoreTokens() )
        {
            fromTokeniser.nextToken();

            relativePath += "..";

            if ( fromTokeniser.hasMoreTokens() )
            {
                relativePath += File.separatorChar;
            }
        }

        if ( relativePath.length() != 0 && toTokeniser.hasMoreTokens() )
        {
            relativePath += File.separatorChar;
        }

        // add fwd fills for whatevers left of to.
        while ( toTokeniser.hasMoreTokens() )
        {
            relativePath += toTokeniser.nextToken();

            if ( toTokeniser.hasMoreTokens() )
            {
                relativePath += File.separatorChar;
            }
        }

        if ( !relativePath.equals( to ) )
        {
            getLog().debug( "Mapped url: " + to + " to relative path: " + relativePath );
        }

        return relativePath;
    }


}


