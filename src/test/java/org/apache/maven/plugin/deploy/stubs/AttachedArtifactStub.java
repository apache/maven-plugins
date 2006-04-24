package org.apache.maven.plugin.deploy.stubs;

import java.io.File;

public class AttachedArtifactStub
    extends DeployArtifactStub
{
    public String getArtifactId()
    {
        return "attached-artifact-test-0";
    }
    
    public File getFile()
    {
        return new File( System.getProperty( "basedir" ), 
            "target/test-classes/unit/basic-deploy-with-attached-artifacts/" +
            "target/deploy-test-file-1.0-SNAPSHOT.jar" ); 
    }    
}
