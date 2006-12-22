package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class FileSetAssemblyPhaseTest
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();
    
    private TestFileManager fileManager = new TestFileManager( "file-set-assembly.test.", "" );
    
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }
    
    public void testShouldNotFailWhenNoFileSetsSpecified()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        Assembly assembly = new Assembly();
        
        assembly.setId( "test" );
        
        MockAndControlForLogger macLogger = new MockAndControlForLogger();
        MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mockManager, fileManager );
        
        mockManager.replayAll();
        
        createPhase( macLogger ).execute( assembly, macTask.archiver, macTask.configSource );
        
        mockManager.verifyAll();
    }

    public void testShouldAddOneFileSet()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        
        FileSet fs = new FileSet();
        fs.setOutputDirectory( "/out" );
        fs.setDirectory( "/input" );
        fs.setFileMode( "777" );
        fs.setDirectoryMode( "777" );
        
        assembly.addFileSet( fs );

        MockAndControlForLogger macLogger = new MockAndControlForLogger();
        MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mockManager, fileManager );
        
        macTask.expectGetArchiveBaseDirectory();
        
        File basedir = fileManager.createTempDir();
        
        MavenProject project = new MavenProject( new Model() );
        
        macLogger.expectDebug( true, true );
        
        int dirMode = Integer.parseInt( "777", 8 );
        int fileMode = Integer.parseInt( "777", 8 );
        
        int[] modes = { -1, -1, dirMode, fileMode };
        
        macTask.expectAdditionOfSingleFileSet( project, basedir, "final-name", false, modes, 1, true );

        mockManager.replayAll();

        createPhase( macLogger ).execute( assembly, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    private FileSetAssemblyPhase createPhase( MockAndControlForLogger macLogger )
    {
        FileSetAssemblyPhase phase = new FileSetAssemblyPhase();
        
        phase.enableLogging( macLogger.logger );
        
        return phase;
    }
    
    private final class MockAndControlForLogger
    {
        Logger logger;

        MockControl control;

        MockAndControlForLogger()
        {
            control = MockControl.createControl( Logger.class );
            mockManager.add( control );

            logger = ( Logger ) control.getMock();
        }

        public void expectDebug( boolean debugCheck, boolean debugEnabled )
        {
            if ( debugCheck )
            {
                logger.isDebugEnabled();
                control.setReturnValue( debugEnabled, MockControl.ONE_OR_MORE );
            }
            
            logger.debug( null );
            control.setMatcher( MockControl.ALWAYS_MATCHER );
            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }
    }

}
