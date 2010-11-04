package org.apache.maven.plugin.dependency.utils;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
/**
 * 
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;

/**
 * @author brianf
 * 
 */
public class TestDependencyUtil
    extends TestCase
{
    List artifacts = new ArrayList();

    Log log = new SilentLog();

    File outputFolder;

    Artifact snap;

    Artifact release;

    Artifact sources;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactHandler ah = new DefaultArtifactHandlerStub( "jar", null );
        VersionRange vr = VersionRange.createFromVersion( "1.1" );
        release = new DefaultArtifact( "test", "one", vr, Artifact.SCOPE_COMPILE, "jar", "sources", ah, false );
        artifacts.add( release );

        ah = new DefaultArtifactHandlerStub( "war", null );
        vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        snap = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "war", null, ah, false );
        artifacts.add( snap );

        ah = new DefaultArtifactHandlerStub( "war", null );
        vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        sources = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "sources", "sources", ah, false );

        // pick random output location
        Random a = new Random();
        outputFolder = new File( "target/copy" + a.nextLong() + "/" );
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    protected void tearDown()
    {

    }

    public void testDirectoryName()
        throws MojoExecutionException
    {
        File folder = new File( "target/a" );
        final Artifact artifact = (Artifact) artifacts.get( 0 );
        File name = DependencyUtil.getFormattedOutputDirectory( false, false, false, false, false, folder, artifact );
        // object is the same.
        assertEquals( folder, name );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, false, true, false, folder, artifact );
        String expectedResult = folder.getAbsolutePath() + File.separatorChar + "test" + File.separatorChar + "one"
            + File.separatorChar + "1.1";
        assertTrue( expectedResult.equalsIgnoreCase( name.getAbsolutePath() ) );

        name = DependencyUtil.getFormattedOutputDirectory( false,  true, false, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "jars";
        assertTrue( expectedResult.equalsIgnoreCase( name.getAbsolutePath() ) );

        name = DependencyUtil.getFormattedOutputDirectory( true,  false, false, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "compile";
        assertEquals( expectedResult,  name.getAbsolutePath() );
        assertTrue( expectedResult.equalsIgnoreCase( name.getAbsolutePath() ) );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "one-sources-1.1-jar";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, true, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "one-sources-jar";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, true, true, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "jars" + File.separatorChar
            + "one-sources-1.1-jar";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, true, true, false, true, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "jars" + File.separatorChar
            + "one-sources-jar";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( true, false, true, false, true, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "compile" + File.separatorChar
            + "one-sources-jar";
        assertEquals( expectedResult, name.getAbsolutePath() );
    }

    public void testDirectoryName2()
        throws MojoExecutionException
    {
        File folder = new File( "target/a" );
        final Artifact artifact = (Artifact) artifacts.get( 1 );
        File name = DependencyUtil.getFormattedOutputDirectory( false, false, false, false, false, folder, artifact );
        // object is the same.
        assertEquals( folder, name );

        name = DependencyUtil.getFormattedOutputDirectory( false, true, false, false, false, folder, artifact );
        String expectedResult = folder.getAbsolutePath() + File.separatorChar + "wars";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, false, true, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "test" + File.separatorChar + "two"
            + File.separatorChar + "1.1-SNAPSHOT";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "two-1.1-SNAPSHOT-war";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, true, true, false, false, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "wars" + File.separatorChar
            + "two-1.1-SNAPSHOT-war";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, true, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "two-war";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, true, true, false, true, folder, artifact );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "wars" + File.separatorChar + "two-war";
        assertEquals( expectedResult, name.getAbsolutePath() );
    }

    public void testDirectoryNameSources()
        throws MojoExecutionException
    {
        File folder = new File( "target/a" );
        File name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, true, folder, sources );
        String expectedResult = folder.getAbsolutePath() + File.separatorChar + "two-sources";
        assertEquals( expectedResult, name.getAbsolutePath() );

        name = DependencyUtil.getFormattedOutputDirectory( false, false, true, false, false, folder, sources );
        expectedResult = folder.getAbsolutePath() + File.separatorChar + "two-sources-1.1-SNAPSHOT-sources";
        assertEquals( expectedResult, name.getAbsolutePath() );
    }

    public void testFileName()
        throws MojoExecutionException
    {
        Artifact artifact = (Artifact) artifacts.get( 0 );

        String name = DependencyUtil.getFormattedFileName( artifact, false );
        String expectedResult = "one-1.1-sources.jar";
        assertEquals( expectedResult, name );
        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "one-sources.jar";
        assertEquals( expectedResult, name );
    }

    public void testTestJar()
    {
        ArtifactHandler ah = new DefaultArtifactHandlerStub( "test-jar", null );
        VersionRange vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        Artifact artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "test-jar", null, ah,
                                                 false );

        String name = DependencyUtil.getFormattedFileName( artifact, false );
        String expectedResult = "two-1.1-SNAPSHOT.jar";
        assertEquals( expectedResult, name );

    }

    public void testFileNameClassifier()
        throws MojoExecutionException
    {
        ArtifactHandler ah = new DefaultArtifactHandlerStub( "jar", "sources" );
        VersionRange vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        Artifact artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "jar", "sources", ah,
                                                 false );

        String name = DependencyUtil.getFormattedFileName( artifact, false );
        String expectedResult = "two-1.1-SNAPSHOT-sources.jar";
        assertEquals( expectedResult, name );

        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "two-sources.jar";
        assertEquals( expectedResult, name );

        ah = new DefaultArtifactHandlerStub( "war", null );
        artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "war", "", ah, false );
        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "two.war";
        assertEquals( expectedResult, name );

    }

    public void testFileNameClassifierWithFile()
        throws MojoExecutionException
    {
        // specifically testing the default operation that getFormattedFileName
        // returns
        // the actual name of the file if available unless remove version is
        // set.
        ArtifactHandler ah = new DefaultArtifactHandlerStub( "war", "sources" );
        VersionRange vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        Artifact artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "war", "sources", ah,
                                                 false );
        File file = new File( "/target", "test-file-name.jar" );
        artifact.setFile( file );

        String name = DependencyUtil.getFormattedFileName( artifact, false );
        String expectedResult = "two-1.1-SNAPSHOT-sources.war";
        assertEquals( expectedResult, name );

        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "two-sources.war";
        assertEquals( expectedResult, name );

        artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "war", "", ah, false );
        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "two.war";
        assertEquals( expectedResult, name );

        // test that we pickup the correct extension in the file name if set.
        ah = new DefaultArtifactHandlerStub( "jar", null );
        artifact = new DefaultArtifact( "test", "two", vr, Artifact.SCOPE_PROVIDED, "war", "", ah, false );
        name = DependencyUtil.getFormattedFileName( artifact, true );
        expectedResult = "two.jar";
        assertEquals( expectedResult, name );

    }
    
    public void testTokenizer()
    {
        
        String [] tokens = DependencyUtil.tokenizer( " \r\n a, \t \n \r b \t \n \r" );
        assertEquals( 2, tokens.length );
        assertEquals( "a", tokens[0] );
        assertEquals( "b", tokens[1] );
        
        tokens = DependencyUtil.tokenizer( null );
        assertEquals( 0, tokens.length );
        
        tokens = DependencyUtil.tokenizer( "  " );
        assertEquals( 0, tokens.length );
        
    }
}
