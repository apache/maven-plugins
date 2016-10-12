package org.apache.maven.plugins.site.deploy;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.doxia.site.decoration.inheritance.URIPathDescriptor;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.site.AbstractSiteMojo;
import org.apache.maven.plugins.site.deploy.wagon.BugFixedRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.classworlds.ClassRealm;
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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Abstract base class for deploy mojos.
 * Since 2.3 this includes {@link SiteStageMojo} and {@link SiteStageDeployMojo}.
 *
 * @author ltheussl
 * @since 2.3
 */
public abstract class AbstractDeployMojo
    extends AbstractSiteMojo
    implements Contextualizable
{
    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @since 2.3
     */
    @Parameter( alias = "outputDirectory", defaultValue = "${project.reporting.outputDirectory}", required = true )
    private File inputDirectory;

    /**
     * Whether to run the "chmod" command on the remote site after the deploy.
     * Defaults to "true".
     *
     * @since 2.1
     */
    @Parameter( property = "maven.site.chmod", defaultValue = "true" )
    private boolean chmod;

    /**
     * The mode used by the "chmod" command. Only used if chmod = true.
     * Defaults to "g+w,a+rX".
     *
     * @since 2.1
     */
    @Parameter( property = "maven.site.chmod.mode", defaultValue = "g+w,a+rX" )
    private String chmodMode;

    /**
     * The options used by the "chmod" command. Only used if chmod = true.
     * Defaults to "-Rf".
     *
     * @since 2.1
     */
    @Parameter( property = "maven.site.chmod.options", defaultValue = "-Rf" )
    private String chmodOptions;

    /**
     * Set this to 'true' to skip site deployment.
     *
     * @since 3.0
     */
    @Parameter( property = "maven.site.deploy.skip", defaultValue = "false" )
    private boolean skipDeploy;

    /**
     */
    @Component
    private WagonManager wagonManager;

    /**
     * The current user system settings for use in Maven.
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    /**
     * @since 3.0-beta-2
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    protected MavenSession mavenSession;

    private String topDistributionManagementSiteUrl;

    private Site deploySite;

    private PlexusContainer container;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip && isDeploy() )
        {
            getLog().info( "maven.site.skip = true: Skipping site deployment" );
            return;
        }

        if ( skipDeploy && isDeploy() )
        {
            getLog().info( "maven.site.deploy.skip = true: Skipping site deployment" );
            return;
        }

        deployTo( new BugFixedRepository( getDeploySite().getId(), getDeploySite().getUrl() ) );
    }

    /**
     * Make sure the given url ends with a slash.
     *
     * @param url a String.
     * @return if url already ends with '/' it is returned unchanged,
     *         otherwise a '/' character is appended.
     */
    protected static String appendSlash( final String url )
    {
        if ( url.endsWith( "/" ) )
        {
            return url;
        }
        else
        {
            return url + "/";
        }
    }


    /**
     * Detect if the mojo is staging or deploying.
     *
     * @return <code>true</code> if the mojo is for deploy and not staging (local or deploy)
     */
    protected abstract boolean isDeploy();

    /**
     * Get the top distribution management site url, used for module relative path calculations.
     * This should be a top-level URL, ie above modules and locale sub-directories. Each deploy mojo
     * can tweak algorithm to determine this top site by implementing determineTopDistributionManagementSiteUrl().
     *
     * @return the site for deployment
     * @throws MojoExecutionException
     * @see #determineTopDistributionManagementSiteUrl()
     */
    protected String getTopDistributionManagementSiteUrl()
        throws MojoExecutionException
    {
        if ( topDistributionManagementSiteUrl == null )
        {
            topDistributionManagementSiteUrl = determineTopDistributionManagementSiteUrl();

            if ( !isDeploy() )
            {
                getLog().debug( "distributionManagement.site.url relative path: " + getDeployModuleDirectory() );
            }
        }
        return topDistributionManagementSiteUrl;
    }

    protected abstract String determineTopDistributionManagementSiteUrl()
        throws MojoExecutionException;

    /**
     * Get the site used for deployment, with its id to look up credential settings and the target URL for the deploy.
     * This should be a top-level URL, ie above modules and locale sub-directories. Each deploy mojo
     * can tweak algorithm to determine this deploy site by implementing determineDeploySite().
     *
     * @return the site for deployment
     * @throws MojoExecutionException
     * @see #determineDeploySite()
     */
    protected Site getDeploySite()
        throws MojoExecutionException
    {
        if ( deploySite == null )
        {
            deploySite = determineDeploySite();
        }
        return deploySite;
    }

    protected abstract Site determineDeploySite()
        throws MojoExecutionException;

    /**
     * Find the relative path between the distribution URLs of the top site and the current project.
     *
     * @return the relative path or "./" if the two URLs are the same.
     * @throws MojoExecutionException
     */
    protected String getDeployModuleDirectory()
        throws MojoExecutionException
    {
        String relative = siteTool.getRelativePath( getSite( project ).getUrl(),
                                                    getTopDistributionManagementSiteUrl() );

        // SiteTool.getRelativePath() uses File.separatorChar,
        // so we need to convert '\' to '/' in order for the URL to be valid for Windows users
        relative = relative.replace( '\\', '/' );

        return ( "".equals( relative ) ) ? "./" : relative;
    }

    /**
     * Use wagon to deploy the generated site to a given repository.
     *
     * @param repository the repository to deploy to.
     *                   This needs to contain a valid, non-null {@link Repository#getId() id}
     *                   to look up credentials for the deploy, and a valid, non-null
     *                   {@link Repository#getUrl() scm url} to deploy to.
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
            getLog().debug( "Deploying to '" + repository.getUrl() + "',\n    Using credentials from server id '"
                                + repository.getId() + "'" );
        }

        deploy( inputDirectory, repository );
    }

    private void deploy( final File directory, final Repository repository )
        throws MojoExecutionException
    {
        // TODO: work on moving this into the deployer like the other deploy methods
        final Wagon wagon = getWagon( repository, wagonManager );

        try
        {
            configureWagon( wagon, repository.getId(), settings, container, getLog() );
        }
        catch ( TransferFailedException e )
        {
            throw new MojoExecutionException( "Unable to configure Wagon: '" + repository.getProtocol() + "'", e );
        }

        try
        {
            final ProxyInfo proxyInfo;
            if ( !isMaven3OrMore() )
            {
                proxyInfo = getProxyInfo( repository, wagonManager );
            }
            else
            {
                try
                {
                    SettingsDecrypter settingsDecrypter = container.lookup( SettingsDecrypter.class );

                    proxyInfo = getProxy( repository, settingsDecrypter );
                }
                catch ( ComponentLookupException cle )
                {
                    throw new MojoExecutionException( "Unable to lookup SettingsDecrypter: " + cle.getMessage(), cle );
                }
            }

            push( directory, repository, wagon, proxyInfo, getLocales(), getDeployModuleDirectory() );

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

    private Wagon getWagon( final Repository repository, final WagonManager manager )
        throws MojoExecutionException
    {
        final Wagon wagon;

        try
        {
            wagon = manager.getWagon( repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            String shortMessage = "Unsupported protocol: '" + repository.getProtocol() + "' for site deployment to "
                + "distributionManagement.site.url=" + repository.getUrl() + ".";
            String longMessage =
                "\n" + shortMessage + "\n" + "Currently supported protocols are: " + getSupportedProtocols() + ".\n"
                    + "    Protocols may be added through wagon providers.\n" + "    For more information, see "
                    + "http://maven.apache.org/plugins/maven-site-plugin/examples/adding-deploy-protocol.html";

            getLog().error( longMessage );

            throw new MojoExecutionException( shortMessage );
        }
        catch ( TransferFailedException e )
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

    private String getSupportedProtocols()
    {
        try
        {
            Set<String> protocols = container.lookupMap( Wagon.class ).keySet();

            return StringUtils.join( protocols.iterator(), ", " );
        }
        catch ( ComponentLookupException e )
        {
            // in the unexpected case there is a problem when instantiating a wagon provider
            getLog().error( e );
        }
        return "";
    }

    private void push( final File inputDirectory, final Repository repository, final Wagon wagon,
                       final ProxyInfo proxyInfo, final List<Locale> localesList, final String relativeDir )
        throws MojoExecutionException
    {
        AuthenticationInfo authenticationInfo = wagonManager.getAuthenticationInfo( repository.getId() );
        getLog().debug( "authenticationInfo with id '" + repository.getId() + "': "
                            + ( ( authenticationInfo == null ) ? "-" : authenticationInfo.getUserName() ) );

        try
        {
            if ( getLog().isDebugEnabled() )
            {
                Debug debug = new Debug();

                wagon.addSessionListener( debug );

                wagon.addTransferListener( debug );
            }

            if ( proxyInfo != null )
            {
                getLog().debug( "connect with proxyInfo" );
                wagon.connect( repository, authenticationInfo, proxyInfo );
            }
            else if ( proxyInfo == null && authenticationInfo != null )
            {
                getLog().debug( "connect with authenticationInfo and without proxyInfo" );
                wagon.connect( repository, authenticationInfo );
            }
            else
            {
                getLog().debug( "connect without authenticationInfo and without proxyInfo" );
                wagon.connect( repository );
            }

            getLog().info( "Pushing " + inputDirectory );

            // Default is first in the list
            final String defaultLocale = localesList.get( 0 ).getLanguage();

            for ( Locale locale : localesList )
            {
                if ( locale.getLanguage().equals( defaultLocale ) )
                {
                    // TODO: this also uploads the non-default locales,
                    // is there a way to exclude directories in wagon?
                    getLog().info( "   >>> to " + repository.getUrl() + relativeDir );

                    wagon.putDirectory( inputDirectory, relativeDir );
                }
                else
                {
                    getLog().info( "   >>> to " + repository.getUrl() + locale.getLanguage() + "/" + relativeDir );

                    wagon.putDirectory( new File( inputDirectory, locale.getLanguage() ),
                                        locale.getLanguage() + "/" + relativeDir );
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

    private static void chmod( final Wagon wagon, final Repository repository, final String chmodOptions,
                               final String chmodMode )
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
     * @param repository   the Repository to extract the ProxyInfo from.
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
        for ( String nonProxyHost : StringUtils.split( nonProxyHostsAsString, ",;|" ) )
        {
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
                if ( StringUtils.isEmpty( nonProxyHostPrefix ) && StringUtils.isNotEmpty( nonProxyHostSuffix )
                    && host.endsWith( nonProxyHostSuffix ) )
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
     * Get proxy information for Maven 3.
     *
     * @param repository
     * @param settingsDecrypter
     * @return
     */
    private ProxyInfo getProxy( Repository repository, SettingsDecrypter settingsDecrypter )
    {
        String protocol = repository.getProtocol();
        String url = repository.getUrl();

        getLog().debug( "repository protocol " + protocol );

        String originalProtocol = protocol;
        // olamy: hackish here protocol (wagon hint in fact !) is dav
        // but the real protocol (transport layer) is http(s)
        // and it's the one use in wagon to find the proxy arghhh
        // so we will check both
        if ( StringUtils.equalsIgnoreCase( "dav", protocol ) && url.startsWith( "dav:" ) )
        {
            url = url.substring( 4 );
            if ( url.startsWith( "http" ) )
            {
                try
                {
                    URL urlSite = new URL( url );
                    protocol = urlSite.getProtocol();
                    getLog().debug( "found dav protocol so transform to real transport protocol " + protocol );
                }
                catch ( MalformedURLException e )
                {
                    getLog().warn( "fail to build URL with " + url );
                }

            }
        }
        else
        {
            getLog().debug( "getProxy 'protocol': " + protocol );
        }
        if ( mavenSession != null && protocol != null )
        {
            MavenExecutionRequest request = mavenSession.getRequest();

            if ( request != null )
            {
                List<Proxy> proxies = request.getProxies();

                if ( proxies != null )
                {
                    for ( Proxy proxy : proxies )
                    {
                        if ( proxy.isActive() && ( protocol.equalsIgnoreCase( proxy.getProtocol() )
                            || originalProtocol.equalsIgnoreCase( proxy.getProtocol() ) ) )
                        {
                            SettingsDecryptionResult result =
                                settingsDecrypter.decrypt( new DefaultSettingsDecryptionRequest( proxy ) );
                            proxy = result.getProxy();

                            ProxyInfo proxyInfo = new ProxyInfo();
                            proxyInfo.setHost( proxy.getHost() );
                            // so hackish for wagon the protocol is https for site dav:
                            // dav:https://dav.codehaus.org/mojo/
                            proxyInfo.setType( protocol ); //proxy.getProtocol() );
                            proxyInfo.setPort( proxy.getPort() );
                            proxyInfo.setNonProxyHosts( proxy.getNonProxyHosts() );
                            proxyInfo.setUserName( proxy.getUsername() );
                            proxyInfo.setPassword( proxy.getPassword() );

                            getLog().debug( "found proxyInfo "
                                                + ( "host:port " + proxyInfo.getHost() + ":" + proxyInfo.getPort()
                                                    + ", " + proxyInfo.getUserName() ) );

                            return proxyInfo;
                        }
                    }
                }
            }
        }
        getLog().debug( "getProxy 'protocol': " + protocol + " no ProxyInfo found" );
        return null;
    }

    /**
     * Configure the Wagon with the information from serverConfigurationMap ( which comes from settings.xml )
     *
     * @param wagon
     * @param repositoryId
     * @param settings
     * @param container
     * @param log
     * @throws TransferFailedException
     * @todo Remove when {@link WagonManager#getWagon(Repository) is available}. It's available in Maven 2.0.5.
     */
    private static void configureWagon( Wagon wagon, String repositoryId, Settings settings, PlexusContainer container,
                                        Log log )
        throws TransferFailedException
    {
        log.debug( " configureWagon " );

        // MSITE-25: Make sure that the server settings are inserted
        for ( Server server : settings.getServers() )
        {
            String id = server.getId();

            log.debug( "configureWagon server " + id );

            if ( id != null && id.equals( repositoryId ) && ( server.getConfiguration() != null ) )
            {
                final PlexusConfiguration plexusConf =
                    new XmlPlexusConfiguration( (Xpp3Dom) server.getConfiguration() );

                ComponentConfigurator componentConfigurator = null;
                try
                {
                    componentConfigurator =
                        (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE, "basic" );
                    if ( isMaven3OrMore() )
                    {
                        componentConfigurator.configureComponent( wagon, plexusConf,
                                                                  container.getContainerRealm() );
                    }
                    else
                    {
                        configureWagonWithMaven2( componentConfigurator, wagon, plexusConf, container );
                    }
                }
                catch ( final ComponentLookupException e )
                {
                    throw new TransferFailedException(
                        "While configuring wagon for \'" + repositoryId + "\': Unable to lookup wagon configurator."
                            + " Wagon configuration cannot be applied.", e );
                }
                catch ( ComponentConfigurationException e )
                {
                    throw new TransferFailedException( "While configuring wagon for \'" + repositoryId
                                                           + "\': Unable to apply wagon configuration.", e );
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

    private static void configureWagonWithMaven2( ComponentConfigurator componentConfigurator, Wagon wagon,
                                                  PlexusConfiguration plexusConf, PlexusContainer container )
        throws ComponentConfigurationException
    {
        // in Maven 2.x   :
        // * container.getContainerRealm() -> org.codehaus.classworlds.ClassRealm
        // * componentConfiguration 3rd param is org.codehaus.classworlds.ClassRealm
        // so use some reflection see MSITE-609
        try
        {
            Method methodContainerRealm = container.getClass().getMethod( "getContainerRealm" );
            ClassRealm realm = (ClassRealm) methodContainerRealm.invoke( container );

            Method methodConfigure = componentConfigurator.getClass().getMethod( "configureComponent",
                                                                                 new Class[]{ Object.class,
                                                                                     PlexusConfiguration.class,
                                                                                     ClassRealm.class } );

            methodConfigure.invoke( componentConfigurator, wagon, plexusConf, realm );
        }
        catch ( Exception e )
        {
            throw new ComponentConfigurationException(
                "Failed to configure wagon component for a Maven2 use " + e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Extract the distributionManagement site from the given MavenProject.
     *
     * @param project the MavenProject. Not null.
     * @return the project site. Not null.
     *         Also site.getUrl() and site.getId() are guaranteed to be not null.
     * @throws MojoExecutionException if any of the site info is missing.
     */
    protected static Site getSite( final MavenProject project )
        throws MojoExecutionException
    {
        final DistributionManagement distributionManagement = project.getDistributionManagement();

        if ( distributionManagement == null )
        {
            throw new MojoExecutionException( "Missing distribution management in project " + getFullName( project ) );
        }

        final Site site = distributionManagement.getSite();

        if ( site == null )
        {
            throw new MojoExecutionException( "Missing site information in the distribution management of the project "
                + getFullName( project ) );
        }

        if ( site.getUrl() == null || site.getId() == null )
        {
            throw new MojoExecutionException( "Missing site data: specify url and id for project "
                + getFullName( project ) );
        }

        return site;
    }

    private static String getFullName( MavenProject project )
    {
        return project.getName() + " (" + project.getGroupId() + ':' + project.getArtifactId() + ':'
            + project.getVersion() + ')';
    }

    /**
     * Extract the distributionManagement site of the top level parent of the given MavenProject.
     * This climbs up the project hierarchy and returns the site of the last project
     * for which {@link #getSite(org.apache.maven.project.MavenProject)} returns a site that resides in the
     * same site. Notice that it doesn't take into account if the parent is in the reactor or not.
     *
     * @param project the MavenProject. Not <code>null</code>.
     * @return the top level site. Not <code>null</code>.
     *         Also site.getUrl() and site.getId() are guaranteed to be not <code>null</code>.
     * @throws MojoExecutionException if no site info is found in the tree.
     * @see URIPathDescriptor#sameSite(java.net.URI)
     */
    protected MavenProject getTopLevelProject( MavenProject project )
        throws MojoExecutionException
    {
        Site site = getSite( project );

        MavenProject parent = project;

        while ( parent.getParent() != null )
        {
            MavenProject oldProject = parent;
            // MSITE-585, MNG-1943
            parent = siteTool.getParentProject( parent, reactorProjects, localRepository );

            Site oldSite = site;

            try
            {
                site = getSite( parent );
            }
            catch ( MojoExecutionException e )
            {
                return oldProject;
            }

            // MSITE-600
            URIPathDescriptor siteURI = new URIPathDescriptor( URIEncoder.encodeURI( site.getUrl() ), "" );
            URIPathDescriptor oldSiteURI = new URIPathDescriptor( URIEncoder.encodeURI( oldSite.getUrl() ), "" );

            if ( !siteURI.sameSite( oldSiteURI.getBaseURI() ) )
            {
                return oldProject;
            }
        }

        return parent;
    }

    private static class URIEncoder
    {
        private static final String MARK = "-_.!~*'()";
        private static final String RESERVED = ";/?:@&=+$,";

        public static String encodeURI( final String uriString )
        {
            final char[] chars = uriString.toCharArray();
            final StringBuilder uri = new StringBuilder( chars.length );

            // MSITE-750: wagon dav: pseudo-protocol
            if ( uriString.startsWith( "dav:http" ) )
            {
                // transform dav:http to dav-http
                chars[3] = '-';
            }

            for ( char c : chars )
            {
                if ( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' )
                        || MARK.indexOf( c ) != -1  || RESERVED.indexOf( c ) != -1 )
                {
                    uri.append( c );
                }
                else
                {
                    uri.append( '%' );
                    uri.append( Integer.toHexString( (int) c ) );
                }
            }
            return uri.toString();
        }
    }
}
