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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugins.assembly.repository.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;

/**
 * @author Jason van Zyl
 */

// todo will need to pop the processed project cache using reflection
@Component( role = RepositoryAssembler.class )
public class DefaultRepositoryAssembler
    extends AbstractLogEnabled
    implements RepositoryAssembler, Contextualizable
{
    private static final String[] PREFERRED_RESOLVER_HINTS = { "project-cache-aware", // Provided in Maven 2.1-SNAPSHOT
        "default" };

    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    protected static final String UTC_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    @Requirement
    protected ArtifactFactory artifactFactory;

    // Replaced by Contextualizable code, to select the resolver in order of preference.
    //
    // @plexus.requirement
    //
    protected ArtifactResolver artifactResolver;

    @Requirement
    protected ArtifactRepositoryLayout repositoryLayout;

    @Requirement
    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement
    protected ArtifactMetadataSource metadataSource;

    @Requirement
    protected MavenProjectBuilder projectBuilder;

    public DefaultRepositoryAssembler()
    {
        // used for plexus init.
    }

    public DefaultRepositoryAssembler( ArtifactFactory artifactFactory, ArtifactResolver artifactResolver,
                                       ArtifactRepositoryLayout repositoryLayout,
                                       ArtifactRepositoryFactory artifactRepositoryFactory,
                                       ArtifactMetadataSource metadataSource, MavenProjectBuilder projectBuilder )
    {
        // used for testing, primarily.
        this.artifactFactory = artifactFactory;
        this.artifactResolver = artifactResolver;
        this.repositoryLayout = repositoryLayout;
        this.artifactRepositoryFactory = artifactRepositoryFactory;
        this.metadataSource = metadataSource;
        this.projectBuilder = projectBuilder;

        enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, getClass().getName() + "::Internal" ) );
    }

    public void buildRemoteRepository( File repositoryDirectory, RepositoryInfo repository,
                                       RepositoryBuilderConfigSource configSource )
        throws RepositoryAssemblyException
    {
        MavenProject project = configSource.getProject();
        ArtifactRepository localRepository = configSource.getLocalRepository();

        Map<String, GroupVersionAlignment> groupVersionAlignments = createGroupVersionAlignments( repository.getGroupVersionAlignments() );

        ArtifactRepository targetRepository = createLocalRepository( repositoryDirectory );

        ArtifactResolutionResult result = null;

        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();

        if ( dependencyArtifacts == null )
        {
            Logger logger = getLogger();

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "dependency-artifact set for project: " + project.getId()
                                  + " is null. Skipping repository processing." );
            }

            return;
        }

        try
        {
            // i have to get everything first as a filter or transformation here
            // doesn't seem to work
            // to align everything. If I use a filter to change the version on
            // the fly then I get the
            // I get JARs but no POMs, and in some directories POMs with no
            // JARs.

            // FIXME I'm not getting runtime dependencies here
            result = artifactResolver.resolveTransitively( dependencyArtifacts, project.getArtifact(),
                                                           getManagedVersionMap( project ), localRepository,
                                                           project.getRemoteArtifactRepositories(), metadataSource );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }

        try
        {
            // Blow the cache in the project builder so that we get POMs again
            // on this next download
            invalidateProccessedProjectCache();
        }
        catch ( Exception e )
        {
            throw new RepositoryAssemblyException( "Error invalidating the processed project cache.", e );
        }

        ArtifactFilter filter = buildRepositoryFilter( repository, project );

        assembleRepositoryArtifacts( result, filter, project, localRepository, targetRepository, repositoryDirectory,
                                     groupVersionAlignments );

        ArtifactRepository centralRepository = findCentralRepository( project );

        if ( repository.isIncludeMetadata() )
        {
            assembleRepositoryMetadata( result, filter, centralRepository, targetRepository );
        }

        addPomWithAncestry( project.getArtifact(), project.getRemoteArtifactRepositories(), localRepository,
                            targetRepository, groupVersionAlignments, project );
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

    private void assembleRepositoryArtifacts( ArtifactResolutionResult result, ArtifactFilter filter,
                                              MavenProject project, ArtifactRepository localRepository,
                                              ArtifactRepository targetRepository, File repositoryDirectory,
                                              Map<String, GroupVersionAlignment> groupVersionAlignments )
        throws RepositoryAssemblyException
    {
        try
        {
            // Now that we have the graph, let's try to align it to versions
            // that we want and remove
            // the repository we previously populated.
            FileUtils.deleteDirectory( repositoryDirectory );

            FileUtils.mkdir( repositoryDirectory.getAbsolutePath() );

            for ( Artifact a : result.getArtifacts() )
            {
                if ( filter.include( a ) )
                {
                    getLogger().debug( "Re-resolving: " + a + " for repository assembly." );

                    setAlignment( a, groupVersionAlignments );

                    // We need to flip it back to not being resolved so we can
                    // look for it again!
                    a.setResolved( false );

                    artifactResolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );

                    a.setVersion( a.getBaseVersion() );

                    File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( a ) );
                    FileUtils.copyFile( a.getFile(), targetFile );

                    writeChecksums( targetFile );

                    addPomWithAncestry( a, project.getRemoteArtifactRepositories(), localRepository, targetRepository,
                                        groupVersionAlignments, project );
                }
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
        }
    }

    private void addPomWithAncestry( final Artifact artifact, List<ArtifactRepository> remoteArtifactRepositories,
                                     ArtifactRepository localRepository, ArtifactRepository targetRepository,
                                     Map<String, GroupVersionAlignment> groupVersionAlignments, MavenProject masterProject )
        throws RepositoryAssemblyException
    {
        String type = artifact.getType();
        Map<String, MavenProject> refs = masterProject.getProjectReferences();

        String projectKey = ArtifactUtils.versionlessKey( artifact );

        MavenProject p;
        if ( artifact == masterProject.getArtifact() )
        {
            p = masterProject;
        }
        else if ( refs.containsKey( projectKey ) )
        {
            p = refs.get( projectKey );
        }
        else
        {
            try
            {
                artifact.isSnapshot();

                Artifact pomArtifact =
                    artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                           artifact.getBaseVersion() );

                getLogger().debug( "Building MavenProject instance for: " + pomArtifact
                                       + ". NOTE: This SHOULD BE available in the Artifact API! ...but it's not." );
                p = projectBuilder.buildFromRepository( pomArtifact, remoteArtifactRepositories, localRepository );
            }
            catch ( ProjectBuildingException e )
            {
                throw new RepositoryAssemblyException( "Error reading POM for: " + artifact.getId(), e );
            }
        }

        // if we're dealing with a POM artifact, then we've already copied the POM itself; only process ancestry.
        // NOTE: We need to preserve the original artifact for comparison here.
        if ( "pom".equals( type ) )
        {
            p = p.getParent();
        }

        while ( p != null )
        {
            Artifact destArtifact =
                artifactFactory.createProjectArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion() );

            setAlignment( destArtifact, groupVersionAlignments );

            File sourceFile = p.getFile();

            // try to use the POM file from the project instance itself first.
            if ( ( sourceFile == null ) || !sourceFile.exists() )
            {
                // something that hasn't been realigned yet...we want to read from the original location.
                Artifact srcArtifact =
                    artifactFactory.createProjectArtifact( p.getGroupId(), p.getArtifactId(), p.getVersion() );

                sourceFile = new File( localRepository.getBasedir(), localRepository.pathOf( srcArtifact ) );
            }

            if ( !sourceFile.exists() )
            {
                break;
            }

            File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( destArtifact ) );

            try
            {
                FileUtils.copyFile( sourceFile, targetFile );
            }
            catch ( IOException e )
            {
                throw new RepositoryAssemblyException( "Error writing POM metdata: " + destArtifact.getId(), e );
            }

            try
            {
                writeChecksums( targetFile );
            }
            catch ( IOException e )
            {
                throw new RepositoryAssemblyException( "Error writing checksums for POM: " + destArtifact.getId(), e );
            }

            p = p.getParent();
        }
    }

    private ArtifactRepository findCentralRepository( MavenProject project )
    {
        ArtifactRepository centralRepository = null;
        for ( ArtifactRepository r : project.getRemoteArtifactRepositories() )
        {
            if ( "central".equals( r.getId() ) )
            {
                centralRepository = r;
            }
        }

        return centralRepository;
    }

    private void assembleRepositoryMetadata( ArtifactResolutionResult result, ArtifactFilter filter,
                                             ArtifactRepository centralRepository, ArtifactRepository targetRepository )
        throws RepositoryAssemblyException
    {
        for ( Artifact a : result.getArtifacts() )
        {
            if ( filter.include( a ) )
            {
                Versioning v = new Versioning();

                v.setRelease( a.getVersion() );

                v.setLatest( a.getVersion() );

                v.addVersion( a.getVersion() );

                v.setLastUpdated( getUtcDateFormatter().format( new Date() ) );

                ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( a, v );
                String path = targetRepository.pathOfLocalRepositoryMetadata( metadata, centralRepository );
                File metadataFile = new File( targetRepository.getBasedir(), path );

                MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();

                Writer writer = null;
                try
                {
                    writer = new FileWriter( metadataFile );

                    metadataWriter.write( writer, metadata.getMetadata() );
                }
                catch ( IOException e )
                {
                    throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
                }
                finally
                {
                    IOUtil.close( writer );
                }

                try
                {
                    writeChecksums( metadataFile );

                    File metadataFileRemote = new File( targetRepository.getBasedir(),
                                                        targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );

                    FileUtils.copyFile( metadataFile, metadataFileRemote );

                    FileUtils.copyFile( new File( metadataFile.getParentFile(), metadataFile.getName() + ".sha1" ),
                                        new File( metadataFileRemote.getParentFile(),
                                                  metadataFileRemote.getName() + ".sha1" ) );

                    FileUtils.copyFile( new File( metadataFile.getParentFile(), metadataFile.getName() + ".md5" ),
                                        new File( metadataFileRemote.getParentFile(),
                                                  metadataFileRemote.getName() + ".md5" ) );
                }
                catch ( IOException e )
                {
                    throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
                }
            }
        }
    }

    private void writeChecksums( File file )
        throws IOException, RepositoryAssemblyException
    {
        FileInputStream data = new FileInputStream( file );
        String md5 = md5Hex( data ).toUpperCase();
        data.close();
        FileInputStream data1 = new FileInputStream( file );
        String sha1 = shaHex( data1 ).toUpperCase();
        data1.close();

        FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".md5" ).getAbsolutePath(),
                             md5.toLowerCase() );
        FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".sha1" ).getAbsolutePath(),
                             sha1.toLowerCase() );
    }

    protected Map<String, GroupVersionAlignment> createGroupVersionAlignments( List<GroupVersionAlignment> versionAlignments )
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

    protected static DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( UTC_TIMESTAMP_PATTERN );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }

    protected ArtifactRepository createLocalRepository( File directory )
    {
        String localRepositoryUrl = directory.getAbsolutePath();

        if ( !localRepositoryUrl.startsWith( "file:" ) )
        {
            localRepositoryUrl = "file://" + localRepositoryUrl;
        }

        return createRepository( "local", localRepositoryUrl, false, true,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
    }

    public ArtifactRepository createRepository( String repositoryId, String repositoryUrl, boolean offline,
                                                boolean updateSnapshots, String globalChecksumPolicy )
    {
        ArtifactRepository localRepository =
            new DefaultArtifactRepository( repositoryId, repositoryUrl, repositoryLayout );

        boolean snapshotPolicySet = false;

        if ( offline )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet && updateSnapshots )
        {
            artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( globalChecksumPolicy );

        return localRepository;
    }

    private void invalidateProccessedProjectCache()
        throws Exception
    {
        Class<DefaultMavenProjectBuilder> klass = DefaultMavenProjectBuilder.class;

        try
        {
            Field field = klass.getDeclaredField( "processedProjectCache" );

            field.setAccessible( true );

            Object cache = field.get( projectBuilder );

            cache.getClass().getDeclaredMethod( "clear", null ).invoke( cache, null );

            field.setAccessible( false );
        }
        catch ( NoSuchFieldException e )
        {
            // fine... no field, no cache. we'll ignore it.
        }
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

    // TODO: Remove this, once we can depend on Maven 2.0.7 or later...in which
    // MavenProject.getManagedVersionMap() exists. This is from MNG-1577.
    private Map<String, Artifact> getManagedVersionMap( MavenProject project )
        throws InvalidVersionSpecificationException
    {
        DependencyManagement dependencyManagement = project.getModel().getDependencyManagement();

        Map<String, Artifact> map;
        List<Dependency> deps = ( dependencyManagement == null ) ? null : dependencyManagement.getDependencies();
        if ( ( deps != null ) && ( deps.size() > 0 ) )
        {
            map = new HashMap<String, Artifact>();

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Adding managed dependencies for " + project.getId() );
            }

            for ( Dependency d : dependencyManagement.getDependencies() )
            {
                VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                Artifact artifact =
                    artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange,
                                                              d.getType(), d.getClassifier(), d.getScope(),
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
                    List<String> exclusions = new ArrayList<String>();
                    for ( Exclusion e : d.getExclusions() )
                    {
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
        else
        {
            map = Collections.emptyMap();
        }
        return map;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        PlexusContainer container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );

        for ( String hint : PREFERRED_RESOLVER_HINTS )
        {
            try
            {
                artifactResolver = container.lookup( ArtifactResolver.class, hint );
                break;
            }
            catch ( ComponentLookupException e )
            {
                getLogger().debug( "No ArtifactResolver with hint " + hint );
            }
        }

        if ( artifactResolver == null )
        {
            try
            {
                artifactResolver = container.lookup( ArtifactResolver.class );
            }
            catch ( ComponentLookupException e )
            {
                getLogger().debug( "Cannot find ArtifactResolver with no hint.", e );
            }
        }

        if ( artifactResolver == null )
        {
            throw new ContextException(
                "Failed to lookup a valid ArtifactResolver implementation. Tried hints:\n" + Arrays.asList(
                    PREFERRED_RESOLVER_HINTS ) );
        }
    }
}
