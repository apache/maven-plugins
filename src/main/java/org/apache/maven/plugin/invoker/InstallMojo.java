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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Installs the project artifacts of the main build into the local repository as a preparation to run the sub projects.
 * More precisely, all artifacts of the project itself, all its locally reachable parent POMs and all its dependencies
 * from the reactor will be installed to the local repository.
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
    private ArtifactRepository localRepository;

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
     * 
     * @throws MojoExecutionException If the artifacts could not be installed.
     */
    public void execute()
        throws MojoExecutionException
    {
        ArtifactRepository testRepository = createTestRepository();
        installProjectArtifacts( project, testRepository );
        installProjectParents( project, testRepository );
        installProjectDependencies( project, reactorProjects, testRepository );
    }

    /**
     * Creates the local repository for the integration tests. If the user specified a custom repository location, the
     * custom repository will have the same identifier, layout and policies as the real local repository. That means
     * apart from the location, the custom repository will be indistinguishable from the real repository such that its
     * usage is transparent to the integration tests.
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
                if ( !localRepositoryPath.exists() && !localRepositoryPath.mkdirs() )
                {
                    throw new IOException( "Failed to create directory: " + localRepositoryPath );
                }

                testRepository =
                    repositoryFactory.createArtifactRepository( localRepository.getId(),
                                                                localRepositoryPath.toURL().toExternalForm(),
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
     * Installs the specified artifact to the local repository.
     * 
     * @param file The file associated with the artifact, must not be <code>null</code>. This is in most cases the value
     *            of <code>artifact.getFile()</code> with the exception of the main artifact from a project with
     *            packaging "pom". Projects with packaging "pom" have no main artifact file. They have however artifact
     *            metadata (e.g. site descriptors) which needs to be installed.
     * @param artifact The artifact to install, must not be <code>null</code>.
     * @param testRepository The local repository to install the artifact to, must not be <code>null</code>.
     * @throws MojoExecutionException If the artifact could not be installed (e.g. has no associated file).
     */
    private void installArtifact( File file, Artifact artifact, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        try
        {
            if ( file == null )
            {
                throw new IllegalStateException( "Artifact has no associated file: " + file );
            }
            if ( !file.isFile() )
            {
                throw new IllegalStateException( "Artifact is not fully assembled: " + file );
            }
            installer.install( file, artifact, testRepository );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install artifact: " + artifact, e );
        }
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

            // Install the main project artifact (if the project has one, e.g. has no "pom" packaging)
            Artifact mainArtifact = mvnProject.getArtifact();
            if ( mainArtifact.getFile() != null )
            {
                installArtifact( mainArtifact.getFile(), mainArtifact, testRepository );
            }

            // Install any attached project artifacts
            Collection attachedArtifacts = mvnProject.getAttachedArtifacts();
            for ( Iterator artifactIter = attachedArtifacts.iterator(); artifactIter.hasNext(); )
            {
                Artifact attachedArtifact = (Artifact) artifactIter.next();
                installArtifact( attachedArtifact.getFile(), attachedArtifact, testRepository );
            }
        }
        catch ( Exception e )
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
        try
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
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install project parents: " + mvnProject, e );
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
            Artifact pomArtifact = null;
            if ( "pom".equals( mvnProject.getPackaging() ) )
            {
                pomArtifact = mvnProject.getArtifact();
            }
            if ( pomArtifact == null )
            {
                pomArtifact =
                    artifactFactory.createProjectArtifact( mvnProject.getGroupId(), mvnProject.getArtifactId(),
                                                           mvnProject.getVersion() );
            }
            installArtifact( mvnProject.getFile(), pomArtifact, testRepository );
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
        // index available reactor projects
        Map projects = new HashMap();
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) it.next();
            String id = reactorProject.getGroupId() + ':' + reactorProject.getArtifactId();
            projects.put( id, reactorProject );
        }

        // collect transitive dependencies
        Collection dependencies = new HashSet();
        for ( Iterator it = mvnProject.getRuntimeArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            String id = ArtifactUtils.versionlessKey( artifact );
            
            dependencies.add( id );
        }

        // install dependencies available in reactor
        try
        {
            for ( Iterator it = dependencies.iterator(); it.hasNext(); )
            {
                String id = (String) it.next();
                MavenProject requiredProject = (MavenProject) projects.remove( id );
                if ( requiredProject != null )
                {
                    it.remove();
                    installProjectArtifacts( requiredProject, testRepository );
                    installProjectParents( requiredProject, testRepository );
                }
            }
            
            for ( Iterator it = mvnProject.getRuntimeArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                String id = ArtifactUtils.versionlessKey( artifact );
                
                if ( dependencies.contains( id ) )
                {
                    File artifactFile = artifact.getFile();
                    
                    installArtifact( artifactFile, artifact, testRepository );
                    
                    Artifact pomArtifact =
                        artifactFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                        artifact.getVersion(), null, "pom" );
                    
                    File pomFile = new File( localRepository.getBasedir(), localRepository.pathOf( pomArtifact ) );
                    
                    if ( pomFile.exists() )
                    {
                        installArtifact( pomFile, pomArtifact, testRepository );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install project dependencies: " + mvnProject, e );
        }
    }

}
