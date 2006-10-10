package org.apache.maven.plugin.dependency.stubs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

public class StubArtifactRepository
    implements ArtifactRepository
{
    String baseDir = null;

    public StubArtifactRepository( String dir )
    {
        baseDir = dir;
    }

    public String pathOf( Artifact artifact )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUrl()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getBasedir()
    {
        return baseDir;
    }

    public String getProtocol()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getKey()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isUniqueVersion()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setBlacklisted( boolean blackListed )
    {
        // TODO Auto-generated method stub

    }

    public boolean isBlacklisted()
    {
        // TODO Auto-generated method stub
        return false;
    }

}
