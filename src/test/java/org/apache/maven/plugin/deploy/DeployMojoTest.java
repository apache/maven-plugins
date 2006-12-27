package org.apache.maven.plugin.deploy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.deploy.DeployMojo;
import org.apache.maven.plugin.deploy.stubs.ArtifactRepositoryStub;
import org.apache.maven.plugin.deploy.stubs.AttachedArtifactStub;
import org.apache.maven.plugin.deploy.stubs.DeployArtifactStub;
import org.apache.maven.plugin.deploy.stubs.ArtifactDeployerStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class DeployMojoTest
    extends AbstractMojoTestCase
{    
    private File remoteRepo;
    
    private File localRepo;
    
    private String LOCAL_REPO = getBasedir() + "/target/local-repo";
    
    private String REMOTE_REPO = getBasedir() + "/target/remote-repo";
    
    DeployArtifactStub artifact;
    
    MavenProjectStub project = new MavenProjectStub();
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        remoteRepo = new File( REMOTE_REPO );
        
        remoteRepo.mkdirs();  
        
        localRepo = new File( LOCAL_REPO );


        if( localRepo.exists() )
        {
            FileUtils.deleteDirectory( localRepo );
        }
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

        ArtifactRepository loc = ( ArtifactRepository ) getVariableValueFromObject( mojo, "localRepository" );

        artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );

        String packaging = ( String ) getVariableValueFromObject( mojo, "packaging" );
        
        assertEquals( "jar", packaging );
        
        artifact.setFile( file );        
        
        ArtifactRepositoryStub repo = ( ArtifactRepositoryStub ) getVariableValueFromObject( mojo, "deploymentRepository" );

        assertNotNull( repo );
        
        repo.setAppendToUrl( "basic-deploy-test" );
        
        assertEquals( "deploy-test", repo.getId() );
        assertEquals( "deploy-test", repo.getKey() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( "file://" + getBasedir() + "/target/remote-repo/basic-deploy-test", repo.getUrl() );
        
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

        File localRepo = new File( LOCAL_REPO, "" );
        
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
        
        remoteRepo = new File( remoteRepo, "basic-deploy-test" );
        
        files = remoteRepo.listFiles();
        
        for( int i=0; i<files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );         
    }

    public void testBasicDeployWithPackagingAsPom()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                        "target/test-classes/unit/basic-deploy-pom/plugin-config.xml" );
        
        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
        
        assertNotNull( mojo );
        
        String packaging = ( String ) getVariableValueFromObject( mojo, "packaging" );
        
        assertEquals( "pom", packaging );
        
        artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
        
        artifact.setArtifactHandlerExtension( packaging );
        
        ArtifactRepositoryStub repo = ( ArtifactRepositoryStub ) getVariableValueFromObject( mojo, "deploymentRepository" ); 
        
        repo.setAppendToUrl( "basic-deploy-pom" );
        
        mojo.execute();
        
        List expectedFiles = new ArrayList();
        List fileList = new ArrayList();
        
        expectedFiles.add( "org" );
        expectedFiles.add( "apache" );
        expectedFiles.add( "maven" );
        expectedFiles.add( "test" );
        expectedFiles.add( "maven-deploy-test" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom.md5" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom.sha1" );
  
        remoteRepo = new File( remoteRepo, "basic-deploy-pom" );
        
        File[] files = remoteRepo.listFiles();
        
        for( int i=0; i<files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );    
    }

    public void testUpdateReleaseParamSetToTrue()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-pom/plugin-config.xml" );
        
        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
        
        assertNotNull( mojo );
        
        boolean updateReleaseInfo = ( ( Boolean ) getVariableValueFromObject( mojo, "updateReleaseInfo" ) ).booleanValue();
        
        assertTrue( updateReleaseInfo );
        
        artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
        
        artifact.setFile( testPom );
        
        ArtifactRepositoryStub repo = ( ArtifactRepositoryStub ) getVariableValueFromObject( mojo, "deploymentRepository" ); 
        
        repo.setAppendToUrl( "basic-deploy-updateReleaseParam" );        
        
        mojo.execute();
        
        assertTrue( artifact.isRelease() );
    }

    public void testDeployIfArtifactFileIsNull()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-test/plugin-config.xml" );
        
        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
        
        assertNotNull( mojo );
        
        artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
        
        artifact.setFile( null );
        
        assertNull( artifact.getFile() );
        
        try
        {
            mojo.execute();

            fail( "Did not throw mojo execution exception" );
        }
        catch( MojoExecutionException e )
        {
            //expected
        }
    }
    
    public void testDeployWithAttachedArtifacts()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-with-attached-artifacts/" +
                                 "plugin-config.xml" );

        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );

        assertNotNull( mojo );

        artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
        
        File file = new File( getBasedir(),
                              "target/test-classes/unit/basic-deploy-with-attached-artifacts/target/" +
                              "deploy-test-file-1.0-SNAPSHOT.jar" );
        
        artifact.setFile( file );
        
        List attachedArtifacts = ( ArrayList ) getVariableValueFromObject( mojo, "attachedArtifacts" );

        ArtifactRepositoryStub repo = ( ArtifactRepositoryStub ) getVariableValueFromObject( mojo, "deploymentRepository" ); 
        
        repo.setAppendToUrl( "basic-deploy-with-attached-artifacts" );          
        
        mojo.execute();

        String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();

        for( Iterator iter=attachedArtifacts.iterator(); iter.hasNext(); )
        {
            AttachedArtifactStub attachedArtifact = ( AttachedArtifactStub ) iter.next();

            File deployedArtifact = new File( remoteRepo, "basic-deploy-with-attached-artifacts" + "/" +
                                               attachedArtifact.getGroupId().replace( '.', '/' ) + "/" + 
                                               attachedArtifact.getArtifactId() + "/" +
                                               attachedArtifact.getVersion() + "/" + attachedArtifact.getArtifactId() + "-" +
                                               attachedArtifact.getVersion() + "." + packaging );
            assertTrue( deployedArtifact.exists() );
        }
        
        //check the artifacts in remote repository
        List expectedFiles = new ArrayList();
        List fileList = new ArrayList();
        
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
        expectedFiles.add( "attached-artifact-test-0" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar.md5" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar.sha1" );   
        
        remoteRepo = new File( remoteRepo, "basic-deploy-with-attached-artifacts" );
        
        File[] files = remoteRepo.listFiles();
        
        for( int i=0; i<files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );               
    }
    

    public void testBasicDeployWithScpAsProtocol()
        throws Exception
    {
        String originalUserHome = System.getProperty( "user.home" );
        
        // FIX THE DAMN user.home BEFORE YOU DELETE IT!!!
        File altHome = new File( getBasedir(), "target/ssh-user-home" );
        altHome.mkdirs();
        
        System.out.println( "Testing user.home value for .ssh dir: " + altHome.getCanonicalPath() );
        
        Properties props = System.getProperties();
        props.setProperty( "user.home", altHome.getCanonicalPath() );
        
        System.setProperties( props );
        
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-deploy-scp/plugin-config.xml" );
        
        DeployMojo mojo = ( DeployMojo ) lookupMojo( "deploy", testPom );
        
        assertNotNull( mojo );
        
        ArtifactDeployerStub deployer = new ArtifactDeployerStub();
        
        setVariableValueToObject( mojo, "deployer", deployer );
        
        File file = new File( getBasedir(),
                              "target/test-classes/unit/basic-deploy-scp/target/" +
                              "deploy-test-file-1.0-SNAPSHOT.jar" );

        assertTrue( file.exists() );
        
        DeployArtifactStub artifact = ( DeployArtifactStub ) getVariableValueFromObject( mojo, "artifact" );        
        
        artifact.setFile( file );
        
        String altUserHome = System.getProperty( "user.home" );
        
        if ( altUserHome.equals( originalUserHome ) )
        {
            // this is *very* bad!
            throw new IllegalStateException( "Setting 'user.home' system property to alternate value did NOT work. Aborting test." );
        }
        
        File sshFile = new File( altUserHome, ".ssh" );
        
        System.out.println( "Testing .ssh dir: " + sshFile.getCanonicalPath() );
        
        //delete first the .ssh folder if existing before executing the mojo
        if( sshFile.exists() )
        {
            FileUtils.deleteDirectory( sshFile );
        }

        mojo.execute();
            
        assertTrue( sshFile.exists() );
        
        FileUtils.deleteDirectory( sshFile );
    }

    
    private void addFileToList( File file, List fileList )
    {
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
