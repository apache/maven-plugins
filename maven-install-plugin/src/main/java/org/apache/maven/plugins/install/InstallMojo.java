package org.apache.maven.plugins.install;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.install.ArtifactInstallerException;
import org.apache.maven.shared.project.NoFileAssignedException;
import org.apache.maven.shared.project.install.ProjectInstaller;
import org.apache.maven.shared.project.install.ProjectInstallerRequest;

/**
 * Installs the project's main artifact, and any other artifacts attached by other plugins in the lifecycle, to the
 * local repository.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
@Mojo( name = "install", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true )
public class InstallMojo
    extends AbstractInstallMojo
{

    /**
     * When building with multiple threads, reaching the last project doesn't have to mean that all projects are ready
     * to be installed
     */
    private static final AtomicInteger READYPROJECTSCOUTNER = new AtomicInteger();

    private static final List<ProjectInstallerRequest> INSTALLREQUESTS =
        Collections.synchronizedList( new ArrayList<ProjectInstallerRequest>() );

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Whether every project should be installed during its own install-phase or at the end of the multimodule build. If
     * set to {@code true} and the build fails, none of the reactor projects is installed.
     * <strong>(experimental)</strong>
     *
     * @since 2.5
     */
    @Parameter( defaultValue = "false", property = "installAtEnd" )
    private boolean installAtEnd;

    /**
     * Set this to <code>true</code> to bypass artifact installation. Use this for artifacts that does not need to be
     * installed in the local repository.
     *
     * @since 2.4
     */
    @Parameter( property = "maven.install.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * The path for a specific local repository directory. If not specified the local repository path configured in the
     * Maven settings will be used.
     *
     * @since 3.0.0
     */
    @Parameter( property = "localRepositoryPath" )
    private File localRepositoryPath;

    @Component
    private ProjectInstaller installer;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ProjectBuildingRequest buildingRequest = session.getProjectBuildingRequest();
        // ----------------------------------------------------------------------
        // Override the default localRepository variable
        // ----------------------------------------------------------------------
        if ( localRepositoryPath != null )
        {
            buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, localRepositoryPath );

            getLog().debug( "localRepoPath: " + repositoryManager.getLocalRepositoryBasedir( buildingRequest ) );
        }

        boolean addedInstallRequest = false;
        if ( skip )
        {
            getLog().info( "Skipping artifact installation" );
        }
        else
        {
            // CHECKSTYLE_OFF: LineLength
            ProjectInstallerRequest projectInstallerRequest =
                new ProjectInstallerRequest().setProject( project ).setCreateChecksum( createChecksum ).setUpdateReleaseInfo( updateReleaseInfo );
            // CHECKSTYLE_ON: LineLength

            if ( !installAtEnd )
            {
                installProject( buildingRequest, projectInstallerRequest );
            }
            else
            {
                INSTALLREQUESTS.add( projectInstallerRequest );
                addedInstallRequest = true;
            }
        }

        boolean projectsReady = READYPROJECTSCOUTNER.incrementAndGet() == reactorProjects.size();
        if ( projectsReady )
        {
            synchronized ( INSTALLREQUESTS )
            {
                while ( !INSTALLREQUESTS.isEmpty() )
                {
                    installProject( buildingRequest, INSTALLREQUESTS.remove( 0 ) );
                }
            }
        }
        else if ( addedInstallRequest )
        {
            getLog().info( "Installing " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                + project.getVersion() + " at end" );
        }
    }

    private void installProject( ProjectBuildingRequest pbr, ProjectInstallerRequest pir )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            installer.install( pbr, pir );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "IOException", e );
        }
        catch ( ArtifactInstallerException e )
        {
            throw new MojoExecutionException( "ArtifactInstallerException", e );
        }
        catch ( NoFileAssignedException e )
        {
            throw new MojoExecutionException( "NoFileAssignedException", e );
        }

    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

}
