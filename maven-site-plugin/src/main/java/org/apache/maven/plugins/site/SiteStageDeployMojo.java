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

import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.util.List;

/**
 * Deploy a staging site in a specific directory.
 * <p>Useful to test the generated site.</p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage-deploy
 * @requiresDependencyResolution test
 */
public class SiteStageDeployMojo
    extends SiteStageMojo implements Contextualizable
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

    private PlexusContainer container;

    private final String STAGING_SERVER_ID = "stagingSite";

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        deployStagingSite();
    }

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

        Repository repository = new Repository( STAGING_SERVER_ID, stagingSiteURL );

        Wagon wagon;
        try
        {
            wagon = wagonManager.getWagon( repository.getProtocol() );
            SiteDeployMojo.configureWagon( wagon, STAGING_SERVER_ID, settings, container, getLog() );
        }
        catch ( UnsupportedProtocolException e )
        {
            throw new MojoExecutionException( "Unsupported protocol: '" + repository.getProtocol() + "'", e );
        }
        catch ( WagonConfigurationException e )
        {
            throw new MojoExecutionException( "Unable to configure Wagon: '" + repository.getProtocol() + "'", e );
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

            ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
            if ( proxyInfo != null )
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( STAGING_SERVER_ID ), proxyInfo );
            }
            else
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( STAGING_SERVER_ID ) );
            }

            wagon.putDirectory( new File( stagingDirectory, getStructure( project, false ) ), "." );
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

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Find the URL where staging will take place.
     *
     * @param currentProject      The currently executing project
     * @param reactorProjects     The projects in the reactor
     * @param usersStagingSiteURL The staging site URL as suggested by the user's configuration
     * @return the site URL for staging
     */
    protected String getStagingSiteURL( MavenProject currentProject, List reactorProjects, String usersStagingSiteURL )
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
