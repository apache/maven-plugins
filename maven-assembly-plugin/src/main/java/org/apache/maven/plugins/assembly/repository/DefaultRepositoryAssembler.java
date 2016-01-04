package org.apache.maven.plugins.assembly.repository;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.assembly.repository.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.TransferUtils;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependency.resolve.DependencyResolver;
import org.apache.maven.shared.dependency.resolve.DependencyResolverException;
import org.apache.maven.shared.repository.RepositoryManager;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Jason van Zyl
 */

// todo will need to pop the processed project cache using reflection
@Component( role = RepositoryAssembler.class )
public class DefaultRepositoryAssembler
    extends AbstractLogEnabled
    implements RepositoryAssembler
{
    @Requirement
    protected ArtifactResolver artifactResolver;

    @Requirement
    private DependencyResolver dependencyResolver;

    @Requirement
    private RepositoryManager repositoryManager;

    public void buildRemoteRepository( File repositoryDirectory, RepositoryInfo repository,
                                       RepositoryBuilderConfigSource configSource )
                                           throws RepositoryAssemblyException
    {
        MavenProject project = configSource.getProject();
        ProjectBuildingRequest buildingRequest = configSource.getProjectBuildingRequest();

        Iterable<ArtifactResult> result = null;

        Collection<Dependency> dependencies = project.getDependencies();

        if ( dependencies == null )
        {
            Logger logger = getLogger();

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "dependency-artifact set for project: " + project.getId()
                    + " is null. Skipping repository processing." );
            }

            return;
        }

        Collection<Dependency> managedDependencies = null;
        if ( project.getDependencyManagement() != null )
        {
            managedDependencies = project.getDependencyManagement().getDependencies();
        }

        // Older Aether versions use an cache which can't be cleared. So can't delete repoDir and use it again.
        // Instead create a temporary repository, delete it at end (should be in a finally-block)

        File tempRepo = new File( repositoryDirectory.getParentFile(), repositoryDirectory.getName() + "_tmp" );

        buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, tempRepo );

        try
        {
            result = dependencyResolver.resolveDependencies( buildingRequest, dependencies, managedDependencies, null );
        }
        catch ( DependencyResolverException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }

        ArtifactFilter filter = buildRepositoryFilter( repository, project );

        buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, repositoryDirectory );

        Map<String, GroupVersionAlignment> groupVersionAlignments =
            createGroupVersionAlignments( repository.getGroupVersionAlignments() );

        assembleRepositoryArtifacts( buildingRequest, result, filter, groupVersionAlignments );

        if ( repository.isIncludeMetadata() )
        {
//            assembleRepositoryMetadata( result, filter, centralRepository, targetRepository );
        }

        try
        {
            FileUtils.deleteDirectory( tempRepo );
        }
        catch ( IOException e )
        {
            // noop
        }
    }

    private ArtifactFilter buildRepositoryFilter( RepositoryInfo repository, MavenProject project )
    {
        AndArtifactFilter filter = new AndArtifactFilter();

        ArtifactFilter scopeFilter = new ScopeArtifactFilter( repository.getScope() );
        filter.add( scopeFilter );

        // ----------------------------------------------------------------------------
        // Includes
        //
        // We'll take everything if no includes are specified to try and make
        // this
        // process more maintainable. Don't want to have to update the assembly
        // descriptor everytime the POM is updated.
        // ----------------------------------------------------------------------------

        List<String> includes = repository.getIncludes();

        if ( ( includes == null ) || includes.isEmpty() )
        {
            List<String> patterns = new ArrayList<String>();

            Set<Artifact> projectArtifacts = project.getDependencyArtifacts();

            if ( projectArtifacts != null )
            {
                for ( Artifact artifact : projectArtifacts )
                {
                    patterns.add( artifact.getDependencyConflictId() );
                }
            }

            PatternIncludesArtifactFilter includeFilter = new PatternIncludesArtifactFilter( patterns, true );

            filter.add( includeFilter );
        }
        else
        {
            filter.add( new PatternIncludesArtifactFilter( repository.getIncludes(), true ) );
        }

        // ----------------------------------------------------------------------------
        // Excludes
        //
        // We still want to make it easy to exclude a few things even if we
        // slurp
        // up everything.
        // ----------------------------------------------------------------------------

        List<String> excludes = repository.getExcludes();

        if ( ( excludes != null ) && !excludes.isEmpty() )
        {
            filter.add( new PatternExcludesArtifactFilter( repository.getExcludes(), true ) );
        }

        return filter;
    }

    private void assembleRepositoryArtifacts( ProjectBuildingRequest buildingRequest, Iterable<ArtifactResult> result,
                                              ArtifactFilter filter,
                                              Map<String, GroupVersionAlignment> groupVersionAlignments )
                                                  throws RepositoryAssemblyException
    {
        try
        {
            for ( ArtifactResult ar : result )
            {
                Artifact a = ar.getArtifact();

                if ( filter.include( a ) )
                {
                    getLogger().debug( "Re-resolving: " + a + " for repository assembly." );

                    setAlignment( a, groupVersionAlignments );

                    artifactResolver.resolveArtifact( buildingRequest, TransferUtils.toArtifactCoordinate( a ) );

                    a.setVersion( a.getBaseVersion() );

                    File targetFile = new File( repositoryManager.getLocalRepositoryBasedir( buildingRequest ),
                                                repositoryManager.getPathForLocalArtifact( buildingRequest, a ) );
                    
                    FileUtils.copyFile( a.getFile(), targetFile );

//                    writeChecksums( targetFile );
                }
            }
        }
        catch ( ArtifactResolverException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
        }
    }

    // CHECKSTYLE_OFF: LineLength
    protected Map<String, GroupVersionAlignment> createGroupVersionAlignments( List<GroupVersionAlignment> versionAlignments )
    // CHECKSTYLE_ON: LineLength
    {
        Map<String, GroupVersionAlignment> groupVersionAlignments = new HashMap<String, GroupVersionAlignment>();

        if ( versionAlignments != null )
        {
            for ( GroupVersionAlignment alignment : versionAlignments )
            {
                groupVersionAlignments.put( alignment.getId(), alignment );
            }
        }

        return groupVersionAlignments;
    }

    private void setAlignment( Artifact artifact, Map<String, GroupVersionAlignment> groupVersionAlignments )
    {
        GroupVersionAlignment alignment = groupVersionAlignments.get( artifact.getGroupId() );

        if ( alignment != null )
        {
            if ( !alignment.getExcludes().contains( artifact.getArtifactId() ) )
            {
                artifact.setVersion( alignment.getVersion() );
            }
        }
    }
}
