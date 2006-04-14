package org.apache.maven.plugin.assembly.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.lang.reflect.Field;

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

    private Map versionAlignmentMap;

    public void assemble( File repositoryDirectory,
                          Repository repository,
                          MavenProject project )
        throws RepositoryAssemblyException
    {
        versionAlignmentMap = createVersionAlignmentMap( repository.getVersionAlignments() );

        ArtifactRepository localRepository = createLocalRepository( repositoryDirectory );

        try
        {
            // i have to get everything first as a filter or transformation here doesn't seem to work
            // to align everything. If I use a filter to change the version on the fly then I get the
            // I get JARs but no POMs, and in some directories POMs with no JARs.

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(),
                                                                                    project.getArtifact(),
                                                                                    project.getRemoteArtifactRepositories(),
                                                                                    localRepository,
                                                                                    metadataSource );

            // Now that we have the graph, let's try to align it to versions that we want and remove
            // the repository we previously populated.
            FileUtils.deleteDirectory( repositoryDirectory );

            FileUtils.mkdir( repositoryDirectory.getAbsolutePath() );

            // Blow the cache in the project builder so that we get POMs again on this next download
            invalidateProccessedProjectCache();

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                String alignedVersion = (String) versionAlignmentMap.get( a.getGroupId() );

                if ( alignedVersion != null )
                {
                    a.setVersion( alignedVersion );
                }

                // We need to flip it back to not being resolved so we can look for it again!
                a.setResolved( false );

                artifactResolver.resolveAlways( a, project.getRemoteArtifactRepositories(), localRepository );

                // The correct metadata does not get pulled down unless this is used. Not
                // sure what the problem is.
                metadataSource.retrieve( a, localRepository, project.getRemoteArtifactRepositories() );
            }

            if ( repository.isIncludeMetadata() )
            {
                for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
                {
                    Artifact a = (Artifact) i.next();

                    File metadataFile = new File( a.getFile().getParentFile().getParent(), "maven-metadata-central.xml" );

                    Metadata m = new Metadata();

                    m.setGroupId( a.getGroupId() );

                    m.setArtifactId( a.getArtifactId() );

                    Versioning v = new Versioning();

                    v.setRelease( a.getVersion() );

                    v.setLatest( a.getVersion() );

                    v.addVersion( a.getVersion() );

                    m.setVersioning( v );

                    v.setLastUpdated( getUtcDateFormatter().format( new Date() ) );

                    MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();

                    Writer writer = new FileWriter( metadataFile );

                    metadataWriter.write( writer, m );

                    writer.close();

                    File metadataFileRemote = new File( a.getFile().getParentFile().getParent(), "maven-metadata.xml" );

                    FileUtils.copyFile( metadataFile, metadataFileRemote );
                }
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts.", e );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new RepositoryAssemblyException( "Error resolving artifacts.", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
        }
        catch ( Exception e )
        {
            throw new RepositoryAssemblyException( "Error invalidating the processed project cache.", e );
        }
    }

    protected Map createVersionAlignmentMap( List versionAlignments )
    {
        Map m = new HashMap();

        for ( Iterator i = versionAlignments.iterator(); i.hasNext(); )
        {
            String alignment = (String) i.next();

            // split into groupId and version
            String[] s = StringUtils.split( alignment, ":" );

            m.put( s[0], s[1] );
        }

        return m;
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

    public ArtifactRepository createRepository( String repositoryId,
                                                String repositoryUrl,
                                                boolean offline,
                                                boolean updateSnapshots,
                                                String globalChecksumPolicy )
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
