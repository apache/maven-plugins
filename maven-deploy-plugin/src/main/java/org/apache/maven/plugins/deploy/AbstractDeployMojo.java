package org.apache.maven.plugins.deploy;

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

import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @version $Id$
 */
public abstract class AbstractDeployMojo
    extends AbstractMojo
{

    /**
     * Map that contains the layouts.
     */
    @Component( role = ArtifactRepositoryLayout.class )
    private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    /**
     * Flag whether Maven is currently in online/offline mode.
     */
    @Parameter( defaultValue = "${settings.offline}", readonly = true )
    private boolean offline;

    /**
     * Parameter used to update the metadata to make the artifact as release.
     */
    @Parameter( property = "updateReleaseInfo", defaultValue = "false" )
    protected boolean updateReleaseInfo;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     * 
     * @since 2.7
     */
    @Parameter( property = "retryFailedDeploymentCount", defaultValue = "1" )
    private int retryFailedDeploymentCount;

    /**
     * Parameter used to control how much time (in seconds) a failed deployment will be retried after a failed
     * connection.
     *
     * @since 2.9
     */
    @Parameter( property = "retryFailedDeploymentDelay", defaultValue = "20" )
    private int retryFailedDeploymentDelay;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;
    
    /* Setters and Getters */

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
        ArtifactRepositoryLayout layout = repositoryLayouts.get( id );

        if ( layout == null )
        {
            throw new MojoExecutionException( "Invalid repository layout: " + id );
        }

        return layout;
    }

    boolean isUpdateReleaseInfo()
    {
        return updateReleaseInfo;
    }

    int getRetryFailedDeploymentCount()
    {
        return retryFailedDeploymentCount;
    }

    int getRetryFailedDeploymentDelay()
    {
        return retryFailedDeploymentDelay;
    }

    protected ArtifactRepository createDeploymentArtifactRepository( String id, String url,
                                                                     ArtifactRepositoryLayout layout )
    {
        return new MavenArtifactRepository( id, url, layout, new ArtifactRepositoryPolicy(),
                                            new ArtifactRepositoryPolicy() );
    }
    
    protected final MavenSession getSession()
    {
        return session;
    }
}
