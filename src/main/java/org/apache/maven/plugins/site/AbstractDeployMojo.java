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
import java.util.Locale;

import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
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
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Abstract base class for deploy mojos.
 * Since 2.3 this includes {@link SiteStageMojo} and {@link SiteStageDeployMojo}.
 *
 * @author ltheussl
 *
 * @since 2.3
 */
public abstract class AbstractDeployMojo
    extends AbstractSiteMojo implements Contextualizable
{
    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter alias="outputDirectory" expression="${project.reporting.outputDirectory}"
     * @required
     */
    private File inputDirectory;

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

    private PlexusContainer container;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        deployTo( new org.apache.maven.plugins.site.wagon.repository.Repository(
            getDeployRepositoryID(), getDeployRepositoryURL() ) );
    }

    /**
     * Specifies the id to look up credential settings.
     *
     * @return the id to look up credentials for the deploy. Not null.
     *
     * @throws MojoExecutionException
     */
    protected abstract String getDeployRepositoryID()
        throws MojoExecutionException;

    /**
     * Specifies the target URL for the deploy.
     * This should be the top-level URL, ie above modules and locale sub-directories.
     *
     * @return the url to deploy to. Not null.
     *
     * @throws MojoExecutionException
     */
    protected abstract String getDeployRepositoryURL()
        throws MojoExecutionException;

    /**
     * Find the relative path between the distribution URLs of the top parent and the current project.
     *
     * @return a String starting with "/".
     */
    private String getDeployModuleDirectory()
        throws MojoExecutionException
    {
        String relative = "/" + siteTool.getRelativePath( getSite( project ).getUrl(),
            getRootSite( project ).getUrl() );

        // SiteTool.getRelativePath() uses File.separatorChar,
        // so we need to convert '\' to '/' in order for the URL to be valid for Windows users
        return relative.replace( '\\', '/' );
    }

    /**
     * Use wagon to deploy the generated site to a given repository.
     *
     * @param repository the repository to deply to.
     *      This needs to contain a valid, non-null {@link Repository#getId() id}
     *      to look up credentials for the deploy, and a valid, non-null
     *      {@link Repository#getUrl() scm url} to deploy to.
     *
     * @throws MojoExecutionException if the deploy fails.
     */
    private void deployTo( final Repository repository )
        throws MojoExecutionException
    {
        if ( !inputDirectory.exists() )
        {
            throw new MojoExecutionException( "The site does not exist, please run site:site first" );
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Deploying to '" + repository.getUrl()
                + "',\n    Using credentials from server id '" + repository.getId() + "'" );
        }

        deploy( inputDirectory, repository );
    }

    private void deploy( final File inputDirectory, final Repository repository )
        throws MojoExecutionException
    {
        // TODO: work on moving this into the deployer like the other deploy methods
        final Wagon wagon = getWagon( repository, wagonManager );

        try
        {
            configureWagon( wagon, repository.getId(), settings, container, getLog() );
        }
        catch ( WagonConfigurationException e )
        {
            throw new MojoExecutionException( "Unable to configure Wagon: '" + repository.getProtocol() + "'", e );
        }

        try
        {
            push( inputDirectory, repository, wagonManager, wagon,
                siteTool.getAvailableLocales( locales ), getDeployModuleDirectory() );

            if ( chmod )
            {
                chmod( wagon, repository, chmodOptions, chmodMode );
            }
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
     * Find the build directory of the top level project in the reactor.
     * If no top level project is found, the build directory of the current project is returned.
     *
     * @return the build directory of the top level project.
     */
    protected File getTopLevelBuildDirectory()
    {
        // Find the top level project in the reactor
        final MavenProject topLevelProject = getTopLevelProject( reactorProjects );

        // Use the top level project's build directory if there is one, otherwise use this project's build directory
        final File buildDirectory;

        if ( topLevelProject == null )
        {
            getLog().debug( "No top level project found in the reactor, using the current project." );

            buildDirectory = new File( project.getBuild().getDirectory() );
        }
        else
        {
            getLog().debug( "Using the top level project found in the reactor." );

            buildDirectory = new File( topLevelProject.getBuild().getDirectory() );
        }

        return buildDirectory;
    }

    private static Wagon getWagon( final Repository repository, final WagonManager manager )
        throws MojoExecutionException
    {
        final Wagon wagon;

        try
        {
            wagon = manager.getWagon( repository );
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

        return wagon;
    }

    private static void push( final File inputDirectory, final Repository repository,
        final WagonManager manager, final Wagon wagon, final List<Locale> localesList, final String relativeDir )
        throws MojoExecutionException
    {
        try
        {
            Debug debug = new Debug();

            wagon.addSessionListener( debug );

            wagon.addTransferListener( debug );

            ProxyInfo proxyInfo = getProxyInfo( repository, manager );

            if ( proxyInfo == null )
            {
                wagon.connect( repository, manager.getAuthenticationInfo( repository.getId() ) );
            }
            else
            {
                wagon.connect( repository, manager.getAuthenticationInfo( repository.getId() ), proxyInfo );
            }

            // Default is first in the list
            final String defaultLocale = localesList.get( 0 ).getLanguage();

            for ( Locale locale : localesList )
            {
                if ( locale.getLanguage().equals( defaultLocale ) )
                {
                    // TODO: this also uploads the non-default locales,
                    // is there a way to exlude directories in wagon?
                    wagon.putDirectory( inputDirectory, relativeDir );
                }
                else
                {
                    wagon.putDirectory( new File( inputDirectory, locale.getLanguage() ),
                        locale.getLanguage() + relativeDir );
                }
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
    }

    private static void chmod( final Wagon wagon, final Repository repository,
        final String chmodOptions, final String chmodMode )
        throws MojoExecutionException
    {
        try
        {
            if ( wagon instanceof CommandExecutor )
            {
                CommandExecutor exec = (CommandExecutor) wagon;
                exec.executeCommand( "chmod " + chmodOptions + " " + chmodMode + " " + repository.getBasedir() );
            }
            // else ? silently ignore, FileWagon is not a CommandExecutor!
        }
        catch ( CommandExecutionException e )
        {
            throw new MojoExecutionException( "Error uploading site", e );
        }
    }

    /**
     * <p>
     * Get the <code>ProxyInfo</code> of the proxy associated with the <code>host</code>
     * and the <code>protocol</code> of the given <code>repository</code>.
     * </p>
     * <p>
     * Extract from <a href="http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html">
     * J2SE Doc : Networking Properties - nonProxyHosts</a> : "The value can be a list of hosts,
     * each separated by a |, and in addition a wildcard character (*) can be used for matching"
     * </p>
     * <p>
     * Defensively support for comma (",") and semi colon (";") in addition to pipe ("|") as separator.
     * </p>
     *
     * @param repository the Repository to extract the ProxyInfo from.
     * @param wagonManager the WagonManager used to connect to the Repository.
     * @return a ProxyInfo object instantiated or <code>null</code> if no matching proxy is found
     */
    public static ProxyInfo getProxyInfo( Repository repository, WagonManager wagonManager )
    {
        ProxyInfo proxyInfo = wagonManager.getProxy( repository.getProtocol() );

        if ( proxyInfo == null )
        {
            return null;
        }

        String host = repository.getHost();
        String nonProxyHostsAsString = proxyInfo.getNonProxyHosts();
        String[] nonProxyHosts = StringUtils.split( nonProxyHostsAsString, ",;|" );
        for ( int i = 0; i < nonProxyHosts.length; i++ )
        {
            String nonProxyHost = nonProxyHosts[i];
            if ( StringUtils.contains( nonProxyHost, "*" ) )
            {
                // Handle wildcard at the end, beginning or middle of the nonProxyHost
                final int pos = nonProxyHost.indexOf( '*' );
                String nonProxyHostPrefix = nonProxyHost.substring( 0, pos );
                String nonProxyHostSuffix = nonProxyHost.substring( pos + 1 );
                // prefix*
                if ( StringUtils.isNotEmpty( nonProxyHostPrefix ) && host.startsWith( nonProxyHostPrefix )
                    && StringUtils.isEmpty( nonProxyHostSuffix ) )
                {
                    return null;
                }
                // *suffix
                if ( StringUtils.isEmpty( nonProxyHostPrefix )
                    && StringUtils.isNotEmpty( nonProxyHostSuffix ) && host.endsWith( nonProxyHostSuffix ) )
                {
                    return null;
                }
                // prefix*suffix
                if ( StringUtils.isNotEmpty( nonProxyHostPrefix ) && host.startsWith( nonProxyHostPrefix )
                    && StringUtils.isNotEmpty( nonProxyHostSuffix ) && host.endsWith( nonProxyHostSuffix ) )
                {
                    return null;
                }
            }
            else if ( host.equals( nonProxyHost ) )
            {
                return null;
            }
        }
        return proxyInfo;
    }

    /**
     * Configure the Wagon with the information from serverConfigurationMap ( which comes from settings.xml )
     *
     * @todo Remove when {@link WagonManager#getWagon(Repository) is available}. It's available in Maven 2.0.5.
     * @param wagon
     * @param repositoryId
     * @param settings
     * @param container
     * @param log
     * @throws WagonConfigurationException
     */
    private static void configureWagon( Wagon wagon, String repositoryId, Settings settings, PlexusContainer container,
        Log log )
        throws WagonConfigurationException
    {
        // MSITE-25: Make sure that the server settings are inserted
        for ( int i = 0; i < settings.getServers().size(); i++ )
        {
            Server server = (Server) settings.getServers().get( i );
            String id = server.getId();
            if ( id != null && id.equals( repositoryId ) )
            {
                if ( server.getConfiguration() != null )
                {
                    final PlexusConfiguration plexusConf =
                        new XmlPlexusConfiguration( (Xpp3Dom) server.getConfiguration() );

                    ComponentConfigurator componentConfigurator = null;
                    try
                    {
                        componentConfigurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE );
                        componentConfigurator.configureComponent( wagon, plexusConf, container.getContainerRealm() );
                    }
                    catch ( final ComponentLookupException e )
                    {
                        throw new WagonConfigurationException( repositoryId, "Unable to lookup wagon configurator."
                            + " Wagon configuration cannot be applied.", e );
                    }
                    catch ( ComponentConfigurationException e )
                    {
                        throw new WagonConfigurationException( repositoryId, "Unable to apply wagon configuration.",
                            e );
                    }
                    finally
                    {
                        if ( componentConfigurator != null )
                        {
                            try
                            {
                                container.release( componentConfigurator );
                            }
                            catch ( ComponentLifecycleException e )
                            {
                                log.error( "Problem releasing configurator - ignoring: " + e.getMessage() );
                            }
                        }
                    }

                }

            }
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Find the top level parent in the reactor, i.e. the execution root.
     *
     * @param reactorProjects The projects in the reactor. May be null in which case null is returnned.
     *
     * @return The top level project in the reactor, or <code>null</code> if none can be found
     */
    private static MavenProject getTopLevelProject( List<MavenProject> reactorProjects )
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

    /**
     * Extract the distributionManagment site from the given MavenProject.
     *
     * @param project the MavenProject. Not null.
     *
     * @return the project site. Not null.
     *      Also site.getUrl() and site.getId() are guaranteed to be not null.
     *
     * @throws MojoExecutionException if any of the site info is missing.
     */
    private static Site getSite( final MavenProject project )
        throws MojoExecutionException
    {
        final String name = project.getName() + " ("
            + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion() + ")";

        final DistributionManagement distributionManagement = project.getDistributionManagement();

        if ( distributionManagement == null )
        {
            throw new MojoExecutionException( "Missing distribution management in project " + name );
        }

        final Site site = distributionManagement.getSite();

        if ( site == null )
        {
            throw new MojoExecutionException(
                "Missing site information in the distribution management of the project " + name );
        }

        if ( site.getUrl() == null || site.getId() == null )
        {
            throw new MojoExecutionException( "Missing site data: specify url and id for project " + name );
        }

        return site;
    }

    /**
     * Extract the distributionManagment site of the top level parent of the given MavenProject.
     * This climbs up the project hirarchy and returns the site of the last project
     * for which {@link #getSite(org.apache.maven.project.MavenProject)} returns a site.
     *
     * @param project the MavenProject. Not null.
     *
     * @return the top level site. Not null.
     *      Also site.getUrl() and site.getId() are guaranteed to be not null.
     *
     * @throws MojoExecutionException if no site info is found in the tree.
     */
    protected static Site getRootSite( MavenProject project )
        throws MojoExecutionException
    {
        Site site = getSite( project );

        MavenProject parent = project;

        while ( parent.getParent() != null )
        {
            parent = parent.getParent();

            try
            {
                site = getSite( parent );
            }
            catch ( MojoExecutionException e )
            {
                break;
            }
        }

        return site;
    }
}
