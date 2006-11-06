package org.apache.maven.plugin.war.stub;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

import java.io.File;

public class EJBArtifactStubWithClassifier
    extends AbstractArtifactStub
{
    protected String groupId;
    protected String classifier;

    public EJBArtifactStubWithClassifier( String _basedir )
    {
        super( _basedir );
    }

    public void setGroupId( String id )
    {
        groupId = id;
    }

    public String getGroupId()
    {
        if ( groupId != null )
        {
            return groupId;
        }
        else
        {
            return "org.sample.ejb";
        }
    }

    public String getType()
    {
        return "ejb";
    }

    public String getArtifactId()
    {
        return "ejbartifact";
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public File getFile()
    {
        return new File( basedir, "/target/test-classes/unit/sample_wars/ejb.jar" );
    }

    public ArtifactHandler getArtifactHandler()
    {
        return new DefaultArtifactHandler()
        {
            public String getExtension()
            {
                return "jar";
            }
        };
    }
}
