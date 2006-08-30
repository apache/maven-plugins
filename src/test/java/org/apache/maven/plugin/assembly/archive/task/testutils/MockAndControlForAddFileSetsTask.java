package org.apache.maven.plugin.assembly.archive.task.testutils;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

import java.io.File;

import junit.framework.Assert;

public class MockAndControlForAddFileSetsTask
{

    public AssemblerConfigurationSource configSource;

    public MockControl configSourceCtl;

    public Archiver archiver;

    public MockControl archiverCtl;

    public TestFileManager fileManager;

    public File archiveBaseDir;

    public MockAndControlForAddFileSetsTask( MockManager mockManager, TestFileManager fileManager )
    {
        this.fileManager = fileManager;

        configSourceCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceCtl );

        configSource = ( AssemblerConfigurationSource ) configSourceCtl.getMock();

        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = ( Archiver ) archiverCtl.getMock();

        archiveBaseDir = fileManager.createTempDir();
    }

    public void expectGetArchiveBaseDirectory()
    {
        configSource.getArchiveBaseDirectory();
        configSourceCtl.setReturnValue( archiveBaseDir, MockControl.ONE_OR_MORE );
    }

    public void expectGetBasedir( File basedir )
    {
        configSource.getBasedir();
        configSourceCtl.setReturnValue( basedir, MockControl.ONE_OR_MORE );
    }

    public void expectModeChanges( int[] modes, int modeChangeCount )
    {
        archiver.getDefaultDirectoryMode();
        archiverCtl.setReturnValue( modes[0] );

        archiver.getDefaultFileMode();
        archiverCtl.setReturnValue( modes[1] );

        if ( modeChangeCount > 1 )
        {
            for ( int i = 1; i < modeChangeCount; i++ )
            {
                archiver.setDefaultDirectoryMode( modes[2] );
                archiver.setDefaultFileMode( modes[3] );
            }
        }

        archiver.setDefaultDirectoryMode( modes[0] );
        archiver.setDefaultFileMode( modes[1] );
    }

    public void expectAdditionOfSingleFileSet( MavenProject project, File basedir, String finalName,
                                               boolean shouldAddDir, int[] modes, int modeChangeCount,
                                               boolean isDebugEnabled )
    {
        expectAdditionOfSingleFileSet( project, basedir, finalName, shouldAddDir, modes, modeChangeCount,
                                       isDebugEnabled, true );

    }

    public void expectAdditionOfSingleFileSet( MavenProject project, File basedir, String finalName,
                                               boolean shouldAddDir, int[] modes, int modeChangeCount,
                                               boolean isDebugEnabled, boolean isProjectUsed )
    {
        // the logger sends a debug message with this info inside the addFileSet(..) method..
        if ( isDebugEnabled )
        {
            archiver.getDefaultDirectoryMode();
            archiverCtl.setReturnValue( modes[0] );

            archiver.getDefaultFileMode();
            archiverCtl.setReturnValue( modes[1] );
        }

        if ( isProjectUsed )
        {
            configSource.getProject();
            configSourceCtl.setReturnValue( project, MockControl.ONE_OR_MORE );
        }

        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );

        if ( shouldAddDir )
        {
            expectModeChanges( modes, modeChangeCount );

            try
            {
                archiver.addDirectory( null, null, null, null );
                archiverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
                archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
            }
            catch ( ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
        }

    }

    public void expectGetProject( MavenProject project )
    {
        configSource.getProject();
        configSourceCtl.setReturnValue( project, MockControl.ONE_OR_MORE );
    }

    public void expectGetFinalName( String finalName )
    {
        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );
    }

}
