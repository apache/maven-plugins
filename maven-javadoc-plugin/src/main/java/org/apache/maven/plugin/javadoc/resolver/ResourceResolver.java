package org.apache.maven.plugin.javadoc.resolver;

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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.javadoc.JavadocUtil;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResourceResolver
{

    private ResourceResolver()
    {
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> resolveSourceDirs( final SourceResolverConfig config )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        final List<String> dirs = new ArrayList<String>();

        //        dirs.addAll( resolveFromProject( config, config.project(), config.project().getArtifact() ) );

        final Map<String, MavenProject> projectMap = new HashMap<String, MavenProject>();
        if ( config.reactorProjects() != null )
        {
            for ( final MavenProject p : config.reactorProjects() )
            {
                projectMap.put( key( p.getGroupId(), p.getArtifactId() ), p );
            }
        }

        final List<Artifact> artifacts = config.project().getTestArtifacts();

        final List<Artifact> forResourceResolution = new ArrayList<Artifact>( artifacts.size() );
        for ( final Artifact artifact : artifacts )
        {
            final String key = key( artifact.getGroupId(), artifact.getArtifactId() );
            final MavenProject p = projectMap.get( key );
            if ( p != null )
            {
                dirs.addAll( resolveFromProject( config, p, artifact ) );
            }
            else
            {
                forResourceResolution.add( artifact );
            }
        }

        dirs.addAll( resolveFromArtifacts( config, forResourceResolution ) );

        return dirs;
    }

    private static List<String> resolveFromArtifacts( final SourceResolverConfig config, final List<Artifact> artifacts )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        final List<Artifact> toResolve = new ArrayList<Artifact>( artifacts.size() );

        for ( final Artifact artifact : artifacts )
        {
            if ( config.filter() != null && !config.filter().include( artifact ) )
            {
                continue;
            }

            if ( config.includeCompileSources() )
            {
                toResolve.add( createSourceArtifact( artifact, "sources", config ) );
            }

            if ( config.includeTestSources() )
            {
                toResolve.add( createSourceArtifact( artifact, "test-sources", config ) );
            }
        }

        return resolveAndUnpack( toResolve, config );
    }

    public static Artifact createSourceArtifact( final Artifact artifact, final String classifier,
                                                 final SourceResolverConfig config )
    {
        final DefaultArtifact a =
            (DefaultArtifact) config.artifactFactory().createArtifactWithClassifier( artifact.getGroupId(),
                                                                                     artifact.getArtifactId(),
                                                                                     artifact.getVersion(), "jar",
                                                                                     classifier );

        a.setRepository( artifact.getRepository() );

        return a;
    }

    @SuppressWarnings( "unchecked" )
    private static List<String> resolveAndUnpack( final List<Artifact> artifacts, final SourceResolverConfig config )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // NOTE: Since these are '-sources' and '-test-sources' artifacts, they won't actually 
        // resolve transitively...this is just used to aggregate resolution failures into a single 
        // exception.
        final Set<Artifact> artifactSet = new LinkedHashSet<Artifact>( artifacts );
        final Artifact pomArtifact = config.project().getArtifact();
        final ArtifactRepository localRepo = config.localRepository();
        final List<ArtifactRepository> remoteRepos = config.project().getRemoteArtifactRepositories();
        final ArtifactMetadataSource metadataSource = config.artifactMetadataSource();
        final ArtifactFilter filter = config.filter();

        final ArtifactResolver resolver = config.artifactResolver();

        final ArtifactResolutionResult resolutionResult =
            resolver.resolveTransitively( artifactSet, pomArtifact, localRepo, remoteRepos, metadataSource, filter );

        final List<String> result = new ArrayList<String>( artifacts.size() );
        for ( final Artifact a : (Collection<Artifact>) resolutionResult.getArtifacts() )
        {
            if ( !"sources".equals( a.getClassifier() ) && !"test-sources".equals( a.getClassifier() ) )
            {
                continue;
            }

            final File d =
                new File( config.outputBasedir(), a.getArtifactId() + "-" + a.getVersion() + "-" + a.getClassifier() );

            if ( !d.exists() )
            {
                d.mkdirs();
            }

            try
            {
                final UnArchiver unArchiver = config.archiverManager().getUnArchiver( a.getType() );

                unArchiver.setDestDirectory( d );
                unArchiver.setSourceFile( a.getFile() );

                unArchiver.extract();

                result.add( d.getAbsolutePath() );
            }
            catch ( final NoSuchArchiverException e )
            {
                throw new ArtifactResolutionException(
                                                       "Failed to retrieve valid un-archiver component: " + a.getType(),
                                                       a, e );
            }
            catch ( final ArchiverException e )
            {
                throw new ArtifactResolutionException( "Failed to unpack: " + a.getId(), a, e );
            }
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private static List<String> resolveFromProject( final SourceResolverConfig config,
                                                    final MavenProject reactorProject, final Artifact artifact )
    {
        final List<String> dirs = new ArrayList<String>();

        if ( config.includeCompileSources() )
        {
            final List<String> srcRoots = reactorProject.getCompileSourceRoots();
            for ( final String root : srcRoots )
            {
                dirs.add( root );
            }
        }

        if ( config.includeTestSources() )
        {
            final List<String> srcRoots = reactorProject.getTestCompileSourceRoots();
            final File basedir = reactorProject.getBasedir();
            for ( final String root : srcRoots )
            {
                dirs.add( new File( basedir, root ).getAbsolutePath() );
            }
        }

        return JavadocUtil.pruneDirs( reactorProject, dirs );
    }

    private static String key( final String gid, final String aid )
    {
        return gid + ":" + aid;
    }

}
