package org.apache.maven.plugin.deploy.stubs;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

public class DeployArtifactStub
    extends ArtifactStub
{
    private Map metadataMap;
    
    private File file;
    
    private boolean release;
    
    public String getArtifactId()
    {
        return "maven-deploy-test";
    }

    public String getGroupId()
    {
        return "org.apache.maven.test";
    }

    public String getVersion()
    {
        return "1.0-SNAPSHOT";
    }
    
    public String getBaseVersion()
    {
        return getVersion();
    }
    
    public void setFile( File file )
    {
        this.file = file;
    }
    
    public File getFile()
    {
        return file;
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
    
    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataMap == null )
        {
            metadataMap = new HashMap();
        }

        ArtifactMetadata m = (ArtifactMetadata) metadataMap.get( metadata.getKey() );
        if ( m != null )
        {
            m.merge( metadata );
        }
        else
        {
            metadataMap.put( metadata.getKey(), metadata );
        }
    }
    
    public Collection getMetadataList()
    {
        return metadataMap == null ? Collections.EMPTY_LIST : metadataMap.values();
    }

    public boolean isRelease()
    {
        return release;
    }

    public void setRelease( boolean release )
    {
        this.release = release;
    }
}
