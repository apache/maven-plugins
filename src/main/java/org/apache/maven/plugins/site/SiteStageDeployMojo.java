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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.WagonConfigurationException;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;

/**
 * Deploys the generated site to a staging or mock directory to the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM. It supports <code>scp</code> and <code>file</code> protocols for
 * deployment.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage-deploy
 * @requiresDependencyResolution test
 */
public class SiteStageDeployMojo
    extends SiteStageMojo
{
    /**
     * The staged site will be deployed to this URL.
     *
     * If you don't specify this, the default-value will be
     * "${project.distributionManagement.site.url}/staging", where "project" is
     * either the current project or, in a reactor build, the top level project
     * in the reactor.
     *
     * @parameter expression="${stagingSiteURL}"
     * @see <a href="http://maven.apache.org/maven-model/maven.html#class_site">MavenModel#class_site</a>
     */
    private String stagingSiteURL;

    /**
     * The identifier of the repository where the staging site will be deployed. This id will be used to lookup a
     * corresponding <code>&lt;server&gt;</code> entry from the <code>settings.xml</code>. If a matching
     * <code>&lt;server&gt;</code> entry is found, its configured credentials will be used for authentication.
     *
     * @parameter expression="${stagingRepositoryId}" default-value="stagingSite"
     * @since 2.0.1
     */
    private String stagingRepositoryId;

    /**
     * Whether to run the "chmod" command on the remote site after the deploy.
     * Defaults to "true".
     *
     * @parameter expression="${maven.site.chmod}" default-value="true"
     * @since 2.1
     */
    private boolean chmod;

    /**
     * The mode used by the "chmod" command. Only used if chmod = true.
     * Defaults to "g+w,a+rX".
     *
     * @parameter expression="${maven.site.chmod.mode}" default-value="g+w,a+rX"
     * @since 2.1
     */
    private String chmodMode;

    /**
     * The options used by the "chmod" command. Only used if chmod = true.
     * Defaults to "-Rf".
     *
     * @parameter expression="${maven.site.chmod.options}" default-value="-Rf"
     * @since 2.1
     */
    private String chmodOptions;

    /**
     * @component
     */
    private WagonManager wagonManager;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        deployStagingSite();
    }


    // FIXME too much duplicate code with SiteDeployMojo

    /**
     * Deploy the staging directory using the stagingSiteURL.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if any
     * @throws org.apache.maven.plugin.MojoFailureException
     *          if any
     */
    private void deployStagingSite()
        throws MojoExecutionException, MojoFailureException
    {
        stagingSiteURL = getStagingSiteURL( project, reactorProjects, stagingSiteURL );
        getLog().info( "Using this URL for staging: " + stagingSiteURL );

        Repository repository = new Repository( stagingRepositoryId, stagingSiteURL );

        Wagon wagon;
        try
        {
            wagon = wagonManager.getWagon( repository );
            SiteDeployMojo.configureWagon( wagon, stagingRepositoryId, settings, plexusContainer, getLog() );
            wagon.connect( repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new MojoExecutionException( "Unsupported protocol: '" + repository.getProtocol() + "'", e );
        }
        catch ( WagonConfigurationException e )
        {
            throw new MojoExecutionException( "Unable to configure Wagon: '" + repository.getProtocol() + "'", e );
        } catch (AuthenticationException e)
        {
            throw new MojoExecutionException( "AuthenticationException : Unable to connect Wagon to repository : '" + repository.getProtocol() + "'", e );
        }
        catch ( ConnectionException e )
        {
            throw new MojoExecutionException( "Unable to connect Wagon to repository : '" + repository.getProtocol() + "'", e );
        }

        if ( !wagon.supportsDirectoryCopy() )
        {
            throw new MojoExecutionException(
                "Wagon protocol '" + repository.getProtocol() + "' doesn't support directory copying" );
        }

        try
        {
            Debug debug = new Debug();


            wagon.addSessionListener( debug );

            wagon.addTransferListener( debug );

            /*
            ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
            if ( proxyInfo != null )
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( stagingRepositoryId ), proxyInfo );
            }
            else
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( stagingRepositoryId ) );
            }
            */
            wagon.connect( repository );
            wagon.putDirectory( new File( stagingDirectory, getStructure( project, false ) ), "." );

            getLog().debug( "putDirectory end ok " );
            if ( chmod && wagon instanceof CommandExecutor )
            {
                CommandExecutor exec = (CommandExecutor) wagon;
                exec.executeCommand( "chmod " + chmodOptions + " " + chmodMode + " " + repository.getBasedir() );
            }
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        catch ( TransferFailedException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        catch ( AuthorizationException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        catch ( ConnectionException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        catch ( AuthenticationException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        catch ( CommandExecutionException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
        finally
        {
            try
            {
                wagon.disconnect();
            }
            catch ( ConnectionException e )
            {
                getLog().error( "Error disconnecting wagon - ignored", e );
            }
        }
    }

    /**
     * Find the URL where staging will take place.
     *
     * @param currentProject      The currently executing project
     * @param reactorProjects     The projects in the reactor
     * @param usersStagingSiteURL The staging site URL as suggested by the user's configuration
     * @return the site URL for staging
     */
    protected String getStagingSiteURL( MavenProject currentProject, List<MavenProject> reactorProjects,
                                        String usersStagingSiteURL )
    {
        String topLevelURL = null;
        String relative = "";

        // If the user has specified a stagingSiteURL - use it
        if ( usersStagingSiteURL != null )
        {
            getLog().debug( "stagingSiteURL specified by the user." );
            topLevelURL = usersStagingSiteURL;
        }
        getLog().debug( "stagingSiteURL NOT specified by the user." );

        // Find the top level project in the reactor
        MavenProject topLevelProject = getTopLevelProject( reactorProjects );

        // Take the distributionManagement site url from the top level project,
        // if there is one, otherwise take it from the current project
        if ( topLevelProject == null )
        {
            if ( topLevelURL == null )
            {
                // The user didn't specify a URL and there is no top level project in the reactor
                // Use current project
                getLog().debug( "No top level project found in the reactor, using the current project." );
                topLevelURL =
                    currentProject.getDistributionManagement().getSite().getUrl() + "/" + DEFAULT_STAGING_DIRECTORY;
            }
        }
        else
        {
            // Find the relative path between the parent and child distribution URLs, if any
            relative = "/" + siteTool.getRelativePath( currentProject.getDistributionManagement().getSite().getUrl(),
                                                       topLevelProject.getDistributionManagement().getSite().getUrl() );
            // SiteTool.getRelativePath() uses File.separatorChar, so we need to convert '\' to '/' in order for the URL
            // to be valid for Windows users
            relative = relative.replace( '\\', '/' );

            if ( topLevelURL == null )
            {
                // The user didn't specify a URL and there is a top level project in the reactor
                // Use the top level project
                getLog().debug( "Using the top level project found in the reactor." );
                topLevelURL =
                    topLevelProject.getDistributionManagement().getSite().getUrl() + "/" + DEFAULT_STAGING_DIRECTORY;
            }
        }

        // Return either
        //   usersURL + relative(from parent, to child)
        // or
        //   topLevelProjectURL + staging + relative(from parent, to child)
        return topLevelURL + relative;
    }
}
