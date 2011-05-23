package org.apache.maven.plugin.deploy;

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
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @version $Id$
 */
public abstract class AbstractDeployMojo
    extends AbstractMojo
{
    /**
     * @component
     */
    private ArtifactDeployer deployer;

    /**
     * Component used to create an artifact.
     *
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Component used to create a repository.
     *
     * @component
     */
    ArtifactRepositoryFactory repositoryFactory;

    /**
     * Map that contains the layouts.
     *
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Flag whether Maven is currently in online/offline mode.
     * 
     * @parameter default-value="${settings.offline}"
     * @readonly
     */
    private boolean offline;

    /**
     * Parameter used to update the metadata to make the artifact as release.
     * 
     * @parameter expression="${updateReleaseInfo}" default-value="false"
     */
    protected boolean updateReleaseInfo;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing.
     * If a value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     *
     * @parameter expression="${retryFailedDeploymentCount}" default-value="1"
     * @since 2.7
     */
    private int retryFailedDeploymentCount;

    /* Setters and Getters */

    public ArtifactDeployer getDeployer()
    {
        return deployer;
    }

    public void setDeployer( ArtifactDeployer deployer )
    {
        this.deployer = deployer;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    void failIfOffline()
        throws MojoFailureException
    {
        if ( offline )
        {
            throw new MojoFailureException( "Cannot deploy artifacts when Maven is in offline mode" );
        }
    }

    ArtifactRepositoryLayout getLayout( String id )
        throws MojoExecutionException
    {
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) repositoryLayouts.get( id );

        if ( layout == null )
        {
            throw new MojoExecutionException( "Invalid repository layout: " + id );
        }

        return layout;
    }

    protected void deploy( File file1, Artifact attached, ArtifactRepository deploymentRepository,
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
                    getLog().info( "Retrying deployment attempt " + (count + 1) + " of " + retryFailedDeploymentCount );
                }
                getDeployer().deploy( file1, attached, deploymentRepository, localRepository );
                exception = null;
            }
            catch ( ArtifactDeploymentException e )
            {
                if (count + 1 < retryFailedDeploymentCount) {
                    getLog().warn( "Something went wrong with the deployment, will try again", e );
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
