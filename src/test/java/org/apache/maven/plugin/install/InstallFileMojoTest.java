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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;

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
    }

    public void testInstallFileTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/install-file-basic-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );
    }

    public void testBasicInstallFile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/install-file-basic-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        mojo.execute();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + packaging );

        assertTrue( installedArtifact.exists() );
    }

    public void testLayoutInstallFile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/install-file-layout-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        //test harness doesn't like expressions.
        mojo.setLocalRepositoryId( "id" );

        mojo.setLocalRepositoryPath( new File( getBasedir(), LOCAL_REPO ) );

        mojo.execute();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + legacyGroupId + "/" + "jars" + "/" + artifactId + "-"
            + version + "." + packaging );

        assertTrue( installedArtifact.exists() );
    }
    
    public void testInstallFileWithClassifier()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-classifier/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        assertNotNull( classifier );

        mojo.execute();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "-" + classifier + "." + packaging );

        assertTrue( installedArtifact.exists() );
    }

    public void testInstallFileWithGeneratePom()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-test-generatePom/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        mojo.execute();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + packaging );

        assertTrue( ( (Boolean) getVariableValueFromObject( mojo, "generatePom" ) ).booleanValue() );

        assertTrue( installedArtifact.exists() );

        File installedPom = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + "pom" );

        MavenXpp3Reader reader = new MavenXpp3Reader();

        Model model = reader.read( new FileReader( installedPom ) );

        assertEquals( "4.0.0", model.getModelVersion() );

        assertEquals( (String) getVariableValueFromObject( mojo, "groupId" ), model.getGroupId() );

        assertEquals( artifactId, model.getArtifactId() );

        assertEquals( version, model.getVersion() );
    }

    public void testInstallFileWithPomFile()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-pomFile-test/plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

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
    }

    public void testInstallFileWithPomAsPackaging()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/install-file-with-pom-as-packaging/" + "plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        assertTrue( file.exists() );

        assertEquals( "pom", packaging );

        mojo.execute();

        File installedPom = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version + "." + "pom" );

        assertTrue( installedPom.exists() );
    }

    public void testInstallFileWithChecksum()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "target/test-classes/unit/install-file-with-checksum/" + "plugin-config.xml" );

        InstallFileMojo mojo = (InstallFileMojo) lookupMojo( "install-file", testPom );

        assertNotNull( mojo );

        assignValuesForParameter( mojo );

        ArtifactRepository localRepo = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );

        boolean createChecksum = ( (Boolean) getVariableValueFromObject( mojo, "createChecksum" ) ).booleanValue();

        assertTrue( createChecksum );

        mojo.execute();

        //get the actual checksum of the artifact
        String actualMd5Sum = mojo.getChecksum( file, "MD5" );
        String actualSha1Sum = mojo.getChecksum( file, "SHA-1" );

        String localPath = getBasedir() + "/" + LOCAL_REPO + groupId + "/" + artifactId + "/" + version + "/" +
            artifactId + "-" + version;

        File installedArtifact = new File( localPath + "." + "jar" );

        File md5 = new File( localPath + ".jar.md5" );
        File sha1 = new File( localPath + ".jar.sha1" );

        assertTrue( md5.exists() );
        assertTrue( sha1.exists() );

        String generatedMd5 = FileUtils.fileRead( md5 );
        String generatedSha1 = FileUtils.fileRead( sha1 );

        assertEquals( actualMd5Sum, generatedMd5 );
        assertEquals( actualSha1Sum, generatedSha1 );

        assertTrue( installedArtifact.exists() );
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
}
