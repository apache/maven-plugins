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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.ide.IdeDependency;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Add eclipse artifacts from an eclipse installation to the local repo. This mojo automatically analize the eclipse
 * directory, copy plugins jars to the local maven repo, and generates appropriate poms (with packaging set to
 * <code>eclipse-plugin</code>).
 * 
 * @author Fabrizio Giustina
 * @version $Id$
 * @goal make-artifacts
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

            Manifest manifest;
            try
            {
                JarFile jar = new JarFile( file );
                manifest = jar.getManifest();
                jar.close();
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to read manifest for jar " + file.getAbsolutePath() );
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

            // @todo could be i18n! Need to be read from plugin.properties
            // Bundle-Name: %pluginName
            String name = manifestEntries.getValue( "Bundle-Name" );

            String requireBundle = manifestEntries.getValue( "Require-Bundle" );
            IdeDependency[] deps = parseDependencies( requireBundle );

            String groupId = null;
            groupId = createGroupId( artifactId );

            StringBuffer pom = new StringBuffer();
            pom
                .append( "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                    + "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\r\n"
                    + "  <modelVersion>4.0.0</modelVersion>\r\n" + "  <groupId>" );
            pom.append( groupId );
            pom.append( "</groupId>\r\n" + "  <artifactId>" );
            pom.append( artifactId );
            pom.append( "</artifactId>\r\n" + "  <packaging>eclipse-plugin</packaging>\r\n" + "  <version>" );

            pom.append( version );
            pom.append( "</version>\r\n" );
            pom.append( "  <name>" );
            pom.append( name );
            pom.append( "</name>\r\n" );

            if ( deps.length > 0 )
            {
                pom.append( "  <dependencies>\r\n" );
                for ( int k = 0; k < deps.length; k++ )
                {
                    IdeDependency dep = deps[k];
                    pom.append( "    <dependency>\r\n" );

                    pom.append( "      <groupId>" );
                    pom.append( dep.getGroupId() );
                    pom.append( "</groupId>\r\n" );

                    pom.append( "      <artifactId>" );
                    pom.append( dep.getArtifactId() );
                    pom.append( "</artifactId>\r\n" );

                    pom.append( "      <version>" );
                    pom.append( dep.getVersion() );
                    pom.append( "</version>\r\n" );

                    pom.append( "    </dependency>\r\n" );
                }
                pom.append( "  </dependencies>\r\n" );
            }

            pom.append( "</project>" );

            File repo = new File( localRepository.getBasedir(), StringUtils.replace( groupId, ".", "/" ) + "/"
                + artifactId + "/" + version );
            repo.mkdirs();

            File destination = new File( repo, artifactId + "-" + version + ".jar" );
            try
            {
                FileUtils.copyFile( file, destination );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to copy file " + file.getAbsolutePath() + " to "
                    + destination.getAbsolutePath() );
            }
            try
            {
                FileUtils.fileWrite( new File( repo, artifactId + "-" + version + ".pom" ).getAbsolutePath(), pom
                    .toString() );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Unable to write pom" );
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
        if ( StringUtils.countMatches( artifactId, "." ) > 2 )
        {
            return StringUtils.substring( artifactId, 0, artifactId.indexOf( ".", artifactId.indexOf( "." ) + 1 ) );
        }
        return artifactId;
    }

    /**
     * Parses the "Require-Bundle" and convert it to a list of dependencies.
     * @param requireBundle "Require-Bundle" entry
     * @return an array of <code>IdeDependency</code>
     */
    protected IdeDependency[] parseDependencies( String requireBundle )
    {
        if ( requireBundle == null )
        {
            return new IdeDependency[0];
        }

        List dependencies = new ArrayList();

        // first split
        String[] splitAtComma = StringUtils.split( requireBundle, "," );
        ArrayList bundles = new ArrayList();

        // not so easy, comma can also be contained in quoted string... find them and concatenate them back
        for ( int j = 0; j < splitAtComma.length; j++ )
        {
            String string = splitAtComma[j];
            if ( StringUtils.countMatches( string, "\"" ) == 1 )
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

            IdeDependency dep = new IdeDependency( createGroupId( artifactId ), artifactId, version, false, false,
                                                   false, false, false, null, "eclipse-plugin" );
            dependencies.add( dep );

        }

        return (IdeDependency[]) dependencies.toArray( new IdeDependency[dependencies.size()] );

    }
}
