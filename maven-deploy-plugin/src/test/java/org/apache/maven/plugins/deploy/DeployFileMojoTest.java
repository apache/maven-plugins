package org.apache.maven.plugins.deploy;

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
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class DeployFileMojoTest
    extends AbstractMojoTestCase
{
    private String LOCAL_REPO = getBasedir() + "/target/local-repo";
    
    private List<String> expectedFiles;

    private List<String> fileList;

    private File remoteRepo;

    MavenProjectStub projectStub = new MavenProjectStub();
    
    @Mock
    private MavenSession session;
    
    @InjectMocks
    private DeployFileMojo mojo;
    
    public void setUp()
        throws Exception
    {
        super.setUp();

        remoteRepo = new File( getBasedir(), "target/remote-repo" );

        if ( !remoteRepo.exists() )
        {
            remoteRepo.mkdirs();
        }
        
        projectStub.setAttachedArtifacts( new ArrayList<Artifact>() );
    }

    public void testDeployTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-test/plugin-config.xml" );

        AbstractDeployMojo mojo = (AbstractDeployMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );
    }

    public void testBasicDeployFile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-test/plugin-config.xml" );

        mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        MockitoAnnotations.initMocks( this );
        
        assertNotNull( mojo );
        
        ProjectBuildingRequest buildingRequest = mock ( ProjectBuildingRequest.class );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( LOCAL_REPO ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        
        setVariableValueToObject( mojo, "project", projectStub );

        String groupId = (String) getVariableValueFromObject( mojo, "groupId" );

        String artifactId = (String) getVariableValueFromObject( mojo, "artifactId" );

        String version = (String) getVariableValueFromObject( mojo, "version" );

        String packaging = (String) getVariableValueFromObject( mojo, "packaging" );

        File file = (File) getVariableValueFromObject( mojo, "file" );

        String repositoryId = (String) getVariableValueFromObject( mojo, "repositoryId" );

        String url = (String) getVariableValueFromObject( mojo, "url" );

        assertEquals( "org.apache.maven.test", groupId );

        assertEquals( "maven-deploy-file-test", artifactId );

        assertEquals( "1.0", version );

        assertEquals( "jar", packaging );

        assertTrue( file.exists() );

        assertEquals( "deploy-test", repositoryId );

        assertEquals( "file://" + getBasedir() + "/target/remote-repo/deploy-file-test", url );
        
        mojo.execute();

        //check the generated pom
        File pom = new File( remoteRepo, "deploy-file-test/" + groupId.replace( '.', '/' ) +
                                          "/" + artifactId + "/" + version + "/" + artifactId +
                                          "-" + version + ".pom" );

        assertTrue( pom.exists() );

        Model model = mojo.readModel( pom );

        assertEquals( "4.0.0", model.getModelVersion() );

        assertEquals( groupId, model.getGroupId() );

        assertEquals( artifactId, model.getArtifactId() );

        assertEquals( version, model.getVersion() );

        assertEquals( packaging, model.getPackaging() );

        assertEquals( "POM was created from deploy:deploy-file", model.getDescription() );

        //check the remote-repo
        expectedFiles = new ArrayList<String>();
        fileList = new ArrayList<String>();

        File repo = new File( remoteRepo, "deploy-file-test" );

        File[] files = repo.listFiles();

        for (File file1 : files) {
            addFileToList(file1, fileList);
        }

        expectedFiles.add( "org" );
        expectedFiles.add( "apache" );
        expectedFiles.add( "maven" );
        expectedFiles.add( "test" );
        expectedFiles.add( "maven-deploy-file-test" );
        expectedFiles.add( "1.0" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar.md5" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar.sha1" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom.md5" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom.sha1" );

        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );
    }

    public void testDeployIfClassifierIsSet()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-classifier/plugin-config.xml" );

        mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        MockitoAnnotations.initMocks( this );
        
        assertNotNull( mojo );
        
        ProjectBuildingRequest buildingRequest = mock ( ProjectBuildingRequest.class );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( LOCAL_REPO ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );

        setVariableValueToObject( mojo, "project", projectStub );

        String classifier = ( String ) getVariableValueFromObject( mojo, "classifier" );

        String groupId = ( String ) getVariableValueFromObject( mojo, "groupId" );

        String artifactId = ( String ) getVariableValueFromObject( mojo, "artifactId" );

        String version = ( String ) getVariableValueFromObject( mojo, "version" );

        assertEquals( "bin", classifier );

        mojo.execute();

        File deployedArtifact = new File( remoteRepo, "deploy-file-classifier/" + groupId.replace( '.', '/' ) +
                                          "/" + artifactId + "/" + version + "/" + artifactId +
                                          "-" + version + "-" + classifier + ".jar");

        assertTrue( deployedArtifact.exists() );

        mojo.setClassifier( "prod" );

        assertEquals( "prod", mojo.getClassifier() );

        mojo.execute();

        File prodDeployedArtifact = new File( remoteRepo, "deploy-file-classifier/" + groupId.replace( '.', '/' ) +
                                          "/" + artifactId + "/" + version + "/" + artifactId +
                                          "-" + version + "-" + mojo.getClassifier() + ".jar");

        assertTrue( prodDeployedArtifact.exists() );
    }

    public void testDeployIfArtifactIsNotJar()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-artifact-not-jar/plugin-config.xml" );

        mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        MockitoAnnotations.initMocks( this );
        
        assertNotNull( mojo );
        
        ProjectBuildingRequest buildingRequest = mock ( ProjectBuildingRequest.class );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( LOCAL_REPO ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );

        setVariableValueToObject( mojo, "project", projectStub );

        String groupId = (String) getVariableValueFromObject( mojo, "groupId" );

        String artifactId = (String) getVariableValueFromObject( mojo, "artifactId" );

        String version = (String) getVariableValueFromObject( mojo, "version" );

        assertEquals( "org.apache.maven.test", groupId );

        assertEquals( "maven-deploy-file-test", artifactId );

        assertEquals( "1.0", version );

        mojo.execute();

        File file = new File( remoteRepo, "deploy-file-artifact-not-jar/" + groupId.replace( '.', '/' ) +
                                          "/" + artifactId + "/" + version + "/" + artifactId +
                                          "-" + version + ".zip");

        assertTrue( file.exists() );
    }

    public void testDeployIfRepositoryLayoutIsLegacy()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-legacy-repository-layout/plugin-config.xml" );

        AbstractDeployMojo mojo = (AbstractDeployMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

        setVariableValueToObject( mojo, "project", projectStub );

        String repositoryLayout = (String) getVariableValueFromObject(  mojo, "repositoryLayout" );

        assertEquals( "legacy", repositoryLayout );

        try
        {
            mojo.execute();
            fail( "legacy is not supported anymore" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "Invalid repository layout: legacy", e.getMessage() );
        }

    }

    private void addFileToList( File file, List<String> fileList )
    {
        if ( !file.isDirectory() )
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
        for ( String fileName : fileList )
        {
            if ( expectedFiles.contains( fileName ) )
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

}

