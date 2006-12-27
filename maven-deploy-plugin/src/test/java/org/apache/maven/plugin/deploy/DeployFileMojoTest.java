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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class DeployFileMojoTest
    extends AbstractMojoTestCase
{
    private List expectedFiles;

    private List fileList;

    private File localRepo;

    private File remoteRepo;

    public void setUp()
        throws Exception
    {
        super.setUp();

        remoteRepo = new File( getBasedir(), "target/remote-repo" );

        if ( !remoteRepo.exists() )
        {
            remoteRepo.mkdirs();
        }
    }

    public void testDeployTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-test/plugin-config.xml" );

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );
    }

    public void testBasicDeployFile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-test/plugin-config.xml" );

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

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
        expectedFiles = new ArrayList();
        fileList = new ArrayList();

        File repo = new File( remoteRepo, "deploy-file-test" );

        File[] files = repo.listFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            addFileToList( files[i], fileList );
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

    public void testDeployIfPomFileParamIsSet()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-pom-file/plugin-config.xml" );

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

        File pomFile = ( File ) getVariableValueFromObject( mojo, "pomFile" );

        assertNotNull( pomFile );

        mojo.execute();

        assertTrue( pomFile.exists() );
    }

    public void testDeployIfClassifierIsSet()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/deploy-file-classifier/plugin-config.xml" );

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

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

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

        String groupId = (String) getVariableValueFromObject( mojo, "groupId" );

        String artifactId = (String) getVariableValueFromObject( mojo, "artifactId" );

        String version = (String) getVariableValueFromObject( mojo, "version" );

        String packaging = (String) getVariableValueFromObject( mojo, "packaging" );

        assertEquals( "org.apache.maven.test", groupId );

        assertEquals( "maven-deploy-file-test", artifactId );

        assertEquals( "1.0", version );

        assertEquals( "zip", packaging );

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

        DeployFileMojo mojo = (DeployFileMojo) lookupMojo( "deploy-file", testPom );

        assertNotNull( mojo );

        String repositoryLayout = (String) getVariableValueFromObject(  mojo, "repositoryLayout" );

        String groupId = (String) getVariableValueFromObject( mojo, "groupId" );

        String artifactId = (String) getVariableValueFromObject( mojo, "artifactId" );

        String version = (String) getVariableValueFromObject( mojo, "version" );

        assertEquals( "legacy", repositoryLayout );

        mojo.execute();

        File artifactFile = new File( remoteRepo, "deploy-file-legacy-repository-layout/" + groupId + "/jars/" + artifactId + "-" + version + ".jar" );

        assertTrue( artifactFile.exists() );

        //check the remote-repo
        expectedFiles = new ArrayList();
        fileList = new ArrayList();

        File repo = new File( remoteRepo, "deploy-file-legacy-repository-layout" );

        File[] files = repo.listFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            addFileToList( files[i], fileList );
        }

        expectedFiles.add( "org.apache.maven.test" );
        expectedFiles.add( "jars" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar.md5" );
        expectedFiles.add( "maven-deploy-file-test-1.0.jar.sha1" );
        expectedFiles.add( "poms" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom.md5" );
        expectedFiles.add( "maven-deploy-file-test-1.0.pom.sha1" );
        expectedFiles.add( "maven-metadata.xml" );
        expectedFiles.add( "maven-metadata.xml.md5" );
        expectedFiles.add( "maven-metadata.xml.sha1" );

        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );
    }

    private void addFileToList( File file, List fileList )
    {
        if ( !file.isDirectory() )
        {
            fileList.add( file.getName() );
        }
        else
        {
            fileList.add( file.getName() );

            File[] files = file.listFiles();

            for ( int i = 0; i < files.length; i++ )
            {
                addFileToList( files[i], fileList );
            }
        }
    }

    private int getSizeOfExpectedFiles( List fileList, List expectedFiles )
    {
        for ( Iterator iter = fileList.iterator(); iter.hasNext(); )
        {
            String fileName = (String) iter.next();

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

