package org.apache.maven.plugin.assembly.artifact;

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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.artifact.DependencyResolver" role-hint="default"
 *
 * @author jdcasey
 * @version $Id$
 */
public class DefaultDependencyResolver
    extends AbstractLogEnabled implements DependencyResolver, Contextualizable
{

    private static final String[] PREFERRED_RESOLVER_HINTS = {
      "project-cache-aware", // Provided in Maven 2.1-SNAPSHOT
      "default"
    };

    // Commenting this out as a component requirement, so we can look for the new
    // resolver in maven 2.1-snapshot, then fail back to the default one.
//     * @plexus.requirement
    private ArtifactResolver resolver;

    /**
     * @plexus.requirement
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    public DefaultDependencyResolver()
    {
        // for plexus init
    }

    public DefaultDependencyResolver( ArtifactResolver resolver, ArtifactMetadataSource metadataSource,
                               ArtifactFactory factory, Logger logger )
    {
        this.resolver = resolver;
        this.metadataSource = metadataSource;
        this.factory = factory;

        enableLogging( logger );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.assembly.artifact.DependencyResolver#resolveDependencies(org.apache.maven.project.MavenProject, java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public Set resolveDependencies( MavenProject project, String scope, ArtifactRepository localRepository,
                                    List remoteRepositories, boolean resolveTransitively )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        List repos = aggregateRemoteArtifactRepositories( remoteRepositories, project );

        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        Artifact artifact =
            factory.createBuildArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                         project.getPackaging() );

        Set dependencyArtifacts =
            MavenMetadataSource.createArtifacts( factory, project.getDependencies(), null, filter, project );

        getLogger().debug( "Project dependencies are:\n" + StringUtils.join( dependencyArtifacts.iterator(), "\n" ) );

        if ( resolveTransitively )
        {
            getLogger().debug( "Resolving project dependencies transitively." );
            return resolveTransitively( dependencyArtifacts, artifact, localRepository, repos, filter, project );
        }
        else
        {
            getLogger().debug( "Resolving project dependencies ONLY. Transitive dependencies WILL NOT be included in the results." );
            return resolveNonTransitively( dependencyArtifacts, artifact, localRepository, repos, filter );
        }
    }

    private Set resolveNonTransitively( Set dependencyArtifacts, Artifact artifact, ArtifactRepository localRepository,
                                        List repos, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        for ( Iterator it = dependencyArtifacts.iterator(); it.hasNext(); )
        {
            Artifact depArtifact = (Artifact) it.next();

            resolver.resolve( depArtifact, repos, localRepository );
        }

        return dependencyArtifacts;
    }

    private Set resolveTransitively( Set dependencyArtifacts, Artifact artifact, ArtifactRepository localRepository,
                                     List repos, ArtifactFilter filter, MavenProject project )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactResolutionResult result;
        try
        {
            result = resolver.resolveTransitively( dependencyArtifacts, artifact, getManagedVersionMap( project ), localRepository, repos, metadataSource, filter );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new InvalidDependencyVersionException( e.getMessage(), e );
        }

        getLogger().debug( "While resolving dependencies of " + project.getId() + ":" );

        FilterUtils.reportFilteringStatistics( Collections.singleton( filter ), getLogger() );

        return result.getArtifacts();
    }

    private List aggregateRemoteArtifactRepositories( List remoteRepositories, MavenProject project )
    {
        List repoLists = new ArrayList();

        repoLists.add( remoteRepositories );
        repoLists.add( project.getRemoteArtifactRepositories() );

        List remoteRepos = new ArrayList();
        Set encounteredUrls = new HashSet();

        for ( Iterator listIterator = repoLists.iterator(); listIterator.hasNext(); )
        {
            List repositoryList = ( List ) listIterator.next();

            if ( ( repositoryList != null ) && !repositoryList.isEmpty() )
            {
                for ( Iterator it = repositoryList.iterator(); it.hasNext(); )
                {
                    ArtifactRepository repo = ( ArtifactRepository ) it.next();

                    if ( !encounteredUrls.contains( repo.getUrl() ) )
                    {
                        remoteRepos.add( repo );
                        encounteredUrls.add( repo.getUrl() );
                    }
                }
            }
        }

        return remoteRepos;
    }

    // TODO: Remove this, once we can depend on Maven 2.0.7 or later...in which
    // MavenProject.getManagedVersionMap() exists. This is from MNG-1577.
    private Map getManagedVersionMap( MavenProject project )
        throws InvalidVersionSpecificationException
    {
        DependencyManagement dependencyManagement = project.getModel().getDependencyManagement();

        Map map = null;
        List deps;
        if ( ( dependencyManagement != null ) && ( ( deps = dependencyManagement.getDependencies() ) != null )
             && ( deps.size() > 0 ) )
        {
            map = new HashMap();

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Adding managed dependencies for " + project.getId() );
            }

            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                Artifact artifact = factory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                              versionRange, d.getType(),
                                                                              d.getClassifier(), d.getScope(),
                                                                              d.isOptional() );
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "  " + artifact );
                }

                // If the dependencyManagement section listed exclusions,
                // add them to the managed artifacts here so that transitive
                // dependencies will be excluded if necessary.
                if ( ( null != d.getExclusions() ) && !d.getExclusions().isEmpty() )
                {
                    List exclusions = new ArrayList();
                    Iterator exclItr = d.getExclusions().iterator();
                    while ( exclItr.hasNext() )
                    {
                        Exclusion e = (Exclusion) exclItr.next();
                        exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                    }
                    ExcludesArtifactFilter eaf = new ExcludesArtifactFilter( exclusions );
                    artifact.setDependencyFilter( eaf );
                }
                else
                {
                    artifact.setDependencyFilter( null );
                }
                map.put( d.getManagementKey(), artifact );
            }
        }
        else if ( map == null )
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        PlexusContainer container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );

        for ( int i = 0; i < PREFERRED_RESOLVER_HINTS.length; i++ )
        {
            String hint = PREFERRED_RESOLVER_HINTS[i];

            try
            {
                resolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE, hint );
                break;
            }
            catch ( ComponentLookupException e )
            {
                getLogger().debug( "Cannot find ArtifactResolver with hint: " + hint, e );
            }
        }

        if ( resolver == null )
        {
            try
            {
                resolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );
            }
            catch ( ComponentLookupException e )
            {
                getLogger().debug( "Cannot find ArtifactResolver with no hint.", e );
            }
        }

        if ( resolver == null )
        {
            throw new ContextException( "Failed to lookup a valid ArtifactResolver implementation. Tried hints:\n"
                                        + Arrays.asList( PREFERRED_RESOLVER_HINTS ) );
        }
    }
}
