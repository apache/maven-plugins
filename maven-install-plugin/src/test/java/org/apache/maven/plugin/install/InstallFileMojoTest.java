package org.apache.maven.plugin.install;

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
import java.io.Reader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.utils.ReaderFactory;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class InstallFileMojoTest
    extends AbstractMojoTestCase
{
    private String groupId;

    private String legacyGroupId;

    private String artifactId;

    private String version;

    private String packaging;

    private String classifier;

    private File file;

    private final String LOCAL_REPO = "target/local-repo/";

    public void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( new File( getBasedir() + "/" + LOCAL_REPO ) );
        
//        LegacySupport legacySupport = lookup( LegacySupport.class );
//        RepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
//        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
//        legacySupport.setSession( new MavenSession( getContainer(), repositorySession, executionRequest, null ) );
    }

    public void testInstallFileTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/install-file-basic-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assertNotNull( mojo );
    }

    public void testBasicInstallFile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/install-file-basic-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        mojo.execute();
        
        File pomFile = (File) getVariableValueFromObject( mojo, "pomFile" );
        org.codehaus.plexus.util.FileUtils.forceDelete( pomFile );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + packaging );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallFileWithClassifier()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-classifier/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        assertNotNull( classifier );

        mojo.execute();

        File pomFile = (File) getVariableValueFromObject( mojo, "pomFile" );
        org.codehaus.plexus.util.FileUtils.forceDelete( pomFile );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "-" + classifier + "." + packaging );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallFileWithGeneratePom()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-test-generatePom/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        mojo.execute();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + packaging );

        assertTrue( (Boolean) getVariableValueFromObject( mojo, "generatePom" ) );

        assertTrue( installedArtifact.exists() );

        File installedPom = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + "pom" );

        Model model;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( installedPom );
            model = new MavenXpp3Reader().read( reader );
            reader.close();
            reader = null;
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertEquals( "4.0.0", model.getModelVersion() );

        assertEquals( (String) getVariableValueFromObject( mojo, "groupId" ), model.getGroupId() );

        assertEquals( artifactId, model.getArtifactId() );

        assertEquals( version, model.getVersion() );

        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallFileWithPomFile()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-pomFile-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        mojo.execute();

        File pomFile = (File) getVariableValueFromObject( mojo, "pomFile" );

        assertTrue( pomFile.exists() );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + packaging );

        assertTrue( installedArtifact.exists() );

        File installedPom = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + "pom" );

        assertTrue( installedPom.exists() );
        
        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallFileWithPomAsPackaging()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/install-file-with-pom-as-packaging/" + "plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        assertTrue( file.exists() );

        assertEquals( "pom", packaging );

        mojo.execute();

        File installedPom = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + "pom" );

        assertTrue( installedPom.exists() );

        assertEquals( 4, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallFileWithChecksum()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-checksum/" + "plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
        
        setVariableValueToObject( mojo, "session", createMavenSession() );

        assignValuesForParameter( mojo );

        boolean createChecksum = (Boolean) getVariableValueFromObject( mojo, "createChecksum" );

        assertTrue( createChecksum );

        mojo.execute();

        //get the actual checksum of the artifact
        mojo.digester.calculate( file );
        String actualMd5Sum = mojo.digester.getMd5();
        String actualSha1Sum = mojo.digester.getSha1();

        String localPath = getBasedir() + "/" + LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version;

        File installedArtifact = new File( localPath + "." + "jar" );

        File md5 = new File( localPath + ".jar.md5" );
        File sha1 = new File( localPath + ".jar.sha1" );

        assertTrue( md5.exists() );
        assertTrue( sha1.exists() );

        String generatedMd5 = FileUtils.fileRead( md5, "UTF-8" );
        String generatedSha1 = FileUtils.fileRead( sha1, "UTF-8" );

        assertEquals( actualMd5Sum, generatedMd5 );
        assertEquals( actualSha1Sum, generatedSha1 );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 9, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    private void assignValuesForParameter( Object obj )
        throws Exception
    {
        this.groupId = dotToSlashReplacer( (String) getVariableValueFromObject( obj, "groupId" ) );

        this.legacyGroupId = (String) getVariableValueFromObject( obj, "groupId" );

        this.artifactId = (String) getVariableValueFromObject( obj, "artifactId" );

        this.version = (String) getVariableValueFromObject( obj, "version" );

        this.packaging = (String) getVariableValueFromObject( obj, "packaging" );

        this.classifier = (String) getVariableValueFromObject( obj, "classifier" );

        this.file = (File) getVariableValueFromObject( obj, "file" );
    }

    private String dotToSlashReplacer( String parameter )
    {
        return parameter.replace( '.', '/' );
    }
    
    private MavenSession createMavenSession()
    {
        MavenSession session = mock( MavenSession.class );
        DefaultRepositorySystemSession repositorySession  = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new EnhancedLocalRepositoryManager( new File( LOCAL_REPO )     ) );
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession( repositorySession );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        return session;
    }
}
