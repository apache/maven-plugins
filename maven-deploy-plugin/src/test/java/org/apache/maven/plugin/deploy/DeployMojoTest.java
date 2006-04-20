package org.apache.maven.plugin.deploy;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    
    private String LOCAL_REPO = getBasedir() + "/target/local-repo";
    
    private String REMOTE_REPO = getBasedir() + "/target/remote-repo";
    
    MavenProjectStub project = new MavenProjectStub();
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        remoteRepo = new File( REMOTE_REPO );
        
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
        
        assertEquals( "jar", packaging );
        
        artifact.setFile( file );        
        
        ArtifactRepository repo = ( ArtifactRepository ) getVariableValueFromObject( mojo, "deploymentRepository" );

        assertNotNull( repo );
        
        assertEquals( "deploy-test", repo.getId() );
        assertEquals( "deploy-test", repo.getKey() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( "file://" + getBasedir() + "/target/remote-repo", repo.getUrl() );
        
        mojo.execute();

        //check the artifact in local repository
        List expectedFiles = new ArrayList();
        List fileList = new ArrayList();
        
        expectedFiles.add( "org" );
        expectedFiles.add( "apache" );
        expectedFiles.add( "maven" );
        expectedFiles.add( "test" );
        expectedFiles.add( "maven-deploy-test" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata-deploy-test.xml" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom" );
        
        File localRepo = new File( LOCAL_REPO );
        
        File[] files = localRepo.listFiles();
        
        for( int i=0; i<files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );        
                  
        //check the artifact in remote repository
        expectedFiles = new ArrayList();
        fileList = new ArrayList();
        
        expectedFiles.add( "org" );
        expectedFiles.add( "apache" );
        expectedFiles.add( "maven" );
        expectedFiles.add( "test" );
        expectedFiles.add( "maven-deploy-test" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.jar.md5" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.jar.sha1" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom.md5" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom.sha1" );
        
        files = remoteRepo.listFiles();
        
        for( int i=0; i<files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );         
    }
    
    private void addFileToList( File file, List fileList )
    {
        System.out.println( ">> " + file.getName() );
        if( !file.isDirectory() )
        {
            fileList.add( file.getName() );
        }
        else
        {
            fileList.add( file.getName() );

            File[] files = file.listFiles();

            for( int i=0; i<files.length; i++ )
            {
                addFileToList( files[i], fileList );
            }
        }
    }    
    
    private int getSizeOfExpectedFiles( List fileList, List expectedFiles )
    {
        for( Iterator iter=fileList.iterator(); iter.hasNext(); )
        {
            String fileName = ( String ) iter.next();

            if( expectedFiles.contains(  fileName ) )
            {
                expectedFiles.remove( fileName );
                assertFalse( expectedFiles.contains( fileName ) );
            }
            else
            {
                fail( fileName + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
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
