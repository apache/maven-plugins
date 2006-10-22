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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.components.interactivity.InputHandler;
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
{

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

        File[] files = pluginDir.listFiles( new FilenameFilter()
        {

            public boolean accept( File dir, String name )
            {
                return name.endsWith( ".jar" );
            }
        } );

        for ( int j = 0; j < files.length; j++ )
        {

            File file = files[j];

            getLog().info( "Processing file " + file.getAbsolutePath() );

            JarFile jar = null;
            Manifest manifest;
            Properties pluginProperties = new Properties();
            try
            {
                jar = new JarFile( file );
                manifest = jar.getManifest();
                ZipEntry jarEntry = jar.getEntry( "plugin.properties" );
                if ( jarEntry != null )
                {
                    InputStream pluginPropertiesStream = jar.getInputStream( jarEntry );
                    pluginProperties.load( pluginPropertiesStream );
                }
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to read manifest for jar " + file.getAbsolutePath() );
            }
            finally
            {
                try
                {
                    // this also closes any opened input stream
                    jar.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }

            if ( manifest == null )
            {
                getLog().warn( "Jar " + file.getAbsolutePath() + " does not have a manifest; skipping.." );
                continue;
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
                continue;
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
            Dependency[] deps = parseDependencies( requireBundle );

            String groupId = null;
            groupId = createGroupId( artifactId );

            Model model = new Model();
            model.setModelVersion( "4.0.0" );
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setName( name );
            model.setVersion( version );
            model.setPackaging( "eclipse-plugin" );

            if ( groupId.startsWith( "org.eclipse" ) )
            {
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

            try
            {
                installer.install( pomFile, pomArtifact, localRepository );
                installer.install( file, artifact, localRepository );
            }
            catch ( ArtifactInstallationException e )
            {
                throw new MojoFailureException( "Unable to install artifact to local repository." );
            }

        }

    }

    /**
     * Get the group id as the two first tokens in artifacts Id (e.g. <code>org.eclipse</code>)
     * @param artifactId artifact id
     * @return group id
     */
    private String createGroupId( String artifactId )
    {
        if ( StringUtils.countMatches( artifactId, "." ) > 1 )
        {
            return StringUtils.substring( artifactId, 0, artifactId.indexOf( ".", artifactId.indexOf( "." ) + 1 ) );
        }
        return artifactId;
    }

    /**
     * Parses the "Require-Bundle" and convert it to a list of dependencies.
     * @param requireBundle "Require-Bundle" entry
     * @return an array of <code>Dependency</code>
     */
    protected Dependency[] parseDependencies( String requireBundle )
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

            if ( version == null )
            {
                getLog().warn( "Missing version for artifact " + artifactId + ", skipping" );
                continue;
            }

            Dependency dep = new Dependency();
            dep.setArtifactId( artifactId );
            dep.setGroupId( createGroupId( artifactId ) );
            dep.setVersion( version );

            dependencies.add( dep );

        }

        return (Dependency[]) dependencies.toArray( new Dependency[dependencies.size()] );

    }
}
