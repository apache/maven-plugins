package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;

public class FileItemAssemblyPhaseTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "file-item-phase.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testExecute_ShouldAddNothingWhenNoFileItemsArePresent()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File basedir = fileManager.createTempDir();

        macCS.expectGetBasedir( basedir );

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, null, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldAddAbsoluteFileNoFilterNoLineEndingConversion()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File basedir = fileManager.createTempDir();

        File file = fileManager.createFile( basedir, "file.txt", "This is a test file." );

        macCS.expectGetBasedir( basedir );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        FileItem fi = new FileItem();
        fi.setSource( file.getAbsolutePath() );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        macArchiver.expectAddFile( file, "file.txt", Integer.parseInt( "777", 8 ) );

        assembly.addFile( fi );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldAddRelativeFileNoFilterNoLineEndingConversion()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File basedir = fileManager.createTempDir();

        File file = fileManager.createFile( basedir, "file.txt", "This is a test file." );

        macCS.expectGetBasedir( basedir );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        FileItem fi = new FileItem();
        fi.setSource( "file.txt" );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        macArchiver.expectAddFile( file, "file.txt", Integer.parseInt( "777", 8 ) );

        assembly.addFile( fi );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    private FileItemAssemblyPhase createPhase( Logger logger )
    {
        FileItemAssemblyPhase phase = new FileItemAssemblyPhase();
        phase.enableLogging( logger );

        return phase;
    }

    private final class MockAndControlForArchiver
    {
        Archiver archiver;

        MockControl control;

        public MockAndControlForArchiver( MockManager mockManager )
        {
            control = MockControl.createControl( Archiver.class );
            mockManager.add( control );

            archiver = ( Archiver ) control.getMock();
        }

        public void expectAddFile( File file, String outputLocation, int fileMode )
        {
            try
            {
                archiver.addFile( file, outputLocation, fileMode );
            }
            catch ( ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
        }
    }

    private final class MockAndControlForConfigSource
    {
        AssemblerConfigurationSource configSource;

        MockControl control;

        public MockAndControlForConfigSource( MockManager mockManager )
        {
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = ( AssemblerConfigurationSource ) control.getMock();
        }

        public void expectGetProject( MavenProject project )
        {
            configSource.getProject();
            control.setReturnValue( project, MockControl.ONE_OR_MORE );
        }

        public void expectGetFinalName( String finalName )
        {
            configSource.getFinalName();
            control.setReturnValue( finalName, MockControl.ONE_OR_MORE );
        }

        public void expectGetTemporaryRootDirectory( File tempRoot )
        {
            configSource.getTemporaryRootDirectory();
            control.setReturnValue( tempRoot, MockControl.ONE_OR_MORE );
        }

        public void expectGetBasedir( File basedir )
        {
            configSource.getBasedir();
            control.setReturnValue( basedir, MockControl.ONE_OR_MORE );
        }
    }

    private final class MockAndControlForLogger
    {
        Logger logger;

        MockControl control;

        public MockAndControlForLogger( MockManager mockManager )
        {
            control = MockControl.createControl( Logger.class );
            mockManager.add( control );

            logger = ( Logger ) control.getMock();
        }
    }

}
