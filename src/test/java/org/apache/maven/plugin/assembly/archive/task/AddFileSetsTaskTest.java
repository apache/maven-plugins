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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

public class AddFileSetsTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private MockAndControlForAddFileSetsTask macTask;

    @Override
    public void setUp()
    {
        mockManager = new MockManager();

        fileManager = new TestFileManager( "add-fileset.test.", "" );

        macTask = new MockAndControlForAddFileSetsTask( mockManager, fileManager );
    }

    @Override
    public void tearDown() throws IOException
    {
        fileManager.cleanUp();
    }

    public void testGetFileSetDirectory_ShouldReturnAbsoluteSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final File dir = fileManager.createTempDir();

        final FileSet fs = new FileSet();

        fs.setDirectory( dir.getAbsolutePath() );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, null, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnBasedir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final File dir = fileManager.createTempDir();

        final FileSet fs = new FileSet();

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, dir, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnDirFromBasedirAndSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final File dir = fileManager.createTempDir();

        final String srcPath = "source";

        final File srcDir = new File( dir, srcPath );

        final FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, dir, null );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnDirFromArchiveBasedirAndSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final File dir = fileManager.createTempDir();

        final String srcPath = "source";

        final File srcDir = new File( dir, srcPath );

        final FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, null, dir );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testAddFileSet_ShouldAddDirectory() throws ArchiveCreationException, AssemblyFormattingException
    {
        final FileSet fs = new FileSet();

        final String dirname = "dir";

        fs.setDirectory( dirname );
        fs.setOutputDirectory( "dir2" );

        // ensure this exists, so the directory addition will proceed.
        final File srcDir = new File( macTask.archiveBaseDir, dirname );
        srcDir.mkdirs();

        final int[] modes = { -1, -1, -1, -1 };

        macTask.expectAdditionOfSingleFileSet( null, null, null, true, modes, 1, true, false );

        macTask.expectGetProject( null );

        final MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, macTask.archiver, macTask.configSource, macTask.archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testAddFileSet_ShouldAddDirectoryUsingSourceDirNameForDestDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final FileSet fs = new FileSet();

        final String dirname = "dir";

        fs.setDirectory( dirname );

        final File archiveBaseDir = fileManager.createTempDir();

        // ensure this exists, so the directory addition will proceed.
        final File srcDir = new File( archiveBaseDir, dirname );
        srcDir.mkdirs();

        final int[] modes = { -1, -1, -1, -1 };

        macTask.expectAdditionOfSingleFileSet( null, null, null, true, modes, 1, true, false );

        macTask.expectGetProject( null );

        final MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, macTask.archiver, macTask.configSource, archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testAddFileSet_ShouldNotAddDirectoryWhenSourceDirNonExistent()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final FileSet fs = new FileSet();

        final String dirname = "dir";

        fs.setDirectory( dirname );

        final File archiveBaseDir = fileManager.createTempDir();

        macTask.expectGetFinalName( "finalName" );

        macTask.expectGetProject( null );

        macTask.archiver.getOverrideDirectoryMode();
        macTask.archiverCtl.setReturnValue( -1 );
        macTask.archiver.getOverrideFileMode();
        macTask.archiverCtl.setReturnValue( -1 );

        final MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, macTask.archiver, macTask.configSource, archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNonExistent()
        throws AssemblyFormattingException
    {
        macTask.archiveBaseDir.delete();

        macTask.expectGetArchiveBaseDirectory();

        mockManager.replayAll();

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        try
        {
            task.execute( macTask.archiver, macTask.configSource );

            fail( "Should throw exception due to non-existent archiveBasedir location that was provided." );
        }
        catch ( final ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        mockManager.verifyAll();
    }

    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNotADirectory()
        throws AssemblyFormattingException, IOException
    {
        final File archiveBaseDir = fileManager.createTempFile();

        macTask.archiveBaseDir = archiveBaseDir;
        macTask.expectGetArchiveBaseDirectory();

        mockManager.replayAll();

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        try
        {
            task.execute( macTask.archiver, macTask.configSource );

            fail( "Should throw exception due to non-directory archiveBasedir location that was provided." );
        }
        catch ( final ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        mockManager.verifyAll();
    }

}
