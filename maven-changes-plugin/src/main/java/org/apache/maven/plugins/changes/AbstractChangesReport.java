package org.apache.maven.plugins.changes;

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

import org.apache.commons.io.IOUtils;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base class with the things that should be in AbstractMavenReport anyway. Note: This file was copied from r415312 of
 * AbstractProjectInfoReport in maven-project-info-reports, as a work-around to MCHANGES-88.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractChangesReport
    extends AbstractMavenReport
{
    /**
     * The current project base directory.
     *
     * @since 2.10
     */
    @Parameter( property = "basedir", required = true )
    protected String basedir;

    /**
     * Report output directory. Note that this parameter is only relevant if the goal is run from the command line or
     * from the default build lifecycle. If the goal is run indirectly as part of a site generation, the output
     * directory configured in the Maven Site Plugin is used instead.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}" )
    private File outputDirectory;

    /**
     * Report output encoding. Note that this parameter is only relevant if the goal is run from the command line or
     * from the default build lifecycle. If the goal is run indirectly as part of a site generation, the output encoding
     * configured in the Maven Site Plugin is used instead.
     *
     * @since 2.4
     */
    @Parameter( property = "outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String outputEncoding;

    /**
     * This will cause the execution to be run only at the top of a given module tree. That is, run in the project
     * contained in the same folder where the mvn execution was launched.
     *
     * @since 2.10
     */
    @Parameter( property = "changes.runOnlyAtExecutionRoot", defaultValue = "false" )
    protected boolean runOnlyAtExecutionRoot;

    /**
     * The Maven Session.
     *
     * @since 2.10
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession mavenSession;

    /**
     * Doxia Site Renderer.
     */
    @Component
    protected Renderer siteRenderer;

    /**
     * The Maven Project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     * Internationalization.
     */
    @Component
    protected I18N i18n;

    private File getSkinArtifactFile()
        throws MojoExecutionException
    {
        Skin skin = Skin.getDefaultSkin();
        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId( skin.getGroupId() );
        coordinate.setArtifactId( skin.getArtifactId() );
        coordinate.setVersion( skin.getVersion() );
        ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest( mavenSession.getProjectBuildingRequest() );
        pbr.setRemoteRepositories( project.getRemoteArtifactRepositories() );
        try
        {
            return resolver.resolveArtifact( pbr, coordinate ).getArtifact().getFile();
        }
        catch ( ArtifactResolverException e )
        {
            throw new MojoExecutionException( "Couldn't resolve the skin.", e );
        }
    }

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
            DecorationModel model = new DecorationModel();
            model.setBody( new Body() );
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put( "outputEncoding", getOutputEncoding() );
            Locale locale = Locale.getDefault();
            SiteRenderingContext siteContext = siteRenderer.createContextForSkin( getSkinArtifactFile(), attributes,
                                                                                  model, getName( locale ), locale );
            siteContext.setOutputEncoding( getOutputEncoding() );

            RenderingContext context = new RenderingContext( outputDirectory, getOutputName() + ".html" );

            SiteRendererSink sink = new SiteRendererSink( context );
            generate( sink, null, locale );

            outputDirectory.mkdirs();

            File file = new File( outputDirectory, getOutputName() + ".html" );
            writer = new OutputStreamWriter( new FileOutputStream( file ), getOutputEncoding() );

            siteRenderer.generateDocument( writer, sink, siteContext );

            writer.close();
            writer = null;

            siteRenderer.copyResources( siteContext, new File( project.getBasedir(), "src/site/resources" ),
                                        outputDirectory );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        finally
        {
            IOUtils.closeQuietly( writer );
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

    /**
     * Returns <code>true</code> if the current project is located at the Execution Root Directory (where mvn was
     * launched).
     *
     * @return <code>true</code> if the current project is at the Execution Root
     */
    protected boolean isThisTheExecutionRoot()
    {
        getLog().debug( "Root Folder:" + mavenSession.getExecutionRootDirectory() );
        getLog().debug( "Current Folder:" + basedir );
        boolean result = mavenSession.getExecutionRootDirectory().equalsIgnoreCase( basedir );
        if ( result )
        {
            getLog().debug( "This is the execution root." );
        }
        else
        {
            getLog().debug( "This is NOT the execution root." );
        }
        return result;
    }
}
