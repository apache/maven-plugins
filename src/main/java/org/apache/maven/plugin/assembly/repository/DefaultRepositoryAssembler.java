package org.apache.maven.plugin.assembly.repository;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.assembly.utils.DigestUtils;
import org.apache.maven.plugins.assembly.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author Jason van Zyl
 */

// todo will need to pop the processed project cache using reflection
public class DefaultRepositoryAssembler
    extends AbstractLogEnabled
    implements RepositoryAssembler
{
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    protected static final String UTC_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    protected ArtifactFactory artifactFactory;

    protected ArtifactResolver artifactResolver;

    protected ArtifactRepositoryLayout repositoryLayout;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    protected ArtifactMetadataSource metadataSource;

    protected MavenProjectBuilder projectBuilder;

    private Map groupVersionAlignments;

    private Set groupVersionAlignmentExcludes;

    private DigestUtils digester = new DigestUtils();

    public void assemble( File repositoryDirectory, Repository repository, MavenProject project,
                          ArtifactRepository localRepository )
        throws RepositoryAssemblyException
    {
        createGroupVersionAlignments( repository.getGroupVersionAlignments() );

        ArtifactRepository targetRepository = createLocalRepository( repositoryDirectory );

        ArtifactResolutionResult result;
        try
        {
            // i have to get everything first as a filter or transformation here doesn't seem to work
            // to align everything. If I use a filter to change the version on the fly then I get the
            // I get JARs but no POMs, and in some directories POMs with no JARs.

            // I'm not getting runtime dependencies here

            result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(), project.getArtifact(),
                                                           project.getRemoteArtifactRepositories(), localRepository,
                                                           metadataSource );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts: " + e.getMessage(), e );
        }

        try
        {
            // Blow the cache in the project builder so that we get POMs again on this next download
            invalidateProccessedProjectCache();
        }
        catch ( Exception e )
        {
            throw new RepositoryAssemblyException( "Error invalidating the processed project cache.", e );
        }

        ArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        try
        {
            // Now that we have the graph, let's try to align it to versions that we want and remove
            // the repository we previously populated.
            FileUtils.deleteDirectory( repositoryDirectory );

            FileUtils.mkdir( repositoryDirectory.getAbsolutePath() );

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( filter.include( a ) )
                {
                    String alignedVersion = (String) groupVersionAlignments.get( a.getGroupId() );

                    if ( alignedVersion != null && !groupVersionAlignmentExcludes.contains( a.getArtifactId() ) )
                    {
                        a.setVersion( alignedVersion );
                    }

                    // We need to flip it back to not being resolved so we can look for it again!
                    a.setResolved( false );

                    artifactResolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );

                    File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( a ) );
                    FileUtils.copyFile( a.getFile(), targetFile );

                    writeChecksums( targetFile );

                    if ( !"pom".equals( a.getType() ) )
                    {
                        // The correct metadata does not get pulled down unless this is used. Not
                        // sure what the problem is.
                        metadataSource.retrieve( a, localRepository, project.getRemoteArtifactRepositories() );

                        Artifact pomArtifact =
                            artifactFactory.createProjectArtifact( a.getGroupId(), a.getArtifactId(), a.getVersion() );
                        File sourceFile =
                            new File( localRepository.getBasedir(), localRepository.pathOf( pomArtifact ) );
                        targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

                        FileUtils.copyFile( sourceFile, targetFile );

                        writeChecksums( targetFile );
                    }
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
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts.", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
        }

        ArtifactRepository centralRepository = null;
        for ( Iterator i = project.getRemoteArtifactRepositories().iterator(); i.hasNext(); )
        {
            ArtifactRepository r = (ArtifactRepository) i.next();
            if ( "central".equals( r.getId() ) )
            {
                centralRepository = r;
            }
        }

        if ( repository.isIncludeMetadata() )
        {
            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

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
                                                            targetRepository.pathOfRemoteRepositoryMetadata(
                                                                metadata ) );

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
    }

    private void writeChecksums( File file )
        throws IOException, RepositoryAssemblyException
    {
        try
        {
            String md5 = digester.createChecksum( file, "MD5" );
            String sha1 = digester.createChecksum( file, "SHA-1" );

            FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".md5" ).getAbsolutePath(), md5 );
            FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".sha1" ).getAbsolutePath(), sha1 );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryAssemblyException( "Unable to get write checksums: " + e.getMessage(), e );
        }
    }

    protected void createGroupVersionAlignments( List versionAlignments )
    {
        groupVersionAlignments = new HashMap();

        groupVersionAlignmentExcludes = new HashSet();

        for ( Iterator i = versionAlignments.iterator(); i.hasNext(); )
        {
            GroupVersionAlignment alignment = (GroupVersionAlignment) i.next();

            groupVersionAlignments.put( alignment.getId(), alignment.getVersion() );

            if ( !alignment.getExcludes().isEmpty() )
            {
                groupVersionAlignmentExcludes.addAll( alignment.getExcludes() );
            }
        }
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
        Class klass = DefaultMavenProjectBuilder.class;

        Field field = klass.getDeclaredField( "processedProjectCache" );

        field.setAccessible( true );

        Object cache = field.get( projectBuilder );

        cache.getClass().getDeclaredMethod( "clear", null ).invoke( cache, null );

        field.setAccessible( false );
    }
}
