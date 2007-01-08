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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.Banner;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Module type exclusion mappings
     * ex: <code>fml  -> **&#47;*-m1.fml</code>  (excludes fml files ending in '-m1.fml' recursively)
     * <p/>
     * The configuration looks like this:
     * <pre>
     *   &lt;moduleExcludes&gt;
     *     &lt;moduleType&gt;filename1.ext,**&#47;*sample.ext&lt;/moduleType&gt;
     *     &lt;!-- moduleType can be one of 'apt', 'fml' or 'xdoc'. --&gt;
     *     &lt;!-- The value is a comma separated list of           --&gt;
     *     &lt;!-- filenames or fileset patterns.                   --&gt;
     *     &lt;!-- Here's an example:                               --&gt;
     *     &lt;xdoc&gt;changes.xml,navigation.xml&lt;/xdoc&gt;
     *   &lt;/moduleExcludes&gt;
     * </pre>
     *
     * @parameter
     */
    protected Map moduleExcludes;

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
     * The template properties for rendering the site.
     *
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
    protected File generatedSiteDirectory;

    protected DecorationModel getDecorationModel( MavenProject project, Locale locale, Map origProps )
        throws MojoExecutionException
    {
        Map props = new HashMap( origProps );

        File siteDescriptor;
        if ( project.getBasedir() == null )
        {
            // POM is in the repository, look there for site descriptor
            try
            {
                siteDescriptor = getSiteDescriptorFromRepository( project, locale );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException(
                    "The site descriptor cannot be resolved from the repository: " + e.getMessage(), e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "The site descriptor cannot be resolved from the repository: " + e.getMessage(), e );
            }
        }
        else
        {
            siteDescriptor = getSiteDescriptorFile( project.getBasedir(), locale );
        }

        String siteDescriptorContent = null;
        try
        {
            if ( siteDescriptor != null && siteDescriptor.exists() )
            {
                getLog().debug( "Reading site descriptor from " + siteDescriptor );
                siteDescriptorContent = FileUtils.fileRead( siteDescriptor );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "The site descriptor cannot be read!", e );
        }

        DecorationModel decoration = null;
        if ( siteDescriptorContent != null )
        {
            try
            {
                siteDescriptorContent = getInterpolatedSiteDescriptorContent( props, project, siteDescriptorContent );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                                                 "The site descriptor cannot interpolate properties: " + e.getMessage(), e );
            }

            decoration = readDecorationModel( siteDescriptorContent );
        }

        MavenProject parentProject = getParentProject( project );
        if ( parentProject != null )
        {
            DecorationModel parent = getDecorationModel( parentProject, locale, props );

            if ( decoration == null )
            {
                decoration = parent;
            }
            else
            {
                assembler.assembleModelInheritance( project.getName(), decoration, parent, project.getUrl(),
                                                    parentProject.getUrl() == null ? project.getUrl() : parentProject.getUrl() );
            }
            if ( decoration != null )
            {
                populateProjectParentMenu( decoration, locale, parentProject, true );
            }
        }

        return decoration;
    }

    private DecorationModel readDecorationModel( String siteDescriptorContent )
        throws MojoExecutionException
    {
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
        return decoration;
    }

    private File getSiteDescriptorFromRepository( MavenProject project, Locale locale )
        throws ArtifactResolutionException, IOException
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
        throws IOException, ArtifactResolutionException, ArtifactNotFoundException
    {
        File result;

        // TODO: this is a bit crude - proper type, or proper handling as metadata rather than an artifact in 2.1?
        Artifact artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(), project.getArtifactId(),
                                                                          project.getVersion(), "xml",
                                                                          "site_" + locale.getLanguage() );

        boolean found = false;
        try
        {
            artifactResolver.resolve( artifact, repositories, localRepository );

            result = artifact.getFile();

            // we use zero length files to avoid re-resolution (see below)
            if ( result.length() > 0 )
            {
                found = true;
            }
            else
            {
                getLog().debug( "Skipped locale's site descriptor" );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().debug( "Unable to locate locale's site descriptor: " + e );

            // we can afford to write an empty descriptor here as we don't expect it to turn up later in the remote
            // repository, because the parent was already released (and snapshots are updated automatically if changed)
            result = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            result.createNewFile();
        }

        if ( !found )
        {
            artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(), project.getArtifactId(),
                                                                     project.getVersion(), "xml", "site" );
            try
            {
                artifactResolver.resolve( artifact, repositories, localRepository );
            }
            catch ( ArtifactNotFoundException e )
            {
                // see above regarding this zero length file
                result = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
                result.createNewFile();

                throw e;
            }

            result = artifact.getFile();

            // we use zero length files to avoid re-resolution (see below)
            if ( result.length() == 0 )
            {
                getLog().debug( "Skipped remote site descriptor check" );
                result = null;
            }
        }

        return result;
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
                // plugins with an earlier version to fail (most of the org.codehaus mojo now fails)
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

        if ( attributes.get( "inputEncoding" ) == null )
        {
            attributes.put( "inputEncoding", inputEncoding );
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

        if ( decorationModel == null )
        {
            String siteDescriptorContent;

            try
            {
                // Note the default is not a super class - it is used when nothing else is found
                siteDescriptorContent = IOUtil.toString( getClass().getResourceAsStream( "/default-site.xml" ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error reading default site descriptor: " + e.getMessage(), e );
            }

            try
            {
                siteDescriptorContent = getInterpolatedSiteDescriptorContent( props, project, siteDescriptorContent );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                                                 "The site descriptor cannot interpolate properties: " + e.getMessage(), e );
            }

            decorationModel = readDecorationModel( siteDescriptorContent );
        }
        populateModules( decorationModel, locale, true );

        if ( decorationModel.getBannerLeft() == null )
        {
            // extra default to set
            Banner banner = new Banner();
            banner.setName( project.getName() );
            decorationModel.setBannerLeft( banner );
        }

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
}


