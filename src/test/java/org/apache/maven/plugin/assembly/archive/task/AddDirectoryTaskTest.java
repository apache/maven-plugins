package org.apache.maven.plugin.assembly.archive.task;

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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

public class AddDirectoryTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private Archiver archiver;

    private MockControl archiverControl;

    public void setUp()
    {
        fileManager = new TestFileManager( "ArchiveAssemblyUtils.test.", "" );

        mockManager = new MockManager();

        archiverControl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverControl );

        archiver = (Archiver) archiverControl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testAddDirectory_ShouldNotAddDirectoryIfNonExistent()
        throws ArchiveCreationException
    {
        File dir = new File( System.getProperty( "java.io.tmpdir" ), "non-existent." + System.currentTimeMillis() );

        configureModeExpectations( -1, -1, -1, -1, false );

        mockManager.replayAll();

        AddDirectoryTask task = new AddDirectoryTask( dir );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testAddDirectory_ShouldAddDirectory()
        throws ArchiveCreationException
    {
        File dir = fileManager.createTempDir();

        try
        {
            archiver.addDirectory( null, null, null, null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        configureModeExpectations( -1, -1, -1, -1, false );

        mockManager.replayAll();

        AddDirectoryTask task = new AddDirectoryTask( dir );

        task.setOutputDirectory( "dir" );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testAddDirectory_ShouldAddDirectoryWithDirMode()
        throws ArchiveCreationException
    {
        File dir = fileManager.createTempDir();

        try
        {
            archiver.addDirectory( null, null, null, null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        int dirMode = Integer.parseInt( "777", 8 );
        int fileMode = Integer.parseInt( "777", 8 );

        configureModeExpectations( -1, -1, dirMode, fileMode, true );

        mockManager.replayAll();

        AddDirectoryTask task = new AddDirectoryTask( dir );

        task.setDirectoryMode( dirMode );
        task.setFileMode( fileMode );
        task.setOutputDirectory( "dir" );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testAddDirectory_ShouldAddDirectoryWithIncludesAndExcludes()
        throws ArchiveCreationException
    {
        File dir = fileManager.createTempDir();

        try
        {
            archiver.addDirectory( null, null, null, null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        configureModeExpectations( -1, -1, -1, -1, false );

        mockManager.replayAll();

        AddDirectoryTask task = new AddDirectoryTask( dir );

        task.setIncludes( Collections.singletonList( "**/*.txt" ) );
        task.setExcludes( Collections.singletonList( "**/README.txt" ) );
        task.setOutputDirectory( "dir" );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    private void configureModeExpectations( int defaultDirMode, int defaultFileMode, int dirMode, int fileMode,
                                            boolean expectTwoSets )
    {
        archiver.getDefaultDirectoryMode();
        archiverControl.setReturnValue( defaultDirMode );

        archiver.getDefaultFileMode();
        archiverControl.setReturnValue( defaultFileMode );

        if ( expectTwoSets )
        {
            archiver.setDefaultDirectoryMode( dirMode );
            archiver.setDefaultFileMode( fileMode );
        }

        archiver.setDefaultDirectoryMode( defaultDirMode );
        archiver.setDefaultFileMode( defaultFileMode );
    }

}
