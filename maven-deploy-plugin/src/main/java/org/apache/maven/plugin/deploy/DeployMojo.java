package org.apache.maven.plugin.deploy;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final AtomicInteger readyProjectsCounter = new AtomicInteger();

    private static final List<DeployRequest> deployRequests =
        Collections.synchronizedList( new ArrayList<DeployRequest>() );

    /**
     */
    @Component
    private MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Whether every project should be deployed during its own deploy-phase or at the end of the multimodule build. If
     * set to {@code true} and the build fails, none of the reactor projects is deployed. <strong>(experimental)</strong>
     * 
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "deployAtEnd" )
    private boolean deployAtEnd;

    /**
     * @deprecated either use project.getArtifact() or reactorProjects.get(i).getArtifact()
     */
    @Parameter( defaultValue = "${project.artifact}", required = true, readonly = true )
    private Artifact artifact;

    /**
     * @deprecated either use project.getPackaging() or reactorProjects.get(i).getPackaging()
     */
    @Parameter( defaultValue = "${project.packaging}", required = true, readonly = true )
    private String packaging;

    /**
     * @deprecated either use project.getFile() or reactorProjects.get(i).getFile()
     */
    @Parameter( defaultValue = "${project.file}", required = true, readonly = true )
    private File pomFile;

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
     * @deprecated either use project.getAttachedArtifacts() or reactorProjects.get(i).getAttachedArtifacts()
     */
    @Parameter( defaultValue = "${project.attachedArtifacts}", required = true, readonly = true )
    private List attachedArtifacts;

    /**
     * Set this to 'true' to bypass artifact deploy
     * 
     * @since 2.4
     */
    @Parameter( property = "maven.deploy.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Set this to 'true' to enable flat pom
     *
     * @since 2.9
     */
    @Parameter( property = "enableFlatPom", defaultValue = "false" )
    private boolean enableFlatPom;

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

            DeployRequest currentExecutionDeployRequest =
                new DeployRequest().setProject( project ).setUpdateReleaseInfo( isUpdateReleaseInfo() ).setRetryFailedDeploymentCount( getRetryFailedDeploymentCount() ).setAltReleaseDeploymentRepository( altReleaseDeploymentRepository ).setAltSnapshotDeploymentRepository( altSnapshotDeploymentRepository ).setAltDeploymentRepository( altDeploymentRepository );

            if ( !deployAtEnd )
            {
                deployProject( currentExecutionDeployRequest );
            }
            else
            {
                deployRequests.add( currentExecutionDeployRequest );
                addedDeployRequest = true;
            }
        }

        boolean projectsReady = readyProjectsCounter.incrementAndGet() == reactorProjects.size();
        if ( projectsReady )
        {
            synchronized ( deployRequests )
            {
                while ( !deployRequests.isEmpty() )
                {
                    deployProject( deployRequests.remove( 0 ) );
                }
            }
        }
        else if ( addedDeployRequest )
        {
            getLog().info( "Deploying " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                               + project.getVersion() + " at end" );
        }
    }

    private void deployProject( DeployRequest request )
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = request.getProject().getArtifact();
        String packaging = request.getProject().getPackaging();
        File pomFile = request.getProject().getFile();

        @SuppressWarnings( "unchecked" )
        List<Artifact> attachedArtifacts = request.getProject().getAttachedArtifacts();

        ArtifactRepository repo =
            getDeploymentRepository( request.getProject(), request.getAltDeploymentRepository(),
                                     request.getAltReleaseDeploymentRepository(),
                                     request.getAltSnapshotDeploymentRepository() );

        String protocol = repo.getProtocol();

        if ( protocol.equalsIgnoreCase( "scp" ) )
        {
            File sshFile = new File( System.getProperty( "user.home" ), ".ssh" );

            if ( !sshFile.exists() )
            {
                sshFile.mkdirs();
            }
        }

        // Deploy the POM
        boolean isPomArtifact = "pom".equals( packaging );
        if ( !isPomArtifact )
        {
            if ( "jar".equals( packaging ) && enableFlatPom )
            {
                useFlatPom( request.getProject() );
                pomFile = request.getProject().getFile();
            }
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }

        if ( request.isUpdateReleaseInfo() )
        {
            artifact.setRelease( true );
        }

        int retryFailedDeploymentCount = request.getRetryFailedDeploymentCount();

        try
        {
            if ( isPomArtifact )
            {
                deploy( pomFile, artifact, repo, getLocalRepository(), retryFailedDeploymentCount );
            }
            else
            {
                File file = artifact.getFile();

                if ( file != null && file.isFile() )
                {
                    deploy( file, artifact, repo, getLocalRepository(), retryFailedDeploymentCount );
                }
                else if ( !attachedArtifacts.isEmpty() )
                {
                    getLog().info( "No primary artifact to deploy, deploying attached artifacts instead." );

                    Artifact pomArtifact =
                        artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                               artifact.getBaseVersion() );
                    pomArtifact.setFile( pomFile );
                    if ( request.isUpdateReleaseInfo() )
                    {
                        pomArtifact.setRelease( true );
                    }

                    deploy( pomFile, pomArtifact, repo, getLocalRepository(), retryFailedDeploymentCount );

                    // propagate the timestamped version to the main artifact for the attached artifacts to pick it up
                    artifact.setResolvedVersion( pomArtifact.getVersion() );
                }
                else
                {
                    String message = "The packaging for this project did not assign a file to the build artifact";
                    throw new MojoExecutionException( message );
                }
            }

            for ( Artifact attached : attachedArtifacts )
            {
                deploy( attached.getFile(), attached, repo, getLocalRepository(), retryFailedDeploymentCount );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void useFlatPom( MavenProject project )
        throws MojoExecutionException
    {
        generateFlatPom( project );
        applyFlatPom( project );
    }

    private void generateFlatPom( MavenProject project )
        throws MojoExecutionException
    {
        getLog().info( "Generating flat pom for project " + project.getName() );
        File flatPomFile = createFlatPomFile( project );
        Model flatPomModel = createFlatModel( project );
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        Writer fileWriter = null;
        try
        {
            fileWriter = WriterFactory.newXmlWriter( flatPomFile );
            pomWriter.write( fileWriter, flatPomModel );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write to flat pom file for project " + project.getName(), e );
        }
        finally
        {
            IOUtil.close( fileWriter );
        }
    }

    private static File createFlatPomFile( MavenProject project )
        throws MojoExecutionException
    {
        File pomFile = project.getFile();
        if ( pomFile == null )
        {
            throw new MojoExecutionException( "Cannot create flat pom file for project " + project.getName()
                + ": pom file is null" );
        }
        String flatPomDir = getFlatPomDir( project.getBasedir() );
        new File( flatPomDir ).mkdirs();
        File flatPomFile = new File( flatPomDir, "pom.xml" );
        try
        {
            flatPomFile.createNewFile();
            return flatPomFile;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot create flat pom file for project " + project.getName(), e );
        }
    }

    private static Model createFlatModel( MavenProject project )
    {
        Model flatModel = new Model();
        flatModel.setModelVersion( project.getModelVersion() );
        flatModel.setGroupId( project.getGroupId() );
        flatModel.setArtifactId( project.getArtifactId() );
        flatModel.setVersion( project.getVersion() );
        flatModel.setPackaging( project.getPackaging() );
        flatModel.setDependencies( project.getDependencies() );
        return flatModel;
    }

    private void applyFlatPom( MavenProject project )
        throws MojoExecutionException
    {
        getLog().info( "Applying flat pom for project " + project.getName() );
        File flatPomFile = new File( getFlatPomDir( project.getBasedir() ), "pom.xml" );
        if ( !flatPomFile.exists() )
        {
            throw new MojoExecutionException( "Cannot find flat pom file in path " + flatPomFile.getAbsolutePath()
                + " for project " + project.getName() );
        }
        project.setFile( flatPomFile );
        Artifact artifact = project.getArtifact();
        if ( artifact instanceof DefaultArtifact )
        {
            ArtifactMetadata flatMetadata = new ProjectArtifactMetadata( artifact, flatPomFile );
            try
            {
                Field fldMetadataMap = DefaultArtifact.class.getDeclaredField( "metadataMap" );
                fldMetadataMap.setAccessible( true );
                Map metadataMap = (Map) fldMetadataMap.get( artifact );
                metadataMap.put( flatMetadata.getKey(), flatMetadata );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Cannot add flat pom metadata. Artifact: " + artifact, e );
            }
        }
        else
        {
            throw new MojoExecutionException( "Artifact is not DefaultArtifact. Class name: "
                + artifact.getClass().getName() + " Artifact: " + artifact );
        }
    }

    private static String getFlatPomDir( File baseDir )
    {
        if ( baseDir == null )
        {
            return null;
        }
        return baseDir.getAbsolutePath() + File.separator + "target" + File.separator + "flat-pom";
    }

    ArtifactRepository getDeploymentRepository( MavenProject project, String altDeploymentRepository,
                                                String altReleaseDeploymentRepository,
                                                String altSnapshotDeploymentRepository )
        throws MojoExecutionException, MojoFailureException
    {
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

                repo = repositoryFactory.createDeploymentArtifactRepository( id, url, repoLayout, true );
            }
        }

        if ( repo == null )
        {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if ( repo == null )
        {
            String msg =
                "Deployment failed: repository element was not specified in the POM inside"
                    + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException( msg );
        }

        return repo;
    }

}
