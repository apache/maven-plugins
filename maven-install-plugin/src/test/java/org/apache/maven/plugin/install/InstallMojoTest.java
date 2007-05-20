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

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.install.stubs.AttachedArtifactStub0;
import org.apache.maven.plugin.install.stubs.InstallArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */

public class InstallMojoTest
    extends AbstractMojoTestCase
{

    InstallArtifactStub artifact;

    private final String LOCAL_REPO = "target/local-repo/";

    public void setUp()
        throws Exception
    {
        super.setUp();

        System.out.println( ">>>Cleaning local repo " + getBasedir() + "/" + LOCAL_REPO + "..." );

        FileUtils.deleteDirectory( new File( getBasedir() + "/" + LOCAL_REPO ) );
    }

    public void testInstallTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );
    }

    public void testBasicInstall()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-install-test/target/" +
            "maven-install-test-1.0-SNAPSHOT.jar" );

        artifact = (InstallArtifactStub) getVariableValueFromObject( mojo, "artifact" );

        artifact.setFile( file );

        mojo.execute();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + packaging );

        assertTrue( installedArtifact.exists() );
    }

    public void testBasicInstallWithAttachedArtifacts()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test-with-attached-artifacts/" +
            "plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        List attachedArtifacts = (ArrayList) getVariableValueFromObject( mojo, "attachedArtifacts" );

        mojo.execute();

        String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();

        String groupId = "";

        for ( Iterator iter = attachedArtifacts.iterator(); iter.hasNext(); )
        {
            AttachedArtifactStub0 attachedArtifact = (AttachedArtifactStub0) iter.next();

            groupId = dotToSlashReplacer( attachedArtifact.getGroupId() );

            File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" +
                attachedArtifact.getArtifactId() + "/" + attachedArtifact.getVersion() + "/" +
                attachedArtifact.getArtifactId() + "-" + attachedArtifact.getVersion() + "." + packaging );

            assertTrue( installedArtifact.exists() );
        }
    }

    public void testUpdateReleaseParamSetToTrue()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/configured-install-test/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/configured-install-test/target/" +
            "maven-install-test-1.0-SNAPSHOT.jar" );

        artifact = (InstallArtifactStub) getVariableValueFromObject( mojo, "artifact" );

        artifact.setFile( file );

        mojo.execute();

        assertTrue( artifact.isRelease() );
    }

    public void testInstallIfArtifactFileIsNull()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        artifact = (InstallArtifactStub) getVariableValueFromObject( mojo, "artifact" );

        artifact.setFile( null );

        assertNull( artifact.getFile() );

        try
        {
            mojo.execute();

            fail( "Did not throw mojo execution exception" );
        }
        catch ( MojoExecutionException e )
        {
            //expected
        }
    }

    public void testInstallIfPackagingIsPom()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-install-test-packaging-pom/" + "plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        String packaging = (String) getVariableValueFromObject( mojo, "packaging" );

        assertEquals( "pom", packaging );

        artifact = (InstallArtifactStub) getVariableValueFromObject( mojo, "artifact" );

        mojo.execute();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + "jar" );

        assertTrue( installedArtifact.exists() );
    }

    public void testBasicInstallAndCreateChecksumIsTrue()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-checksum/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-install-checksum/" + "maven-test-jar.jar" );

        artifact = (InstallArtifactStub) getVariableValueFromObject( mojo, "artifact" );

        boolean createChecksum = ( (Boolean) getVariableValueFromObject( mojo, "createChecksum" ) ).booleanValue();

        assertTrue( createChecksum );

        artifact.setFile( file );

        mojo.execute();

        ArtifactMetadata metadata = null;
        for ( Iterator iter = artifact.getMetadataList().iterator(); iter.hasNext(); )
        {
            metadata = (ArtifactMetadata) iter.next();
            if ( metadata.getRemoteFilename().endsWith( "pom" ) )
            {
                break;
            }
        }

        ArtifactRepository localRepo = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );

        File pom = new File( localRepo.getBasedir(), localRepo.pathOfLocalRepositoryMetadata( metadata, localRepo ) );

        assertTrue( pom.exists() );

        //get the actual checksum of the pom
        String actualPomMd5Sum = mojo.getChecksum( pom, "MD5" );
        String actualPomSha1Sum = mojo.getChecksum( pom, "SHA-1" );

        //get the actual checksum of the artifact
        String actualMd5Sum = mojo.getChecksum( file, "MD5" );
        String actualSha1Sum = mojo.getChecksum( file, "SHA-1" );

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();

        String localPath = getBasedir() + "/" + LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion();

        File installedArtifact = new File( localPath + "." + packaging );

        File pomMd5 = new File( localPath + ".pom.md5" );
        File pomSha1 = new File( localPath + ".pom.sha1" );

        File md5 = new File( localPath + "." + packaging + ".md5" );
        File sha1 = new File( localPath + "." + packaging + ".sha1" );

        assertTrue( pomMd5.exists() );
        assertTrue( pomSha1.exists() );
        assertTrue( md5.exists() );
        assertTrue( sha1.exists() );

        String generatedMd5 = FileUtils.fileRead( md5 );
        String generatedSha1 = FileUtils.fileRead( sha1 );
        String generatedPomMd5 = FileUtils.fileRead( pomMd5 );
        String generatedPomSha1 = FileUtils.fileRead( pomSha1 );

        assertEquals( actualMd5Sum, generatedMd5 );
        assertEquals( actualSha1Sum, generatedSha1 );
        assertEquals( actualPomMd5Sum, generatedPomMd5 );
        assertEquals( actualPomSha1Sum, generatedPomSha1 );

        assertTrue( installedArtifact.exists() );
    }

    private String dotToSlashReplacer( String parameter )
    {
        return parameter.replace( '.', '/' );
    }
}
