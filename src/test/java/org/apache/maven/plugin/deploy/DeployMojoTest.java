package org.apache.maven.plugin.deploy;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.deploy.DeployMojo;
import org.apache.maven.plugin.deploy.stubs.DeployArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class DeployMojoTest
    extends AbstractMojoTestCase
{    
    private File remoteRepo;
    
    private final String LOCAL_REPO = "/target/local-repo/";
    
    MavenProjectStub project = new MavenProjectStub();
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        remoteRepo = new File( getBasedir(), "target/remote-repo" );
        
        remoteRepo.mkdirs();    
    }
    
    public void testDeployTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-test/plugin-config.xml" );
    
        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
    
        assertNotNull( mojo );
    }
    
    public void testBasicDeploy()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-test/plugin-config.xml" );

        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
        
        assertNotNull( mojo );
        
        File file = new File( getBasedir(),
                              "target/test-classes/unit/basic-deploy-test/target/" +
                              "deploy-test-file-1.0-SNAPSHOT.jar" );

        assertTrue( file.exists() );
        
        DeployArtifactStub artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );

        String packaging = ( String ) getVariableValueFromObject( mojo, "packaging" );
        
        artifact.setFile( file );        
        
        File deployedArtifact = new File( getBasedir(), 
                                           "target/remote-repo/" + artifact.getGroupId().replace( '.', '/' ) + 
                                           "/" + artifact.getArtifactId() + "/" + artifact.getVersion() +  
                                           "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + packaging );
        
        ArtifactRepository repo = ( ArtifactRepository ) getVariableValueFromObject( mojo, "deploymentRepository" );

        assertNotNull( repo );
        
        assertEquals( "deploy-test", repo.getId() );
        assertEquals( "deploy-test", repo.getKey() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( "file://" + getBasedir() + "/target/remote-repo", repo.getUrl() );
        
        mojo.execute();
        
        assertTrue( "Artifact has been deployed", deployedArtifact.exists() );
    }
    
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        
        if( remoteRepo.exists() )
        {
            //FileUtils.deleteDirectory( remoteRepo );
        }
    }
}
