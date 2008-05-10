package org.apache.maven.plugin.invoker;

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
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Installs the project artifacts into the local repository as a preparation to run the integration tests.
 * 
 * @goal install
 * @phase pre-integration-test
 * @since 1.2
 * 
 * @author Paul Gier
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class InstallMojo
    extends AbstractMojo
{

    /**
     * Maven artifact install component to copy artifacts to the local repository.
     * 
     * @component
     */
    private ArtifactInstaller installer;

    /**
     * The component used to create artifacts.
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * The component used to create artifacts.
     * 
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * The path to the local repository into which the project artifacts should be installed for the integration tests.
     * If not set, the regular local repository will be used.
     * 
     * @parameter expression="${invoker.localRepositoryPath}"
     */
    private File localRepositoryPath;

    /**
     * The current Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Performs this mojo's tasks.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactRepository testRepository = createTestRepository();
        installProjectArtifacts( testRepository );
    }

    /**
     * Installs the main project artifact and any attached artifacts to the local repository.
     * 
     * @param testRepository The local repository to install the artifacts into, must not be <code>null</code>.
     * @throws MojoExecutionException If any artifact could not be installed.
     */
    private void installProjectArtifacts( ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        try
        {
            // Install the pom
            Artifact pomArtifact =
                artifactFactory.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                                null, "pom" );
            installer.install( project.getFile(), pomArtifact, testRepository );

            // Install the main project artifact
            installer.install( project.getArtifact().getFile(), project.getArtifact(), testRepository );

            // Install any attached project artifacts
            Collection attachedArtifacts = project.getAttachedArtifacts();
            for ( Iterator artifactIter = attachedArtifacts.iterator(); artifactIter.hasNext(); )
            {
                Artifact theArtifact = (Artifact) artifactIter.next();
                installer.install( theArtifact.getFile(), theArtifact, testRepository );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Failed to install project artifacts", e );
        }
    }

    /**
     * Creates the local repository for the integration tests.
     * 
     * @return The local repository for the integration tests, never <code>null</code>.
     * @throws MojoExecutionException If the repository could not be created.
     */
    private ArtifactRepository createTestRepository()
        throws MojoExecutionException
    {
        ArtifactRepository testRepository = localRepository;

        if ( localRepositoryPath != null )
        {
            try
            {
                if ( !localRepositoryPath.exists() )
                {
                    localRepositoryPath.mkdirs();
                }

                testRepository =
                    repositoryFactory.createArtifactRepository( "it-repo", localRepositoryPath.toURL().toString(),
                                                                localRepository.getLayout(),
                                                                localRepository.getSnapshots(),
                                                                localRepository.getReleases() );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Failed to create local repository", e );
            }
        }

        return testRepository;
    }

}
