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
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
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
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

    /**
     * Specifies the output encoding.
     *
     * @parameter expression="${outputEncoding}"
     * default-value="ISO-8859-1"
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
     * Directory which contains the resources for the site.
     *
     * @parameter expression="${basedir}/src/site/resources"
     * @required
     */
    protected File resourcesDirectory;

    /**
     * Site renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;

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

        MavenProject parentProject = project.getParent();
        if ( parentProject != null && project.getUrl() != null && parentProject.getUrl() != null )
        {
            props.put( "parentProject", getProjectParentMenu( locale ) );
        }
        else
        {
            props.put( "parentProject", "" );
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

    protected SiteRenderingContext createSiteRenderingContext( Locale locale, DecorationModel decoration,
                                                               Renderer siteRenderer )
        throws IOException, MojoExecutionException, MojoFailureException
    {
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

        File skinFile = getSkinArtifactFile( decoration );
        SiteRenderingContext context;
        if ( templateFile != null )
        {
            if ( !templateFile.exists() )
            {
                throw new MojoFailureException( "Template file '" + templateFile + "' does not exist" );
            }
            context = siteRenderer.createContextForTemplate( templateFile, attributes, decoration, locale,
                                                             project.getName(), skinFile, resourcesDirectory );
        }
        else
        {
            context = siteRenderer.createContextForSkin( skinFile, attributes, decoration, locale, project.getName(),
                                                         resourcesDirectory );
        }
        return context;
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

    protected void setDefaultAttributes()
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
    }
}
