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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
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
import org.codehaus.plexus.util.StringUtils;

/**
 * Utilities methods to play with repository
 *
 * @version $Id$
 * @since 2.1
 */
public class RepositoryUtils
{
    private static final List<String> UNKNOWN_HOSTS = new ArrayList<String>();

    private final Log log;

    private final WagonManager wagonManager;

    private final Settings settings;

    private final MavenProjectBuilder mavenProjectBuilder;

    private final ArtifactFactory factory;

    private final List<ArtifactRepository> remoteRepositories;

    private final List<ArtifactRepository> pluginRepositories;

    private final ArtifactResolver resolver;

    private final ArtifactRepository localRepository;

    /**
     * @param log
     * @param wagonManager
     * @param settings
     * @param mavenProjectBuilder
     * @param factory
     * @param resolver
     * @param remoteRepositories
     * @param pluginRepositories
     * @param localRepository
     * @param repositoryMetadataManager
     */
    public RepositoryUtils( Log log, WagonManager wagonManager, Settings settings,
                            MavenProjectBuilder mavenProjectBuilder, ArtifactFactory factory,
                            ArtifactResolver resolver, List<ArtifactRepository> remoteRepositories,
                            List<ArtifactRepository> pluginRepositories, ArtifactRepository localRepository,
                            RepositoryMetadataManager repositoryMetadataManager )
    {
        this.log = log;
        this.wagonManager = wagonManager;
        this.settings = settings;
        this.mavenProjectBuilder = mavenProjectBuilder;
        this.factory = factory;
        this.resolver = resolver;
        this.remoteRepositories = remoteRepositories;
        this.pluginRepositories = pluginRepositories;
        this.localRepository = localRepository;
    }

    /**
     * @return localrepo
     */
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     * @return remote artifact repo
     */
    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        return remoteRepositories;
    }

    /**
     * @return plugin artifact repo
     */
    public List<ArtifactRepository> getPluginArtifactRepositories()
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
        List<ArtifactRepository> repos =
            new ArrayList<ArtifactRepository>( pluginRepositories.size() + remoteRepositories.size() );
        repos.addAll( pluginRepositories );
        repos.addAll( remoteRepositories );

        resolver.resolve( artifact, repos, localRepository );
    }

    /**
     * @param repo not null
     * @param artifact not null
     * @return <code>true</code> if the artifact exists in the given repo, <code>false</code> otherwise or if
     * the repo is blacklisted.
     */
    public boolean dependencyExistsInRepo( ArtifactRepository repo, Artifact artifact )
    {
        if ( repo.isBlacklisted() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "The repo '" + repo.getId() + "' is black listed - Ignored it" );
            }
            return false;
        }

        if ( UNKNOWN_HOSTS.contains( repo.getUrl() ) )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "The repo url '" + repo.getUrl() + "' is unknowned - Ignored it" );
            }
            return false;
        }

        repo = wagonManager.getMirrorRepository( repo );

        String id = repo.getId();
        Repository repository = new Repository( id, repo.getUrl() );

        Wagon wagon;
        try
        {
            wagon = wagonManager.getWagon( repository );
        }
        catch ( UnsupportedProtocolException e )
        {
            logError( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
            return false;
        }
        catch ( WagonConfigurationException e )
        {
            logError( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
            return false;
        }

        wagon.setTimeout( 1000 );

        if ( log.isDebugEnabled() )
        {
            Debug debug = new Debug();

            wagon.addSessionListener( debug );
            wagon.addTransferListener( debug );
        }

        try
        {
            // FIXME when upgrading to maven 3.x : this must be changed.
            AuthenticationInfo auth = wagonManager.getAuthenticationInfo( repo.getId() );

            ProxyInfo proxyInfo = getProxyInfo();
            if ( proxyInfo != null )
            {
                wagon.connect( repository, auth, proxyInfo );
            }
            else
            {
                wagon.connect( repository, auth );
            }

            return wagon.resourceExists( StringUtils.replace( getDependencyUrlFromRepository( artifact, repo ),
                                                              repo.getUrl(), "" ) );
        }
        catch ( ConnectionException e )
        {
            logError( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        catch ( AuthenticationException e )
        {
            logError( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        catch ( TransferFailedException e )
        {
            if ( e.getCause() instanceof UnknownHostException )
            {
                log.error( "Unknown host " + e.getCause().getMessage() + " - ignored it" );
                UNKNOWN_HOSTS.add( repo.getUrl() );
            }
            else
            {
                logError( "Unable to determine if resource " + artifact + " exists in " + repo.getUrl(), e );
            }
            return false;
        }
        catch ( AuthorizationException e )
        {
            logError( "Unable to connect to: " + repo.getUrl(), e );
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
                logError( "Error disconnecting wagon - ignored", e );
            }
        }
    }

    /**
     * Get the <code>Maven project</code> from the repository depending the <code>Artifact</code> given.
     *
     * @param artifact an artifact
     * @return the Maven project for the given artifact
     * @throws ProjectBuildingException if any
     */
    public MavenProject getMavenProjectFromRepository( Artifact artifact )
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

    /**
     * @param artifact not null
     * @param repo not null
     * @return the artifact url in the given repo for the given artifact. If it is a snapshot artifact, the version
     * will be the timestamp and the build number from the metadata. Could return null if the repo is blacklisted.
     */
    public String getDependencyUrlFromRepository( Artifact artifact, ArtifactRepository repo )
    {
        if ( repo.isBlacklisted() )
        {
            return null;
        }

        Artifact copyArtifact = ArtifactUtils.copyArtifact( artifact );
        // Try to get the last artifact repo name depending the snapshot version
        if ( ( artifact.isSnapshot() && repo.getSnapshots().isEnabled() ) )
        {
            if ( artifact.getBaseVersion().equals( artifact.getVersion() ) )
            {
                // Try to resolve it if not already done
                if ( artifact.getMetadataList() == null || artifact.getMetadataList().isEmpty() )
                {
                    try
                    {
                        resolve( artifact );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " could not be resolved." );
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " was not found." );
                    }
                }

                for ( ArtifactMetadata m : artifact.getMetadataList() )
                {
                    if ( m instanceof SnapshotArtifactRepositoryMetadata )
                    {
                        SnapshotArtifactRepositoryMetadata snapshotMetadata = (SnapshotArtifactRepositoryMetadata) m;

                        Metadata metadata = snapshotMetadata.getMetadata();
                        if ( metadata.getVersioning() == null || metadata.getVersioning().getSnapshot() == null
                            || metadata.getVersioning().getSnapshot().isLocalCopy()
                            || metadata.getVersioning().getSnapshot().getTimestamp() == null )
                        {
                            continue;
                        }

                        // create the version according SnapshotTransformation
                        String version =
                            StringUtils.replace( copyArtifact.getVersion(), Artifact.SNAPSHOT_VERSION,
                                                 metadata.getVersioning().getSnapshot().getTimestamp() )
                                + "-" + metadata.getVersioning().getSnapshot().getBuildNumber();
                        copyArtifact.setVersion( version );
                    }
                }
            }
        }

        return repo.getUrl() + "/" + repo.pathOf( copyArtifact );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Convenience method to map a <code>Proxy</code> object from the user system settings to a <code>ProxyInfo</code>
     * object.
     *
     * @return a proxyInfo object instanced or null if no active proxy is define in the settings.xml
     */
    private ProxyInfo getProxyInfo()
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
     * Log an error, adding the stacktrace only is debug is enabled.
     * 
     * @param message the error message
     * @param e the cause
     */
    private void logError( String message, Exception e )
    {
        if ( log.isDebugEnabled() )
        {
            log.error( message, e );
        }
        else
        {
            log.error( message );
        }
    }
}
