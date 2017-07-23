package org.apache.maven.plugins.javadoc.resolver;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugins.javadoc.options.JavadocOptions;
import org.apache.maven.plugins.javadoc.options.io.xpp3.JavadocOptionsXpp3Reader;
import org.apache.maven.plugins.javadoc.AbstractJavadocMojo;
import org.apache.maven.plugins.javadoc.JavadocUtil;
import org.apache.maven.plugins.javadoc.ResourcesBundleMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.resolve.transform.ArtifactIncludeFilterTransformer;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * 
 */
@Component( role = ResourceResolver.class )
public final class ResourceResolver extends AbstractLogEnabled
{
    @Requirement
    private ArtifactFactory artifactFactory;
    
    @Requirement
    private ArtifactResolver resolver;
    
    @Requirement
    private DependencyResolver dependencyResolver;

    @Requirement
    private ArtifactMetadataSource artifactMetadataSource;
    
    @Requirement
    private ArchiverManager archiverManager;

    /**
     * The classifier for sources.
     */
    public static final String SOURCES_CLASSIFIER = "sources";

    /**
     * The classifier for test sources 
     */
    public static final String TEST_SOURCES_CLASSIFIER = "test-sources";

    private static final List<String> SOURCE_VALID_CLASSIFIERS = Arrays.asList( SOURCES_CLASSIFIER,
                                                                                TEST_SOURCES_CLASSIFIER );

    private static final List<String> RESOURCE_VALID_CLASSIFIERS =
        Arrays.asList( AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER,
                       AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER );

    /**
     * @param config {@link SourceResolverConfig}
     * @return list of {@link JavadocBundle}.
     * @throws IOException {@link IOException}
     */
    public List<JavadocBundle> resolveDependencyJavadocBundles( final SourceResolverConfig config )
        throws IOException
    {
        final List<JavadocBundle> bundles = new ArrayList<>();

        final Map<String, MavenProject> projectMap = new HashMap<>();
        if ( config.reactorProjects() != null )
        {
            for ( final MavenProject p : config.reactorProjects() )
            {
                projectMap.put( key( p.getGroupId(), p.getArtifactId() ), p );
            }
        }

        final List<Artifact> artifacts = config.project().getTestArtifacts();

        final List<Artifact> forResourceResolution = new ArrayList<>( artifacts.size() );
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

    /**
     * @param config {@link SourceResolverConfig}
     * @return The list of resolved dependencies.
     * @throws ArtifactResolutionException {@link ArtifactResolutionException}
     * @throws ArtifactNotFoundException {@link ArtifactNotFoundException}
     */
    public List<String> resolveDependencySourcePaths( final SourceResolverConfig config )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        final List<String> dirs = new ArrayList<>();

        final Map<String, MavenProject> projectMap = new HashMap<>();
        if ( config.reactorProjects() != null )
        {
            for ( final MavenProject p : config.reactorProjects() )
            {
                projectMap.put( key( p.getGroupId(), p.getArtifactId() ), p );
            }
        }

        final List<Artifact> artifacts = config.project().getTestArtifacts();

        final List<Artifact> forResourceResolution = new ArrayList<>( artifacts.size() );
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
        List<JavadocBundle> bundles = new ArrayList<>();
        
        List<String> classifiers = new ArrayList<>();
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
            File optionsFile =
                new File( project.getBuild().getDirectory(), "javadoc-bundle-options/javadoc-options-" + classifier
                    + ".xml" );
            if ( !optionsFile.exists() )
            {
                continue;
            }
            
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( optionsFile );
                JavadocOptions options = new JavadocOptionsXpp3Reader().read( stream );
                stream.close();
                stream = null;
                bundles.add( new JavadocBundle( options, new File( project.getBasedir(),
                                                                   options.getJavadocResourcesDirectory() ) ) );
            }
            catch ( XmlPullParserException e )
            {
                IOException error =
                    new IOException( "Failed to read javadoc options from: " + optionsFile + "\nReason: "
                        + e.getMessage() );
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

    private List<JavadocBundle> resolveBundlesFromArtifacts( final SourceResolverConfig config,
                                                                    final List<Artifact> artifacts )
        throws IOException
    {
        final List<Artifact> toResolve = new ArrayList<>( artifacts.size() );

        for ( final Artifact artifact : artifacts )
        {
            if ( config.filter() != null
                && !new ArtifactIncludeFilterTransformer().transform( config.filter() ).include( artifact ) )
            {
                continue;
            }

            if ( config.includeCompileSources() )
            {
                toResolve.add( createResourceArtifact( artifact,
                                                       AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER,
                                                       config ) );
            }

            if ( config.includeTestSources() )
            {
                toResolve.add( createResourceArtifact( artifact,
                                                       AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER,
                                                       config ) );
            }
        }

        List<String> dirs = null;
        try
        {
            dirs = resolveAndUnpack( toResolve, config, RESOURCE_VALID_CLASSIFIERS, false );
        }
        catch ( ArtifactResolutionException e )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( e.getMessage(), e );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( e.getMessage(), e );
            }
        }
        
        List<JavadocBundle> result = new ArrayList<>();

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

    private List<String> resolveFromArtifacts( final SourceResolverConfig config,
                                                      final List<Artifact> artifacts )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        final List<Artifact> toResolve = new ArrayList<>( artifacts.size() );

        for ( final Artifact artifact : artifacts )
        {
            if ( config.filter() != null
                && !new ArtifactIncludeFilterTransformer().transform( config.filter() ).include( artifact ) )
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

    private Artifact createResourceArtifact( final Artifact artifact, final String classifier,
                                                    final SourceResolverConfig config )
    {
        final DefaultArtifact a =
            (DefaultArtifact) artifactFactory.createArtifactWithClassifier( artifact.getGroupId(),
                                                                                     artifact.getArtifactId(),
                                                                                     artifact.getVersion(), "jar",
                                                                                     classifier );

        a.setRepository( artifact.getRepository() );

        return a;
    }

    private List<String> resolveAndUnpack( final List<Artifact> artifacts, final SourceResolverConfig config,
                                                  final List<String> validClassifiers, final boolean propagateErrors )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // NOTE: Since these are '-sources' and '-test-sources' artifacts, they won't actually 
        // resolve transitively...this is just used to aggregate resolution failures into a single 
        // exception.
        final Set<Artifact> artifactSet = new LinkedHashSet<>( artifacts );

        final ArtifactFilter filter;
        if ( config.filter() != null )
        {
            filter = new ArtifactIncludeFilterTransformer().transform( config.filter() );
        }
        else
        {
            filter = null;
        }
        
        final List<String> result = new ArrayList<>( artifacts.size() );
        for ( final Artifact a : artifactSet )
        {
            if ( !validClassifiers.contains( a.getClassifier() ) || ( filter != null && !filter.include( a ) ) )
            {
                continue;
            }
            
            Artifact resolvedArtifact;
            try
            {
                resolvedArtifact = resolver.resolveArtifact( config.getBuildingRequest(), a ).getArtifact();
            }
            catch ( ArtifactResolverException e1 )
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
                final UnArchiver unArchiver = archiverManager.getUnArchiver( a.getType() );

                unArchiver.setDestDirectory( d );
                unArchiver.setSourceFile( resolvedArtifact.getFile() );

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

    private static List<String> resolveFromProject( final SourceResolverConfig config,
                                                    final MavenProject reactorProject, final Artifact artifact )
    {
        final List<String> dirs = new ArrayList<>();

        if ( config.filter() == null
            || new ArtifactIncludeFilterTransformer().transform( config.filter() ).include( artifact ) )
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
