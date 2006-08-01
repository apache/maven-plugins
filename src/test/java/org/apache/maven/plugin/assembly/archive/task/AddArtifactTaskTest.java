package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class AddArtifactTaskTest
    extends TestCase
{

    private MockManager mockManager;
    
    private MockAndControlForAddArtifactTask macForAddArtifact;

    public void setUp()
        throws IOException
    {
        mockManager = new MockManager();
        
        macForAddArtifact = new MockAndControlForAddArtifactTask( mockManager );
        macForAddArtifact.expectArtifactGetFile();
    }

    public void testShouldAddArchiveFileWithoutUnpacking()
        throws ArchiveCreationException
    {
        String outputLocation = "artifact";

        macForAddArtifact.expectAddFile( outputLocation );
        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( macForAddArtifact.artifact, outputLocation );

        task.execute( macForAddArtifact.archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpack()
        throws ArchiveCreationException
    {
        macForAddArtifact.expectModeChange( -1, -1, -1, -1, 1 );

        String outputLocation = "artifact";

        try
        {
            macForAddArtifact.archiver.addArchivedFileSet( macForAddArtifact.artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( macForAddArtifact.artifact, outputLocation );

        task.setUnpack( true );

        task.execute( macForAddArtifact.archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackAndModes()
        throws ArchiveCreationException
    {
        int directoryMode = Integer.parseInt( "777", 8 );
        int fileMode = Integer.parseInt( "777", 8 );
        
        macForAddArtifact.expectModeChange( -1, -1, directoryMode, fileMode, 2 );

        String outputLocation = "artifact";

        try
        {
            macForAddArtifact.archiver.addArchivedFileSet( macForAddArtifact.artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( macForAddArtifact.artifact, outputLocation );

        task.setUnpack( true );
        task.setDirectoryMode( directoryMode );
        task.setFileMode( fileMode );

        task.execute( macForAddArtifact.archiver, null );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackIncludesAndExcludes()
        throws ArchiveCreationException
    {
        macForAddArtifact.expectModeChange( -1, -1, -1, -1, 1 );

        String outputLocation = "artifact";

        String[] includes = { "**/*.txt" };
        String[] excludes = { "**/README.txt" };

        macForAddArtifact.expectAddArchivedFileSet( outputLocation, includes, excludes );

        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( macForAddArtifact.artifact, outputLocation );

        task.setUnpack( true );
        task.setIncludes( Arrays.asList( includes ) );
        task.setExcludes( Arrays.asList( excludes ) );

        task.execute( macForAddArtifact.archiver, null );

        mockManager.verifyAll();
    }

}
