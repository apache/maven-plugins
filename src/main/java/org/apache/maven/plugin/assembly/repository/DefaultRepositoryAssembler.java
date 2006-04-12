package org.apache.maven.plugin.assembly.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
 * @author Jason van Zyl
 */
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

    public void assemble( File outputDirectory, List artifacts, List remoteRepositories, boolean includeMetadata )
        throws RepositoryAssemblyException
    {
        ArtifactRepository localRepository = createLocalRepository( outputDirectory );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact ma = (Artifact) i.next();

            Artifact a = artifactFactory.createBuildArtifact( ma.getGroupId() , ma.getArtifactId(), ma.getVersion(), ma.getType() );

            try
            {
                artifactResolver.resolveAlways( a, remoteRepositories, localRepository );

                if ( includeMetadata )
                {
                    File metadataFile = new File( a.getFile().getParentFile().getParent(), "maven-metadata.xml" );

                    Metadata m = new Metadata();

                    m.setGroupId( a.getGroupId() );

                    m.setArtifactId( a.getArtifactId() );

                    Versioning v = new Versioning();

                    v.setRelease( a.getVersion() );

                    v.addVersion( a.getVersion() );

                    m.setVersioning( v );

                    v.setLastUpdated( getUtcDateFormatter().format( new Date() ));

                    MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();

                    metadataWriter.write( new FileWriter( metadataFile ), m );
                }
            }
            catch ( ArtifactResolutionException e )
            {
                throw new RepositoryAssemblyException( "Error resolving artifact: " + a, e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new RepositoryAssemblyException( "Cannot find artifact: " + a, e );
            }
            catch ( IOException e )
            {
                throw new RepositoryAssemblyException( "Error writing artifact metdata.", e );
            }
        }
    }

    public static DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( UTC_TIMESTAMP_PATTERN );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }


    public ArtifactRepository createLocalRepository( File directory )
    {
        String localRepositoryUrl = directory.getAbsolutePath();

        if ( !localRepositoryUrl.startsWith( "file:" ) )
        {
            localRepositoryUrl = "file://" + localRepositoryUrl;
        }

        return createRepository( "local", localRepositoryUrl, false, true, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
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
}
