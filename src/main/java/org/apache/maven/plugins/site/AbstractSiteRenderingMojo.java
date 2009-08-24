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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
    protected List<ArtifactRepository> repositories;

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
    
    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;
   
    /**
    *
    * @component
    * @readonly
    */    
    protected PlexusContainer plexusContainer;

    /**
     *
     * @component
     * @readonly
     */
    protected MavenPluginManager mavenPluginManager;
   
    protected Map<MavenReport, ClassLoader> getReports()  throws MojoExecutionException
    {
        if ( this.project.getReporting() == null || this.project.getReporting().getPlugins().isEmpty() )
        {
            return Collections.emptyMap();
        }
        return buildMavenReports();
    }
    
    private Xpp3Dom convert( MojoDescriptor mojoDescriptor  )
    {
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        PlexusConfiguration[] ces = c.getChildren();

        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName() );
                    e.setValue( value );
                    if ( defaultValue != null )
                    {
                        e.setAttribute( "default-value", defaultValue );
                    }
                    dom.addChild( e );
                }
            }
        }

        return dom;
    }    
    
    private Map<MavenReport, ClassLoader> buildMavenReports()
        throws MojoExecutionException
    {
        List<String> imports = new ArrayList<String>();
     
        
        imports.add( "org.apache.maven.reporting.MavenReport" );
        //imports.add( "org.apache.maven.reporting.AbstractMavenReport" );
        imports.add( "org.apache.maven.doxia.siterenderer.Renderer" );
        imports.add( "org.apache.maven.doxia.sink.SinkFactory" );
        imports.add( "org.codehaus.doxia.sink.Sink" );
        imports.add( "org.apache.maven.doxia.sink.Sink" );
        imports.add( "org.apache.maven.doxia.sink.SinkEventAttributes" );
        
        /*
        imports.add( "org.codehaus.plexus.util.xml.Xpp3Dom" );
        imports.add( "org.codehaus.plexus.util.xml.pull.XmlPullParser" );
        imports.add( "org.codehaus.plexus.util.xml.pull.XmlPullParserException" );
        imports.add( "org.codehaus.plexus.util.xml.pull.XmlSerializer" );
        */
        
        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository( localRepository );
        repositoryRequest.setRemoteRepositories( mavenSession.getRequest().getRemoteRepositories() );
        
        try
        {

            Map<MavenReport, ClassLoader> reports = new HashMap<MavenReport, ClassLoader>();

            for ( ReportPlugin reportPlugin : this.project.getReporting().getPlugins() )
            {
                Plugin plugin = new Plugin();
                plugin.setGroupId( reportPlugin.getGroupId() );
                plugin.setArtifactId( reportPlugin.getArtifactId() );
                plugin.setVersion( reportPlugin.getVersion() );

                List<String> goals = new ArrayList<String>();
                for ( ReportSet reportSet : reportPlugin.getReportSets() )
                {
                    goals.addAll( reportSet.getReports() );
                }
                // no report set we will execute all from the report plugin
                boolean emptyReports = goals.isEmpty();

                PluginDescriptor pluginDescriptor = mavenPluginManager.getPluginDescriptor( plugin, repositoryRequest );
                
                if (emptyReports)
                {
                    List<MojoDescriptor> mojoDescriptors = pluginDescriptor.getMojos();
                    for (MojoDescriptor mojoDescriptor : mojoDescriptors)
                    {
                        goals.add( mojoDescriptor.getGoal() );
                    }
                }
                
                for ( String goal : goals )
                {
                    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
                    if ( mojoDescriptor == null )
                    {
                        throw new MojoNotFoundException( goal, pluginDescriptor );
                    }

                    MojoExecution mojoExecution = new MojoExecution( plugin, goal, "report:" + goal );
                    mojoExecution.setConfiguration( convert( mojoDescriptor ) );
                    mojoExecution.setMojoDescriptor( mojoDescriptor );
                    mavenPluginManager.setupPluginRealm( pluginDescriptor, mavenSession, Thread.currentThread().getContextClassLoader(), imports );
                    MavenReport mavenReport = getConfiguredMavenReport( mojoExecution, pluginDescriptor );
                    if (mavenReport != null)
                    {
                        reports.put( mavenReport, pluginDescriptor.getClassRealm() );
                    }
                }
            }
            return reports;
        }
        catch ( Throwable e )
        {
            throw new MojoExecutionException( "failed to get Reports ", e );
        }
    }
    
    private MavenReport getConfiguredMavenReport( MojoExecution mojoExecution, PluginDescriptor pluginDescriptor )
        throws Throwable
    {
        if ( !isMavenReport( mojoExecution, pluginDescriptor ) )
        {
            return null;
        }
        try
        {
            // FIXME here we need something to prevent MJAVADOC-251 config injection order can be different from mvn < 3.x
            MavenReport mavenReport =
                (MavenReport) mavenPluginManager.getConfiguredMojo( Mojo.class, this.mavenSession, mojoExecution );
            return mavenReport;

        } catch (ClassCastException e)
        {
            getLog().warn( "skip ClassCastException " + e.getMessage() );
            return null;
        }
        catch ( Throwable e )
        {
            getLog().error( "error configuring mojo " + mojoExecution.toString() + " : " + e.getMessage() , e);
            throw e;
        }
    }

    private boolean isMavenReport( MojoExecution mojoExecution, PluginDescriptor pluginDescriptor )
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoExecution.getGoal() );
            Thread.currentThread().setContextClassLoader( mojoDescriptor.getRealm() );
            boolean isMavenReport = MavenReport.class.isAssignableFrom( mojoDescriptor.getImplementationClass() );
            if (!isMavenReport)
            {
                getLog().info( " skip non MavenReport " + mojoExecution.getMojoDescriptor().getId() );
            }
            return isMavenReport;
        }
        catch ( LinkageError e )
        {
            getLog().warn( "skip LinkageError  mojoExecution.goal"  +  mojoExecution.getGoal() + " : " + e.getMessage(), e  );
            //pluginRealm.display();
            return false;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

    }
    
    protected Map<MavenReport, ClassLoader> filterReports( Map<MavenReport, ClassLoader> reports )
    {
    	Map<MavenReport, ClassLoader> filteredReports = new HashMap<MavenReport, ClassLoader>();
        for ( MavenReport report : reports.keySet() )
        {
            //noinspection ErrorNotRethrown,UnusedCatchParameter
            try
            {
                if ( report.canGenerateReport() )
                {
                    filteredReports.put( report, reports.get(report) );
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
                filteredReports.put( report, reports.get(report) );
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

    /**
     * Go through the list of reports and process each one like this:
     * <ul>
     * <li>Add the report to a map of reports keyed by filename having the report itself as value
     * <li>If the report is not yet in the map of documents, add it together with a suitable renderer
     * </ul>
     *
     * @param reports A List of MavenReports
     * @param documents A Map of documents, keyed by filename
     * @return A map with all reports keyed by filename having the report itself as value. The map will be used to
     * populate a menu.
     */
    protected Map locateReports( Map<MavenReport, ClassLoader> reports, Map documents, Locale locale )
    {
        Map reportsByOutputName = new HashMap();
        for ( Iterator i = reports.keySet().iterator(); i.hasNext(); )
        {
            MavenReport report = (MavenReport) i.next();

            String outputName = report.getOutputName() + ".html";

            // Always add the report to the menu, see MSITE-150
            reportsByOutputName.put( report.getOutputName(), report );

            if ( documents.containsKey( outputName ) )
            {
                String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

                getLog().info( "Skipped \"" + report.getName( locale ) + "\" report, file \"" + outputName
                                   + "\" already exists for the " + displayLanguage + " version." );
                i.remove();
            }
            else
            {
                RenderingContext renderingContext = new RenderingContext( siteDirectory, outputName );
                ReportDocumentRenderer renderer = new ReportDocumentRenderer( report, renderingContext, getLog(), reports.get( report ) );
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

    protected Map locateDocuments( SiteRenderingContext context, Map<MavenReport, ClassLoader> reports, Locale locale )
        throws IOException, RendererException
    {
        Map documents = siteRenderer.locateDocumentFiles( context );

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
            List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
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
