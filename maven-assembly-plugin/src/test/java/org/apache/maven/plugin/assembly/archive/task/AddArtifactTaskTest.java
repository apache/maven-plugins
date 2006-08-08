package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class AddArtifactTaskTest
    extends TestCase
{

    private MockManager mockManager;
    
    private MockAndControlForAddArtifactTask mac;

    public void setUp()
        throws IOException
    {
        mockManager = new MockManager();
        
        mac = new MockAndControlForAddArtifactTask( mockManager );
        mac.expectArtifactGetFile();
        mac.expectGetFinalName( "final-name" );
        mac.expectGetClassifier( null );
        mac.expectGetArtifactHandler();
    }

    public void testShouldAddArchiveFileWithoutUnpacking()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        String outputLocation = "artifact";

        mac.expectAddFile( outputLocation );
        mockManager.replayAll();

        AddArtifactTask task = createTask( mac.artifact );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    private AddArtifactTask createTask( Artifact artifact )
    {
        AddArtifactTask task = new AddArtifactTask( artifact );

        task.setFileNameMapping( "artifact" );

        return task;
    }

    public void testShouldAddArchiveFileWithUnpack()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        mac.expectModeChange( -1, -1, -1, -1, 1 );

        String outputLocation = "artifact/";

        try
        {
            mac.archiver.addArchivedFileSet( mac.artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = createTask( mac.artifact );

        task.setUnpack( true );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackAndModes()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        int directoryMode = Integer.parseInt( "777", 8 );
        int fileMode = Integer.parseInt( "777", 8 );
        
        mac.expectModeChange( -1, -1, directoryMode, fileMode, 2 );

        String outputLocation = "artifact/";

        try
        {
            mac.archiver.addArchivedFileSet( mac.artifactFile, outputLocation, null, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = createTask( mac.artifact );

        task.setUnpack( true );
        task.setDirectoryMode( "777" );
        task.setFileMode( "777" );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackIncludesAndExcludes()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        mac.expectModeChange( -1, -1, -1, -1, 1 );

        String outputLocation = "artifact/";

        String[] includes = { "**/*.txt" };
        String[] excludes = { "**/README.txt" };

        mac.expectAddArchivedFileSet( outputLocation, includes, excludes );

        mockManager.replayAll();

        AddArtifactTask task = createTask( mac.artifact );

        task.setUnpack( true );
        task.setIncludes( Arrays.asList( includes ) );
        task.setExcludes( Arrays.asList( excludes ) );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

}
