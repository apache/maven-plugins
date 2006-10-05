package org.apache.maven.plugin.dependency.stubs;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;

public class StubArtifactResolver
    implements ArtifactResolver
{

    boolean throwArtifactResolutionException;
    boolean throwArtifactNotFoundException;
    public StubArtifactResolver(boolean throwArtifactResolutionException, boolean throwArtifactNotFoundException)
    {
        this.throwArtifactNotFoundException = throwArtifactNotFoundException;
        this.throwArtifactResolutionException = throwArtifactResolutionException;
    }

    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if (!this.throwArtifactNotFoundException && !this.throwArtifactResolutionException)
        {
        // TODO Auto-generated method stub
        ArtifactStubFactory factory = new ArtifactStubFactory(new File(localRepository.getBasedir()),true);
        factory.setArtifactFile(artifact);
        }
        else
        {
            if (throwArtifactResolutionException)
            {
                throw new ArtifactResolutionException("Catch!", artifact);
            }
            else
            {
                throw new ArtifactNotFoundException("Catch!",artifact);
            }
        }
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        List remoteRepositories, ArtifactRepository localRepository,
                                                        ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        List remoteRepositories, ArtifactRepository localRepository,
                                                        ArtifactMetadataSource source, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        ArtifactRepository localRepository, List remoteRepositories,
                                                        ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source,
                                                        ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source,
                                                        ArtifactFilter filter, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void resolveAlways( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub

    }

}
