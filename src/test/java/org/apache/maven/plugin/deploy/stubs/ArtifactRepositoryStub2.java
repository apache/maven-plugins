package org.apache.maven.plugin.deploy.stubs;

public class ArtifactRepositoryStub2
    extends ArtifactRepositoryStub
{
    public String getUrl()
    {
        return "file://" + System.getProperty( "basedir" ) + "/target/remote-repo/basic-deploy-scp";
    }
    
    public String getBasedir()
    {
        return System.getProperty( "basedir" );
    }
    
    public String getProtocol()
    {
        return "scp";
    }
}
