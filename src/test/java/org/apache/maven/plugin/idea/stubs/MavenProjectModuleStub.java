package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class MavenProjectModuleStub
    extends MavenProjectStub
{
    private File basedir;

    private String artifactId;

    public MavenProjectModuleStub( String artifactId, File basedir )
    {
        super();

        this.basedir = basedir;

        this.artifactId = artifactId;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public String getArtifactId()
    {
        return artifactId;
    }
}
