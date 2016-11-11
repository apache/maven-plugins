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

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.project.NoFileAssignedException;
import org.apache.maven.shared.project.deploy.ProjectDeployer;
import org.apache.maven.shared.project.deploy.ProjectDeployerRequest;

/**
 * Deploys an artifact to remote repository.
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 * @version $Id$
 */
@Mojo( name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true )
public class DeployMojo
    extends AbstractDeployMojo
{

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" );

    /**
     * When building with multiple threads, reaching the last project doesn't have to mean that all projects are ready
     * to be deployed
     */
    private static final AtomicInteger READYPROJECTSCOUNTER = new AtomicInteger();

    private static final List<ProjectDeployerRequest> DEPLOYREQUESTS =
        Collections.synchronizedList( new ArrayList<ProjectDeployerRequest>() );

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Whether every project should be deployed during its own deploy-phase or at the end of the multimodule build. If
     * set to {@code true} and the build fails, none of the reactor projects is deployed.
     * <strong>(experimental)</strong>
     * 
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "deployAtEnd" )
    private boolean deployAtEnd;

    /**
     * Specifies an alternative repository to which the project artifacts should be deployed ( other than those
     * specified in &lt;distributionManagement&gt; ). <br/>
     * Format: id::layout::url
     * <dl>
     * <dt>id</dt>
     * <dd>The id can be used to pick up the correct credentials from the settings.xml</dd>
     * <dt>layout</dt>
     * <dd>Either <code>default</code> for the Maven2 layout or <code>legacy</code> for the Maven1 layout. Maven3 also
     * uses the <code>default</code> layout.</dd>
     * <dt>url</dt>
     * <dd>The location of the repository</dd>
     * </dl>
     */
    @Parameter( property = "altDeploymentRepository" )
    private String altDeploymentRepository;

    /**
     * The alternative repository to use when the project has a snapshot version.
     * 
     * @since 2.8
     * @see DeployMojo#altDeploymentRepository
     */
    @Parameter( property = "altSnapshotDeploymentRepository" )
    private String altSnapshotDeploymentRepository;

    /**
     * The alternative repository to use when the project has a final version.
     * 
     * @since 2.8
     * @see DeployMojo#altDeploymentRepository
     */
    @Parameter( property = "altReleaseDeploymentRepository" )
    private String altReleaseDeploymentRepository;

    /**
     * Set this to 'true' to bypass artifact deploy
     * 
     * @since 2.4
     */
    @Parameter( property = "maven.deploy.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Component used to deploy project.
     */
    @Component
    private ProjectDeployer projectDeployer;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean addedDeployRequest = false;
        if ( skip )
        {
            getLog().info( "Skipping artifact deployment" );
        }
        else
        {
            failIfOffline();

            // CHECKSTYLE_OFF: LineLength
            // @formatter:off
            ProjectDeployerRequest pdr = new ProjectDeployerRequest()
                .setProject( project )
                .setUpdateReleaseInfo( isUpdateReleaseInfo() )
                .setRetryFailedDeploymentCount( getRetryFailedDeploymentCount() )
                .setAltReleaseDeploymentRepository( altReleaseDeploymentRepository )
                .setAltSnapshotDeploymentRepository( altSnapshotDeploymentRepository )
                .setAltDeploymentRepository( altDeploymentRepository );
            // @formatter:on
            // CHECKSTYLE_ON: LineLength

            ArtifactRepository repo = getDeploymentRepository( pdr );

            if ( !deployAtEnd )
            {
                deployProject( getSession().getProjectBuildingRequest(), pdr, repo );
            }
            else
            {
                DEPLOYREQUESTS.add( pdr );
                addedDeployRequest = true;
            }
        }

        boolean projectsReady = READYPROJECTSCOUNTER.incrementAndGet() == reactorProjects.size();
        if ( projectsReady )
        {
            synchronized ( DEPLOYREQUESTS )
            {
                while ( !DEPLOYREQUESTS.isEmpty() )
                {
                    ArtifactRepository repo = getDeploymentRepository( DEPLOYREQUESTS.get( 0 ) );

                    deployProject( getSession().getProjectBuildingRequest(), DEPLOYREQUESTS.remove( 0 ), repo );
                }
            }
        }
        else if ( addedDeployRequest )
        {
            getLog().info( "Deploying " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                + project.getVersion() + " at end" );
        }
    }

    private void deployProject( ProjectBuildingRequest pbr, ProjectDeployerRequest pir, ArtifactRepository repo )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            projectDeployer.deploy( pbr, pir, repo );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "IOException", e );
        }
        catch ( NoFileAssignedException e )
        {
            throw new MojoExecutionException( "NoFileAssignedException", e );
        }

    }

    ArtifactRepository getDeploymentRepository( ProjectDeployerRequest pdr )

        throws MojoExecutionException, MojoFailureException
    {
        MavenProject project = pdr.getProject();
        String altDeploymentRepository = pdr.getAltDeploymentRepository();
        String altReleaseDeploymentRepository = pdr.getAltReleaseDeploymentRepository();
        String altSnapshotDeploymentRepository = pdr.getAltSnapshotDeploymentRepository();

        ArtifactRepository repo = null;

        String altDeploymentRepo;
        if ( ArtifactUtils.isSnapshot( project.getVersion() ) && altSnapshotDeploymentRepository != null )
        {
            altDeploymentRepo = altSnapshotDeploymentRepository;
        }
        else if ( !ArtifactUtils.isSnapshot( project.getVersion() ) && altReleaseDeploymentRepository != null )
        {
            altDeploymentRepo = altReleaseDeploymentRepository;
        }
        else
        {
            altDeploymentRepo = altDeploymentRepository;
        }

        if ( altDeploymentRepo != null )
        {
            getLog().info( "Using alternate deployment repository " + altDeploymentRepo );

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher( altDeploymentRepo );

            if ( !matcher.matches() )
            {
                throw new MojoFailureException( altDeploymentRepo, "Invalid syntax for repository.",
                                                "Invalid syntax for alternative repository. Use \"id::layout::url\"." );
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String layout = matcher.group( 2 ).trim();
                String url = matcher.group( 3 ).trim();

                ArtifactRepositoryLayout repoLayout = getLayout( layout );

                repo = createDeploymentArtifactRepository( id, url, repoLayout );
            }
        }

        if ( repo == null )
        {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if ( repo == null )
        {
            String msg = "Deployment failed: repository element was not specified in the POM inside"
                + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException( msg );
        }

        return repo;
    }

}
