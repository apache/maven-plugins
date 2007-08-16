package org.apache.maven.plugin.assembly.archive.task;

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
import java.util.Collections;

import junit.framework.TestCase;

public class AddFileSetsTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private MockAndControlForAddFileSetsTask macTask;

    public void setUp()
    {
        mockManager = new MockManager();

        fileManager = new TestFileManager( "add-fileset.test.", "" );

        macTask = new MockAndControlForAddFileSetsTask( mockManager, fileManager );
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testGetFileSetDirectory_ShouldReturnAbsoluteSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File dir = fileManager.createTempDir();

        FileSet fs = new FileSet();

        fs.setDirectory( dir.getAbsolutePath() );

        File result = new AddFileSetsTask( Collections.EMPTY_LIST ).getFileSetDirectory( fs, null, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnBasedir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File dir = fileManager.createTempDir();

        FileSet fs = new FileSet();

        File result = new AddFileSetsTask( Collections.EMPTY_LIST ).getFileSetDirectory( fs, dir, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnDirFromBasedirAndSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File dir = fileManager.createTempDir();

        String srcPath = "source";

        File srcDir = new File( dir, srcPath );

        FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        File result = new AddFileSetsTask( Collections.EMPTY_LIST ).getFileSetDirectory( fs, dir, null );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testGetFileSetDirectory_ShouldReturnDirFromArchiveBasedirAndSourceDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File dir = fileManager.createTempDir();

        String srcPath = "source";

        File srcDir = new File( dir, srcPath );

        FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        File result = new AddFileSetsTask( Collections.EMPTY_LIST ).getFileSetDirectory( fs, null, dir );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    public void testAddFileSet_ShouldAddDirectory()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String dirname = "dir";

        fs.setDirectory( dirname );
        fs.setOutputDirectory( "dir2" );

        // ensure this exists, so the directory addition will proceed.
        File srcDir = new File( macTask.archiveBaseDir, dirname );
        srcDir.mkdirs();

        int[] modes = { -1, -1, Integer.parseInt( fs.getDirectoryMode(), 8 ), Integer.parseInt( fs.getFileMode(), 8 ) };

        macTask.expectAdditionOfSingleFileSet( null, null, null, true, modes, 2, true, false );

        macTask.expectGetProject( null );

        MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, macTask.archiver, macTask.configSource, macTask.archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testAddFileSet_ShouldAddDirectoryUsingSourceDirNameForDestDir()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String dirname = "dir";

        fs.setDirectory( dirname );

        File archiveBaseDir = fileManager.createTempDir();

        // ensure this exists, so the directory addition will proceed.
        File srcDir = new File( archiveBaseDir, dirname );
        srcDir.mkdirs();

        int[] modes = { -1, -1, Integer.parseInt( fs.getDirectoryMode(), 8 ), Integer.parseInt( fs.getFileMode(), 8 ) };

        macTask.expectAdditionOfSingleFileSet( null, null, null, true, modes, 2, true, false );

        macTask.expectGetProject( null );

        MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, macTask.archiver, macTask.configSource, archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testAddFileSet_ShouldNotAddDirectoryWhenSourceDirNonExistent()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String dirname = "dir";

        fs.setDirectory( dirname );

        File archiveBaseDir = fileManager.createTempDir();

        macTask.expectGetFinalName( "finalName" );

        macTask.expectGetProject( null );

        macTask.archiver.getDefaultDirectoryMode();
        macTask.archiverCtl.setReturnValue( -1 );
        macTask.archiver.getDefaultFileMode();
        macTask.archiverCtl.setReturnValue( -1 );

        MavenProject project = new MavenProject( new Model() );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

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

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        try
        {
            task.execute( macTask.archiver, macTask.configSource );

            fail( "Should throw exception due to non-existent archiveBasedir location that was provided." );
        }
        catch ( ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        mockManager.verifyAll();
    }

    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNotADirectory()
        throws AssemblyFormattingException, IOException
    {
        File archiveBaseDir = fileManager.createTempFile();

        macTask.archiveBaseDir = archiveBaseDir;
        macTask.expectGetArchiveBaseDirectory();

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        try
        {
            task.execute( macTask.archiver, macTask.configSource );

            fail( "Should throw exception due to non-directory archiveBasedir location that was provided." );
        }
        catch ( ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        mockManager.verifyAll();
    }

}
