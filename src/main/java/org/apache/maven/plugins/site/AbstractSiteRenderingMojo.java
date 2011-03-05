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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.exec.MavenReportExecution;
import org.apache.maven.reporting.exec.MavenReportExecutor;
import org.apache.maven.reporting.exec.MavenReportExecutorRequest;
import org.apache.maven.reporting.exec.ReportPlugin;

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
    private Map<String, String> moduleExcludes;

    /**
     * Remote repositories used for the project.
     *
     * @todo this is used for site descriptor resolution - it should relate to the actual project but for some reason they are not always filled in
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List<ArtifactRepository> repositories;

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
     * @since 2.0-beta-5
     */
    private File templateFile;

    /**
     * The template properties for rendering the site.
     *
     * @parameter expression="${attributes}"
     */
    private Map<Object, Object> attributes;

    /**
     * Site renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;


    /**
     * Alternative directory for xdoc source, useful for m1 to m2 migration
     *
     * @parameter default-value="${basedir}/xdocs"
     * @deprecated use the standard m2 directory layout
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

    /**
     * The current Maven session.
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * Reports (Maven 2).
     * 
     * @parameter expression="${reports}"
     * @required
     * @readonly
     */
    protected List<MavenReport> reports;

    /**
     * Report plugins (Maven 3).
     * 
     * @parameter
     * @since 3.0-beta-1
     */
    private ReportPlugin[] reportPlugins;

    /**
     * The report executor (Maven 3).
     * 
     * @component
     * @readonly
     */
    private MavenReportExecutor mavenReportExecutor;

    protected List<MavenReportExecution> getReports()
        throws MojoExecutionException
    {
        if ( reportPlugins != null && reportPlugins.length > 0 )
        { // to be reviewed : is this the right test to detect Maven 3?
            MavenReportExecutorRequest mavenReportExecutorRequest = new MavenReportExecutorRequest();
            mavenReportExecutorRequest.setLocalRepository( localRepository );
            mavenReportExecutorRequest.setMavenSession( mavenSession );
            mavenReportExecutorRequest.setProject( project );
            mavenReportExecutorRequest.setReportPlugins( reportPlugins );

            return mavenReportExecutor.buildMavenReports( mavenReportExecutorRequest );
        }

        List<MavenReportExecution> reportExecutions = new ArrayList<MavenReportExecution>( reports.size() );
        for ( MavenReport report : reports )
        {
            if ( report.canGenerateReport() )
            {
                reportExecutions.add( new MavenReportExecution( report ) );
            }
        }
        return reportExecutions;
    }

    protected SiteRenderingContext createSiteRenderingContext( Locale locale )
        throws MojoExecutionException, IOException, MojoFailureException
    {
        if ( attributes == null )
        {
            attributes = new HashMap<Object, Object>();
        }

        if ( attributes.get( "project" ) == null )
        {
            attributes.put( "project", project );
        }

        if ( attributes.get( "inputEncoding" ) == null )
        {
            attributes.put( "inputEncoding", getInputEncoding() );
        }

        if ( attributes.get( "outputEncoding" ) == null )
        {
            attributes.put( "outputEncoding", getOutputEncoding() );
        }

        // Put any of the properties in directly into the Velocity context
        attributes.putAll( project.getProperties() );
       
        DecorationModel decorationModel;
        try
        {
            decorationModel = siteTool.getDecorationModel( project, reactorProjects, localRepository, repositories,
                                                           toRelative( project.getBasedir(),
                                                                       siteDirectory.getAbsolutePath() ),
                                                           locale, getInputEncoding(), getOutputEncoding() );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }
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

        File skinFile;
        try
        {
            Artifact skinArtifact =
                siteTool.getSkinArtifactFromRepository( localRepository, repositories, decorationModel );
            getLog().info( "Rendering site with " + skinArtifact.getId() + " skin." );

            skinFile = skinArtifact.getFile();
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }
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
            context = siteRenderer.createContextForSkin( skinFile, attributes, decorationModel, project.getName(),
                                                         locale );
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

    /**
     * Go through the list of reports and process each one like this:
     * <ul>
     * <li>Add the report to a map of reports keyed by filename having the report itself as value
     * <li>If the report is not yet in the map of documents, add it together with a suitable renderer
     * </ul>
     *
     * @param reports A List of MavenReports
     * @param documents A Map of documents, keyed by filename
     * @param locale the Locale the reports are processed for.
     * @return A map with all reports keyed by filename having the report itself as value.
     * The map will be used to populate a menu.
     */
    protected Map<String, MavenReport> locateReports( List<MavenReportExecution> reports,
                                                      Map<String, DocumentRenderer> documents, Locale locale )
    {
        // copy Collection to prevent ConcurrentModificationException
        List<MavenReportExecution> filtered = new ArrayList<MavenReportExecution>( reports );

        Map<String, MavenReport> reportsByOutputName = new HashMap<String, MavenReport>();
        for ( MavenReportExecution mavenReportExecution : filtered )
        {
            MavenReport report = mavenReportExecution.getMavenReport();

            String outputName = report.getOutputName() + ".html";

            // Always add the report to the menu, see MSITE-150
            reportsByOutputName.put( report.getOutputName(), report );

            if ( documents.containsKey( outputName ) )
            {
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                getLog().info( "Skipped \"" + report.getName( locale ) + "\" report, file \"" + outputName
                                   + "\" already exists for the " + displayLanguage + " version." );
                reports.remove( mavenReportExecution );
            }
            else
            {
                RenderingContext renderingContext = new RenderingContext( siteDirectory, outputName );
                DocumentRenderer renderer = new ReportDocumentRenderer( mavenReportExecution, renderingContext, getLog() );
                documents.put( outputName, renderer );
            }
        }
        return reportsByOutputName;
    }

    /**
     * Go through the collection of reports and put each report into a list for the appropriate category. The list is
     * put into a map keyed by the name of the category.
     *
     * @param reports A Collection of MavenReports
     * @return A map keyed category having the report itself as value
     */
    protected Map<String, List<MavenReport>> categoriseReports( Collection<MavenReport> reports )
    {
        Map<String, List<MavenReport>> categories = new HashMap<String, List<MavenReport>>();
        for ( MavenReport report : reports )
        {
            List<MavenReport> categoryReports = categories.get( report.getCategoryName() );
            if ( categoryReports == null )
            {
                categoryReports = new ArrayList<MavenReport>();
                categories.put( report.getCategoryName(), categoryReports );
            }
            categoryReports.add( report );
        }
        return categories;
    }

    protected Map<String, DocumentRenderer> locateDocuments( SiteRenderingContext context, List<MavenReportExecution> reports, Locale locale )
        throws IOException, RendererException
    {
        Map<String, DocumentRenderer> documents = siteRenderer.locateDocumentFiles( context );

        Map<String, MavenReport> reportsByOutputName = locateReports( reports, documents, locale );

        // TODO: I want to get rid of categories eventually. There's no way to add your own in a fully i18n manner
        Map<String, List<MavenReport>> categories = categoriseReports( reportsByOutputName.values() );

        siteTool.populateReportsMenu( context.getDecoration(), locale, categories );
        populateReportItems( context.getDecoration(), locale, reportsByOutputName );

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_INFORMATION ) )
        {
            List<MavenReport> categoryReports = categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );

            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-info.html" );
            String title = i18n.getString( "site-plugin", locale, "report.information.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.information.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.information.description2" );
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports, getLog() );

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
            List<MavenReport> categoryReports = categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-reports.html" );
            String title = i18n.getString( "site-plugin", locale, "report.project.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.project.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.project.description2" );
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports, getLog() );

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
