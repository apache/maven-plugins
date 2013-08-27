package org.apache.maven.plugin.assembly.archive;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collections;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;

import junit.framework.TestCase;
import org.easymock.MockControl;

public class ManifestCreationFinalizerTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "manifest-finalizer.test.", ".jar" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldDoNothingWhenArchiveConfigIsNull()
        throws ArchiverException
    {
        new ManifestCreationFinalizer( null, null, null ).finalizeArchiveCreation( null );
    }

    public void testShouldDoNothingWhenArchiverIsNotJarArchiver()
        throws ArchiverException
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        mm.replayAll();

        new ManifestCreationFinalizer( null, project, config ).finalizeArchiveCreation( macArchiver.archiver );

        mm.verifyAll();
    }

    public void testShouldAddManifestWhenArchiverIsJarArchiver()
        throws ArchiverException, IOException
    {
        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        File tempDir = fileManager.createTempDir();

        File manifestFile = fileManager.createFile( tempDir, "MANIFEST.MF", "Main-Class: Stuff\n" );

        config.setManifestFile( manifestFile );

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(
            Collections.<ArchiveFinalizer>singletonList( new ManifestCreationFinalizer( null, project, config ) ) );

        File file = fileManager.createTempFile();

        archiver.setDestFile( file );

        archiver.createArchive();

        URL resource = new URL( "jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF" );

        BufferedReader reader = new BufferedReader( new InputStreamReader( resource.openStream() ) );

        StringWriter writer = new StringWriter();

        IOUtil.copy( reader, writer );

        assertTrue( writer.toString().contains( "Main-Class: Stuff" ) );

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ( (JarURLConnection) resource.openConnection() ).getJarFile().close();
    }

    public void testShouldAddManifestEntriesWhenArchiverIsJarArchiver()
        throws ArchiverException, IOException
    {
        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        String testKey = "Test-Key";
        String testValue = "test-value";

        config.addManifestEntry( testKey, testValue );

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(
            Collections.<ArchiveFinalizer>singletonList( new ManifestCreationFinalizer( null, project, config ) ) );

        File file = fileManager.createTempFile();

        archiver.setDestFile( file );

        archiver.createArchive();

        URL resource = new URL( "jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF" );

        BufferedReader reader = new BufferedReader( new InputStreamReader( resource.openStream() ) );

        StringWriter writer = new StringWriter();

        IOUtil.copy( reader, writer );

        System.out.println( "Test Manifest:\n\n" + writer );

        assertTrue( writer.toString().contains( testKey + ": " + testValue ) );

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ( (JarURLConnection) resource.openConnection() ).getJarFile().close();
    }

    private final class MockAndControlForArchiver
    {
        Archiver archiver;

        MockControl control;

        MockAndControlForArchiver( MockManager mm )
        {
            control = MockControl.createControl( Archiver.class );
            mm.add( control );

            archiver = (Archiver) control.getMock();
        }
    }

}
