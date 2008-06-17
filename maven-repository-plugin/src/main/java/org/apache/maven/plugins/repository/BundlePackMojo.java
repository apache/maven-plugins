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
package org.apache.maven.plugins.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Packs artifacts already available in a local repository in a bundle for an
 * upload requests. It requires that the artifact has a POM in the local
 * repository. It will check for mandatory elements, asking interactively for
 * missing values. Can be used to generate bundles for third parties artifacts
 * that have been manually added to the local repository.
 *
 * @goal bundle-pack
 * @requiresProject false
 * @since 2.1
 */
public class BundlePackMojo
    extends AbstractMojo
{
    public static final String POM = "pom.xml";

    /**
     * Jar archiver.
     * 
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    protected JarArchiver jarArchiver;

    /**
     * Artifact resolver.
     * 
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * Artifact factory.
     * 
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Local maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     */
    protected InputHandler inputHandler;

    /**
     * Directory where the upload-bundle will be created.
     *
     * @parameter expression="${basedir}"
     */
    protected String basedir;

    /**
     * GroupId for the artifact to create an upload bundle for.
     *
     * @parameter expression="${groupId}"
     */
    protected String groupId;

    /**
     * ArtifactId for the artifact to create an upload bundle for.
     *
     * @parameter expression="${artifactId}"
     */
    protected String artifactId;

    /**
     * Version for the artifact to create an upload bundle for.
     * 
     * @parameter expression="${version}"
     */
    protected String version;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            if ( groupId == null )
            {
                getLog().info( "groupId? " );

                groupId = inputHandler.readLine();

            }

            if ( artifactId == null )
            {
                getLog().info( "artifactId? " );
                artifactId = inputHandler.readLine();
            }

            if ( version == null )
            {
                getLog().info( "version? " );
                version = inputHandler.readLine();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        Artifact artifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        try
        {
            artifactResolver.resolve( artifact, Collections.EMPTY_LIST, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifact " + artifact.getId(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Artifact " + artifact.getId() + " not found in local repository", e );
        }

        File pom = artifact.getFile();

        File dir = pom.getParentFile();

        Model model;
        try
        {
            // TODO use ReaderFactory.newXmlReader() when plexus-utils is upgraded to 1.4.5+
            model = new MavenXpp3Reader().read( new InputStreamReader( new FileInputStream( pom ), "UTF-8" ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException(
                                              "Unable to parse pom at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to read pom at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to read pom at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }

        boolean rewrite = false;
        try
        {

            if ( model.getName() == null )
            {
                getLog().info( "Project name is missing, please type the project name [" + artifactId + "]:" );
                model.setName( inputHandler.readLine() );
                if ( model.getName() == null )
                {
                    model.setName( artifactId );
                }
                rewrite = true;
            }
            if ( model.getUrl() == null )
            {
                getLog().info( "Project Url is missing, please type the project URL:" );
                model.setUrl( inputHandler.readLine() );
                rewrite = true;
            }
            if ( model.getPackaging() == null )
            {
                model.setPackaging( "jar" );
                rewrite = true;
            }
            if ( model.getDescription() == null )
            {
                getLog().info( "Project Description is missing, please type the project Description:" );
                model.setDescription( inputHandler.readLine() );
                rewrite = true;
            }

            List licenses = model.getLicenses();
            if ( licenses.isEmpty() )
            {
                License license = new License();

                getLog().info( "License name is missing, please type the license name:" );
                license.setName( inputHandler.readLine() );
                getLog().info( "License URL is missing, please type the license URL:" );
                license.setUrl( inputHandler.readLine() );
                licenses.add( license );
                rewrite = true;
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        try
        {

            if ( rewrite )
            {
                // TODO use WriterFactory.newXmlWriter() when plexus-utils is upgraded to 1.4.5+
                new MavenXpp3Writer().write( new OutputStreamWriter( new FileOutputStream( pom ), "UTF-8" ), model );
            }

            String finalName = null;

            if ( model.getBuild() != null )
            {
                finalName = model.getBuild().getFinalName();
            }
            if ( finalName == null )
            {
                finalName = model.getArtifactId() + "-" + model.getVersion();
            }

            File mainArtifact = new File( dir, finalName + "." + model.getPackaging() );
            File sourceArtifact = new File( dir, finalName + "-sources.jar" );
            File javadocArtifact = new File( dir, finalName + "-javadoc.jar" );
            File bundle = new File( basedir, finalName + "-bundle.jar" );

            jarArchiver.addFile( pom, POM );

            jarArchiver.addFile( mainArtifact, mainArtifact.getName() );

            if ( sourceArtifact.exists() )
            {
                jarArchiver.addFile( sourceArtifact, sourceArtifact.getName() );
            }
            else
            {
                getLog().warn( "Sources not included in upload bundle." );
            }

            if ( javadocArtifact.exists() )
            {
                jarArchiver.addFile( javadocArtifact, javadocArtifact.getName() );
            }
            else
            {
                getLog().warn( "Javadoc not included in upload bundle." );
            }

            jarArchiver.setDestFile( bundle );

            jarArchiver.createArchive();

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }

}
