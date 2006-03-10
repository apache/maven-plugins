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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Skin;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.net.URLClassLoader;
import java.net.URL;

/**
 * Base class for site mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractSiteMojo
    extends AbstractMojo
{
    /**
     * The locale by default for all default bundles
     */
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

    /**
     * A comma separated list of locales supported by Maven. The first valid token will be the default Locale
     * for this instance of the Java Virtual Machine.
     *
     * @parameter expression="${locales}"
     */
    private String locales;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

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

    protected static final String RESOURCE_DIR = "org/apache/maven/plugins/site";

    private static final String DEFAULT_TEMPLATE = RESOURCE_DIR + "/default-site.vm";

    /**
     * Directory containing the template page.
     *
     * @parameter expression="${templateDirectory}"
     */
    private File templateDirectory;

    /**
     * Default template page.
     *
     * @parameter expression="${template}"
     */
    private String template;

    /**
     * @parameter expression="${attributes}"
     */
    protected Map attributes;

    private static final String SKIN_TEMPLATE_LOCATION = "META-INF/maven/site.vm";

    /**
     * Directory which contains the resources for the site.
     *
     * @parameter expression="${basedir}/src/site/resources"
     * @required
     */
    protected File resourcesDirectory;

    /**
     * Get the path of the site descriptor for a given locale.
     *
     * @param locale the locale
     * @return the site descriptor path
     */
    protected File getSiteDescriptorFile( File basedir, Locale locale )
    {
        // TODO: get proper siteDirectory from site configuration of the project this relates to

        File siteDescriptor = new File( basedir, "src/site/site_" + locale.getLanguage() + ".xml" );

        if ( !siteDescriptor.exists() )
        {
            siteDescriptor = new File( basedir, "src/site/site.xml" );
        }
        return siteDescriptor;
    }

    /**
     * Init the <code>localesList</code> variable.
     * <p>If <code>locales</code> variable is available, the first valid token will be the <code>defaultLocale</code>
     * for this instance of the Java Virtual Machine.</p>
     *
     * @return a list of <code>Locale</code>
     */
    protected List initLocalesList()
    {
        List localesList = new ArrayList();
        if ( locales != null )
        {
            String[] localesArray = StringUtils.split( locales, "," );

            for ( int i = 0; i < localesArray.length; i++ )
            {
                Locale locale = codeToLocale( localesArray[i] );

                if ( locale != null )
                {
                    if ( !Arrays.asList( Locale.getAvailableLocales() ).contains( locale ) )
                    {
                        getLog().warn( "The locale parsed defined by '" + locale +
                            "' is not available in this Java Virtual Machine (" + System.getProperty( "java.version" ) +
                            " from " + System.getProperty( "java.vendor" ) + ") - IGNORING" );
                        continue;
                    }

                    // Default bundles are in English
                    if ( !locale.getLanguage().equals( DEFAULT_LOCALE.getLanguage() ) )
                    {
                        if ( !i18n.getBundle( "site-plugin", locale ).getLocale().getLanguage().equals( locale
                            .getLanguage() ) )
                        {
                            StringBuffer sb = new StringBuffer();

                            sb.append( "The locale '" ).append( locale ).append( "' (" );
                            sb.append( locale.getDisplayName( Locale.ENGLISH ) );
                            sb.append( ") is not currently support by Maven - IGNORING. " );
                            sb.append( "\n" );
                            sb.append( "Contribution are welcome and greatly appreciated! " );
                            sb.append( "\n" );
                            sb.append( "If you want to contribute a new translation, please visit " );
                            sb.append( "http://maven.apache.org/plugins/maven-site-plugin/i18n.html " );
                            sb.append( "for detailed instructions." );

                            getLog().warn( sb.toString() );

                            continue;
                        }
                    }

                    localesList.add( locale );
                }
            }
        }

        if ( localesList.isEmpty() )
        {
            localesList = Collections.singletonList( DEFAULT_LOCALE );
        }

        return localesList;
    }

    /**
     * Converts a locale code like "en", "en_US" or "en_US_win" to a <code>java.util.Locale</code>
     * object.
     * <p>If localeCode = <code>default</code>, return the current value of the default locale for this instance
     * of the Java Virtual Machine.</p>
     *
     * @param localeCode the locale code string.
     * @return a java.util.Locale object instancied or null if errors occurred
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Locale.html">java.util.Locale#getDefault()</a>
     */
    private Locale codeToLocale( String localeCode )
    {
        if ( localeCode == null )
        {
            return null;
        }

        if ( "default".equalsIgnoreCase( localeCode ) )
        {
            return Locale.getDefault();
        }

        String language = "";
        String country = "";
        String variant = "";

        StringTokenizer tokenizer = new StringTokenizer( localeCode, "_" );
        if ( tokenizer.countTokens() > 3 )
        {
            getLog().warn( "Invalid java.util.Locale format for '" + localeCode + "' entry - IGNORING" );
            return null;
        }

        if ( tokenizer.hasMoreTokens() )
        {
            language = tokenizer.nextToken();
            if ( tokenizer.hasMoreTokens() )
            {
                country = tokenizer.nextToken();
                if ( tokenizer.hasMoreTokens() )
                {
                    variant = tokenizer.nextToken();
                }
            }
        }

        return new Locale( language, country, variant );
    }

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

    protected SiteRenderingContext createSiteRenderingContext( File skinArtifactFile, Locale locale,
                                                               DecorationModel decoration )
        throws IOException, MojoExecutionException
    {
        boolean hasSiteTemplate = false;
        if ( template == null )
        {
            if ( skinArtifactFile != null && skinArtifactFile.exists() )
            {
                ZipFile zipFile = new ZipFile( skinArtifactFile );
                try
                {
                    if ( zipFile.getEntry( SKIN_TEMPLATE_LOCATION ) != null )
                    {
                        hasSiteTemplate = true;
                        template = SKIN_TEMPLATE_LOCATION;
                    }
                }
                finally
                {
                    zipFile.close();
                }
            }
        }

        if ( template == null )
        {
            template = DEFAULT_TEMPLATE;
        }

        ClassLoader templateClassLoader;

        if ( hasSiteTemplate )
        {
            templateClassLoader = new URLClassLoader( new URL[]{skinArtifactFile.toURL()} );
        }
        else if ( templateDirectory == null )
        {
            templateClassLoader = SiteMojo.class.getClassLoader();
        }
        else
        {
            if ( !templateDirectory.exists() )
            {
                throw new MojoExecutionException(
                    "This templateDirectory=[" + templateDirectory + "] doesn't exist." );
            }

            templateClassLoader = new URLClassLoader( new URL[]{templateDirectory.toURL()} );
        }

        SiteRenderingContext context = new SiteRenderingContext();
        context.setTemplate( template );
        context.setTemplateProperties( attributes );
        context.setLocale( locale );
        context.setTemplateClassLoader( templateClassLoader );
        context.setDecoration( decoration );
        context.setDefaultWindowTitle( project.getName() );
        return context;
    }

    protected File getSkinArtifactFile( DecorationModel decoration )
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

    /**
     * Copy Resources
     *
     * @param outputDir the output directory
     * @param skinFile
     * @throws java.io.IOException if any
     * @todo move to skin functionality in site renderer
     */
    protected void copyResources( File outputDir, File skinFile )
        throws IOException
    {
        InputStream resourceList = getStream( RESOURCE_DIR + "/resources.txt" );

        if ( resourceList != null )
        {
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( resourceList ) );

            String line = reader.readLine();

            while ( line != null )
            {
                InputStream is = getStream( RESOURCE_DIR + "/" + line );

                if ( is == null )
                {
                    throw new IOException( "The resource " + line + " doesn't exist." );
                }

                File outputFile = new File( outputDir, line );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream w = new FileOutputStream( outputFile );

                IOUtil.copy( is, w );

                IOUtil.close( is );

                IOUtil.close( w );

                line = reader.readLine();
            }
        }

        // TODO: plexus-archiver, if it could do the excludes
        ZipFile file = new ZipFile( skinFile );
        try
        {
            for ( Enumeration e = file.entries(); e.hasMoreElements(); )
            {
                ZipEntry entry = (ZipEntry) e.nextElement();

                if ( !entry.getName().startsWith( "META-INF/" ) )
                {
                    File destFile = new File( outputDir, entry.getName() );
                    if ( !entry.isDirectory() )
                    {
                        destFile.getParentFile().mkdirs();

                        FileOutputStream fos = new FileOutputStream( destFile );

                        try
                        {
                            IOUtil.copy( file.getInputStream( entry ), fos );
                        }
                        finally
                        {
                            IOUtil.close( fos );
                        }
                    }
                    else
                    {
                        destFile.mkdirs();
                    }
                }
            }
        }
        finally
        {
            file.close();
        }
    }

    /**
     * Get the resource as stream
     *
     * @param name
     * @return the inputstream
     */
    private InputStream getStream( String name )
    {
        return SiteMojo.class.getClassLoader().getResourceAsStream( name );
    }

    /**
     * Copy the directory
     *
     * @param source      source file to be copied
     * @param destination destination file
     * @throws java.io.IOException if any
     */
    protected void copyDirectory( File source, File destination )
        throws IOException
    {
        if ( source.exists() )
        {
            DirectoryScanner scanner = new DirectoryScanner();

            String[] includedResources = {"**/**"};

            scanner.setIncludes( includedResources );

            scanner.addDefaultExcludes();

            scanner.setBasedir( source );

            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                File sourceFile = new File( source, name );

                File destinationFile = new File( destination, name );

                FileUtils.copyFile( sourceFile, destinationFile );
            }
        }
    }
}
