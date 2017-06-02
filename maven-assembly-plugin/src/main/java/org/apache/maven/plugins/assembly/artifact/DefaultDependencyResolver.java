package org.apache.maven.plugins.assembly.artifact;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.phase.ModuleSetAssemblyPhase;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.resolved.AssemblyId;
import org.apache.maven.plugins.assembly.utils.FilterUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.artifact.filter.resolve.transform.ArtifactIncludeFilterTransformer;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author jdcasey
 * @version $Id$
 */
@Component( role = DependencyResolver.class )
public class DefaultDependencyResolver
    extends AbstractLogEnabled
    implements DependencyResolver
{
    @Requirement
    private RepositorySystem resolver;
    
    @Requirement
    private org.apache.maven.shared.dependencies.resolve.DependencyResolver dependencyResolver;

    @Override
    public Map<DependencySet, Set<Artifact>> resolveDependencySets( final Assembly assembly, ModuleSet moduleSet,
                                                                    final AssemblerConfigurationSource configSource,
                                                                    List<DependencySet> dependencySets )
        throws DependencyResolutionException
    {
        Map<DependencySet, Set<Artifact>> result = new LinkedHashMap<DependencySet, Set<Artifact>>();

        for ( DependencySet dependencySet : dependencySets )
        {

            final MavenProject currentProject = configSource.getProject();

            final ResolutionManagementInfo info = new ResolutionManagementInfo( currentProject );
            updateRepositoryResolutionRequirements( assembly, info );
            final AssemblyId assemblyId = AssemblyId.createAssemblyId( assembly );
            updateDependencySetResolutionRequirements( dependencySet, info, assemblyId,
                                                       configSource.getMavenSession().getProjectBuildingRequest(),
                                                       currentProject );
            updateModuleSetResolutionRequirements( assemblyId, moduleSet, dependencySet, info, configSource );

            resolve( assembly, configSource, result, dependencySet, info );

        }
        return result;
    }

    private void resolve( Assembly assembly, AssemblerConfigurationSource configSource,
                          Map<DependencySet, Set<Artifact>> result, DependencySet dependencySet,
                          ResolutionManagementInfo info )
        throws DependencyResolutionException
    {
        Set<Artifact> artifacts;
        if ( info.isResolutionRequired() )
        {
            final List<ArtifactRepository> repos =
                aggregateRemoteArtifactRepositories( configSource.getRemoteRepositories(), info.getEnabledProjects() );

            artifacts = info.getArtifacts();
            if ( info.isResolvedTransitively() )
            {
                getLogger().debug( "Resolving project dependencies transitively." );
                
                ArtifactFilter filter = new ArtifactIncludeFilterTransformer().transform( info.getScopeFilter() );
                artifacts = resolveTransitively( artifacts, repos, filter, configSource );
            }
            else
            {
                getLogger().debug( "Resolving project dependencies ONLY. "
                                       + "Transitive dependencies WILL NOT be included in the results." );
                artifacts = resolveNonTransitively( assembly, artifacts, configSource, repos );
            }
        }
        else
        {
            artifacts = new HashSet<Artifact>();
        }
        result.put( dependencySet, artifacts );
    }

    @Override
    public Map<DependencySet, Set<Artifact>> resolveDependencySets( final Assembly assembly,
                                                                    final AssemblerConfigurationSource configSource,
                                                                    List<DependencySet> dependencySets )
        throws DependencyResolutionException
    {
        Map<DependencySet, Set<Artifact>> result = new LinkedHashMap<DependencySet, Set<Artifact>>();

        for ( DependencySet dependencySet : dependencySets )
        {

            final MavenProject currentProject = configSource.getProject();

            final ResolutionManagementInfo info = new ResolutionManagementInfo( currentProject );
            updateRepositoryResolutionRequirements( assembly, info );
            final AssemblyId assemblyId = AssemblyId.createAssemblyId( assembly );
            updateDependencySetResolutionRequirements( dependencySet, info, assemblyId,
                                                       configSource.getMavenSession().getProjectBuildingRequest(),
                                                       currentProject );

            resolve( assembly, configSource, result, dependencySet, info );

        }
        return result;
    }

    Set<Artifact> resolveNonTransitively( final Assembly assembly, final Set<Artifact> dependencyArtifacts,
                                          final AssemblerConfigurationSource configSource,
                                          final List<ArtifactRepository> repos )
        throws DependencyResolutionException
    {

        final List<Artifact> missing = new ArrayList<Artifact>();
        final Set<Artifact> resolved = new LinkedHashSet<Artifact>();
        for ( final Artifact depArtifact : dependencyArtifacts )
        {
            ArtifactResolutionRequest req = new ArtifactResolutionRequest();
            req.setLocalRepository( configSource.getLocalRepository() );
            req.setRemoteRepositories( repos );
            req.setArtifact( depArtifact );

            ArtifactResolutionResult resolve = resolver.resolve( req );
            if ( resolve.hasExceptions() )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Failed to resolve: " + depArtifact.getId() + " for assembly: " + assembly.getId() );
                }
                missing.add( depArtifact );
            }
            else
            {
                resolved.add( depArtifact );
            }
        }

        if ( !missing.isEmpty() )

        {
            final MavenProject project = configSource.getProject();
            final Artifact rootArtifact = project.getArtifact();

            final Throwable error =
                new MultipleArtifactsNotFoundException( rootArtifact, new ArrayList<Artifact>( resolved ), missing,
                                                        repos );

            throw new DependencyResolutionException( "Failed to resolve dependencies for: " + assembly.getId(), error );
        }

        return resolved;
    }

    private Set<Artifact> resolveTransitively( final Set<Artifact> dependencyArtifacts,
                                               final List<ArtifactRepository> repos,
                                               final ArtifactFilter filter,
                                               final AssemblerConfigurationSource configSource )
        throws DependencyResolutionException
    {
        /*
         * Do not try to resolve artifacts that correspond to reactor projects.
         */

        final Set<Artifact> withoutReactors = new HashSet<Artifact>(dependencyArtifacts);
        for (final MavenProject reactorProject : configSource.getReactorProjects()) {
            final Artifact artifact = reactorProject.getArtifact();
            if (artifact.isResolved()) {
                throw new DependencyResolutionException("The given reactor artifact has not been resolved: " + artifact.getId(),
                                                        null);
            }
            withoutReactors.remove(artifact);
        }

        final MavenProject project = configSource.getProject();

        final ArtifactResolutionRequest req = new ArtifactResolutionRequest();
        req.setLocalRepository( configSource.getLocalRepository() );
        req.setResolveRoot( false );
        req.setRemoteRepositories( repos );
        req.setResolveTransitively( true );
        req.setArtifact( project.getArtifact() );
        req.setArtifactDependencies( withoutReactors );
        req.setManagedVersionMap( project.getManagedVersionMap() );
        req.setCollectionFilter( filter );
        req.setOffline( configSource.getMavenSession().isOffline() );
        req.setForceUpdate( configSource.getMavenSession().getRequest().isUpdateSnapshots() );
        req.setServers( configSource.getMavenSession().getRequest().getServers() );
        req.setMirrors( configSource.getMavenSession().getRequest().getMirrors() );
        req.setProxies( configSource.getMavenSession().getRequest().getProxies() );

        final ArtifactResolutionResult result = resolver.resolve( req );
        if ( result.hasExceptions() )
        {
            throw new DependencyResolutionException( "Failed to resolve dependencies for assembly: ",
                                                     result.getExceptions().get( 0 ) );
        }

        getLogger().debug( "While resolving dependencies of " + project.getId() + ":" );

        FilterUtils.reportFilteringStatistics( Collections.singleton( filter ), getLogger() );

        /*
         * Add the original reactor artifacts to the resolved dependencies.
         */

        final Set<Artifact> results = new HashSet<Artifact>(result.getArtifacts());
        for (final MavenProject reactorProject : configSource.getReactorProjects()) {
            results.add(reactorProject.getArtifact());
        }

        return results;
    }

    void updateRepositoryResolutionRequirements( final Assembly assembly, final ResolutionManagementInfo requirements )
    {
        final List<Repository> repositories = assembly.getRepositories();

        Set<String> rootScopes = new HashSet<String>();
        
        if ( repositories != null && !repositories.isEmpty() )
        {
            
            requirements.setResolutionRequired( true );
            for ( final Repository repo : repositories )
            {
                rootScopes.add( repo.getScope() );
            }
        }
        
        requirements.setScopeFilter( FilterUtils.newScopeFilter( rootScopes ) );
    }


    void updateModuleSetResolutionRequirements( AssemblyId assemblyId, ModuleSet set, DependencySet dependencySet,
                                                final ResolutionManagementInfo requirements,
                                                final AssemblerConfigurationSource configSource )
        throws DependencyResolutionException
    {
        final ModuleBinaries binaries = set.getBinaries();
        if ( binaries != null )
        {
            Set<MavenProject> projects;
            try
            {
                projects = ModuleSetAssemblyPhase.getModuleProjects( set, configSource, getLogger() );
            }
            catch ( final ArchiveCreationException e )
            {
                throw new DependencyResolutionException( "Error determining project-set for moduleSet with binaries.",
                                                         e );
            }

            if ( !projects.isEmpty() )
            {
                for ( final MavenProject p : projects )
                {
                    requirements.enableProjectResolution( p );

                    if ( p.getArtifact() == null )
                    {
                        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of
                        // type
                        final Artifact artifact =
                            resolver.createArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion(),
                                                     p.getPackaging() );
                        p.setArtifact( artifact );
                    }
                }
            }

            if ( binaries.isIncludeDependencies() )
            {
                updateDependencySetResolutionRequirements( dependencySet, requirements, assemblyId,
                                                           configSource.getMavenSession().getProjectBuildingRequest(),
                                                           projects.toArray( new MavenProject[projects.size()] ) );
            }
        }
    }

    void updateDependencySetResolutionRequirements( final DependencySet set,
                                                    final ResolutionManagementInfo requirements, AssemblyId assemblyId,
                                                    ProjectBuildingRequest buildingRequest,
                                                    final MavenProject... projects )
        throws DependencyResolutionException
    {
        requirements.setResolutionRequired( true );

        requirements.setResolvedTransitively( set.isUseTransitiveDependencies() );

        ScopeFilter scopeFilter = FilterUtils.newScopeFilter( set.getScope() );
        
        requirements.setScopeFilter( scopeFilter );
        
        for ( final MavenProject project : projects )
        {
            if ( project == null )
            {
                continue;
            }

            Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
            if ( dependencyArtifacts == null )
            {
                try
                {
                    Iterable<ArtifactResult> artifactResults =
                        dependencyResolver.resolveDependencies( buildingRequest, project.getModel(), scopeFilter );
                    
                    dependencyArtifacts = new HashSet<Artifact>();
                    
                    for ( ArtifactResult artifactResult : artifactResults )
                    {
                        dependencyArtifacts.add( artifactResult.getArtifact() );
                    }
                    
                    project.setDependencyArtifacts( dependencyArtifacts );
                }
                catch ( final DependencyResolverException e )
                {
                    throw new DependencyResolutionException(
                        "Failed to create dependency artifacts for resolution. Assembly: " + assemblyId, e );
                }
            }

            requirements.addArtifacts( dependencyArtifacts );
            getLogger().debug( "Dependencies for project: " + project.getId() + " are:\n" + StringUtils.join(
                dependencyArtifacts.iterator(), "\n" ) );
        }
    }

    List<ArtifactRepository> aggregateRemoteArtifactRepositories( final List<ArtifactRepository> remoteRepositories,
                                                                  final Set<MavenProject> projects )
    {
        final List<List<ArtifactRepository>> repoLists = new ArrayList<List<ArtifactRepository>>();

        repoLists.add( remoteRepositories );
        for ( final MavenProject project : projects )
        {
            repoLists.add( project.getRemoteArtifactRepositories() );
        }

        final List<ArtifactRepository> remoteRepos = new ArrayList<ArtifactRepository>();
        final Set<String> encounteredUrls = new HashSet<String>();

        for ( final List<ArtifactRepository> repositoryList : repoLists )
        {
            if ( ( repositoryList != null ) && !repositoryList.isEmpty() )
            {
                for ( final ArtifactRepository repo : repositoryList )
                {
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

}
