package org.apache.maven.plugins.repository;

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
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Packs artifacts already available in a local repository in a bundle for an
 * upload requests. It requires that the artifact has a POM in the local
 * repository. It will check for mandatory elements, asking interactively for
 * missing values. Can be used to generate bundles for third parties artifacts
 * that have been manually added to the local repository.
 *
 * @since 2.1
 */
@Mojo( name = "bundle-pack", requiresProject = false )
public class BundlePackMojo
    extends AbstractMojo
{
    public static final String POM = "pom.xml";

    /**
     * Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar" )
    protected JarArchiver jarArchiver;

    /**
     * Artifact resolver.
     */
    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * Artifact factory.
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * Local maven repository.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository localRepository;

    /**
     */
    @Component
    protected InputHandler inputHandler;

    /**
     * Directory where the upload-bundle will be created.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true )
    protected String basedir;

    /**
     * GroupId for the artifact to create an upload bundle for.
     */
    @Parameter( property = "groupId" )
    protected String groupId;

    /**
     * ArtifactId for the artifact to create an upload bundle for.
     */
    @Parameter( property = "artifactId" )
    protected String artifactId;

    /**
     * Version for the artifact to create an upload bundle for.
     */
    @Parameter( property = "version" )
    protected String version;

    /**
     * Viewable URL for SCM connections, in cases where this isn't provided by the POM.
     */
    @Parameter( property = "scmUrl" )
    protected String scmUrl;

    /**
     * Read-only URL for SCM tool connections, in cases where this isn't provided by the POM.
     * <br/>
     * <b>NOTE:</b> This should be a standard maven-scm URL. See the
     * <a href="http://maven.apache.org/scm/scm-url-format.html">format guidelines</a> for more
     * information.
     */
    @Parameter( property = "scmConnection" )
    protected String scmConnection;

    /**
     */
    @Component
    protected Settings settings;

    /**
     * Disable validations to make sure bundle supports project materialization.
     * <br/>
     * <b>WARNING: This means your project will be MUCH harder to use.</b>
     */
    @Parameter( property = "bundle.disableMaterialization", defaultValue = "false" )
    private boolean disableMaterialization;

    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException
    {
        readArtifactDataFromUser();

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

        Model model = readPom( pom );

        boolean rewrite = false;
        try
        {

            if ( model.getPackaging() == null )
            {
                model.setPackaging( "jar" );
                rewrite = true;
            }
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
            if ( model.getDescription() == null )
            {
                getLog().info( "Project description is missing, please type the project description:" );
                model.setDescription( inputHandler.readLine() );
                rewrite = true;
            }
            if ( model.getUrl() == null )
            {
                getLog().info( "Project URL is missing, please type the project URL:" );
                model.setUrl( inputHandler.readLine() );
                rewrite = true;
            }

            List<License> licenses = model.getLicenses();
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
            
            if ( disableMaterialization )
            {
                getLog().warn( "Validations to confirm support for project materialization have been DISABLED." +
                        "\n\nYour project may not provide the POM elements necessary to allow users to retrieve sources on-demand," +
                        "\nor to easily checkout your project in an IDE. THIS CAN SERIOUSLY INCONVENIENCE YOUR USERS." +
                        "\n\nContinue? [y/N]" );
                
                try
                {
                    if ( 'y' != inputHandler.readLine().toLowerCase().charAt( 0 ) )
                    {
                        disableMaterialization = false;
                    }
                }
                catch ( IOException e )
                {
                    getLog().debug( "Error reading confirmation: " + e.getMessage(), e );
                }
                
            }
            
            if ( !disableMaterialization )
            {
                Scm scm = model.getScm();
                if ( scm == null )
                {
                    scm = new Scm();
                    model.setScm( scm );
                }
                
                if ( scm.getUrl() == null )
                {
                    if ( scmUrl != null )
                    {
                        scm.setUrl( scmUrl );
                    }
                    else
                    {
                        getLog().info( "SCM view URL is missing, please type the URL for the viewable SCM interface:" );
                        scm.setUrl( inputHandler.readLine() );
                        rewrite = true;
                    }
                }
                
                if ( scm.getConnection() == null )
                {
                    if ( scmConnection != null )
                    {
                        scm.setConnection( scmConnection );
                    }
                    else
                    {
                        getLog().info( "SCM read-only connection URL is missing, please type the read-only SCM URL:" );
                        scm.setConnection( inputHandler.readLine() );
                        rewrite = true;
                    }
                }
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
                new MavenXpp3Writer().write( WriterFactory.newXmlWriter( pom ), model );
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
            
            boolean batchMode = settings == null ? false : !settings.isInteractiveMode();
            List<File> files = BundleUtils.selectProjectFiles( dir, inputHandler, finalName, pom, getLog(), batchMode );

            File bundle = new File( basedir, finalName + "-bundle.jar" );

            jarArchiver.addFile( pom, POM );

            boolean artifactChecks = !"pom".equals( model.getPackaging() );
            boolean sourcesFound = false;
            boolean javadocsFound = false;
            
            for ( File f : files )
            {
                if ( artifactChecks && f.getName().endsWith( finalName + "-sources.jar" ) )
                {
                    sourcesFound = true;
                }
                else if ( artifactChecks && f.getName().equals( finalName + "-javadoc.jar" ) )
                {
                    javadocsFound = true;
                }
                
                jarArchiver.addFile( f, f.getName() );
            }
            
            if ( artifactChecks && !sourcesFound )
            {
                getLog().warn( "Sources not included in upload bundle." );
            }

            if ( artifactChecks && !javadocsFound )
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

    /**
     * Read groupId, artifactId and version from the user on the command line,
     * if they were not provided as parameters.
     *
     * @throws MojoExecutionException If the values can't be read
     */
    private void readArtifactDataFromUser()
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
    }

    /**
     * Read the POM file.
     *
     * @param pom The file to read
     * @return A Maven Model
     * @throws MojoExecutionException if something goes wrong when reading the file
     */
    private Model readPom( File pom )
        throws MojoExecutionException
    {
        Model model;
        try
        {
            model = new MavenXpp3Reader().read( ReaderFactory.newXmlReader( pom ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Unable to parse POM at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to read POM at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to read POM at " + pom.getAbsolutePath() + ": " + e.getMessage(),
                                              e );
        }
        return model;
    }

}
