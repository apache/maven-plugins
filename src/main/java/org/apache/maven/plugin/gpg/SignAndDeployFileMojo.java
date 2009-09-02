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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.deploy.DeployFileMojo;
import org.apache.maven.settings.Settings;

/**
 * Signs artifacts and installs the artifact in the remote repository.
 * 
 * @author Daniel Kulp
 * @goal sign-and-deploy-file
 * @requiresProject false
 * @since 1.0-beta-4
 */
public class SignAndDeployFileMojo
    extends DeployFileMojo
{

    /**
     * The directory from which gpg will load keyrings. If not specified, gpg will use the value configured for its
     * installation, e.g. <code>~/.gnupg</code> or <code>%APPDATA%/gnupg</code>.
     * 
     * @parameter expression="${gpg.homedir}"
     * @since 1.0
     */
    private File homedir;

    /**
     * The passphrase to use when signing.
     * 
     * @parameter expression="${gpg.passphrase}"
     */
    private String passphrase;

    /**
     * The "name" of the key to sign with. Passed to gpg as --local-user.
     * 
     * @parameter expression="${gpg.keyname}"
     */
    private String keyname;

    /**
     * Passes --use-agent or --no-use-agent to gpg. If using an agent, the password is optional as the agent will
     * provide it.
     * 
     * @parameter expression="${gpg.useagent}" default-value="false"
     * @required
     */
    private boolean useAgent;

    /**
     * The directory where to store signature files.
     * 
     * @parameter expression="${gpg.asc.directory}"
     */
    private File ascDirectory;

    /**
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * Maven ArtifactHandlerManager
     * 
     * @component
     * @required
     * @readonly
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /*
     * stuff I need to copy since the plugin:plugin doesn't support inheritance outside the current jar
     */
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
     * 
     * @parameter expression="${packaging}"
     */
    private String packaging;

    /**
     * Description passed to a generated POM file (in case of generatePom=true)
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
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of settings.xml In most cases, this parameter
     * will be required for authentication.
     * 
     * @parameter expression="${repositoryId}" default-value="remote-repository"
     * @required
     */
    private String repositoryId;

    /**
     * The type of remote repository layout to deploy to. Try <i>legacy</i> for a Maven 1.x-style repository layout.
     * 
     * @parameter expression="${repositoryLayout}" default-value="default"
     * @required
     */
    private String repositoryLayout;

    /**
     * Map that contains the layouts
     * 
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file://C:\m2-repo or scp://host.com/path/to/repo )
     * 
     * @parameter expression="${url}"
     * @required
     */
    private String url;

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
     * Add classifier to the artifact
     * 
     * @parameter expression="${classifier}";
     */
    private String classifier;

    /**
     * Whether to deploy snapshots with a unique version or not.
     * 
     * @parameter expression="${uniqueVersion}" default-value="true"
     */
    private boolean uniqueVersion;

    private final GpgSigner signer = new GpgSigner();

    public void execute()
        throws MojoExecutionException
    {
        ArtifactHandler handler = new DefaultArtifactHandler( "asc" );
        Map map = new HashMap();
        map.put( "asc", handler );
        artifactHandlerManager.addHandlers( map );

        copyToParent();
        ArtifactDeployer deployer = getDeployer();
        signer.setInteractive( settings.isInteractiveMode() );
        signer.setKeyName( keyname );
        signer.setUseAgent( useAgent );
        signer.setOutputDirectory( ascDirectory );
        signer.setBaseDirectory( new File( "foo" ).getAbsoluteFile().getParentFile().getAbsoluteFile() );
        signer.setHomeDirectory( homedir );

        setDeployer( new SignedArtifactDeployer( deployer, passphrase ) );
        super.execute();
    }

    /*
     * this sucks. The plugin:plugin won't find properties in parent classes unless they exist in the same compilation
     * unit. Thus, we need to declare our own and copy them to the parent. HOWEVER, the DeployFileMojo doesn't have
     * public setters. Thus, we need to do crappy field copies.
     */
    private void copyToParent()
        throws MojoExecutionException
    {
        this.setDeployer( deployer );
        this.setLocalRepository( localRepository );

        setDeployFileMojoField( "groupId", groupId );
        setDeployFileMojoField( "artifactId", artifactId );
        setDeployFileMojoField( "version", version );
        setDeployFileMojoField( "packaging", packaging );
        setDeployFileMojoField( "description", description );
        setDeployFileMojoField( "file", file );
        setDeployFileMojoField( "repositoryId", repositoryId );
        setDeployFileMojoField( "repositoryLayout", repositoryLayout );
        setDeployFileMojoField( "repositoryLayouts", repositoryLayouts );
        setDeployFileMojoField( "url", url );
        setDeployFileMojoField( "artifactFactory", artifactFactory );
        setDeployFileMojoField( "repositoryFactory", repositoryFactory );
        setDeployFileMojoField( "pomFile", pomFile );
        setDeployFileMojoField( "generatePom", Boolean.valueOf( generatePom ) );
        setDeployFileMojoField( "classifier", classifier );
        setDeployFileMojoField( "uniqueVersion", Boolean.valueOf( uniqueVersion ) );
    }

    private void setDeployFileMojoField( String name, Object value )
        throws MojoExecutionException
    {
        try
        {
            Field f = DeployFileMojo.class.getDeclaredField( name );
            f.setAccessible( true );
            f.set( this, value );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not set field " + name, e );
        }
    }

    public void setDeployer( ArtifactDeployer deployer )
    {
        this.deployer = deployer;
        super.setDeployer( deployer );
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        super.setLocalRepository( localRepository );
    }

    private class SignedArtifactDeployer
        implements ArtifactDeployer
    {
        ArtifactDeployer deployer;

        String pass;

        public SignedArtifactDeployer( ArtifactDeployer dep, String passphrase )
            throws MojoExecutionException
        {
            deployer = dep;
            pass = passphrase;
            if ( !useAgent && null == pass )
            {
                if ( !settings.isInteractiveMode() )
                {
                    throw new MojoExecutionException( "Cannot obtain passphrase in batch mode" );
                }
                try
                {
                    pass = signer.getPassphrase( null );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Exception reading password", e );
                }
            }
        }

        public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                            ArtifactRepository localRepository )
            throws ArtifactDeploymentException
        {
            try
            {
                File fileSig = signer.generateSignatureForArtifact( source, pass );
                ArtifactMetadata metadata = new AscArtifactMetadata( artifact, fileSig, false );
                artifact.addMetadata( metadata );

                if ( !generatePom && pomFile != null )
                {
                    fileSig = signer.generateSignatureForArtifact( pomFile, pass );
                    metadata = new AscArtifactMetadata( artifact, fileSig, true );
                    artifact.addMetadata( metadata );
                }

                deployer.deploy( source, artifact, deploymentRepository, localRepository );
            }
            catch ( MojoExecutionException e )
            {
                throw new ArtifactDeploymentException( e.getMessage(), e );
            }

        }

        public void deploy( String basedir, String finalName, Artifact artifact,
                            ArtifactRepository deploymentRepository, ArtifactRepository localRepository )
            throws ArtifactDeploymentException
        {
            String extension = artifact.getArtifactHandler().getExtension();
            File source = new File( basedir, finalName + "." + extension );
            deploy( source, artifact, deploymentRepository, localRepository );
        }
    }

}
