package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugins.testing.stubs.StubArtifact;

/**
 * @author Edwin Punzalan
 */
public class ArtifactStub
    extends StubArtifact
{
    private String groupId;

    private String artifactId;

    private String version;


    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getArtifactId( String artifactId )
    {
        return artifactId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return new DefaultArtifactVersion( getVersion() );
    }
}
