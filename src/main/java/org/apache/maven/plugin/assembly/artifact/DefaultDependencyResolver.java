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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.apache.maven.artifact.resolver.DebugResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
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
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.artifact.DependencyResolver" role-hint="default"
 *
 * @author jdcasey
 * @version $Id$
 */
public class DefaultDependencyResolver
    extends AbstractLogEnabled implements DependencyResolver
{
    
    /**
     * @plexus.requirement
     */
    private ArtifactResolver resolver;

    /**
     * @plexus.requirement
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;
    
    /**
     * @plexus.requirement
     */
    private ArtifactCollector collector;

    public DefaultDependencyResolver()
    {
        // for plexus init
    }

    protected DefaultDependencyResolver( ArtifactResolver resolver, ArtifactMetadataSource metadataSource,
                               ArtifactFactory factory, ArtifactCollector collector, Logger logger )
    {
        this.resolver = resolver;
        this.metadataSource = metadataSource;
        this.factory = factory;
        this.collector = collector;

        enableLogging( logger );
    }

    public Map buildManagedVersionMap( Assembly assembly, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, InvalidVersionSpecificationException, InvalidDependencyVersionException,
        ArtifactResolutionException
    {
        MavenProject currentProject = configSource.getProject();
        
        ResolutionManagementInfo depInfo = new ResolutionManagementInfo( currentProject );
        
        getDependencySetResolutionRequirements( assembly.getDependencySets(), depInfo );
        getModuleSetResolutionRequirements( assembly.getModuleSets(), depInfo, configSource );
        getRepositoryResolutionRequirements( assembly.getRepositories(), depInfo );
        
        if ( !depInfo.isResolutionRequired() )
        {
            return new HashMap();
        }
        
        Map managedVersions = new HashMap();
        Set allRequiredArtifacts = new LinkedHashSet();
        List allRemoteRepos = new ArrayList();
        
        for ( Iterator it = depInfo.getEnabledProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            
            Map projectManagedVersions = getManagedVersionMap( project );
            if ( projectManagedVersions != null )
            {
                for ( Iterator versionIterator = projectManagedVersions.keySet().iterator(); versionIterator.hasNext(); )
                {
                    String id = (String) versionIterator.next();
                    if ( !managedVersions.containsKey( id ) )
                    {
                        managedVersions.put( id, projectManagedVersions.get( id ) );
                    }
                }
            }
        }
        
        if ( configSource.getRemoteRepositories() != null && !configSource.getRemoteRepositories().isEmpty() )
        {
            allRemoteRepos.addAll( configSource.getRemoteRepositories() );
        }
        
        for ( Iterator it = depInfo.getEnabledProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            
            allRequiredArtifacts.addAll( MavenMetadataSource.createArtifacts( factory, project.getDependencies(), null,
                                                                              depInfo.getScopeFilter(), project ) );
            
            allRemoteRepos = aggregateRemoteArtifactRepositories( allRemoteRepos, project );
        }
        
        if ( depInfo.isResolvedTransitively() )
        {
            // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
            Artifact projectArtifact =
                factory.createBuildArtifact( currentProject.getGroupId(), currentProject.getArtifactId()
                    + "-[assembly process]", currentProject.getVersion(), currentProject.getPackaging() );

            List listeners =
                getLogger().isDebugEnabled() ? Collections.singletonList( new DebugResolutionListener( getLogger() ) )
                                : Collections.EMPTY_LIST;
            
            ArtifactResolutionResult resolutionResult =
                collector.collect( allRequiredArtifacts, projectArtifact, managedVersions,
                                   configSource.getLocalRepository(), allRemoteRepos, metadataSource,
                                   depInfo.getScopeFilter(), listeners );
            
            Set artifacts = resolutionResult.getArtifacts();
            if ( artifacts != null )
            {
                for ( Iterator versionIterator = artifacts.iterator(); versionIterator.hasNext(); )
                {
                    Artifact artifact = (Artifact) versionIterator.next();
                    String id = artifact.getDependencyConflictId();
                    if ( !managedVersions.containsKey( id ) )
                    {
                        managedVersions.put( id, artifact );
                    }
                }
            }
        }
        else
        {
            if ( allRequiredArtifacts != null )
            {
                for ( Iterator versionIterator = allRequiredArtifacts.iterator(); versionIterator.hasNext(); )
                {
                    Artifact artifact = (Artifact) versionIterator.next();
                    if ( artifact.getVersion() == null )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                              "Not sure what to do with the version range: "
                                                  + artifact.getVersionRange()
                                                  + " in non-transitive mode, encountered while building managed versions collection; skipping it for now. Artifact: "
                                                  + artifact );
                        }
                        
                        continue;
                    }
                    
                    String id = artifact.getDependencyConflictId();
                    if ( !managedVersions.containsKey( id ) )
                    {
                        managedVersions.put( id, artifact );
                    }
                }
            }
        }
        
        return managedVersions;
    }
    
    /* (non-Javadoc)
     * @see org.apache.maven.plugin.assembly.artifact.DependencyResolver#resolveDependencies(org.apache.maven.project.MavenProject, java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public Set resolveDependencies( MavenProject project, String scope, Map managedVersions, ArtifactRepository localRepository,
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

        getLogger().debug(
                           "Dependencies for project: " + project.getId() + " are:\n"
                               + StringUtils.join( dependencyArtifacts.iterator(), "\n" ) );
        
        for ( Iterator it = dependencyArtifacts.iterator(); it.hasNext(); )
        {
            Artifact depArtifact = (Artifact) it.next();
            
            if ( managedVersions.containsKey( depArtifact.getDependencyConflictId() ) )
            {
                manageArtifact( depArtifact, managedVersions );
            }
        }

        if ( resolveTransitively )
        {
            getLogger().debug( "Resolving project dependencies transitively." );
            return resolveTransitively( dependencyArtifacts, artifact, managedVersions, localRepository, repos, filter, project );
        }
        else
        {
            getLogger().debug( "Resolving project dependencies ONLY. Transitive dependencies WILL NOT be included in the results." );
            return resolveNonTransitively( dependencyArtifacts, artifact, managedVersions, localRepository, repos, filter );
        }
    }

    protected Set resolveNonTransitively( Set dependencyArtifacts, Artifact artifact, Map managedVersions,
                                          ArtifactRepository localRepository, List repos, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        for ( Iterator it = dependencyArtifacts.iterator(); it.hasNext(); )
        {
            Artifact depArtifact = (Artifact) it.next();
            
            resolver.resolve( depArtifact, repos, localRepository );
        }

        return dependencyArtifacts;
    }

    private Set resolveTransitively( Set dependencyArtifacts, Artifact artifact, Map managedVersions, ArtifactRepository localRepository,
                                     List repos, ArtifactFilter filter, MavenProject project )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactResolutionResult result =
            resolver.resolveTransitively( dependencyArtifacts, artifact, managedVersions, localRepository, repos,
                                          metadataSource, filter );

        getLogger().debug( "While resolving dependencies of " + project.getId() + ":" );

        FilterUtils.reportFilteringStatistics( Collections.singleton( filter ), getLogger() );

        return result.getArtifacts();
    }

    // Copied from DefaultArtifactCollector, SVN: http://svn.apache.org/repos/asf/maven/components/branches/maven-2.1.x@679206
    // with modifications to work with non-transitive resolution processes. 
    protected void manageArtifact( Artifact targetArtifact, Map managedVersions )
    {
        Artifact artifact = (Artifact) managedVersions.get( targetArtifact.getDependencyConflictId() );
        
        if ( artifact == null )
        {
            return;
        }

        // Before we update the version of the artifact, we need to know
        // whether we are working on a transitive dependency or not.  This
        // allows depMgmt to always override transitive dependencies, while
        // explicit child override depMgmt (viz. depMgmt should only
        // provide defaults to children, but should override transitives).
        // We can do this by calling isChildOfRootNode on the current node.

        if ( artifact.getVersion() != null )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Managing version for: " + targetArtifact.getDependencyConflictId() + " to: " + artifact.getVersion() );
            }
            
            targetArtifact.setVersion( artifact.getVersion() );
        }

        if ( artifact.getScope() != null )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Managing scope for: " + targetArtifact.getDependencyConflictId() + " to: " + artifact.getScope() );
            }
            
            targetArtifact.setScope( artifact.getScope() );
        }
    }
    
    protected List aggregateRemoteArtifactRepositories( List remoteRepositories, MavenProject project )
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
    protected Map getManagedVersionMap( MavenProject project )
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

    protected void getRepositoryResolutionRequirements( List repositories, ResolutionManagementInfo requirements )
    {
        if ( repositories != null && !repositories.isEmpty() )
        {
            requirements.setResolutionRequired( true );
            for ( Iterator it = repositories.iterator(); it.hasNext(); )
            {
                Repository repo = (Repository) it.next();
                enableScope( repo.getScope(), requirements );
            }
        }
    }

    protected void getModuleSetResolutionRequirements( List moduleSets, ResolutionManagementInfo requirements,
                                                     AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        if ( moduleSets != null && !moduleSets.isEmpty() )
        {
            for ( Iterator it = moduleSets.iterator(); it.hasNext(); )
            {
                ModuleSet set = (ModuleSet) it.next();
                
                ModuleBinaries binaries = set.getBinaries();
                if ( binaries != null )
                {
                    Set projects = ModuleSetAssemblyPhase.getModuleProjects( set, configSource, getLogger() );
                    if ( projects != null && !projects.isEmpty() )
                    {
                        for ( Iterator projectIterator = projects.iterator(); projectIterator.hasNext(); )
                        {
                            MavenProject p = (MavenProject) projectIterator.next();
                            requirements.enableProjectResolution( p );
                        }
                    }
                    
                    if ( binaries.isIncludeDependencies() )
                    {
                        getDependencySetResolutionRequirements( ModuleSetAssemblyPhase.getDependencySets( binaries ), requirements );
                    }
                }
            }
        }
    }

    protected void getDependencySetResolutionRequirements( List depSets, ResolutionManagementInfo requirements )
    {
        if ( depSets != null && !depSets.isEmpty() )
        {
            requirements.setResolutionRequired( true );
            for ( Iterator it = depSets.iterator(); it.hasNext(); )
            {
                DependencySet set = (DependencySet) it.next();
                
                requirements.setResolvedTransitively( requirements.isResolvedTransitively() || set.isUseTransitiveDependencies() );
                enableScope( set.getScope(), requirements );
            }
        }
    }

    private void enableScope( String scope, ResolutionManagementInfo requirements )
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

    protected ArtifactResolver getArtifactResolver()
    {
        return resolver;
    }

    protected DefaultDependencyResolver setArtifactResolver( ArtifactResolver resolver )
    {
        this.resolver = resolver;
        
        return this;
    }

    protected ArtifactMetadataSource getArtifactMetadataSource()
    {
        return metadataSource;
    }

    protected DefaultDependencyResolver setArtifactMetadataSource( ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;
        
        return this;
    }

    protected ArtifactFactory getArtifactFactory()
    {
        return factory;
    }

    protected DefaultDependencyResolver setArtifactFactory( ArtifactFactory factory )
    {
        this.factory = factory;
        
        return this;
    }

    protected ArtifactCollector getArtifactCollector()
    {
        return collector;
    }

    protected DefaultDependencyResolver setArtifactCollector( ArtifactCollector collector )
    {
        this.collector = collector;
        
        return this;
    }
    
    protected DefaultDependencyResolver setLogger( Logger logger )
    {
        enableLogging( logger );
        
        return this;
    }

}
