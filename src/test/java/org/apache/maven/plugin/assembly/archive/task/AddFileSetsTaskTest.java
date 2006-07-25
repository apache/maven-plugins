package org.apache.maven.plugin.assembly.archive.task;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import junit.framework.TestCase;

public class AddFileSetsTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceCtl;

    private Archiver archiver;

    private MockControl archiverCtl;

    public void setUp()
    {
        mockManager = new MockManager();

        fileManager = new TestFileManager( "add-fileset.test.", "" );

        configSourceCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceCtl );

        configSource = (AssemblerConfigurationSource) configSourceCtl.getMock();

        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();
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

        File archiveBaseDir = fileManager.createTempDir();

        // ensure this exists, so the directory addition will proceed.
        File srcDir = new File( archiveBaseDir, dirname );
        srcDir.mkdirs();

        int[] modes = { -1, -1, Integer.parseInt( fs.getDirectoryMode(), 8 ), Integer.parseInt( fs.getFileMode(), 8 ) };

        setupForAddingSingleFileSet( null, null, null, true, modes, true, true );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        task.addFileSet( fs, archiver, configSource, archiveBaseDir );

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

        setupForAddingSingleFileSet( null, null, null, true, modes, true, true );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        task.addFileSet( fs, archiver, configSource, archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testAddFileSet_ShouldNotAddDirectoryWhenSourceDirNonExistent()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String dirname = "dir";

        fs.setDirectory( dirname );

        File archiveBaseDir = fileManager.createTempDir();

        int[] modes = { -1, -1, Integer.parseInt( fs.getDirectoryMode(), 8 ), Integer.parseInt( fs.getFileMode(), 8 ) };

        setupForAddingSingleFileSet( null, null, null, false, modes, false, true );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        task.addFileSet( fs, archiver, configSource, archiveBaseDir );

        mockManager.verifyAll();
    }

    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNonExistent()
        throws AssemblyFormattingException
    {
        File archiveBaseDir = fileManager.createTempDir();

        archiveBaseDir.delete();

        configSource.getArchiveBaseDirectory();
        configSourceCtl.setReturnValue( archiveBaseDir );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        try
        {
            task.execute( archiver, configSource );

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

        configSource.getArchiveBaseDirectory();
        configSourceCtl.setReturnValue( archiveBaseDir );

        mockManager.replayAll();

        AddFileSetsTask task = new AddFileSetsTask( Collections.EMPTY_LIST );

        try
        {
            task.execute( archiver, configSource );

            fail( "Should throw exception due to non-directory archiveBasedir location that was provided." );
        }
        catch ( ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        mockManager.verifyAll();
    }

    private void configureModeExpectations( int[] modes, boolean expectModeChange, boolean isDebugEnabled )
    {
        archiver.getDefaultDirectoryMode();
        archiverCtl.setReturnValue( modes[0] );

        archiver.getDefaultFileMode();
        archiverCtl.setReturnValue( modes[1] );

        if ( expectModeChange )
        {
            archiver.setDefaultDirectoryMode( modes[2] );
            archiver.setDefaultFileMode( modes[3] );
        }

        archiver.setDefaultDirectoryMode( modes[0] );
        archiver.setDefaultFileMode( modes[1] );
    }

    private void setupForAddingSingleFileSet( MavenProject project, File basedir, String finalName,
                                              boolean shouldAddDir, int[] modes, boolean expectModeChange,
                                              boolean isDebugEnabled )
    {
        // the logger sends a debug message with this info inside the addFileSet(..) method..
        if ( isDebugEnabled )
        {
            archiver.getDefaultDirectoryMode();
            archiverCtl.setReturnValue( modes[0] );

            archiver.getDefaultFileMode();
            archiverCtl.setReturnValue( modes[1] );
        }

        configSource.getProject();
        configSourceCtl.setReturnValue( project, MockControl.ONE_OR_MORE );

        configSource.getBasedir();
        configSourceCtl.setReturnValue( basedir, MockControl.ONE_OR_MORE );

        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );

        if ( shouldAddDir )
        {
            configureModeExpectations( modes, expectModeChange, isDebugEnabled );

            try
            {
                archiver.addDirectory( null, null, null, null );
                archiverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
                archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
            }
            catch ( ArchiverException e )
            {
                fail( "Should never happen." );
            }
        }

    }

}
