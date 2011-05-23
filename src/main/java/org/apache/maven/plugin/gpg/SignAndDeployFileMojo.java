package org.apache.maven.plugin.gpg;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Signs artifacts and installs the artifact in the remote repository.
 * 
 * @author Daniel Kulp
 * @goal sign-and-deploy-file
 * @requiresProject false
 * @threadSafe
 * @since 1.0-beta-4
 */
public class SignAndDeployFileMojo
    extends AbstractGpgMojo
{

    /**
     * The directory where to store signature files.
     * 
     * @parameter expression="${gpg.ascDirectory}"
     */
    private File ascDirectory;

    /**
     * Flag whether Maven is currently in online/offline mode.
     * 
     * @parameter default-value="${settings.offline}"
     * @readonly
     */
    private boolean offline;

    /**
     * GroupId of the artifact to be deployed. Retrieved from POM file if specified.
     * 
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * ArtifactId of the artifact to be deployed. Retrieved from POM file if specified.
     * 
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * Version of the artifact to be deployed. Retrieved from POM file if specified.
     * 
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * Type of the artifact to be deployed. Retrieved from POM file if specified.
     * Defaults to file extension if not specified via command line or POM.
     *
     * @parameter expression="${packaging}"
     */
    private String packaging;

    /**
     * Add classifier to the artifact
     * 
     * @parameter expression="${classifier}";
     */
    private String classifier;

    /**
     * Description passed to a generated POM file (in case of generatePom=true).
     * 
     * @parameter expression="${generatePom.description}"
     */
    private String description;

    /**
     * File to be deployed.
     * 
     * @parameter expression="${file}"
     * @required
     */
    private File file;

    /**
     * Location of an existing POM file to be deployed alongside the main artifact, given by the ${file} parameter.
     * 
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Upload a POM for this artifact. Will generate a default POM if none is supplied with the pomFile argument.
     * 
     * @parameter expression="${generatePom}" default-value="true"
     */
    private boolean generatePom;

    /**
     * Whether to deploy snapshots with a unique version or not.
     * 
     * @parameter expression="${uniqueVersion}" default-value="true"
     */
    private boolean uniqueVersion;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file:///C:/m2-repo or scp://host.com/path/to/repo )
     * 
     * @parameter expression="${url}"
     * @required
     */
    private String url;

    /**
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of <code>settings.xml</code>. In most cases, this
     * parameter will be required for authentication.
     * 
     * @parameter expression="${repositoryId}" default-value="remote-repository"
     * @required
     */
    private String repositoryId;

    /**
     * The type of remote repository layout to deploy to. Try <i>legacy</i> for a Maven 1.x-style repository layout.
     * 
     * @parameter expression="${repositoryLayout}" default-value="default"
     */
    private String repositoryLayout;

    /**
     * @component
     */
    private ArtifactDeployer deployer;

    /**
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Map that contains the layouts.
     * 
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * Component used to create an artifact
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Component used to create a repository
     * 
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * The component used to validate the user-supplied artifact coordinates.
     * 
     * @component
     */
    private ModelValidator modelValidator;

    /**
     * The default Maven project created when building the plugin
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     * @since 1.3
     */
    private MavenProject project;

    /**
     * Used for attaching the source and javadoc jars to the project.
     *
     * @component
     * @since 1.3
     */
    private MavenProjectHelper projectHelper;

    /**
     * The bundled API docs for the artifact.
     *
     * @parameter expression="${javadoc}"
     * @since 1.3
     */
    private File javadoc;

    /**
     * The bundled sources for the artifact.
     *
     * @parameter expression="${sources}"
     * @since 1.3
     */
    private File sources;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing.
     * If a value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     *
     * @parameter expression="${retryFailedDeploymentCount}" default-value="1"
     * @since 1.3
     */
    private int retryFailedDeploymentCount;

    /**
     * Parameter used to update the metadata to make the artifact as release.
     *
     * @parameter expression="${updateReleaseInfo}" default-value="false"
     * @since 1.3
     */
    protected boolean updateReleaseInfo;

    private void initProperties()
        throws MojoExecutionException
    {
        // Process the supplied POM (if there is one)
        if ( pomFile != null )
        {
            generatePom = false;

            Model model = readModel( pomFile );

            processModel( model );
        }

        if ( packaging == null && file != null )
        {
            packaging = FileUtils.getExtension( file.getName() );
        }
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        GpgSigner signer = newSigner( null );
        signer.setOutputDirectory( ascDirectory );
        signer.setBaseDirectory( new File( "" ).getAbsoluteFile() );

        if ( offline )
        {
            throw new MojoFailureException( "Cannot deploy artifacts when Maven is in offline mode" );
        }

        initProperties();

        validateArtifactInformation();

        if ( !file.exists() )
        {
            throw new MojoFailureException( file.getPath() + " not found." );
        }

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) repositoryLayouts.get( repositoryLayout );
        if ( layout == null )
        {
            throw new MojoFailureException( "Invalid repository layout: " + repositoryLayout );
        }

        ArtifactRepository deploymentRepository =
            repositoryFactory.createDeploymentArtifactRepository( repositoryId, url, layout, uniqueVersion );

        if ( StringUtils.isEmpty( deploymentRepository.getProtocol() ) )
        {
            throw new MojoFailureException( "No transfer protocol found." );
        }

        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );

        if ( file.equals( getLocalRepoFile( artifact ) ) )
        {
            throw new MojoFailureException( "Cannot deploy artifact from the local repository: " + file );
        }

        File fileSig = signer.generateSignatureForArtifact( file );
        ArtifactMetadata metadata = new AscArtifactMetadata( artifact, fileSig, false );
        artifact.addMetadata( metadata );

        if ( !"pom".equals( packaging ) )
        {
            if ( pomFile == null && generatePom )
            {
                pomFile = generatePomFile();
            }
            if ( pomFile != null )
            {
                metadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( metadata );

                fileSig = signer.generateSignatureForArtifact( pomFile );
                metadata = new AscArtifactMetadata( artifact, fileSig, true );
                artifact.addMetadata( metadata );
            }
        }

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        project.setArtifact( artifact );

        try
        {
            deploy( file, artifact, deploymentRepository, localRepository );
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        if ( sources != null )
        {
            projectHelper.attachArtifact( project, "jar", "sources", sources );
        }

        if ( javadoc != null )
        {
            projectHelper.attachArtifact( project, "jar", "javadoc", javadoc );
        }

        List attachedArtifacts = project.getAttachedArtifacts();

        for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
        {
            Artifact attached = (Artifact) i.next();

            fileSig = signer.generateSignatureForArtifact( attached.getFile() );
            attached = new AttachedSignedArtifact(attached, new AscArtifactMetadata( attached, fileSig, false ) );
            try
            {
                deploy( attached.getFile(), attached, deploymentRepository, localRepository );
            }
            catch ( ArtifactDeploymentException e )
            {
                throw new MojoExecutionException(
                    "Error deploying attached artifact " + attached.getFile() + ": " + e.getMessage(), e );
            }
        }

    }

    /**
     * Gets the path of the specified artifact within the local repository. Note that the returned path need not exist
     * (yet).
     * 
     * @param artifact The artifact whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact when installed, never <code>null</code>.
     */
    private File getLocalRepoFile( Artifact artifact )
    {
        String path = localRepository.pathOf( artifact );
        return new File( localRepository.getBasedir(), path );
    }

    /**
     * Process the supplied pomFile to get groupId, artifactId, version, and packaging
     *
     * @param model The POM to extract missing artifact coordinates from, must not be <code>null</code>.
     */
    private void processModel( Model model )
    {
        Parent parent = model.getParent();

        if ( this.groupId == null )
        {
            this.groupId = model.getGroupId();
            if ( this.groupId == null && parent != null )
            {
                this.groupId = parent.getGroupId();
            }
        }
        if ( this.artifactId == null )
        {
            this.artifactId = model.getArtifactId();
        }
        if ( this.version == null )
        {
            this.version = model.getVersion();
            if ( this.version == null && parent != null )
            {
                this.version = parent.getVersion();
            }
        }
        if ( this.packaging == null )
        {
            this.packaging = model.getPackaging();
        }
    }

    /**
     * Extract the model from the specified POM file.
     * 
     * @param pomFile The path of the POM file to parse, must not be <code>null</code>.
     * @return The model from the POM file, never <code>null</code>.
     * @throws MojoExecutionException If the file doesn't exist of cannot be read.
     */
    private Model readModel( File pomFile )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            return new MavenXpp3Reader().read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "POM not found " + pomFile, e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading POM " + pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing POM " + pomFile, e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Generates a minimal POM from the user-supplied artifact information.
     * 
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException If the generation failed.
     */
    private File generatePomFile()
        throws MojoExecutionException
    {
        Model model = generateModel();

        Writer fw = null;
        try
        {
            File tempFile = File.createTempFile( "mvndeploy", ".pom" );
            tempFile.deleteOnExit();

            fw = WriterFactory.newXmlWriter( tempFile );
            new MavenXpp3Writer().write( fw, model );

            return tempFile;
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

    /**
     * Validates the user-supplied artifact information.
     * 
     * @throws MojoFailureException If any artifact coordinate is invalid.
     */
    private void validateArtifactInformation()
        throws MojoFailureException
    {
        Model model = generateModel();

        ModelValidationResult result = modelValidator.validate( model );

        if ( result.getMessageCount() > 0 )
        {
            throw new MojoFailureException( "The artifact information is incomplete or not valid:\n"
                + result.render( "  " ) );
        }
    }

    /**
     * Generates a minimal model from the user-supplied artifact information.
     *
     * @return The generated model, never <code>null</code>.
     */
    private Model generateModel()
    {
        Model model = new Model();

        model.setModelVersion( "4.0.0" );

        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( packaging );

        model.setDescription( description );

        return model;
    }

    /**
     * Deploy an artifact from a particular file.
     *
     * @param source the file to deploy
     * @param artifact the artifact definition
     * @param deploymentRepository the repository to deploy to
     * @param localRepository the local repository to install into
     * @throws ArtifactDeploymentException if an error occurred deploying the artifact
     */
    protected void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                           ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        int retryFailedDeploymentCount = Math.max( 1, Math.min( 10, this.retryFailedDeploymentCount ) );
        ArtifactDeploymentException exception = null;
        for ( int count = 0; count < retryFailedDeploymentCount; count++ )
        {
            try
            {
                if (count > 0)
                {
                    getLog().info(
                        "Retrying deployment attempt " + ( count + 1 ) + " of " + retryFailedDeploymentCount );
                }
                deployer.deploy( source, artifact, deploymentRepository, localRepository );
                for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
                {
                    ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                    getLog().info( "Metadata[" + metadata.getKey() + "].filename = " + metadata.getRemoteFilename());
                }
                exception = null;
            }
            catch ( ArtifactDeploymentException e )
            {
                if (count + 1 < retryFailedDeploymentCount) {
                    getLog().warn( "Encountered issue during deployment: " + e.getLocalizedMessage());
                    getLog().debug( e );
                }
                if ( exception == null )
                {
                    exception = e;
                }
            }
        }
        if ( exception != null )
        {
            throw exception;
        }
    }
}
