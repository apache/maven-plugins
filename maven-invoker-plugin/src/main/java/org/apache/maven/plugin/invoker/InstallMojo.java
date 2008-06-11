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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
 * Installs the project artifacts into the local repository as a preparation to run the integration tests. More
 * precisely, all artifacts of the project itself, all its locally reachable parent POMs and all its dependencies from
 * the reactor will be installed to the local repository.
 * 
 * @goal install
 * @phase pre-integration-test
 * @requiresDependencyResolution runtime
 * @since 1.2
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
     * If not set, the regular local repository will be used. To prevent soiling of your regular local repository with
     * possibly broken artifacts, it is strongly recommended to use an isolated repository for the integration tests
     * (e.g. <code>${project.build.directory}/it-repo</code>).
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
     * The set of Maven projects in the reactor build.
     * 
     * @parameter default-value="${reactorProjects}"
     * @readonly
     */
    private Collection reactorProjects;

    /**
     * Performs this mojo's tasks.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactRepository testRepository = createTestRepository();
        installProjectArtifacts( project, testRepository );
        installProjectParents( project, testRepository );
        installProjectDependencies( project, reactorProjects, testRepository );
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
                throw new MojoExecutionException( "Failed to create local repository: " + localRepositoryPath, e );
            }
        }

        return testRepository;
    }

    /**
     * Installs the main artifact and any attached artifacts of the specified project to the local repository.
     * 
     * @param mvnProject The project whose artifacts should be installed, must not be <code>null</code>.
     * @param testRepository The local repository to install the artifacts to, must not be <code>null</code>.
     * @throws MojoExecutionException If any artifact could not be installed.
     */
    private void installProjectArtifacts( MavenProject mvnProject, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        try
        {
            installProjectPom( mvnProject, testRepository );

            // Install the main project artifact
            installer.install( mvnProject.getArtifact().getFile(), mvnProject.getArtifact(), testRepository );

            // Install any attached project artifacts
            Collection attachedArtifacts = mvnProject.getAttachedArtifacts();
            for ( Iterator artifactIter = attachedArtifacts.iterator(); artifactIter.hasNext(); )
            {
                Artifact theArtifact = (Artifact) artifactIter.next();
                installer.install( theArtifact.getFile(), theArtifact, testRepository );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Failed to install project artifacts: " + mvnProject, e );
        }
    }

    /**
     * Installs the (locally reachable) parent POMs of the specified project to the local repository. The parent POMs
     * from the reactor must be installed or the forked IT builds will fail when using a clean repository.
     * 
     * @param mvnProject The project whose parent POMs should be installed, must not be <code>null</code>.
     * @param testRepository The local repository to install the POMs to, must not be <code>null</code>.
     * @throws MojoExecutionException If any POM could not be installed.
     */
    private void installProjectParents( MavenProject mvnProject, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        for ( MavenProject parent = mvnProject.getParent(); parent != null; parent = parent.getParent() )
        {
            if ( parent.getFile() == null )
            {
                break;
            }
            installProjectPom( parent, testRepository );
        }
    }

    /**
     * Installs the POM of the specified project to the local repository.
     * 
     * @param mvnProject The project whose POM should be installed, must not be <code>null</code>.
     * @param testRepository The local repository to install the POM to, must not be <code>null</code>.
     * @throws MojoExecutionException If the POM could not be installed.
     */
    private void installProjectPom( MavenProject mvnProject, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        try
        {
            Artifact pomArtifact =
                artifactFactory.createProjectArtifact( mvnProject.getGroupId(), mvnProject.getArtifactId(),
                                                       mvnProject.getVersion() );
            installer.install( mvnProject.getFile(), pomArtifact, testRepository );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install POM: " + mvnProject, e );
        }
    }

    /**
     * Installs the dependent projects from the reactor to the local repository. The dependencies on other modules from
     * the reactor must be installed or the forked IT builds will fail when using a clean repository.
     * 
     * @param mvnProject The project whose dependent projects should be installed, must not be <code>null</code>.
     * @param reactorProjects The set of projects in the reactor build, must not be <code>null</code>.
     * @param testRepository The local repository to install the POMs to, must not be <code>null</code>.
     * @throws MojoExecutionException If any dependency could not be installed.
     */
    private void installProjectDependencies( MavenProject mvnProject, Collection reactorProjects,
                                             ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        Map projects = new HashMap();
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) it.next();
            projects.put( reactorProject.getId(), reactorProject );
        }

        for ( Iterator it = mvnProject.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            String id =
                artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getType() + ':'
                    + artifact.getVersion();
            MavenProject requiredProject = (MavenProject) projects.remove( id );
            if ( requiredProject != null )
            {
                installProjectArtifacts( requiredProject, testRepository );
                installProjectParents( requiredProject, testRepository );
            }
        }
    }

}
