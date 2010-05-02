package org.apache.maven.report.projectinfo;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base class with the things that should be in AbstractMavenReport anyway.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @since 2.0
 */
public abstract class AbstractProjectInfoReport
    extends AbstractMavenReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * SiteTool component.
     *
     * @since 2.1
     * @component
     */
    protected SiteTool siteTool;

    /**
     * Doxia Site Renderer component.
     *
     * @component
     */
    protected Renderer siteRenderer;

    /**
     * Artifact Resolver component.
     *
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * Artifact Factory component.
     *
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * Internationalization component.
     *
     * @component
     */
    protected I18N i18n;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The output directory for the report. Note that this parameter is only evaluated if the goal is run directly from
     * the command line. If the goal is run indirectly as part of a site generation, the output directory configured in
     * the Maven Site Plugin is used instead.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Local Repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            return;
        }

        // TODO: push to a helper? Could still be improved by taking more of the site information from the site plugin
        Writer writer = null;
        try
        {
            String filename = getOutputName() + ".html";

            DecorationModel model = new DecorationModel();
            model.setBody( new Body() );

            Map attributes = new HashMap();
            attributes.put( "outputEncoding", "UTF-8" );
            attributes.put( "project", project );

            Locale locale = Locale.getDefault();
            Artifact defaultSkin =
                siteTool.getDefaultSkinArtifact( localRepository, project.getRemoteArtifactRepositories() );

            SiteRenderingContext siteContext = siteRenderer.createContextForSkin( defaultSkin.getFile(), attributes,
                                                                                  model, getName( locale ), locale );

            RenderingContext context = new RenderingContext( outputDirectory, filename );

            SiteRendererSink sink = new SiteRendererSink( context );

            generate( sink, null, locale );

            outputDirectory.mkdirs();

            writer = new OutputStreamWriter( new FileOutputStream( new File( outputDirectory, filename ) ), "UTF-8" );

            siteRenderer.generateDocument( writer, sink, siteContext );

            siteRenderer.copyResources( siteContext, new File( project.getBasedir(), "src/site/resources" ),
                                        outputDirectory );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException(
                "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation.", e );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException(
                "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation.", e );
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException(
                "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation.", e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /** {@inheritDoc} */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /** {@inheritDoc} */
    public File getReportOutputDirectory()
    {
        return outputDirectory;
    }

    /** {@inheritDoc} */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        this.outputDirectory = reportOutputDirectory;
    }

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return project;
    }

    /** {@inheritDoc} */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected String getI18nString( Locale locale, String key )
    {
        return i18n.getString( "project-info-report", locale, "report." + getI18Nsection() + '.' + key );
    }

    protected abstract String getI18Nsection();

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getI18nString( locale, "name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getI18nString( locale, "description" );
    }
}
