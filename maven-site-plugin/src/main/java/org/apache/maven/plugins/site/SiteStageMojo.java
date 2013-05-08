package org.apache.maven.plugins.site;

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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Deploys the generated site to a local staging or mock directory based on the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM.
 * <p>
 * It can be used to test that links between module sites in a multi-module
 * build work.
 * </p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@Mojo( name = "stage", requiresDependencyResolution = ResolutionScope.TEST )
public class SiteStageMojo
    extends AbstractDeployMojo
{
    /**
     * Staging directory location. This needs to be an absolute path, like
     * <code>C:\stagingArea\myProject\</code> on Windows or
     * <code>/stagingArea/myProject/</code> on Unix.
     * If this is not specified, the site will be staged in ${project.build.directory}/staging.
     */
    @Parameter( property = "stagingDirectory" )
    private File stagingDirectory;

    /**
     * Set this to 'true' to skip site generation and staging.
     *
     * @since 3.2
     */
    @Parameter( property = "maven.site.skip", defaultValue = "false" )
    private boolean skip;

    @Override
    protected String getDeployRepositoryID()
        throws MojoExecutionException
    {
        return "stagingLocal";
    }

    @Override
    protected String getDeployRepositoryURL()
        throws MojoExecutionException
    {
        final String stageDir = ( stagingDirectory == null ) ? null : stagingDirectory.getAbsolutePath();
        final String outputDir = getStagingDirectory( stageDir );

        getLog().info( "Using this base directory for staging: " + outputDir );

        final File outputDirectory = new File( outputDir );
        // Safety
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        return "file://" + outputDirectory.getAbsolutePath();
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "maven.site.skip = true: Skipping site staging" );
            return;
        }

        super.execute();
    }

    protected boolean isDeploy()
    {
        // this mojo is for staging, not deploy
        return false;
    }

    /**
     * Find the directory where staging will take place.
     *
     * @param usersStagingDirectory The staging directory as suggested by the user's configuration
     *
     * @return the directory for staging
     */
    private String getStagingDirectory( String usersStagingDirectory )
    {
        String stagingDirectory = null;

        if ( usersStagingDirectory != null )
        {
            // the user has specified a stagingDirectory - use it
            getLog().debug( "stagingDirectory specified by the user: " + usersStagingDirectory );
            stagingDirectory = usersStagingDirectory;
        }
        else
        {
            // The user didn't specify a URL, use the top level target dir
            stagingDirectory =
                getExecutionRootBuildDirectory().getAbsolutePath() + "/" + DEFAULT_STAGING_DIRECTORY;
            getLog().debug( "stagingDirectory NOT specified, using the execution root project: " + stagingDirectory );
        }

        // Return either
        //   usersURL
        // or
        //   executionRootProjectURL + "staging"
        return stagingDirectory;
    }

    /**
     * Find the build directory of the execution root project in the reactor.
     * If no execution root project is found, the build directory of the current project is returned.
     *
     * @return the build directory of the execution root project.
     */
    protected File getExecutionRootBuildDirectory()
    {
        // Find the top level project in the reactor
        final MavenProject executionRootProject = getExecutionRootProject( reactorProjects );

        // Use the top level project's build directory if there is one, otherwise use this project's build directory
        final File buildDirectory;

        if ( executionRootProject == null )
        {
            getLog().debug( "No execution root project found in the reactor, using the current project." );

            buildDirectory = new File( project.getBuild().getDirectory() );
        }
        else
        {
            getLog().debug( "Using the execution root project found in the reactor: " + executionRootProject.getArtifactId() );

            buildDirectory = new File( executionRootProject.getBuild().getDirectory() );
        }

        return buildDirectory;
    }

    /**
     * Find the execution root in the reactor.
     *
     * @param reactorProjects The projects in the reactor. May be <code>null</code> in which case <code>null</code> is returned.
     * @return The execution root project in the reactor, or <code>null</code> if none can be found
     */
    private static MavenProject getExecutionRootProject( List<MavenProject> reactorProjects )
    {
        if ( reactorProjects == null )
        {
            return null;
        }

        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject.isExecutionRoot() )
            {
                return reactorProject;
            }
        }

        return null;
    }
}
