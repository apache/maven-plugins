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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.ModuleSetAssemblyPhase;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.ModuleBinaries;
import org.apache.maven.plugin.assembly.model.ModuleSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
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
    private ArtifactResolver resolver;

    @Requirement
    private ArtifactMetadataSource metadataSource;

    @Requirement
    private ArtifactFactory factory;

    @Requirement
    private ArtifactCollector collector;

    public DefaultDependencyResolver()
    {
        // for plexus init
    }

    protected DefaultDependencyResolver( final ArtifactResolver resolver, final ArtifactMetadataSource metadataSource,
                                         final ArtifactFactory factory, final ArtifactCollector collector,
                                         final Logger logger )
    {
        this.resolver = resolver;
        this.metadataSource = metadataSource;
        this.factory = factory;
        this.collector = collector;

        enableLogging( logger );
    }

    public void resolve( final Assembly assembly, final AssemblerConfigurationSource configSource,
                         final AssemblyContext context )
        throws DependencyResolutionException
    {
        final MavenProject currentProject = configSource.getProject();

        final ResolutionManagementInfo info = new ResolutionManagementInfo( currentProject );
        getRepositoryResolutionRequirements( assembly, info, currentProject );
        getDependencySetResolutionRequirements( assembly, assembly.getDependencySets(), info, currentProject );
        getModuleSetResolutionRequirements( assembly, info, configSource );

        if ( !info.isResolutionRequired() )
        {
            context.setResolvedArtifacts( new HashSet<Artifact>() );
            return;
        }

        final List<ArtifactRepository> repos =
            aggregateRemoteArtifactRepositories( configSource.getRemoteRepositories(), info.getEnabledProjects() );

        Set<Artifact> artifacts = info.getArtifacts();
        if ( info.isResolvedTransitively() )
        {
            getLogger().debug( "Resolving project dependencies transitively." );
            artifacts = resolveTransitively( artifacts, repos, info, configSource );
        }
        else
        {
            getLogger().debug( "Resolving project dependencies ONLY. Transitive dependencies WILL NOT be included in the results." );
            artifacts = resolveNonTransitively( assembly, artifacts, configSource, repos );
        }

        context.setResolvedArtifacts( artifacts );
    }

    protected Set<Artifact> resolveNonTransitively( final Assembly assembly, final Set<Artifact> dependencyArtifacts,
                                                    final AssemblerConfigurationSource configSource,
                                                    final List<ArtifactRepository> repos )
        throws DependencyResolutionException
    {

        final List<Artifact> missing = new ArrayList<Artifact>();
        final Set<Artifact> resolved = new LinkedHashSet<Artifact>();
        for ( final Iterator<Artifact> it = dependencyArtifacts.iterator(); it.hasNext(); )
        {
            final Artifact depArtifact = it.next();

            try
            {
                resolver.resolve( depArtifact, repos, configSource.getLocalRepository() );
                resolved.add( depArtifact );
            }
            catch ( final ArtifactResolutionException e )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Failed to resolve: " + depArtifact.getId() + " for assembly: "
                                           + assembly.getId() );
                }
                missing.add( depArtifact );
            }
            catch ( final ArtifactNotFoundException e )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Failed to resolve: " + depArtifact.getId() + " for assembly: "
                                           + assembly.getId() );
                }
                missing.add( depArtifact );
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

    @SuppressWarnings( "unchecked" )
    private Set<Artifact> resolveTransitively( final Set<Artifact> dependencyArtifacts,
                                               final List<ArtifactRepository> repos,
                                               final ResolutionManagementInfo info,
                                               final AssemblerConfigurationSource configSource )
        throws DependencyResolutionException
    {
        final MavenProject project = configSource.getProject();

        final ArtifactFilter filter = info.getScopeFilter();
        final ArtifactRepository localRepository = configSource.getLocalRepository();

        ArtifactResolutionResult result;
        try
        {
            result =
                resolver.resolveTransitively( dependencyArtifacts, project.getArtifact(),
                                              project.getManagedVersionMap(), localRepository, repos, metadataSource,
                                              filter );
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new DependencyResolutionException( "Failed to resolve dependencies for assembly: ", e );
        }
        catch ( final ArtifactNotFoundException e )
        {
            throw new DependencyResolutionException( "Failed to resolve dependencies for assembly: ", e );
        }

        getLogger().debug( "While resolving dependencies of " + project.getId() + ":" );

        FilterUtils.reportFilteringStatistics( Collections.singleton( filter ), getLogger() );

        return result.getArtifacts();
    }

    protected void getRepositoryResolutionRequirements( final Assembly assembly,
                                                        final ResolutionManagementInfo requirements,
                                                        final MavenProject... project )
    {
        final List<Repository> repositories = assembly.getRepositories();

        if ( repositories != null && !repositories.isEmpty() )
        {
            requirements.setResolutionRequired( true );
            for ( final Repository repo : repositories )
            {
                enableScope( repo.getScope(), requirements );
            }
        }
    }

    protected void getModuleSetResolutionRequirements( final Assembly assembly,
                                                       final ResolutionManagementInfo requirements,
                                                       final AssemblerConfigurationSource configSource )
        throws DependencyResolutionException
    {
        final List<ModuleSet> moduleSets = assembly.getModuleSets();

        if ( moduleSets != null && !moduleSets.isEmpty() )
        {
            for ( final ModuleSet set : moduleSets )
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
                        throw new DependencyResolutionException(
                                                                 "Error determining project-set for moduleSet with binaries.",
                                                                 e );
                    }

                    if ( projects != null && !projects.isEmpty() )
                    {
                        for ( final MavenProject p : projects )
                        {
                            requirements.enableProjectResolution( p );

                            if ( p.getArtifact() == null )
                            {
                                // TODO: such a call in MavenMetadataSource too - packaging not really the intention of
                                // type
                                final Artifact artifact =
                                    factory.createBuildArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion(),
                                                                 p.getPackaging() );
                                p.setArtifact( artifact );
                            }
                        }
                    }

                    if ( binaries.isIncludeDependencies() )
                    {
                        getDependencySetResolutionRequirements( assembly,
                                                                ModuleSetAssemblyPhase.getDependencySets( binaries ),
                                                                requirements, projects.toArray( new MavenProject[] {} ) );
                    }
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void getDependencySetResolutionRequirements( final Assembly assembly, final List<DependencySet> depSets,
                                                           final ResolutionManagementInfo requirements,
                                                           final MavenProject... projects )
        throws DependencyResolutionException
    {
        if ( depSets != null && !depSets.isEmpty() )
        {
            requirements.setResolutionRequired( true );

            for ( final DependencySet set : depSets )
            {
                requirements.setResolvedTransitively( set.isUseTransitiveDependencies() );

                enableScope( set.getScope(), requirements );
            }

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
                        dependencyArtifacts = project.createArtifacts( factory, null, requirements.getScopeFilter() );
                        project.setDependencyArtifacts( dependencyArtifacts );
                    }
                    catch ( final InvalidDependencyVersionException e )
                    {
                        throw new DependencyResolutionException(
                                                                 "Failed to create dependency artifacts for resolution. Assembly: "
                                                                     + assembly.getId(), e );
                    }
                }

                requirements.addArtifacts( dependencyArtifacts );
                getLogger().debug( "Dependencies for project: " + project.getId() + " are:\n"
                                       + StringUtils.join( dependencyArtifacts.iterator(), "\n" ) );
            }
        }
    }

    private void enableScope( final String scope, final ResolutionManagementInfo requirements )
    {
        if ( Artifact.SCOPE_COMPILE.equals( scope ) )
        {
            requirements.enableCompileScope();
        }
        else if ( Artifact.SCOPE_PROVIDED.equals( scope ) )
        {
            requirements.enableProvidedScope();
        }
        else if ( Artifact.SCOPE_RUNTIME.equals( scope ) )
        {
            requirements.enableRuntimeScope();
        }
        else if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
        {
            requirements.enableSystemScope();
        }
        else if ( Artifact.SCOPE_TEST.equals( scope ) )
        {
            requirements.enableTestScope();
        }
    }

    @SuppressWarnings( "unchecked" )
    protected List<ArtifactRepository> aggregateRemoteArtifactRepositories( final List<ArtifactRepository> remoteRepositories,
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

        for ( final Iterator<List<ArtifactRepository>> listIterator = repoLists.iterator(); listIterator.hasNext(); )
        {
            final List<ArtifactRepository> repositoryList = listIterator.next();

            if ( ( repositoryList != null ) && !repositoryList.isEmpty() )
            {
                for ( final Iterator<ArtifactRepository> it = repositoryList.iterator(); it.hasNext(); )
                {
                    final ArtifactRepository repo = it.next();

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

    protected ArtifactResolver getArtifactResolver()
    {
        return resolver;
    }

    protected DefaultDependencyResolver setArtifactResolver( final ArtifactResolver resolver )
    {
        this.resolver = resolver;

        return this;
    }

    protected ArtifactMetadataSource getArtifactMetadataSource()
    {
        return metadataSource;
    }

    protected DefaultDependencyResolver setArtifactMetadataSource( final ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;

        return this;
    }

    protected ArtifactFactory getArtifactFactory()
    {
        return factory;
    }

    protected DefaultDependencyResolver setArtifactFactory( final ArtifactFactory factory )
    {
        this.factory = factory;

        return this;
    }

    protected ArtifactCollector getArtifactCollector()
    {
        return collector;
    }

    protected DefaultDependencyResolver setArtifactCollector( final ArtifactCollector collector )
    {
        this.collector = collector;

        return this;
    }

    protected DefaultDependencyResolver setLogger( final Logger logger )
    {
        enableLogging( logger );

        return this;
    }

}
