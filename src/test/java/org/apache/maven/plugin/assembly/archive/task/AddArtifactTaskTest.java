package org.apache.maven.plugin.assembly.archive.task;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

public class AddArtifactTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private Artifact artifact;

    private MockControl artifactCtl;

    private File artifactFile;

    private Archiver archiver;

    private MockControl archiverCtl;

    public void setUp()
        throws IOException
    {
        mockManager = new MockManager();

        artifactCtl = MockControl.createControl( Artifact.class );
        mockManager.add( artifactCtl );

        artifact = (Artifact) artifactCtl.getMock();

        artifactFile = File.createTempFile( "add-artifact-task.test.", ".jar" );

        artifact.getFile();
        artifactCtl.setReturnValue( artifactFile );

        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();
    }

    public void testShouldAddArchiveFileWithoutUnpacking()
        throws ArchiveCreationException
    {
        String outputLocation = "artifact";

        try
        {
            archiver.addFile( artifactFile, outputLocation );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( artifact, outputLocation );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpack()
        throws ArchiveCreationException
    {
        configureModeExpectations( -1, -1, -1, -1, false );

        String outputLocation = "artifact";

        try
        {
            archiver.addArchivedFileSet( artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( artifact, outputLocation );

        task.setUnpack( true );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackAndModes()
        throws ArchiveCreationException
    {
        int directoryMode = Integer.parseInt( "777", 8 );
        int fileMode = Integer.parseInt( "777", 8 );
        
        configureModeExpectations( -1, -1, directoryMode, fileMode, true );

        String outputLocation = "artifact";

        try
        {
            archiver.addArchivedFileSet( artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( artifact, outputLocation );

        task.setUnpack( true );
        task.setDirectoryMode( directoryMode );
        task.setFileMode( fileMode );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackIncludesAndExcludes()
        throws ArchiveCreationException
    {
        configureModeExpectations( -1, -1, -1, -1, false );

        String outputLocation = "artifact";

        String[] includes = { "**/*.txt" };
        String[] excludes = { "**/README.txt" };

        try
        {
            archiver.addArchivedFileSet( artifactFile, outputLocation, includes, excludes );
            archiverCtl.setMatcher( MockControl.ARRAY_MATCHER );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( artifact, outputLocation );

        task.setUnpack( true );
        task.setIncludes( Arrays.asList( includes ) );
        task.setExcludes( Arrays.asList( excludes ) );

        task.execute( archiver, null );

        mockManager.verifyAll();
    }

    private void configureModeExpectations( int defaultDirMode, int defaultFileMode, int dirMode, int fileMode,
                                            boolean expectTwoSets )
    {
        archiver.getDefaultDirectoryMode();
        archiverCtl.setReturnValue( defaultDirMode );

        archiver.getDefaultFileMode();
        archiverCtl.setReturnValue( defaultFileMode );

        if ( expectTwoSets )
        {
            archiver.setDefaultDirectoryMode( dirMode );
            archiver.setDefaultFileMode( fileMode );
        }

        archiver.setDefaultDirectoryMode( defaultDirMode );
        archiver.setDefaultFileMode( defaultFileMode );
    }
}
