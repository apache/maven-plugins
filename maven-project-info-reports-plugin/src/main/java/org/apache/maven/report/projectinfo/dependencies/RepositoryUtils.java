package org.apache.maven.report.projectinfo.dependencies;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id$
 * @since 2.1
 */
public class RepositoryUtils
{
    private Log log;

    private WagonManager wagonManager;

    private Settings settings;

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactFactory factory;

    private List remoteRepositories;

    private List pluginRepositories;

    private ArtifactResolver resolver;

    private ArtifactRepository localRepository;

    /**
     * @param wagonManager
     * @param settings
     * @param mavenProjectBuilder
     * @param factory
     * @param resolver
     * @param remoteRepositories
     * @param pluginRepositories
     * @param localRepository
     */
    public RepositoryUtils( WagonManager wagonManager, Settings settings, MavenProjectBuilder mavenProjectBuilder,
                            ArtifactFactory factory, ArtifactResolver resolver, List remoteRepositories,
                            List pluginRepositories, ArtifactRepository localRepository )
    {
        this.wagonManager = wagonManager;
        this.settings = settings;
        this.mavenProjectBuilder = mavenProjectBuilder;
        this.factory = factory;
        this.resolver = resolver;
        this.remoteRepositories = remoteRepositories;
        this.pluginRepositories = pluginRepositories;
        this.localRepository = localRepository;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getRemoteArtifactRepositories()
    {
        return remoteRepositories;
    }

    public List getPluginArtifactRepositories()
    {
        return pluginRepositories;
    }

    /**
     * @param artifact not null
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException if any
     * @see ArtifactResolver#resolve(Artifact, List, ArtifactRepository)
     */
    public void resolve( Artifact artifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        List repos = new ArrayList();
        repos.addAll( pluginRepositories );
        repos.addAll( remoteRepositories );

        resolver.resolve( artifact, repos, localRepository );
    }

    /**
     * @param repo not null
     * @param artifact not null
     * @return <code>true</code> if the artifact exists in the given repo, <code>false</ccode> otherwise
     */
    public boolean dependencyExistsInRepo( ArtifactRepository repo, Artifact artifact )
    {
        String id = repo.getId();
        Repository repository = new Repository( id, repo.getUrl() );

        Wagon wagon;
        try
        {
            wagon = wagonManager.getWagon( repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            log.error( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
            return false;
        }
        catch ( WagonConfigurationException e )
        {
            log.error( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
            return false;
        }

        if ( log.isDebugEnabled() )
        {
            Debug debug = new Debug();

            wagon.addSessionListener( debug );
            wagon.addTransferListener( debug );
        }

        try
        {
            AuthenticationInfo auth = wagonManager.getAuthenticationInfo( repo.getId() );

            ProxyInfo proxyInfo = getProxyInfo( settings );
            if ( proxyInfo != null )
            {
                wagon.connect( repository, auth, proxyInfo );
            }
            else
            {
                wagon.connect( repository, auth );
            }

            return ( wagon.resourceExists( repo.pathOf( artifact ) ) );
        }
        catch ( ConnectionException e )
        {
            log.error( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        catch ( AuthenticationException e )
        {
            log.error( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        catch ( TransferFailedException e )
        {
            log.error( "Unable to determine if resource " + artifact + " exists in " + repo.getUrl(), e );
            return false;
        }
        catch ( AuthorizationException e )
        {
            log.error( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        catch ( AbstractMethodError e )
        {
            log.error( "Wagon " + wagon.getClass().getName() + " does not support the resourceExists method" );
            return false;
        }
        finally
        {
            try
            {
                wagon.disconnect();
            }
            catch ( ConnectionException e )
            {
                log.error( "Error disconnecting wagon - ignored", e );
            }
        }
    }

    /**
     * Convenience method to map a <code>Proxy</code> object from the user system settings to a <code>ProxyInfo</code>
     * object.
     *
     * @param settings the system settings
     * @return a proxyInfo object instancied or null if no active proxy is define in the settings.xml
     */
    public ProxyInfo getProxyInfo( Settings settings )
    {
        ProxyInfo proxyInfo = null;
        if ( settings != null && settings.getActiveProxy() != null )
        {
            Proxy settingsProxy = settings.getActiveProxy();

            proxyInfo = new ProxyInfo();
            proxyInfo.setHost( settingsProxy.getHost() );
            proxyInfo.setType( settingsProxy.getProtocol() );
            proxyInfo.setPort( settingsProxy.getPort() );
            proxyInfo.setNonProxyHosts( settingsProxy.getNonProxyHosts() );
            proxyInfo.setUserName( settingsProxy.getUsername() );
            proxyInfo.setPassword( settingsProxy.getPassword() );
        }

        return proxyInfo;
    }

    /**
     * Get the <code>Maven project</code> from the repository depending the <code>Artifact</code> given.
     *
     * @param artifact an artifact
     * @param localRepository the local repository
     * @return the Maven project for the given artifact
     * @throws org.apache.maven.project.ProjectBuildingException if any
     */
    public MavenProject getMavenProjectFromRepository( Artifact artifact, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        Artifact projectArtifact = artifact;

        boolean allowStubModel = false;
        if ( !"pom".equals( artifact.getType() ) )
        {
            projectArtifact = factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                             artifact.getVersion(), artifact.getScope() );
            allowStubModel = true;
        }

        // TODO: we should use the MavenMetadataSource instead
        return mavenProjectBuilder.buildFromRepository( projectArtifact, remoteRepositories, localRepository,
                                                        allowStubModel );
    }
}
