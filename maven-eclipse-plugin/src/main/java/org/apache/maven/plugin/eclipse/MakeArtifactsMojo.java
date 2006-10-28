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
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Add eclipse artifacts from an eclipse installation to the local repo. This mojo automatically analize the eclipse
 * directory, copy plugins jars to the local maven repo, and generates appropriate poms (with packaging set to
 * <code>eclipse-plugin</code>).
 * 
 * @author Fabrizio Giustina
 * @version $Id$
 * @goal make-artifacts
 * @requiresProject false
 */
public class MakeArtifactsMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * A pattern the <code>deployTo</code> param should match.
     */
    private static final Pattern DEPLOYTO_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" );

    /**
     * A pattern for a 3 digit osgi version number.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile( "(([0-9]+\\.)+[0-9]+)" );

    /**
     * Plexus container, needed to manually lookup components for deploy of artifacts.
     */
    private PlexusContainer container;

    /**
     * Local maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * ArtifactFactory component.
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * ArtifactInstaller component.
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * ArtifactDeployer component.
     * @component
     */
    private ArtifactDeployer deployer;

    /**
     * Eclipse installation dir. If not set, a value for this parameter will be asked on the command line.
     * 
     * @parameter expression="${eclipseDir}"
     */
    private File eclipseDir;

    /**
     * Input handler, needed for comand line handling.
     * @component
     */
    protected InputHandler inputHandler;

    /**
     * Strip qualifier (fourth token) from the plugin version. Qualifiers are for eclipse plugin the equivalent of
     * timestamped snapshot versions for Maven, but the date is maintained also for released version (e.g. a jar 
     * for the release <code>3.2</code> can be named <code>org.eclipse.core.filesystem_1.0.0.v20060603.jar</code>.
     * It's usually handy to not to include this qualifier when generating maven artifacts for major releases, while
     * it's needed when working with eclipse integration/nightly builds.
     * 
     * @parameter expression="${stripQualifier}" default-value="true"
     */
    private boolean stripQualifier;

    /**
     * Specifies a remote repository to which generated artifacts should be deployed to. If this property is specified,
     * artifacts are also deployed to the remote repo.
     * The format for this parameter is <code>id::layout::url</code>
     * 
     * @parameter expression="${deployTo}"
     */
    private String deployTo;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( eclipseDir == null )
        {
            getLog().info( "Eclipse directory? " );

            String eclipseDirString;
            try
            {
                eclipseDirString = inputHandler.readLine();
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to read from standard input" );
            }
            eclipseDir = new File( eclipseDirString );
        }

        if ( !eclipseDir.isDirectory() )
        {
            throw new MojoFailureException( "Directory " + eclipseDir.getAbsolutePath() + " doesn't exists" );
        }

        File pluginDir = new File( eclipseDir, "plugins" );

        if ( !pluginDir.isDirectory() )
        {
            throw new MojoFailureException( "Plugin directory " + pluginDir.getAbsolutePath() + " doesn't exists" );
        }

        File[] files = pluginDir.listFiles();

        ArtifactRepository remoteRepo = resolveRemoteRepo();

        if ( remoteRepo != null )
        {
            getLog().info( "Will deploy artifacts to remote repository " + deployTo );
        }

        for ( int j = 0; j < files.length; j++ )
        {
            processSingleFile( files[j], remoteRepo );

        }

    }

    /**
     * Process a single plugin jar/dir found in the target dir.
     * @param file plugin jar or dir
     * @param remoteRepo remote repository (if set)
     * @throws MojoExecutionException if anything bad happens while parsing files
     */
    private void processSingleFile( File file, ArtifactRepository remoteRepo )
        throws MojoExecutionException
    {

        getLog().info( "Processing file " + file.getAbsolutePath() );

        Manifest manifest = null;
        Properties pluginProperties = new Properties();
        boolean wasUnpacked = false;

        // package directories in a temp jar
        if ( file.isDirectory() )
        {
            try
            {
                File manifestFile = new File( file, "META-INF/MANIFEST.MF" );
                if ( !manifestFile.exists() )
                {
                    getLog().warn(
                                   "Plugin in folder " + file.getAbsolutePath()
                                       + " does not have a manifest; skipping.." );
                    return;
                }

                File tmpJar = File.createTempFile( "mvn-eclipse", null );
                tmpJar.deleteOnExit();

                JarArchiver jarArchiver = new JarArchiver();

                jarArchiver.setDestFile( tmpJar );
                jarArchiver.addDirectory( file );
                jarArchiver.setManifest( manifestFile );
                jarArchiver.createArchive();

                file = tmpJar;
                wasUnpacked = true;
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Unable to jar plugin in folder " + file.getAbsolutePath(), e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to jar plugin in folder " + file.getAbsolutePath(), e );
            }
        }

        if ( file.getName().endsWith( ".jar" ) || wasUnpacked )
        {
            try
            {
                JarFile jar = new JarFile( file );
                manifest = jar.getManifest();
                pluginProperties = loadPluginProperties( jar );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to read manifest or plugin properties for "
                    + file.getAbsolutePath() );
            }
        }
        else
        {
            getLog().debug( "Ignoring file " + file.getAbsolutePath() );
            return;
        }

        if ( manifest == null )
        {
            getLog().warn( "Jar " + file.getAbsolutePath() + " does not have a manifest; skipping.." );
            return;
        }

        Attributes manifestEntries = manifest.getMainAttributes();

        String artifactId = manifestEntries.getValue( "Bundle-SymbolicName" );

        int separator = artifactId.indexOf( ";" );
        if ( separator > 0 )
        {
            artifactId = StringUtils.substring( artifactId, 0, separator );
        }
        artifactId = StringUtils.trim( artifactId );

        String version = manifestEntries.getValue( "Bundle-Version" );

        if ( artifactId == null || version == null )
        {
            getLog().error( "Unable to read artifact/version from manifest, skipping..." );
            return;
        }

        if ( stripQualifier && StringUtils.countMatches( version, "." ) > 2 )
        {
            version = StringUtils.substring( version, 0, version.lastIndexOf( "." ) );
        }

        String name = manifestEntries.getValue( "Bundle-Name" );

        // if Bundle-Name is %pluginName fetch the full name from plugin.properties
        if ( name != null && name.startsWith( "%" ) )
        {
            String nameFromProperties = pluginProperties.getProperty( name.substring( 1 ) );
            if ( nameFromProperties != null )
            {
                name = nameFromProperties;
            }
        }

        String requireBundle = manifestEntries.getValue( "Require-Bundle" );
        Dependency[] deps = parseDependencies( requireBundle, !this.stripQualifier );

        String groupId = null;
        groupId = createGroupId( artifactId );

        Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setName( name );
        model.setVersion( version );

        /* set the pom property to install unpacked if it was unpacked */
        if ( wasUnpacked )
        {
            Properties properties = new Properties();
            properties.setProperty( InstallPluginsMojo.PROP_UNPACK_PLUGIN, Boolean.TRUE.toString() );
            model.setProperties( properties );
        }

        if ( groupId.startsWith( "org.eclipse" ) )
        {
            // why do we need a parent?

            // Parent parent = new Parent();
            // parent.setGroupId( "org.eclipse" );
            // parent.setArtifactId( "eclipse" );
            // parent.setVersion( "1" );
            // model.setParent( parent );

            // infer license for know projects, everything at eclipse is licensed under EPL
            // maybe too simplicistic, but better than nothing
            License license = new License();
            license.setName( "Eclipse Public License - v 1.0" );
            license.setUrl( "http://www.eclipse.org/org/documents/epl-v10.html" );
            model.addLicense( license );
        }

        if ( deps.length > 0 )
        {
            for ( int k = 0; k < deps.length; k++ )
            {
                model.getDependencies().add( deps[k] );
            }

        }

        FileWriter fw = null;
        ArtifactMetadata metadata = null;
        File pomFile = null;
        Artifact pomArtifact = artifactFactory.createArtifact( groupId, artifactId, version, null, "pom" );
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, "jar" );
        try
        {
            pomFile = File.createTempFile( "pom", ".xml" );
            pomFile.deleteOnExit();

            fw = new FileWriter( pomFile );
            pomFile.deleteOnExit();
            new MavenXpp3Writer().write( fw, model );
            metadata = new ProjectArtifactMetadata( pomArtifact, pomFile );
            pomArtifact.addMetadata( metadata );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing temporary pom file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( fw );
        }

        if ( remoteRepo != null )
        {

            try
            {
                deployer.deploy( pomFile, pomArtifact, remoteRepo, localRepository );
                deployer.deploy( pomFile, artifact, remoteRepo, localRepository );
            }
            catch ( ArtifactDeploymentException e )
            {
                throw new MojoExecutionException( "Unable to deploy artifact to repository.", e );
            }
        }
        try
        {
            installer.install( pomFile, pomArtifact, localRepository );
            installer.install( file, artifact, localRepository );
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Unable to install artifact to repository.", e );
        }

    }

    /**
     * Resolves the deploy<code>deployTo</code> parameter to an <code>ArtifactRepository</code> instance (if set).
     * 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     * @return ArtifactRepository instance of null if <code>deployTo</code> is not set.
     */
    private ArtifactRepository resolveRemoteRepo()
        throws MojoFailureException, MojoExecutionException
    {
        if ( deployTo != null )
        {
            Matcher matcher = DEPLOYTO_PATTERN.matcher( deployTo );

            if ( !matcher.matches() )
            {
                throw new MojoFailureException( deployTo, "Invalid syntax for repository.",
                                                "Invalid syntax for remote repository. Use \"id::layout::url\"." );
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String layout = matcher.group( 2 ).trim();
                String url = matcher.group( 3 ).trim();

                ArtifactRepositoryLayout repoLayout;
                try
                {
                    repoLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
                }
                catch ( ComponentLookupException e )
                {
                    throw new MojoExecutionException( "Cannot find repository layout: " + layout, e );
                }

                return new DefaultArtifactRepository( id, url, repoLayout );
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Get the group id as the three first tokens in artifacts Id (e.g. <code>org.eclipse.jdt</code>)
     * @param artifactId artifact id
     * @return group id
     */
    String createGroupId( String artifactId )
    {
        if ( StringUtils.countMatches( artifactId, "." ) > 1 )
        {
            StringTokenizer st = new StringTokenizer( artifactId, "." );
            int i = 0;
            String groupId = "";
            while ( st.hasMoreTokens() && ( i < 3 ) )
            {
                groupId += "." + st.nextToken();
                i++;
            }
            return groupId.substring( 1 );
        }
        return artifactId;
    }

    /**
     * Parses the "Require-Bundle" and convert it to a list of dependencies.
     * @param requireBundle "Require-Bundle" entry
     * @param addQualifier if true, a 4th version digit is added to dependency versions. This is required for maven to
     * allow dependencies with a qualifier to match the version range.
     * @return an array of <code>Dependency</code>
     */
    protected Dependency[] parseDependencies( String requireBundle, boolean addQualifier )
    {
        if ( requireBundle == null )
        {
            return new Dependency[0];
        }

        List dependencies = new ArrayList();

        // first split
        String[] splitAtComma = StringUtils.split( requireBundle, "," );
        ArrayList bundles = new ArrayList();

        // not so easy, comma can also be contained in quoted string... find them and concatenate them back
        for ( int j = 0; j < splitAtComma.length; j++ )
        {
            String string = splitAtComma[j];
            if ( StringUtils.countMatches( string, "\"" ) % 2 != 0 )
            {
                j++;
                bundles.add( string + "," + splitAtComma[j] );
                continue;
            }
            bundles.add( string );
        }

        // now iterates on bundles and extract dependencies
        for ( Iterator iter = bundles.iterator(); iter.hasNext(); )
        {
            String[] bundleTokens = StringUtils.split( (String) iter.next(), ";" );

            String artifactId = bundleTokens[0];
            String version = null;
            for ( int k = 1; k < bundleTokens.length; k++ )
            {
                String string = bundleTokens[k];
                if ( string.startsWith( "bundle-version=" ) )
                {
                    version = StringUtils.strip( StringUtils.substring( string, string.indexOf( "=" ) + 1 ), "\"" );
                }
            }

            if ( addQualifier )
            {
                version = addQualifierToVersionsInRange( version );
            }

            if ( version == null )
            {
                getLog().info( "Missing version for artifact " + artifactId + ", assuming any version > 0" );
                version = "[0.0.0.0,)";
            }

            Dependency dep = new Dependency();
            dep.setArtifactId( artifactId );
            dep.setGroupId( createGroupId( artifactId ) );
            dep.setVersion( version );

            dependencies.add( dep );

        }

        return (Dependency[]) dependencies.toArray( new Dependency[dependencies.size()] );

    }

    /**
     * Adds a qualifier (4th digit) to each version in a range. This is needed in maven poms in order to make dependency
     * ranges work when using qualifiers while generating artifacts.
     * In maven the range <code>[3.2.0,4.0.0)</code> doesn't match version <code>3.2.100.v20060905</code>, while
     * <code>[3.2.0.0,4.0.0.0)</code> do.
     * @param versionRange input range
     * @return modified version range
     */
    protected String addQualifierToVersionsInRange( String versionRange )
    {
        StringBuffer newVersionRange = new StringBuffer();

        Matcher matcher = VERSION_PATTERN.matcher( versionRange );

        while ( matcher.find() )
        {
            String currentVersion = matcher.group();
            int digitsToAdd = 3 - StringUtils.countMatches( currentVersion, "." );
            if ( digitsToAdd > 0 )
            {
                matcher.appendReplacement( newVersionRange, matcher.group() + ".0" );
            }
        }

        matcher.appendTail( newVersionRange );

        return newVersionRange.toString();
    }

    /**
     * Loads the plugin.properties file from a jar, usually needed in order to resolve the artifact name.
     * @param file jar file
     * @return loaded Properties (or an empty properties if no plugin.properties is found)
     * @throws IOException for exceptions while reading the jar file
     */
    private Properties loadPluginProperties( JarFile file )
        throws IOException
    {
        InputStream pluginPropertiesStream = null;
        try
        {
            Properties pluginProperties = new Properties();
            ZipEntry jarEntry = file.getEntry( "plugin.properties" );
            if ( jarEntry != null )
            {
                pluginPropertiesStream = file.getInputStream( jarEntry );
                pluginProperties.load( pluginPropertiesStream );
            }
            return pluginProperties;
        }
        finally
        {
            if ( pluginPropertiesStream != null )
            {
                pluginPropertiesStream.close();
            }
        }
    }

}
