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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

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
    private Collection<MavenProject> reactorProjects;

    /**
     * A flag used to disable the installation procedure. This is primarily intended for usage from the command line to
     * occasionally adjust the build.
     * 
     * @parameter expression="${invoker.skip}" default-value="false"
     * @since 1.4
     */
    private boolean skipInstallation;

    /**
     * The identifiers of already installed artifacts, used to avoid multiple installation of the same artifact.
     */
    private Collection<String> installedArtifacts;

    /**
     * The identifiers of already copied artifacts, used to avoid multiple installation of the same artifact.
     */
    private Collection<String> copiedArtifacts;

    /**
     * Extra dependencies that need to be installed on the local repository.<BR>
     * Format:
     * 
     * <pre>
     * groupId:artifactId:version:type:classifier
     * </pre>
     * 
     * Examples:
     * 
     * <pre>
     * org.apache.maven.plugins:maven-clean-plugin:2.4:maven-plugin
     * org.apache.maven.plugins:maven-clean-plugin:2.4:jar:javadoc
     * </pre>
     * 
     * If the type is 'maven-plugin' the plugin will try to resolve the artifact using plugin remote repositories,
     * instead of using artifact remote repositories.
     * 
     * @parameter
     * @since 1.6
     */
    private String[] extraArtifacts;

    /**
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @readonly
     */
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * @parameter default-value="${project.pluginArtifactRepositories}"
     * @readonly
     */
    private List<ArtifactRepository> remotePluginRepositories;

    /**
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * Performs this mojo's tasks.
     * 
     * @throws MojoExecutionException If the artifacts could not be installed.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( skipInstallation )
        {
            getLog().info( "Skipping artifact installation per configuration." );
            return;
        }

        ArtifactRepository testRepository = createTestRepository();

        installedArtifacts = new HashSet<String>();
        copiedArtifacts = new HashSet<String>();

        installProjectDependencies( project, reactorProjects, testRepository );
        installProjectParents( project, testRepository );
        installProjectArtifacts( project, testRepository );

        installExtraArtifacts( testRepository, extraArtifacts );
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
     * Installs the specified artifact to the local repository. Note: This method should only be used for artifacts that
     * originate from the current (reactor) build. Artifacts that have been grabbed from the user's local repository
     * should be installed to the test repository via {@link #copyArtifact(File, Artifact, ArtifactRepository)}.
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

            if ( installedArtifacts.add( artifact.getId() ) )
            {
                installer.install( file, artifact, testRepository );
            }
            else
            {
                getLog().debug( "Not re-installing " + artifact + ", " + file );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install artifact: " + artifact, e );
        }
    }

    /**
     * Installs the specified artifact to the local repository. This method serves basically the same purpose as
     * {@link #installArtifact(File, Artifact, ArtifactRepository)} but is meant for artifacts that have been resolved
     * from the user's local repository (and not the current build outputs). The subtle difference here is that
     * artifacts from the repository have already undergone transformations and these manipulations should not be redone
     * by the artifact installer. For this reason, this method performs plain copy operations to install the artifacts.
     * 
     * @param file The file associated with the artifact, must not be <code>null</code>.
     * @param artifact The artifact to install, must not be <code>null</code>.
     * @param testRepository The local repository to install the artifact to, must not be <code>null</code>.
     * @throws MojoExecutionException If the artifact could not be installed (e.g. has no associated file).
     */
    private void copyArtifact( File file, Artifact artifact, ArtifactRepository testRepository )
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

            if ( copiedArtifacts.add( artifact.getId() ) )
            {
                File destination = new File( testRepository.getBasedir(), testRepository.pathOf( artifact ) );

                getLog().debug( "Installing " + file + " to " + destination );

                copyFileIfDifferent( file, destination );

                MetadataUtils.createMetadata( destination, artifact );
            }
            else
            {
                getLog().debug( "Not re-installing " + artifact + ", " + file );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to stage artifact: " + artifact, e );
        }
    }

    private void copyFileIfDifferent( File src, File dst )
        throws IOException
    {
        if ( src.lastModified() != dst.lastModified() || src.length() != dst.length() )
        {
            FileUtils.copyFile( src, dst );
            dst.setLastModified( src.lastModified() );
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
            // Install POM (usually attached as metadata but that happens only as a side effect of the Install Plugin)
            installProjectPom( mvnProject, testRepository );

            // Install the main project artifact (if the project has one, e.g. has no "pom" packaging)
            Artifact mainArtifact = mvnProject.getArtifact();
            if ( mainArtifact.getFile() != null )
            {
                installArtifact( mainArtifact.getFile(), mainArtifact, testRepository );
            }

            // Install any attached project artifacts
            Collection<Artifact> attachedArtifacts = mvnProject.getAttachedArtifacts();
            for ( Artifact attachedArtifact : attachedArtifacts )
            {
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
                    copyParentPoms( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), testRepository );
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
    private void installProjectDependencies( MavenProject mvnProject, Collection<MavenProject> reactorProjects,
                                             ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        // index available reactor projects
        Map<String, MavenProject> projects = new HashMap<String, MavenProject>();
        for ( MavenProject reactorProject : reactorProjects )
        {
            String projectId =
                reactorProject.getGroupId() + ':' + reactorProject.getArtifactId() + ':' + reactorProject.getVersion();

            projects.put( projectId, reactorProject );
        }

        // group transitive dependencies (even those that don't contribute to the class path like POMs) ...
        Collection<Artifact> artifacts = mvnProject.getArtifacts();
        // ... into dependencies that were resolved from reactor projects ...
        Collection<String> dependencyProjects = new LinkedHashSet<String>();
        // ... and those that were resolved from the (local) repo
        Collection<Artifact> dependencyArtifacts = new LinkedHashSet<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            // workaround for MNG-2961 to ensure the base version does not contain a timestamp
            artifact.isSnapshot();

            String projectId = artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion();

            if ( projects.containsKey( projectId ) )
            {
                dependencyProjects.add( projectId );
            }
            else
            {
                dependencyArtifacts.add( artifact );
            }
        }

        // install dependencies
        try
        {
            // copy dependencies that where resolved from the local repo
            for ( Artifact artifact : dependencyArtifacts )
            {
                copyArtifact( artifact, testRepository );
            }

            // install dependencies that were resolved from the reactor
            for ( String projectId : dependencyProjects )
            {
                MavenProject dependencyProject = projects.get( projectId );

                installProjectArtifacts( dependencyProject, testRepository );
                installProjectParents( dependencyProject, testRepository );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to install project dependencies: " + mvnProject, e );
        }
    }

    private void copyArtifact( Artifact artifact, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        copyPoms( artifact, testRepository );

        Artifact depArtifact =
            artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                          artifact.getBaseVersion(), artifact.getType(),
                                                          artifact.getClassifier() );

        File artifactFile = artifact.getFile();

        copyArtifact( artifactFile, depArtifact, testRepository );
    }

    private void copyPoms( Artifact artifact, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        Artifact pomArtifact =
            artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                   artifact.getBaseVersion() );

        File pomFile = new File( localRepository.getBasedir(), localRepository.pathOf( pomArtifact ) );

        if ( pomFile.isFile() )
        {
            copyArtifact( pomFile, pomArtifact, testRepository );
            copyParentPoms( pomFile, testRepository );
        }
    }

    /**
     * Installs all parent POMs of the specified POM file that are available in the local repository.
     * 
     * @param pomFile The path to the POM file whose parents should be installed, must not be <code>null</code>.
     * @param testRepository The local repository to install the POMs to, must not be <code>null</code>.
     * @throws MojoExecutionException If any (existing) parent POM could not be installed.
     */
    private void copyParentPoms( File pomFile, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        Model model = PomUtils.loadPom( pomFile );
        Parent parent = model.getParent();
        if ( parent != null )
        {
            copyParentPoms( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), testRepository );
        }
    }

    /**
     * Installs the specified POM and all its parent POMs to the local repository.
     * 
     * @param groupId The group id of the POM which should be installed, must not be <code>null</code>.
     * @param artifactId The artifact id of the POM which should be installed, must not be <code>null</code>.
     * @param version The version of the POM which should be installed, must not be <code>null</code>.
     * @param testRepository The local repository to install the POMs to, must not be <code>null</code>.
     * @throws MojoExecutionException If any (existing) parent POM could not be installed.
     */
    private void copyParentPoms( String groupId, String artifactId, String version, ArtifactRepository testRepository )
        throws MojoExecutionException
    {
        Artifact pomArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        if ( installedArtifacts.contains( pomArtifact.getId() ) || copiedArtifacts.contains( pomArtifact.getId() ) )
        {
            getLog().debug( "Not re-installing " + pomArtifact );
            return;
        }

        File pomFile = new File( localRepository.getBasedir(), localRepository.pathOf( pomArtifact ) );
        if ( pomFile.isFile() )
        {
            copyArtifact( pomFile, pomArtifact, testRepository );
            copyParentPoms( pomFile, testRepository );
        }
    }

    private void installExtraArtifacts( ArtifactRepository testRepository, String[] extraArtifacts )
        throws MojoExecutionException
    {
        if ( extraArtifacts == null )
        {
            return;
        }

        Artifact originatingArtifact = project.getArtifact();

        for ( int i = 0; i < extraArtifacts.length; i++ )
        {
            String[] gav = extraArtifacts[i].split( ":" );
            if ( gav.length < 3 || gav.length > 5 )
            {
                throw new MojoExecutionException( "Invalid artifact " + extraArtifacts[i] );
            }

            String groupId = gav[0];
            String artifactId = gav[1];
            String version = gav[2];

            String type = "jar";
            if ( gav.length > 3 )
            {
                type = gav[3];
            }

            String classifier = null;
            if ( gav.length == 5 )
            {
                classifier = gav[4];
            }

            List<ArtifactRepository> remoteRepositories;
            if ( "maven-plugin".equals( type ) )
            {
                remoteRepositories = this.remotePluginRepositories;
            }
            else
            {
                remoteRepositories = this.remoteArtifactRepositories;
            }

            Artifact artifact = null;
            try
            {
                artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );

                ArtifactResolutionResult arr =
                    resolver.resolveTransitively( Collections.singleton( artifact ), originatingArtifact,
                                                  remoteRepositories, localRepository, artifactMetadataSource );

                if ( !groupId.equals( artifact.getGroupId() ) || !artifactId.equals( artifact.getArtifactId() )
                    || !version.equals( artifact.getVersion() ) )
                {
                    artifact =
                        artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
                    copyPoms( artifact, testRepository );
                }

                for ( Artifact arrArtifact : (Set<Artifact>) arr.getArtifacts() )
                {
                    copyArtifact( arrArtifact, testRepository );
                }
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to resolve dependencies for: " + artifact, e );
            }
        }
    }

}
