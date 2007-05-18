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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base class with the things that should be in AbstractMavenReport anyway.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractProjectInfoReport
    extends AbstractMavenReport
{
    /**
     * Report output directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;

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

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    private File getSkinArtifactFile()
        throws MojoExecutionException
    {
        Skin skin = Skin.getDefaultSkin();

        String version = skin.getVersion();
        Artifact artifact;
        try
        {
            if ( version == null )
            {
                version = Artifact.RELEASE_VERSION;
            }
            VersionRange versionSpec = VersionRange.createFromVersionSpec( version );
            artifact = factory.createDependencyArtifact( skin.getGroupId(), skin.getArtifactId(), versionSpec, "jar",
                                                         null, null );

            resolver.resolve( artifact, project.getRemoteArtifactRepositories(), localRepository );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "The skin version '" + version + "' is not valid: " + e.getMessage() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to find skin", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "The skin does not exist: " + e.getMessage() );
        }

        return artifact.getFile();
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            return;
        }

        // TODO: push to a helper? Could still be improved by taking more of the site information from the site plugin
        try
        {
            DecorationModel model = new DecorationModel();
            model.setBody( new Body() );
            Map attributes = new HashMap();
            attributes.put( "outputEncoding", "UTF-8" );
            attributes.put( "project", project );
            Locale locale = Locale.getDefault();
            SiteRenderingContext siteContext = siteRenderer.createContextForSkin( getSkinArtifactFile(), attributes,
                                                                                  model, getName( locale ), locale );

            RenderingContext context = new RenderingContext( outputDirectory, getOutputName() + ".html" );

            SiteRendererSink sink = new SiteRendererSink( context );
            generate( sink, locale );

            outputDirectory.mkdirs();

            Writer writer = new FileWriter( new File( outputDirectory, getOutputName() + ".html" ) );

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
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException(
                "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation.", e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }
}
