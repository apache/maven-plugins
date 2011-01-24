package org.apache.maven.plugin.changes;

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
import org.codehaus.plexus.util.ReaderFactory;

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
 * Note: This file was copied from r415312 of AbstractProjectInfoReport in
 * maven-project-info-reports, as a work-around to MCHANGES-88.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 */
public abstract class AbstractChangesReport
    extends AbstractMavenReport
{
    /**
     * Report output directory. Note that this parameter is only relevant if the goal is run from the command line or
     * from the default build lifecycle. If the goal is run indirectly as part of a site generation, the output
     * directory configured in the Maven Site Plugin is used instead.
     * 
     * @parameter default-value="${project.reporting.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * Report output encoding. Note that this parameter is only relevant if the goal is run from the command line or
     * from the default build lifecycle. If the goal is run indirectly as part of a site generation, the output
     * encoding configured in the Maven Site Plugin is used instead.
     *
     * @parameter expression="${outputEncoding}" default-value="${project.reporting.outputEncoding}"
     * @since 2.4
     */
    private String outputEncoding;

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
            attributes.put( "outputEncoding", getOutputEncoding() );
            Locale locale = Locale.getDefault();
            SiteRenderingContext siteContext = siteRenderer.createContextForSkin( getSkinArtifactFile(), attributes,
                                                                                  model, getName( locale ), locale );
            siteContext.setOutputEncoding( getOutputEncoding() );

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
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /**
     * Get the effective reporting output file encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     * @since 2.4
     */
    protected String getOutputEncoding()
    {
        return ( outputEncoding != null ) ? outputEncoding : ReaderFactory.UTF_8;
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
