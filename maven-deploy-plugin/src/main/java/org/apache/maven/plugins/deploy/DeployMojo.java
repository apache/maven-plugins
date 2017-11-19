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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
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

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)" );

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
     * <dt>url</dt>
     * <dd>The location of the repository</dd>
     * </dl>
     * <b>Note: Since 3.0.0 the layout part has been removed.</b>
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
        if ( skip )
        {
            getLog().info( "Skipping artifact deployment" );
            queueDeployment( null );
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

            if ( !deployAtEnd )
            {
                queueDeployment( null );
                deployProject( pdr );
            }
            else
            {
                queueDeployment( pdr );
            }
        }
        deployQueuedRequests();
    }

    /**
     * Queue a deployment request.  In a reactor, some deployments may be delayed or skipped while others are not
     * @param pdr The deployment request.  Null indicates that the deployment was not delayed
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private void queueDeployment( ProjectDeployerRequest pdr )
        throws MojoExecutionException, MojoFailureException
    {
        List<ProjectDeployerRequest> deployRequests = getDeployRequests();

        // build may be parallel, protect against multiple threads accessing
        synchronized ( deployRequests )
        {

            deployRequests.add( pdr );
            if ( pdr != null && deployRequests.size() != reactorProjects.size() )
            {
                getLog().info( "Deploying " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                  + project.getVersion() + " at end" );
            }
        }
    }

    public void deployQueuedRequests() throws MojoExecutionException, MojoFailureException
    {
        List deployRequests = getDeployRequests();
        // build may be parallel, protect against multiple threads accessing
        synchronized ( deployRequests )
        {
            if ( deployRequests.size() != reactorProjects.size() )
            {
                return;
            }
            for ( Object dr : deployRequests )
            {
                if ( dr != null )
                {
                    /*
                     * Cast the instance to a ProjectDeployerRequest.  This specialized casting would
                     * not be necessary if ProjectDeployerRequest were in core.
                     */
                    deployProject( CastHelper.castToSameClassLoader( ProjectDeployerRequest.class, dr ) );
                }
            }
        }
    }

    private List<ProjectDeployerRequest> getDeployRequests()
    {
        Properties projectProperties = getSession().getUserProperties();

        // Plugin instances may be in different Classworlds if they are loaded in different modules
        // containing different extensions.  The plugin cannot rely on static variables, only injected
        // or session shared variables
        synchronized ( projectProperties )
        {
            String propertyKey = getClass().getCanonicalName();
            List<ProjectDeployerRequest> reqs = (List<ProjectDeployerRequest>) projectProperties.get( propertyKey );
            if ( reqs == null )
            {
                reqs = new ArrayList<ProjectDeployerRequest>( reactorProjects.size() );
                projectProperties.put( propertyKey, reqs );
            }
            return reqs;
        }
    }

    private void deployProject( ProjectDeployerRequest pir )
        throws MojoFailureException, MojoExecutionException
    {
        ArtifactRepository repo = getDeploymentRepository( pir );
        ProjectBuildingRequest pbr = getSession().getProjectBuildingRequest();

        try
        {
            projectDeployer.deploy( pbr, pir, repo );
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
                                                "Invalid syntax for alternative repository. Use \"id::url\"." );
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String url = matcher.group( 2 ).trim();

                repo = createDeploymentArtifactRepository( id, url );
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
