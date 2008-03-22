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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Deploys the site using scp/file protocol.
 * For scp protocol, website files are packaged into zip archive,
 * then the archive is transfered to the remote host, next it is un-archived.
 * This method of deployment should normally be much faster
 * then making a file by file copy.  For file protocol, the files are copied
 * directly to the destination directory.
 *
 * @author <a href="mailto:michal@org.codehaus.org">Michal Maczka</a>
 * @version $Id$
 * @goal deploy
 */
public class SiteDeployMojo
    extends AbstractMojo implements Contextualizable
{
    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter alias="outputDirectory" expression="${project.reporting.outputDirectory}"
     * @required
     */
    private File inputDirectory;

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

    private PlexusContainer container;

    /** Map( String, XmlPlexusConfiguration ) with the repository id and the wagon configuration */
    private Map serverConfigurationMap = new HashMap();

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

        Repository repository = new Repository( id, url );

        // TODO: work on moving this into the deployer like the other deploy methods

        Wagon wagon;

        try
        {
            // @todo Use WagonManager#getWagon(Repository) when available. It's available in Maven 2.0.5.
            wagon = wagonManager.getWagon( repository.getProtocol() );
            configureWagon( wagon, repository.getId() );
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

            ProxyInfo proxyInfo = getProxyInfo( repository, wagonManager );
            if ( proxyInfo != null )
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( id ), proxyInfo );
            }
            else
            {
                wagon.connect( repository, wagonManager.getAuthenticationInfo( id ) );
            }

            wagon.putDirectory( inputDirectory, "." );

            // TODO: current wagon uses zip which will use the umask on remote host instead of honouring our settings
            //  Force group writeable
            if ( wagon instanceof CommandExecutor )
            {
                CommandExecutor exec = (CommandExecutor) wagon;
                exec.executeCommand( "chmod -Rf g+w,a+rX " + repository.getBasedir() );
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
     * and the <code>protocol</code> of the given <code>repository</code> 
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
                String nonProxyHostPrefix = StringUtils.substringBefore( nonProxyHost, "*" );
                String nonProxyHostSuffix = StringUtils.substringAfter( nonProxyHost, "*" );
                // prefix*
                if ( StringUtils.isNotEmpty(nonProxyHostPrefix) && host.startsWith( nonProxyHostPrefix )
                    && StringUtils.isEmpty(nonProxyHostSuffix) )
                {
                    return null;
                }
                // *suffix
                if ( StringUtils.isEmpty(nonProxyHostPrefix)
                    && StringUtils.isNotEmpty(nonProxyHostSuffix) && host.endsWith( nonProxyHostSuffix ))
                {
                    return null;
                }
                // prefix*suffix
                if ( StringUtils.isNotEmpty(nonProxyHostPrefix) && host.startsWith( nonProxyHostPrefix )
                    && StringUtils.isNotEmpty(nonProxyHostSuffix) && host.endsWith( nonProxyHostSuffix ))
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
     * @throws WagonConfigurationException
     */
    private void configureWagon( Wagon wagon, String repositoryId )
        throws WagonConfigurationException
    {
        // MSITE-25: Make sure that the server settings are inserted
        for ( int i = 0; i < settings.getServers().size(); i++ )
        {
            Server server = (Server) settings.getServers().get( i );
            if ( server.getConfiguration() != null )
            {
                final XmlPlexusConfiguration xmlConf =
                    new XmlPlexusConfiguration( (Xpp3Dom) server.getConfiguration() );
                serverConfigurationMap.put( server.getId(), xmlConf );
            }
        }

        if ( serverConfigurationMap.containsKey( repositoryId ) )
        {
            ComponentConfigurator componentConfigurator = null;
            try
            {
                componentConfigurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE );
                componentConfigurator.configureComponent( wagon, (PlexusConfiguration) serverConfigurationMap
                    .get( repositoryId ), container.getContainerRealm() );
            }
            catch ( final ComponentLookupException e )
            {
                throw new WagonConfigurationException( repositoryId, "Unable to lookup wagon configurator. Wagon configuration cannot be applied.", e );
            }
            catch ( ComponentConfigurationException e )
            {
                throw new WagonConfigurationException( repositoryId, "Unable to apply wagon configuration.", e );
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
                        getLog().error( "Problem releasing configurator - ignoring: " + e.getMessage() );
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
