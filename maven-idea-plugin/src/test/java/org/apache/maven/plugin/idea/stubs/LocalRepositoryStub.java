package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

/**
 * @author Edwin Punzalan
 */
public class LocalRepositoryStub
    extends DefaultArtifactRepository
{
    public LocalRepositoryStub()
    {
        super( "local-repo", "file://" + System.getProperty( "localRepository") , new DefaultRepositoryLayout() );
    }
}
