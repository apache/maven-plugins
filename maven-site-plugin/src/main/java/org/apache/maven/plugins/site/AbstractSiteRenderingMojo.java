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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
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
     * @since 2.0-beta-5
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

                getLog().warn(
                               "Error loading report " + report.getClass().getName()
                                   + " - AbstractMethodError: canGenerateReport()" );
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

        // Put any of the properties in directly into the Velocity context
        attributes.putAll( project.getProperties() );

        DecorationModel decorationModel;
        try
        {
            decorationModel = siteTool.getDecorationModel( project, reactorProjects, localRepository, repositories, toRelative( project.getBasedir(), siteDirectory.getAbsolutePath() ), locale, inputEncoding, outputEncoding );
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
            skinFile = siteTool.getSkinArtifactFromRepository( localRepository, repositories, decorationModel )
                .getFile();
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

                getLog().info(
                               "Skipped \"" + report.getName( locale ) + "\" report, file \"" + outputName
                                   + "\" already exists for the " + displayLanguage + " version." );
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

        // TODO: temporary solution for MSITE-289. We need to upgrade doxia sit tools
        Map tmp = new HashMap();
        for ( Iterator it = documents.keySet().iterator(); it.hasNext(); )
        {
            String key = (String) it.next();
            tmp.put( StringUtils.replace( key, "\\", "/" ), documents.get( key ) );
        }
        documents = tmp;

        Map reportsByOutputName = locateReports( reports, documents, locale );

        // TODO: I want to get rid of categories eventually. There's no way to add your own in a fully i18n manner
        Map categories = categoriseReports( reportsByOutputName.values() );

        siteTool.populateReportsMenu( context.getDecoration(), locale, categories );
        populateReportItems( context.getDecoration(), locale, reportsByOutputName );

        if ( categories.containsKey( MavenReport.CATEGORY_PROJECT_INFORMATION ) )
        {
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );

            RenderingContext renderingContext = new RenderingContext( siteDirectory, "project-info.html" );
            String title = i18n.getString( "site-plugin", locale, "report.information.title" );
            String desc1 = i18n.getString( "site-plugin", locale, "report.information.description1" );
            String desc2 = i18n.getString( "site-plugin", locale, "report.information.description2" );
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports );

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
            DocumentRenderer renderer = new CategorySummaryDocumentRenderer( renderingContext, title, desc1, desc2,
                                                                             i18n, categoryReports );

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
