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
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.assembly.repository.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
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
import org.apache.maven.shared.utils.io.IOUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
    implements RepositoryAssembler
{
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    protected static final String UTC_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    @Requirement
    protected ArtifactFactory artifactFactory;

    // Replaced by Contextualizable code, to select the resolver in order of preference.
    //
    // @plexus.requirement
    //
    @Requirement
    protected ArtifactResolver artifactResolver;

    @Requirement
    protected ArtifactRepositoryLayout repositoryLayout;

    @Requirement
    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement
    protected ArtifactMetadataSource metadataSource;

    @Requirement
    protected MavenProjectBuilder projectBuilder;

    @Requirement
    private DependencyResolver dependencyResolver;
    
    @Requirement
    private RepositoryManager repositoryManager;

    public void buildRemoteRepository( File repositoryDirectory, RepositoryInfo repository,
                                       RepositoryBuilderConfigSource configSource )
        throws RepositoryAssemblyException
    {
        MavenProject project = configSource.getProject();
        ArtifactRepository localRepository = configSource.getLocalRepository();
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

        buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, repositoryDirectory );
        
        ArtifactRepository targetRepository = createLocalRepository( repositoryDirectory );

        Map<String, GroupVersionAlignment> groupVersionAlignments =
                        createGroupVersionAlignments( repository.getGroupVersionAlignments() );

        assembleRepositoryArtifacts( buildingRequest, result, filter, project, localRepository, targetRepository,
                                     groupVersionAlignments );

        ArtifactRepository centralRepository = findCentralRepository( project );

        if ( repository.isIncludeMetadata() )
        {
            assembleRepositoryMetadata( result, filter, centralRepository, targetRepository );
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
                                              ArtifactFilter filter, MavenProject project,
                                              ArtifactRepository localRepository, ArtifactRepository targetRepository,
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

                    File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( a ) );
                    FileUtils.copyFile( a.getFile(), targetFile );

                    writeChecksums( targetFile );
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

    private void assembleRepositoryMetadata( Iterable<ArtifactResult> result, ArtifactFilter filter,
                                             ArtifactRepository centralRepository, ArtifactRepository targetRepository )
        throws RepositoryAssemblyException
    {
        for ( ArtifactResult ar : result )
        {
            Artifact a = ar.getArtifact();
            
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
}
