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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.deploy.stubs.ArtifactDeployerStub;
import org.apache.maven.plugin.deploy.stubs.ArtifactRepositoryStub;
import org.apache.maven.plugin.deploy.stubs.AttachedArtifactStub;
import org.apache.maven.plugin.deploy.stubs.DeployArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
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

        if ( localRepo.exists() )
        {
            FileUtils.deleteDirectory( localRepo );
        }

        if ( remoteRepo.exists() )
        {
            FileUtils.deleteDirectory( remoteRepo );
        }
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

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        
        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        
        artifact = ( DeployArtifactStub ) project.getArtifact();

        String packaging = project.getPackaging();
        
        assertEquals( "jar", packaging );
        
        artifact.setFile( file );        
        
        ArtifactRepositoryStub repo = getRepoStub( mojo );

        assertNotNull( repo );
        
        repo.setAppendToUrl( "basic-deploy-test" );
        
        assertEquals( "deploy-test", repo.getId() );
        assertEquals( "deploy-test", repo.getKey() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( "file://" + getBasedir() + "/target/remote-repo/basic-deploy-test", repo.getUrl() );
        
        mojo.execute();

        //check the artifact in local repository
        List<String> expectedFiles = new ArrayList<String>();
        List<String> fileList = new ArrayList<String>();
        
        expectedFiles.add( "org" );
        expectedFiles.add( "apache" );
        expectedFiles.add( "maven" );
        expectedFiles.add( "test" );
        expectedFiles.add( "maven-deploy-test" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata-deploy-test.xml" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-deploy-test-1.0-SNAPSHOT.pom" );
        // as we are in SNAPSHOT the file is here twice
        expectedFiles.add( "maven-metadata-deploy-test.xml" );
        
        File localRepo = new File( LOCAL_REPO, "" );
        
        File[] files = localRepo.listFiles();

        for (File file2 : files) {
            addFileToList(file2, fileList);
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );        
                  
        //check the artifact in remote repository
        expectedFiles = new ArrayList<String>();
        fileList = new ArrayList<String>();
        
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
        // as we are in SNAPSHOT the file is here twice
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        
        remoteRepo = new File( remoteRepo, "basic-deploy-test" );
        
        files = remoteRepo.listFiles();

        for (File file1 : files) {
            addFileToList(file1, fileList);
        }
        
        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );         
    }

    public void testSkippingDeploy()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-deploy-test/plugin-config.xml" );

        DeployMojo mojo = (DeployMojo) lookupMojo( "deploy", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-deploy-test/target/"
            + "deploy-test-file-1.0-SNAPSHOT.jar" );

        assertTrue( file.exists() );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        
        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();

        String packaging = project.getPackaging();

        assertEquals( "jar", packaging );

        artifact.setFile( file );

        ArtifactRepositoryStub repo = getRepoStub( mojo );

        assertNotNull( repo );

        repo.setAppendToUrl( "basic-deploy-test" );

        assertEquals( "deploy-test", repo.getId() );
        assertEquals( "deploy-test", repo.getKey() );
        assertEquals( "file", repo.getProtocol() );
        assertEquals( "file://" + getBasedir() + "/target/remote-repo/basic-deploy-test", repo.getUrl() );

        setVariableValueToObject( mojo, "skip", Boolean.TRUE );
        
        mojo.execute();

        File localRepo = new File( LOCAL_REPO, "" );

        File[] files = localRepo.listFiles();

        assertNull( files );
       
        remoteRepo = new File( remoteRepo, "basic-deploy-test" );

        files = remoteRepo.listFiles();

        assertNull( files );
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
        
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();
        
        artifact.setArtifactHandlerExtension( packaging );
        
        ArtifactRepositoryStub repo = getRepoStub( mojo );
        
        repo.setAppendToUrl( "basic-deploy-pom" );
        
        mojo.execute();
        
        List<String> expectedFiles = new ArrayList<String>();
        List<String> fileList = new ArrayList<String>();
        
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
        // as we are in SNAPSHOT the file is here twice
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" ); 
        remoteRepo = new File( remoteRepo, "basic-deploy-pom" );
        
        File[] files = remoteRepo.listFiles();

        for (File file : files) {
            addFileToList(file, fileList);
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
        
        boolean updateReleaseInfo = (Boolean) getVariableValueFromObject(mojo, "updateReleaseInfo");
        
        assertTrue( updateReleaseInfo );
        
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();
        
        artifact.setFile( testPom );
        
        ArtifactRepositoryStub repo = getRepoStub( mojo );
        
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
        
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();
        
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

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();
        
        File file = new File( getBasedir(),
                              "target/test-classes/unit/basic-deploy-with-attached-artifacts/target/" +
                              "deploy-test-file-1.0-SNAPSHOT.jar" );
        
        artifact.setFile( file );
        
        
        @SuppressWarnings( "unchecked" )
        List<AttachedArtifactStub> attachedArtifacts = project.getAttachedArtifacts();

        ArtifactRepositoryStub repo = getRepoStub( mojo );
        
        repo.setAppendToUrl( "basic-deploy-with-attached-artifacts" );          
        
        mojo.execute();

        String packaging = project.getPackaging();

        for( AttachedArtifactStub attachedArtifact : attachedArtifacts )
        {
            File deployedArtifact = new File( remoteRepo, "basic-deploy-with-attached-artifacts" + "/" +
                                               attachedArtifact.getGroupId().replace( '.', '/' ) + "/" + 
                                               attachedArtifact.getArtifactId() + "/" +
                                               attachedArtifact.getVersion() + "/" + attachedArtifact.getArtifactId() + "-" +
                                               attachedArtifact.getVersion() + "." + packaging );
            assertTrue( deployedArtifact.exists() );
        }
        
        //check the artifacts in remote repository
        List<String> expectedFiles = new ArrayList<String>();
        List<String> fileList = new ArrayList<String>();
        
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
        // as we are in SNAPSHOT the file is here twice
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );         
        expectedFiles.add( "attached-artifact-test-0" );
        expectedFiles.add( "1.0-SNAPSHOT" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar.md5" );
        expectedFiles.add( "attached-artifact-test-0-1.0-SNAPSHOT.jar.sha1" );
        // as we are in SNAPSHOT the file is here twice
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );         
        
        remoteRepo = new File( remoteRepo, "basic-deploy-with-attached-artifacts" );
        
        File[] files = remoteRepo.listFiles();

        for (File file1 : files) {
            addFileToList(file1, fileList);
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
        
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );

        artifact = (DeployArtifactStub) project.getArtifact();
        
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
    
    public void testAltSnapshotDeploymentRepository() throws Exception
    {
        DeployMojo mojo = new DeployMojo();
        
        ArtifactRepositoryLayout repositoryLayout = mock( ArtifactRepositoryLayout.class );
        setVariableValueToObject( mojo, "repositoryLayouts", Collections.singletonMap( "default", repositoryLayout ) );

        ArtifactRepositoryFactory repositoryFactory = mock( ArtifactRepositoryFactory.class );
        ArtifactRepository repository = mock( ArtifactRepository.class);
        when( repositoryFactory.createDeploymentArtifactRepository( "altSnapshotDeploymentRepository", "http://localhost", repositoryLayout, true ) ).thenReturn( repository );
        setVariableValueToObject( mojo, "repositoryFactory", repositoryFactory );
        
        project.setVersion( "1.0-SNAPSHOT" );
        
        assertEquals( repository, mojo.getDeploymentRepository( project, null, null, "altSnapshotDeploymentRepository::default::http://localhost" ) );
    }

    public void testAltReleaseDeploymentRepository() throws Exception
    {
        DeployMojo mojo = new DeployMojo();
        
        ArtifactRepositoryLayout repositoryLayout = mock( ArtifactRepositoryLayout.class );
        setVariableValueToObject( mojo, "repositoryLayouts", Collections.singletonMap( "default", repositoryLayout ) );

        ArtifactRepositoryFactory repositoryFactory = mock( ArtifactRepositoryFactory.class );
        ArtifactRepository repository = mock( ArtifactRepository.class);
        when( repositoryFactory.createDeploymentArtifactRepository( "altReleaseDeploymentRepository", "http://localhost", repositoryLayout, true ) ).thenReturn( repository );
        setVariableValueToObject( mojo, "repositoryFactory", repositoryFactory );
        
        project.setVersion( "1.0" );
        
        assertEquals( repository, mojo.getDeploymentRepository( project, null, "altReleaseDeploymentRepository::default::http://localhost", null ) );
    }

    
    private void addFileToList( File file, List<String> fileList )
    {
        if( !file.isDirectory() )
        {
            fileList.add( file.getName() );
        }
        else
        {
            fileList.add( file.getName() );

            File[] files = file.listFiles();

            for (File file1 : files) {
                addFileToList(file1, fileList);
            }
        }
    }    
    
    private int getSizeOfExpectedFiles( List<String> fileList, List<String> expectedFiles )
    {
        for( String fileName : fileList )
        {
            if( !expectedFiles.remove( fileName ) )
            {
                fail( fileName + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
    }    

    private ArtifactRepositoryStub getRepoStub( Object mojo )
        throws Exception
    {
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        return (ArtifactRepositoryStub) project.getDistributionManagementArtifactRepository();
    }

}
