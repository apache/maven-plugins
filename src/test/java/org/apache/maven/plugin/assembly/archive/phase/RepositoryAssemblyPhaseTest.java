package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;

public class RepositoryAssemblyPhaseTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "repository-phase.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testExecute_ShouldNotIncludeRepositoryIfNonSpecifiedInAssembly()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );

        Assembly assembly = new Assembly();

        assembly.setId( "test" );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, macLogger.logger ).execute( assembly, macArchiver.archiver,
                                                                              macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldIncludeOneRepository()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );
        macCS.expectGetProject( new MavenProject( new Model() ) );
        macCS.expectGetFinalName( "final-name" );

        Assembly assembly = new Assembly();

        assembly.setId( "test" );
        
        Repository repo = new Repository();
        
        repo.setOutputDirectory( "out" );
        repo.setDirectoryMode( "777" );
        repo.setFileMode( "777" );
        
        int mode = Integer.parseInt( "777", 8 );
        
        File outDir = new File( tempRoot, "out" );
        
        macArchiver.expectModeChange( -1, -1, mode, mode, true );
        macArchiver.expectAddDirectory( outDir, "out/", null, FileUtils.getDefaultExcludes() );
        
        macRepo.expectAssemble( outDir, repo, macCS.configSource );
        
        assembly.addRepository( repo );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, macLogger.logger ).execute( assembly, macArchiver.archiver,
                                                                              macCS.configSource );

        mm.verifyAll();
    }

    private RepositoryAssemblyPhase createPhase( RepositoryAssembler repositoryAssembler, Logger logger )
    {
        RepositoryAssemblyPhase phase = new RepositoryAssemblyPhase( repositoryAssembler );
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
        
        public void expectAddDirectory( File outDir, String location, String[] includes, String[] excludes )
        {
            try
            {
                archiver.addDirectory( outDir, location, includes, excludes );
            }
            catch ( ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
            
            control.setMatcher( MockControl.ARRAY_MATCHER );
            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }

        void expectModeChange( int defaultDirMode, int defaultFileMode, int dirMode, int fileMode,
                                                boolean expectTwoSets )
        {
            archiver.getDefaultDirectoryMode();
            control.setReturnValue( defaultDirMode );

            archiver.getDefaultFileMode();
            control.setReturnValue( defaultFileMode );

            if ( expectTwoSets )
            {
                archiver.setDefaultDirectoryMode( dirMode );
                archiver.setDefaultFileMode( fileMode );
            }

            archiver.setDefaultDirectoryMode( defaultDirMode );
            archiver.setDefaultFileMode( defaultFileMode );
        }


        // public void expectAddFile( File file, String outputLocation, int fileMode )
        // {
        // try
        // {
        // archiver.addFile( file, outputLocation, fileMode );
        // }
        // catch ( ArchiverException e )
        // {
        // Assert.fail( "Should never happen." );
        // }
        // }
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

        //
        // public void expectGetBasedir( File basedir )
        // {
        // configSource.getBasedir();
        // control.setReturnValue( basedir, MockControl.ONE_OR_MORE );
        // }
    }

    private final class MockAndControlForRepositoryAssembler
    {
        RepositoryAssembler repositoryAssembler;

        MockControl control;

        MockAndControlForRepositoryAssembler( MockManager mockManager )
        {
            control = MockControl.createControl( RepositoryAssembler.class );
            mockManager.add( control );

            repositoryAssembler = ( RepositoryAssembler ) control.getMock();
        }

        public void expectAssemble( File dir, Repository repo, AssemblerConfigurationSource configSource )
        {
            try
            {
                repositoryAssembler.buildRemoteRepository( dir, new RepoInfoWrapper( repo ), new RepoBuilderConfigSourceWrapper( configSource ) );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
            }
            catch ( RepositoryAssemblyException e )
            {
                Assert.fail( "Should never happen" );
            }
            
            control.setVoidCallable( MockControl.ONE_OR_MORE );
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
