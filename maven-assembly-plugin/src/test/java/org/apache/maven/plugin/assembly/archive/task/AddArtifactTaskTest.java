package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

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
        mac.expectGetArtifactHandler();
    }

    public void testShouldAddArchiveFileWithoutUnpacking()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        String outputLocation = "artifact";

        mac.expectAddFile( outputLocation );
        mac.expectIsSnapshot( false );
        mockManager.replayAll();

        AddArtifactTask task = createTask( mac.artifact );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    private AddArtifactTask createTask( Artifact artifact )
    {
        AddArtifactTask task = new AddArtifactTask( artifact, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        task.setFileNameMapping( "artifact" );

        return task;
    }

    public void testShouldAddArchiveFileWithUnpack()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        mac.expectModeChange( -1, -1, -1, -1, 1 );
        mac.expectIsSnapshot( false );

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
        int directoryMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        int fileMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        mac.expectModeChange( -1, -1, directoryMode, fileMode, 2 );
        mac.expectIsSnapshot( false );

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
        mac.expectIsSnapshot( false );

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
