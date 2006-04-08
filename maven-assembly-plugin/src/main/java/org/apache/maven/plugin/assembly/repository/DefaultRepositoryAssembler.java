package org.apache.maven.plugin.assembly.repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public class DefaultRepositoryAssembler
    extends AbstractLogEnabled
    implements RepositoryAssembler
{
    protected ArtifactFactory artifactFactory;

    protected ArtifactResolver artifactResolver;

    protected ArtifactRepositoryLayout repositoryLayout;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    public void assemble( File outputDirectory, List artifacts, List remoteRepositories )
        throws RepositoryAssemblyException
    {
        ArtifactRepository localRepository = createLocalRepository( outputDirectory );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact ma = (Artifact) i.next();

            Artifact a = artifactFactory.createProjectArtifact( ma.getGroupId(), ma.getArtifactId(), ma.getVersion() );

            try
            {
                // Not sure if i want to do this transitively
                artifactResolver.resolveAlways( a, remoteRepositories, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new RepositoryAssemblyException( "Error resolving artifact: " + a, e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new RepositoryAssemblyException( "Cannot find artifact: " + a, e );
            }
        }
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
