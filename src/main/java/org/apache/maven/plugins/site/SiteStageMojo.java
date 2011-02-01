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

import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.site.wagon.repository.Repository;

import org.codehaus.plexus.util.StringUtils;

/**
 * Generates a site in a local staging or mock directory based on the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM.
 * <p>
 * It can be used to test that links between module sites in a multi module
 * build works.
 * </p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage
 * @requiresDependencyResolution test
 */
public class SiteStageMojo
    extends SiteDeployMojo
{
    protected static final String DEFAULT_STAGING_DIRECTORY = "staging";

    /**
     * Staging directory location. This needs to be an absolute path, like
     * <code>C:\stagingArea\myProject\</code> on Windows or
     * <code>/stagingArea/myProject/</code> on Unix.
     *
     * @parameter expression="${stagingDirectory}"
     */
    protected File stagingDirectory;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        String structureProject;

        try
        {
            structureProject = getStructure( project, false );
        }
        catch ( MojoFailureException ex )
        {
            throw new MojoExecutionException( "Missing site information.", ex );
        }

        if ( structureProject == null )
        {
            throw new MojoExecutionException( "Missing site information." );
        }

        stagingDirectory = getStagingDirectory( project, reactorProjects, stagingDirectory );
        getLog().info( "Using this directory for staging: " + stagingDirectory );

        final File outputDirectory = new File( stagingDirectory, structureProject );

        // Safety
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        final String url = "file://" + outputDirectory.getAbsolutePath();
        final String id = "stagingLocal";

        deployTo( id, url );
    }

    /**
     * Find the directory where staging will take place.
     *
     * @param currentProject        The currently executing project
     * @param reactorProjects       The projects in the reactor
     * @param usersStagingDirectory The staging directory as suggested by the user's configuration
     * @return the directory for staging
     */
    protected File getStagingDirectory( MavenProject currentProject, List<MavenProject> reactorProjects,
                                        File usersStagingDirectory )
    {
        // Check if the user has specified a stagingDirectory
        if ( usersStagingDirectory != null )
        {
            getLog().debug( "stagingDirectory specified by the user." );
            return usersStagingDirectory;
        }
        getLog().debug( "stagingDirectory NOT specified by the user." );

        // Find the top level project in the reactor
        MavenProject topLevelProject = getTopLevelProject( reactorProjects );

        // Use the top level project's build directory if there is one, otherwise use this project's build directory
        File buildDirectory;
        if ( topLevelProject == null )
        {
            getLog().debug( "No top level project found in the reactor, using the current project." );
            buildDirectory = new File( currentProject.getBuild().getDirectory() );
        }
        else
        {
            getLog().debug( "Using the top level project found in the reactor." );
            buildDirectory = new File( topLevelProject.getBuild().getDirectory() );
        }

        return new File( buildDirectory, DEFAULT_STAGING_DIRECTORY );
    }

    /**
     * Generates the site structure using the project hiearchy (project and its modules) or using the
     * distributionManagement elements from the pom.xml.
     *
     * @param project
     * @param ignoreMissingSiteUrl
     * @return the structure relative path
     * @throws MojoFailureException if any
     */
    protected static String getStructure( MavenProject project, boolean ignoreMissingSiteUrl )
        throws MojoFailureException
    {
        if ( project.getDistributionManagement() == null )
        {
            String hierarchy = project.getArtifactId();

            MavenProject parent = project.getParent();
            while ( parent != null )
            {
                hierarchy = parent.getArtifactId() + "/" + hierarchy;
                parent = parent.getParent();
            }

            return hierarchy;
        }

        Site site = project.getDistributionManagement().getSite();
        if ( site == null )
        {
            if ( !ignoreMissingSiteUrl )
            {
                throw new MojoFailureException(
                    "Missing site information in the distribution management element in the project: '"
                    + project.getName() + "'." );
            }

            return null;
        }

        if ( StringUtils.isEmpty( site.getUrl() ) )
        {
            if ( !ignoreMissingSiteUrl )
            {
                throw new MojoFailureException( "The URL in the site is missing in the project descriptor." );
            }

            return null;
        }

        Repository repository = new Repository( site.getId(), site.getUrl() );
        StringBuffer hierarchy = new StringBuffer( 1024 );
        hierarchy.append( repository.getHost() );
        if ( !StringUtils.isEmpty( repository.getBasedir() ) )
        {
            if ( !repository.getBasedir().startsWith( "/" ) )
            {
                hierarchy.append( '/' );
            }
            hierarchy.append( repository.getBasedir() );
        }

        return hierarchy.toString().replaceAll( "[\\:\\?\\*]", "" );
    }
}
