package org.apache.maven.plugin.install;

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
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;

/**
 * Installs a file in local repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal install-file
 * @requiresProject false
 * @aggregator
 */
public class InstallFileMojo
    extends AbstractInstallMojo
{
    /**
     * GroupId of the artifact to be installed. Retrieved from POM file if specified.
     *
     * @parameter expression="${groupId}"
     */
    protected String groupId;

    /**
     * ArtifactId of the artifact to be installed. Retrieved from POM file if specified.
     *
     * @parameter expression="${artifactId}"
     */
    protected String artifactId;

    /**
     * Version of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${version}"
     */
    protected String version;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${packaging}"
     */
    protected String packaging;

    /**
     * Classifier type of the artifact to be installed.  For example, "sources" or "javadoc".
     * Defaults to none which means this is the project's main jar.
     *
     * @parameter expression="${classifier}"
     */
    protected String classifier;

    /**
     * The file to be deployed
     *
     * @parameter expression="${file}"
     * @required
     */
    private File file;

    /**
     * Location of an existing POM file to be deployed alongside the main
     * artifact, given by the ${file} parameter.
     *
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Install a POM for this artifact.  Will generate a default POM if none is
     * supplied with the pomFile argument.
     *
     * @parameter expression="${generatePom}" default-value="false"
     */
    private boolean generatePom;

    /**
     * Used to create artifacts
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Default repository layout
     *
     * @component roleHint="default"
     */
    private ArtifactRepositoryLayout repositoryLayout;

    /**
     * The path for a specific local repository directory. It will wrap into an <code>ArtifactRepository</code>
     * with <code>localRepoId</code> as <code>id</code> and with default <code>repositoryLayout</code>
     *
     * @parameter expression="${localRepositoryPath}"
     */
    private File localRepositoryPath;

    /**
     * The <code>id</code> for the <code>localRepo</code>
     *
     * @parameter expression="${localRepositoryId}"
     */
    private String localRepositoryId;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // ----------------------------------------------------------------------
        // Override the default localRepository variable
        // ----------------------------------------------------------------------
        if ( StringUtils.isNotEmpty( localRepositoryId ) && ( localRepositoryPath != null ) )
        {
            try
            {
                localRepository = new DefaultArtifactRepository( localRepositoryId, localRepositoryPath.toURL()
                    .toString(), repositoryLayout );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
            }
        }

        ArtifactMetadata metadata = null;

        Artifact pomArtifact = null;

        File pom = null;

        if ( pomFile != null && pomFile.exists() )
        {
            processModel( readPom( pomFile ) );

            pomArtifact = artifactFactory.createArtifact( groupId, artifactId, version, null, "pom" );
        }
        else //if pomFile is not provided check the groupId, artifactId, version and packaging
        {
            // Verify arguments
            if ( groupId == null || artifactId == null || version == null || packaging == null )
            {
                throw new MojoExecutionException( "Missing group, artifact, version, or packaging information" );
            }
        }

        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );

        // TODO: check if it exists first, and default to true if not
        if ( generatePom )
        {
            FileWriter fw = null;
            try
            {
                File tempFile = File.createTempFile( "mvninstall", ".pom" );
                tempFile.deleteOnExit();

                Model model = new Model();
                model.setModelVersion( "4.0.0" );
                model.setGroupId( groupId );
                model.setArtifactId( artifactId );
                model.setVersion( version );
                model.setPackaging( packaging );
                model.setDescription( "POM was created from install:install-file" );
                fw = new FileWriter( tempFile );
                tempFile.deleteOnExit();
                new MavenXpp3Writer().write( fw, model );
                metadata = new ProjectArtifactMetadata( artifact, tempFile );
                artifact.addMetadata( metadata );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error writing temporary pom file: " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( fw );
            }
        }

        // TODO: validate
        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        try
        {
            String localPath = localRepository.pathOf( artifact );

            File destination = new File( localRepository.getBasedir(), localPath );

            if ( !file.getPath().equals( destination.getPath() ) )
            {
                installer.install( file, artifact, localRepository );

                if ( createChecksum )
                {
                    if ( generatePom )
                    {
                        //create checksums for pom and artifact
                        pom = new File( localRepository.getBasedir(),
                                        localRepository.pathOfLocalRepositoryMetadata( metadata, localRepository ) );

                        installCheckSum( pom, true );
                    }
                    installCheckSum( file, artifact, false );
                }

                if ( pomFile != null && pomFile.exists() )
                {
                    installer.install( pomFile, pomArtifact, localRepository );

                    if ( createChecksum )
                    {
                        installCheckSum( pomFile, pomArtifact, false );
                    }
                }
            }
            else
            {
                throw new MojoFailureException(
                    "Cannot install artifact. Artifact is already in the local repository.\n\nFile in question is: " +
                        file + "\n" );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException(
                "Error installing artifact '" + artifact.getDependencyConflictId() + "': " + e.getMessage(), e );
        }
    }

    /**
     * @param aFile
     * @return the model from a file
     * @throws MojoExecutionException if any
     */
    private Model readPom( File aFile )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = new FileReader( aFile );

            MavenXpp3Reader mavenReader = new MavenXpp3Reader();

            return mavenReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "File not found " + aFile, e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading pom", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error reading pom", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private void processModel( Model model )
    {
        Parent parent = model.getParent();

        if ( this.groupId == null )
        {
            if ( parent != null && parent.getGroupId() != null )
            {
                this.groupId = parent.getGroupId();
            }
            if ( model.getGroupId() != null )
            {
                this.groupId = model.getGroupId();
            }
        }
        if ( this.artifactId == null && model.getArtifactId() != null )
        {
            this.artifactId = model.getArtifactId();
        }
        if ( this.version == null && model.getVersion() != null )
        {
            this.version = model.getVersion();
        }
        if ( this.packaging == null && model.getPackaging() != null )
        {
            this.packaging = model.getPackaging();
        }
    }
}
