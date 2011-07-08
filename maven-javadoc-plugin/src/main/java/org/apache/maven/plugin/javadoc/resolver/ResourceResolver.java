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

import static org.codehaus.plexus.util.IOUtil.close;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.javadoc.AbstractJavadocMojo;
import org.apache.maven.plugin.javadoc.JavadocUtil;
import org.apache.maven.plugin.javadoc.ResourcesBundleMojo;
import org.apache.maven.plugin.javadoc.options.JavadocOptions;
import org.apache.maven.plugin.javadoc.options.io.xpp3.JavadocOptionsXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResourceResolver
{

    public static final String SOURCES_CLASSIFIER = "sources";

    public static final String TEST_SOURCES_CLASSIFIER = "test-sources";

    private static final List<String> SOURCE_VALID_CLASSIFIERS =
        Arrays.asList( new String[] { SOURCES_CLASSIFIER, TEST_SOURCES_CLASSIFIER } );

    private static final List<String> RESOURCE_VALID_CLASSIFIERS =
        Arrays.asList( new String[] { AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER,
            AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER } );

    private ResourceResolver()
    {
    }

    @SuppressWarnings( "unchecked" )
    public static List<JavadocBundle> resolveDependencyJavadocBundles( final SourceResolverConfig config )
        throws IOException
    {
        final List<JavadocBundle> bundles = new ArrayList<JavadocBundle>();

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
                bundles.addAll( resolveBundleFromProject( config, p, artifact ) );
            }
            else
            {
                forResourceResolution.add( artifact );
            }
        }

        bundles.addAll( resolveBundlesFromArtifacts( config, forResourceResolution ) );

        return bundles;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> resolveDependencySourcePaths( final SourceResolverConfig config )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        final List<String> dirs = new ArrayList<String>();

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

    private static List<JavadocBundle> resolveBundleFromProject( SourceResolverConfig config, MavenProject project,
                                                           Artifact artifact ) throws IOException
    {
        List<JavadocBundle> bundles = new ArrayList<JavadocBundle>();
        
        List<String> classifiers = new ArrayList<String>();
        if ( config.includeCompileSources() )
        {
            classifiers.add( AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER );
        }
        
        if ( config.includeTestSources() )
        {
            classifiers.add( AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER );
        }
        
        for ( String classifier : classifiers )
        {
            File optionsFile = new File( project.getBuild().getDirectory(), "javadoc-bundle-options/javadoc-options-" + classifier + ".xml" );
            if ( !optionsFile.exists() )
            {
                continue;
            }
            
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( optionsFile );
                JavadocOptions options = new JavadocOptionsXpp3Reader().read( stream );
                
                bundles.add( new JavadocBundle( options, new File( project.getBasedir(), options.getJavadocResourcesDirectory() ) ) );
            }
            catch ( XmlPullParserException e )
            {
                IOException error = new IOException( "Failed to read javadoc options from: " + optionsFile + "\nReason: " + e.getMessage() );
                error.initCause( e );
                
                throw error;
            }
            finally
            {
                close( stream );
            }
        }

        return bundles;
    }

    private static List<JavadocBundle> resolveBundlesFromArtifacts( final SourceResolverConfig config,
                                                                    final List<Artifact> artifacts )
        throws IOException
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
                toResolve.add( createResourceArtifact( artifact, AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER, config ) );
            }

            if ( config.includeTestSources() )
            {
                toResolve.add( createResourceArtifact( artifact, AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER, config ) );
            }
        }

        List<String> dirs = null;
        try
        {
            dirs = resolveAndUnpack( toResolve, config, RESOURCE_VALID_CLASSIFIERS, false );
        }
        catch ( ArtifactResolutionException e )
        {
            if ( config.log().isDebugEnabled() )
            {
                config.log().debug( e.getMessage(), e );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            if ( config.log().isDebugEnabled() )
            {
                config.log().debug( e.getMessage(), e );
            }
        }
        
        List<JavadocBundle> result = new ArrayList<JavadocBundle>();

        if ( dirs != null )
        {
            for ( String d : dirs )
            {
                File dir = new File( d );
                File resources = new File( dir, ResourcesBundleMojo.RESOURCES_DIR_PATH );
                JavadocOptions options = null;

                File javadocOptions = new File( dir, ResourcesBundleMojo.BUNDLE_OPTIONS_PATH );
                if ( javadocOptions.exists() )
                {
                    FileInputStream reader = null;
                    try
                    {
                        reader = new FileInputStream( javadocOptions );
                        options = new JavadocOptionsXpp3Reader().read( reader );
                    }
                    catch ( XmlPullParserException e )
                    {
                        IOException error = new IOException( "Failed to parse javadoc options: " + e.getMessage() );
                        error.initCause( e );
                        
                        throw error;
                    }
                    finally
                    {
                        close( reader );
                    }
                }
                
                result.add( new JavadocBundle( options, resources ) );
            }
        }
        
        return result;
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
                toResolve.add( createResourceArtifact( artifact, SOURCES_CLASSIFIER, config ) );
            }

            if ( config.includeTestSources() )
            {
                toResolve.add( createResourceArtifact( artifact, TEST_SOURCES_CLASSIFIER, config ) );
            }
        }

        return resolveAndUnpack( toResolve, config, SOURCE_VALID_CLASSIFIERS, true );
    }

    private static Artifact createResourceArtifact( final Artifact artifact, final String classifier,
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
    private static List<String> resolveAndUnpack( final List<Artifact> artifacts, final SourceResolverConfig config,
                                                  final List<String> validClassifiers, final boolean propagateErrors )
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
        ArtifactFilter resolutionFilter = null;
        if ( filter != null )
        {
            // Wrap the filter in a ProjectArtifactFilter in order to always include the pomArtifact for resolution.
            // NOTE that this is necessary, b/c the -sources artifacts are added dynamically to the pomArtifact
            // and the resolver also checks the dependency trail with the given filter, thus the pomArtifact has
            // to be explicitly included by the filter, otherwise the -sources artifacts won't be resolved.
            resolutionFilter = new ProjectArtifactFilter(pomArtifact, filter);
        }

        final ArtifactResolver resolver = config.artifactResolver();

        final ArtifactResolutionResult resolutionResult = resolver.resolveTransitively(
                artifactSet, pomArtifact, localRepo, remoteRepos, metadataSource, resolutionFilter );

        final List<String> result = new ArrayList<String>( artifacts.size() );
        for ( final Artifact a : (Collection<Artifact>) resolutionResult.getArtifacts() )
        {
            if ( !validClassifiers.contains( a.getClassifier() ) || ( filter != null && !filter.include( a ) ) )
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
                if ( propagateErrors )
                {
                    throw new ArtifactResolutionException( "Failed to retrieve valid un-archiver component: "
                        + a.getType(), a, e );
                }
            }
            catch ( final ArchiverException e )
            {
                if ( propagateErrors )
                {
                    throw new ArtifactResolutionException( "Failed to unpack: " + a.getId(), a, e );
                }
            }
        }

        return result;
    }

    @SuppressWarnings( "unchecked" )
    private static List<String> resolveFromProject( final SourceResolverConfig config,
                                                    final MavenProject reactorProject, final Artifact artifact )
    {
        final List<String> dirs = new ArrayList<String>();

        if ( config.filter() == null || config.filter().include( artifact ) )
        {
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
                for ( final String root : srcRoots )
                {
                    dirs.add( root );
                }
            }
        }

        return JavadocUtil.pruneDirs( reactorProject, dirs );
    }

    private static String key( final String gid, final String aid )
    {
        return gid + ":" + aid;
    }

}
