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
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.filter.AssemblyExcludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyIncludesArtifactFilter;
import org.apache.maven.plugin.assembly.utils.DigestUtils;
import org.apache.maven.plugins.assembly.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author Jason van Zyl
 * @plexus.component role="org.apache.maven.plugin.assembly.repository.RepositoryAssembler" role-hint="default"
 */

// todo will need to pop the processed project cache using reflection
public class DefaultRepositoryAssembler
    extends AbstractLogEnabled
    implements RepositoryAssembler
{
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    protected static final String UTC_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    /**
     * @plexus.requirement
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @plexus.requirement
     */
    protected ArtifactRepositoryLayout repositoryLayout;

    /**
     * @plexus.requirement
     */
    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * @plexus.requirement
     */
    protected ArtifactMetadataSource metadataSource;

    /**
     * @plexus.requirement
     */
    protected MavenProjectBuilder projectBuilder;

    public void assemble( File repositoryDirectory, Repository repository, AssemblerConfigurationSource configSource )
        throws RepositoryAssemblyException
    {
        MavenProject project = configSource.getProject();
        ArtifactRepository localRepository = configSource.getLocalRepository();

        Map groupVersionAlignments = createGroupVersionAlignments( repository.getGroupVersionAlignments() );

        ArtifactRepository targetRepository = createLocalRepository( repositoryDirectory );

        ArtifactResolutionResult result = null;
        
        Set dependencyArtifacts = project.getDependencyArtifacts();
        
        if ( dependencyArtifacts == null )
        {
            Logger logger = getLogger();
            
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "dependency-artifact set for project: " + project.getId() + " is null. Skipping repository processing." );
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
                project.getRemoteArtifactRepositories(), localRepository, metadataSource );
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
            // Blow the cache in the project builder so that we get POMs again
            // on this next download
            invalidateProccessedProjectCache();
        }
        catch ( Exception e )
        {
            throw new RepositoryAssemblyException( "Error invalidating the processed project cache.", e );
        }
        
        ArtifactFilter filter = buildRepositoryFilter( repository, project);

        assembleRepositoryArtifacts( result, filter, project, localRepository, targetRepository, repositoryDirectory, groupVersionAlignments );
        
        ArtifactRepository centralRepository = findCentralRepository( project );

        if ( repository.isIncludeMetadata() )
        {
            assembleRepositoryMetadata( result, filter, centralRepository, targetRepository );
        }
    }

    private ArtifactFilter buildRepositoryFilter(Repository repository, MavenProject project)
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

        if ( repository.getIncludes().isEmpty() )
        {
            List patterns = new ArrayList();

            Set projectArtifacts = project.getDependencyArtifacts();

            if ( projectArtifacts != null )
            {
                for ( Iterator it = projectArtifacts.iterator(); it.hasNext(); )
                {
                    Artifact artifact = (Artifact) it.next();

                    patterns.add( artifact.getDependencyConflictId() );
                }
            }

            AssemblyIncludesArtifactFilter includeFilter = new AssemblyIncludesArtifactFilter( patterns, true );

            filter.add( includeFilter );
        }
        else
        {
            filter.add( new AssemblyIncludesArtifactFilter( repository.getIncludes(), true ) );
        }

        // ----------------------------------------------------------------------------
        // Excludes
        //
        // We still want to make it easy to exclude a few things even if we
        // slurp
        // up everything.
        // ----------------------------------------------------------------------------

        if ( !repository.getExcludes().isEmpty() )
        {
            filter.add( new AssemblyExcludesArtifactFilter( repository.getExcludes(), true ) );
        }

        return filter;
    }

    private void assembleRepositoryArtifacts( ArtifactResolutionResult result, ArtifactFilter filter,
                                              MavenProject project, ArtifactRepository localRepository,
                                              ArtifactRepository targetRepository, File repositoryDirectory, Map groupVersionAlignments )
        throws RepositoryAssemblyException
    {
        try
        {
            // Now that we have the graph, let's try to align it to versions
            // that we want and remove
            // the repository we previously populated.
            FileUtils.deleteDirectory( repositoryDirectory );

            FileUtils.mkdir( repositoryDirectory.getAbsolutePath() );

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( filter.include( a ) )
                {
                    setAlignment( a, groupVersionAlignments );

                    // We need to flip it back to not being resolved so we can
                    // look for it again!
                    a.setResolved( false );

                    artifactResolver.resolve( a, project.getRemoteArtifactRepositories(), localRepository );

                    File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( a ) );
                    FileUtils.copyFile( a.getFile(), targetFile );

                    writeChecksums( targetFile );

                    if ( !"pom".equals( a.getType() ) )
                    {
                        a = artifactFactory.createProjectArtifact( a.getGroupId(), a.getArtifactId(), a.getVersion() );

                        MavenProject p = projectBuilder.buildFromRepository( a,
                            project.getRemoteArtifactRepositories(), localRepository );

                        do
                        {
                            a = artifactFactory.createProjectArtifact( p.getGroupId(), p.getArtifactId(), p
                                .getVersion() );

                            setAlignment( a, groupVersionAlignments );

                            File sourceFile = new File( localRepository.getBasedir(), localRepository.pathOf( a ) );

                            if ( !sourceFile.exists() )
                            {
                                break;
                            }

                            targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( a ) );

                            FileUtils.copyFile( sourceFile, targetFile );

                            writeChecksums( targetFile );

                            p = p.getParent();
                        } while ( p != null );
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
        catch ( IOException e )
        {
            throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new RepositoryAssemblyException( "Error reading POM.", e );
        }
    }

    private ArtifactRepository findCentralRepository( MavenProject project )
    {
        ArtifactRepository centralRepository = null;
        for ( Iterator i = project.getRemoteArtifactRepositories().iterator(); i.hasNext(); )
        {
            ArtifactRepository r = (ArtifactRepository) i.next();
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

                    File metadataFileRemote = new File( targetRepository.getBasedir(), targetRepository
                        .pathOfRemoteRepositoryMetadata( metadata ) );

                    FileUtils.copyFile( metadataFile, metadataFileRemote );

                    FileUtils.copyFile( new File( metadataFile.getParentFile(), metadataFile.getName() + ".sha1" ),
                        new File( metadataFileRemote.getParentFile(), metadataFileRemote.getName() + ".sha1" ) );

                    FileUtils.copyFile( new File( metadataFile.getParentFile(), metadataFile.getName() + ".md5" ),
                        new File( metadataFileRemote.getParentFile(), metadataFileRemote.getName() + ".md5" ) );
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
        try
        {
            String md5 = DigestUtils.createChecksum( file, "MD5" );
            String sha1 = DigestUtils.createChecksum( file, "SHA-1" );

            FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".md5" ).getAbsolutePath(), md5
                .toLowerCase() );
            FileUtils.fileWrite( new File( file.getParentFile(), file.getName() + ".sha1" ).getAbsolutePath(), sha1
                .toLowerCase() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryAssemblyException( "Unable to get write checksums: " + e.getMessage(), e );
        }
    }

    protected Map createGroupVersionAlignments( List versionAlignments )
    {
        Map groupVersionAlignments = new HashMap();

        for ( Iterator i = versionAlignments.iterator(); i.hasNext(); )
        {
            GroupVersionAlignment alignment = (GroupVersionAlignment) i.next();

            groupVersionAlignments.put( alignment.getId(), alignment );
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
        ArtifactRepository localRepository = new DefaultArtifactRepository( repositoryId, repositoryUrl,
            repositoryLayout );

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

    private void setAlignment( Artifact artifact, Map groupVersionAlignments )
    {
        GroupVersionAlignment alignment = (GroupVersionAlignment) groupVersionAlignments.get( artifact.getGroupId() );

        if ( alignment != null )
        {
            if ( !alignment.getExcludes().contains( artifact.getArtifactId() ) )
            {
                artifact.setVersion( alignment.getVersion() );
            }
        }
    }
}
