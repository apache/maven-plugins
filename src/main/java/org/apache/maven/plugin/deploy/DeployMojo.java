package org.apache.maven.plugin.deploy;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deploys an artifact to remote repository.
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 * @version $Id$
 * @goal deploy
 * @phase deploy
 */
public class DeployMojo
    extends AbstractDeployMojo
    implements Contextualizable
{

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" );

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * @parameter expression="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @readonly
     */
    private ArtifactRepository deploymentRepository;

    /**
     * Specifies an alternative repository to which the project artifacts should be deployed ( other
     * than those specified in &lt;distributionManagement&gt; ).
     * <br/>
     * Format: id::layout::url
     * 
     * @parameter expression="${altDeploymentRepository}"
     */
    private String altDeploymentRepository;
    
    /**
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    /**
     * Parameter used to update the metadata to make the artifact as release.
     * 
     * @parameter expression="${updateReleaseInfo}" default-value="false"
     */
    private boolean updateReleaseInfo;

    /**
     * Contextualized.
     */
    private PlexusContainer container;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        System.out.println( "altDeploymentRepository = " + altDeploymentRepository );

        ArtifactRepository repo = getDeploymentRepository();

        String protocol = repo.getProtocol();

        if ( protocol.equals( "scp" ) )
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
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        try
        {
            if ( isPomArtifact )
            {
                getDeployer().deploy( pomFile, artifact, repo, getLocalRepository() );
            }
            else
            {
                File file = artifact.getFile();
                if ( file == null )
                {
                    throw new MojoExecutionException(
                                                      "The packaging for this project did not assign a file to the build artifact" );
                }
                getDeployer().deploy( file, artifact, repo, getLocalRepository() );
            }

            for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
            {
                Artifact attached = ( Artifact ) i.next();

                getDeployer().deploy( attached.getFile(), attached, repo, getLocalRepository() );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private ArtifactRepository getDeploymentRepository()
        throws MojoExecutionException, MojoFailureException
    {
        if ( deploymentRepository == null && altDeploymentRepository == null )
        {
            String msg =
                "Deployment failed: repository element was not specified in the pom inside"
                                + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException( msg );
        }

        ArtifactRepository repo = null;

        if ( altDeploymentRepository != null )
        {
            getLog().info( "Using alternate deployment repository " + altDeploymentRepository );

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher( altDeploymentRepository );

            if ( !matcher.matches() )
            {
                throw new MojoFailureException( altDeploymentRepository, "Invalid syntax for repository.",
                                                "Invalid syntax for alternative repository. Use \"id::layout::url\"." );
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String layout = matcher.group( 2 ).trim();
                String url = matcher.group( 3 ).trim();
                
                ArtifactRepositoryLayout repoLayout;
                try
                {
                    repoLayout = ( ArtifactRepositoryLayout ) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
                }
                catch ( ComponentLookupException e )
                {
                    throw new MojoExecutionException( "Cannot find repository layout: " + layout, e );
                }
                
                repo = new DefaultArtifactRepository( id, url, repoLayout );
            }
        }
        
        if ( repo == null )
        {
            repo = deploymentRepository;
        }

        return repo;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
