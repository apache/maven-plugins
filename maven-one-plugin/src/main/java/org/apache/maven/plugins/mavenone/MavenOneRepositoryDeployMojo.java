package org.apache.maven.plugins.mavenone;

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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Deploy the artifact in a Maven 1 remote repository.
 *
 * @goal deploy-maven-one-repository
 * @phase deploy
 */
public class MavenOneRepositoryDeployMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @component
     * @todo Write Javadoc for this
     */
    protected ArtifactDeployer deployer;

    /**
     * @component
     * @todo Write Javadoc for this
     */
    protected ArtifactRepositoryFactory factory;

    /**
     * The id to use in <code>settings.xml</code> if you want to configure server settings there.
     *
     * @parameter expression="${remoteRepositoryId}" default-value="mavenOneRemoteRepository"
     * @required
     */
    protected String remoteRepositoryId;

    /**
     * The URL to the remote repository.
     *
     * @parameter expression="${remoteRepositoryUrl}"
     * @required
     */
    protected String remoteRepositoryUrl;

    /**
     * Whether the remote repository uses a legacy layout or not.
     *
     * @component roleHint="legacy"
     */
    private ArtifactRepositoryLayout legacyLayout;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            ArtifactRepository deploymentRepository = factory.createDeploymentArtifactRepository( remoteRepositoryId,
                                                                                                  remoteRepositoryUrl,
                                                                                                  legacyLayout, false );

            boolean isPomArtifact = "pom".equals( packaging );

            if ( isPomArtifact )
            {
                deployer.deploy( pomFile, artifact, deploymentRepository, localRepository );
            }
            else
            {
                File file = artifact.getFile();
                if ( file == null )
                {
                    throw new MojoExecutionException(
                        "The packaging for this project did not assign a file to the build artifact" );
                }
                deployer.deploy( file, artifact, deploymentRepository, localRepository );
            }

            if ( attachedArtifacts != null && !attachedArtifacts.isEmpty() )
            {
                for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
                {
                    Artifact attached = (Artifact) i.next();

                    deployer.deploy( attached.getFile(), attached, deploymentRepository, localRepository );
                }
            }            
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
