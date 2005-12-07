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

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Installs the artifact in the remote repository.
 * 
 * @goal deploy-file
 * @requiresProject false 
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class DeployFileMojo
	extends AbstractDeployMojo
{
    /**
     * GroupId of the artifact to be deployed.
     * 
     * @parameter expression="${groupId}"
     * @required
     */
    private String groupId;

    /**
     * ArtifactId of the artifact to be deployed.
     * 
     * @parameter expression="${artifactId}"
     * @required
     */
    private String artifactId;

    /**
     * Version of the artifact to be deployed.
     * 
     * @parameter expression="${version}"
     * @required
     */
    private String version;

    /**
     * Type of the artifact to be deployed.
     * 
     * @parameter expression="${packaging}"
     * @required
     */
    private String packaging;	
	
	/**
	 * File to be deployed.
	 * 
	 * @parameter expression="${file}"
	 * @required
	 */
	private File file;
	
	/**
	 * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of settings.xml
	 * 
	 * @parameter expression="${repositoryId}"
	 * @required
	 */
	private String repositoryId;
	
	/**
	 * URL where the artifact will be deployed. <br/>
	 * ie ( file://C:\m2-repo )
	 * 
	 * @parameter expression="${url}"
	 * @required
	 */
	private String url;
	
    /**
     * @component
     */
    private ArtifactFactory artifactFactory;	
    
    /**
     * @component
     */
    private ArtifactRepositoryLayout layout;    
    
    /**
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;
	
	public void execute() throws MojoExecutionException
	{
		try
		{	
			Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, packaging );
			
			ArtifactRepository deploymentRepository = 
				repositoryFactory.createDeploymentArtifactRepository( repositoryId, url, layout, false );
			
	        if ( file == null )
	        {
	            throw new MojoExecutionException(
	                "The packaging for this project did not assign a file to the build artifact" );
	        }
	        else
	        {
	        	if( file.exists() )
	        	{
	        		getDeployer().deploy( file, artifact, deploymentRepository, getLocalRepository() );	
	        	}
	        }
		}
		catch( ArtifactDeploymentException e )
		{
            throw new MojoExecutionException( e.getMessage(), e );
		}
	}
}
