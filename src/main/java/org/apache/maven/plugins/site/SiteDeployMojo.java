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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.WagonConfigurationException;
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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Deploys the generated site using <code>scp</code> or <code>file</code>
 * protocol to the site URL specified in the
 * <code>&lt;distributionManagement&gt;</code> section of the POM.
 * <p>
 * For <code>scp</code> protocol, the website files are packaged into zip archive,
 * then the archive is transfered to the remote host, next it is un-archived.
 * This method of deployment should normally be much faster
 * than making a file by file copy.  For <code>file</code> protocol, the files are copied
 * directly to the destination directory.
 * </p>
 *
 * @author <a href="mailto:michal@org.codehaus.org">Michal Maczka</a>
 * @version $Id$
 * @goal deploy
 */
public class SiteDeployMojo
    extends AbstractMojo
    implements Contextualizable
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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

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
     * @parameter expression="${session}"
     * @required
     * @readonly
     * @since 3.0-beta-2
     */
    protected MavenSession mavenSession;    
    
    
    /**
     * @since 3.0-beta-2
     * @component
     * @readonly
     */
    private SettingsDecrypter settingsDecrypter;    

    private PlexusContainer container;

    // FIXME too much duplicate code with SiteStageDeployMojo
    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( !inputDirectory.exists() )
        {
            throw new MojoExecutionException( "The site does not exist, please run site:site first" );
        }

        DistributionManagement distributionManagement = project.getDistributionManagement();

        if ( distributionManagement == null )
        {
            throw new MojoExecutionException( "Missing distribution management information in the project" );
        }

        Site site = distributionManagement.getSite();

        if ( site == null )
        {
            throw new MojoExecutionException(
                "Missing site information in the distribution management element in the project.." );
        }

        String url = site.getUrl();

        String id = site.getId();

        if ( url == null )
        {
            throw new MojoExecutionException( "The URL to the site is missing in the project descriptor." );
        }
        getLog().debug( "The site will be deployed to url '" + url + "' with id '" + id + "'");

        Repository repository = new Repository( id, url );

        // TODO: work on moving this into the deployer like the other deploy methods

        Wagon wagon;
        
        String protocol = repository.getProtocol();
        
        getLog().debug( "repository protocol " + protocol );
        
        ProxyInfo proxyInfo = getProxy( protocol, url, getLog(), mavenSession, settingsDecrypter );
        getLog().debug( "found proxyInfo "
                            + ( proxyInfo == null ? "null" : "host:port " + proxyInfo.getHost() + ":"
                                + proxyInfo.getPort() + ", " + proxyInfo.getUserName() ) );
        AuthenticationInfo authenticationInfo = wagonManager.getAuthenticationInfo( id );
        getLog().debug( "authenticationInfo with id '" + id + "' : " + authenticationInfo.getUserName() );        
        
        try
        {
            wagon = wagonManager.getWagon( repository );
            configureWagon( wagon, repository, settings, container, getLog() );
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
            wagon.putDirectory( inputDirectory, "." );

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
    public static ProxyInfo getProxyInfo( Repository repository, WagonManager wagonManager, Log log )
    {
        
        ProxyInfo proxyInfo = wagonManager.getProxy( repository.getProtocol() );

        
        if ( proxyInfo == null )
        {
            log.debug( "no ProxyInfo for Repository 'id' " + repository.getId() + " 'protocol' " + repository.getProtocol() );
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
                String nonProxyHostPrefix = StringUtils.substringBefore( nonProxyHost, "*" );
                String nonProxyHostSuffix = StringUtils.substringAfter( nonProxyHost, "*" );
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
    
    public static ProxyInfo getProxy( String protocol, String url, Log log, MavenSession mavenSession, SettingsDecrypter settingsDecrypter )
    {
        String originalProtocol = protocol;
        // olamy : hackish here protocol (wagon hint in fact !) is dav
        // but the real protocol (transport layer) is http(s) 
        // and it's the one use in wagon to find the proxy arghhh
        // so we will check both
        if (StringUtils.equalsIgnoreCase( "dav", protocol ) && StringUtils.startsWith( url, "dav:" ))
        {
            url = StringUtils.substringAfter( url, "dav:" );
            if (StringUtils.startsWith( url, "http" ))
            {
                try
                {
                    URL urlSite = new URL( url );
                    protocol = urlSite.getProtocol();
                    log.debug( "find dav protocol so transform to real transport protocol " + protocol );
                }
                catch ( MalformedURLException e )
                {
                    log.warn( "fail to build URL with " + url );
                }
                
            }
        }
        else
        {
            log.debug( "getProxy 'protocol' : " +  protocol );
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
                        if ( proxy.isActive()
                            && ( protocol.equalsIgnoreCase( proxy.getProtocol() ) || originalProtocol
                                .equalsIgnoreCase( proxy.getProtocol() ) ) )
                        {
                            SettingsDecryptionResult result = settingsDecrypter
                                .decrypt( new DefaultSettingsDecryptionRequest( proxy ) );
                            proxy = result.getProxy();

                            ProxyInfo proxyInfo = new ProxyInfo();
                            proxyInfo.setHost( proxy.getHost() );
                            // so hackish for wagon the protocol is https for site dav : dav:https://dav.codehaus.org/mojo/ 
                            proxyInfo.setType( protocol );//proxy.getProtocol() );
                            proxyInfo.setPort( proxy.getPort() );
                            proxyInfo.setNonProxyHosts( proxy.getNonProxyHosts() );
                            proxyInfo.setUserName( proxy.getUsername() );
                            proxyInfo.setPassword( proxy.getPassword() );

                            return proxyInfo;
                        }
                    }
                }
            }
        }
        log.debug( "getProxy 'protocol' : " +  protocol  + " no ProxyInfo found");
        return null;
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
    static void configureWagon( Wagon wagon, Repository repository, Settings settings, PlexusContainer container,
                                Log log )
        throws WagonConfigurationException
    {
        log.debug( " configureWagon " );
                
        // MSITE-25: Make sure that the server settings are inserted
        for ( int i = 0; i < settings.getServers().size(); i++ )
        {
            Server server = (Server) settings.getServers().get( i );
            log.debug( "configureWagon server " + server.getId() );
            String id = server.getId();
            if ( id != null && id.equals( repository.getId() ) )
            {
                if ( server.getConfiguration() != null )
                {
                    final PlexusConfiguration plexusConf =
                        new XmlPlexusConfiguration( (Xpp3Dom) server.getConfiguration() );

                    ComponentConfigurator componentConfigurator = null;
                    try
                    {
                        // FIXME role-hint basic ?
                        componentConfigurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE );
                        componentConfigurator.configureComponent( wagon, plexusConf, container.getContainerRealm() );
                    }
                    catch ( final ComponentLookupException e )
                    {
                        throw new WagonConfigurationException( repository.getId(), "Unable to lookup wagon configurator."
                            + " Wagon configuration cannot be applied.", e );
                    }
                    catch ( ComponentConfigurationException e )
                    {
                        throw new WagonConfigurationException( repository.getId(), "Unable to apply wagon configuration.",
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

}
