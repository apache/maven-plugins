package org.apache.maven.plugin.deploy;

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
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Deploys an artifact to remote repository.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 * @version $Id$
 * @goal deploy
 * @phase deploy
 */
public class DeployMojo
    extends AbstractDeployMojo
{

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * @parameter expression="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @readonly
     */
    private ArtifactRepository deploymentRepository;

    /**
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    /**
     * Parameter used to update the metadata to make the artifact as release.
     *
     * @parameter expression="${updateReleaseInfo}" default-value="false"
     */
    private boolean updateReleaseInfo;

    public void execute()
        throws MojoExecutionException
    {
        if ( deploymentRepository == null )
        {
            String msg = "Deployment failed: repository element was not specified in the pom inside" +
                " distributionManagement element";
            throw new MojoExecutionException( msg );
        }
        
        String protocol = deploymentRepository.getProtocol();
        
        if( protocol.equals( "scp" ) )
        {
                File sshFile = new File( System.getProperty( "user.home" ), ".ssh" );

                if( !sshFile.exists() )
                {
                        sshFile.mkdirs();
                }	
        }
        
        // Deploy the POM
        boolean isPomArtifact = "pom".equals( packaging );
        if ( !isPomArtifact )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        try
        {
            if ( isPomArtifact )
            {
                getDeployer().deploy( pomFile, artifact, deploymentRepository, getLocalRepository() );
            }
            else
            {
                File file = artifact.getFile();
                if ( file == null )
                {
                    throw new MojoExecutionException(
                        "The packaging for this project did not assign a file to the build artifact" );
                }
                getDeployer().deploy( file, artifact, deploymentRepository, getLocalRepository() );
            }
            
            for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
            {	
                Artifact attached = (Artifact) i.next();
                
                getDeployer().deploy( attached.getFile(), attached, deploymentRepository, getLocalRepository() );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
