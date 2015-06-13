package org.apache.maven.plugins.assembly.archive.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;
import org.apache.maven.shared.repository.model.RepositoryInfo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import java.io.File;
import java.io.IOException;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class RepositoryAssemblyPhaseTest
    extends TestCase
{

    private final TestFileManager fileManager = new TestFileManager( "repository-phase.test.", "" );

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testExecute_ShouldNotIncludeRepositoryIfNonSpecifiedInAssembly()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );

        final Assembly assembly = new Assembly();

        assembly.setId( "test" );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ).execute( assembly,
                                                                                                             macArchiver.archiver,
                                                                                                             macCS
                                                                                                                 .configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldIncludeOneRepository()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );
        macCS.expectGetProject( new MavenProject( new Model() ) );
        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();

        final Assembly assembly = new Assembly();

        assembly.setId( "test" );

        final Repository repo = new Repository();

        repo.setOutputDirectory( "out" );
        repo.setDirectoryMode( "777" );
        repo.setFileMode( "777" );

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        final File outDir = new File( tempRoot, "out" );

        macArchiver.expectModeChange( -1, -1, mode, mode, true );
        macArchiver.expectAddDirectory( outDir, "out/", null, null );

        macRepo.expectAssemble();

        assembly.addRepository( repo );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ).execute( assembly,
                                                                                                             macArchiver.archiver,
                                                                                                             macCS
                                                                                                                 .configSource );

        mm.verifyAll();
    }

    private RepositoryAssemblyPhase createPhase( final RepositoryAssembler repositoryAssembler, final Logger logger )
    {
        final RepositoryAssemblyPhase phase = new RepositoryAssemblyPhase( repositoryAssembler );
        phase.enableLogging( logger );

        return phase;
    }

    private final class MockAndControlForArchiver
    {
        final Archiver archiver;

        public MockAndControlForArchiver( final EasyMockSupport mockManager )
        {

            archiver = mockManager.createMock( Archiver.class );
        }

        public void expectAddDirectory( final File outDir, final String location, final String[] includes,
                                        final String[] excludes )
        {
            try
            {
                final DefaultFileSet fs = new DefaultFileSet();
                fs.setDirectory( outDir );
                fs.setPrefix( location );
                fs.setIncludes( includes );
                fs.setExcludes( excludes );

                archiver.addFileSet( (FileSet) anyObject() );
            }
            catch ( final ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }

            EasyMock.expectLastCall().atLeastOnce();
        }

        void expectModeChange( final int defaultDirMode, final int defaultFileMode, final int dirMode,
                               final int fileMode, final boolean expectTwoSets )
        {
            expect( archiver.getOverrideDirectoryMode() ).andReturn( defaultDirMode );

            expect( archiver.getOverrideFileMode() ).andReturn( defaultFileMode );

            if ( expectTwoSets )
            {
                archiver.setDirectoryMode( dirMode );
                archiver.setFileMode( fileMode );
            }

            archiver.setDirectoryMode( defaultDirMode );
            archiver.setFileMode( defaultFileMode );
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
        final AssemblerConfigurationSource configSource;

        public MockAndControlForConfigSource( final EasyMockSupport mockManager )
        {
            configSource = mockManager.createMock( AssemblerConfigurationSource.class );

            expect( configSource.getMavenSession() ).andReturn( null ).anyTimes();
        }

        public void expectGetProject( final MavenProject project )
        {
            expect( configSource.getProject() ).andReturn( project ).atLeastOnce();
        }

        public void expectGetFinalName( final String finalName )
        {
            expect( configSource.getFinalName() ).andReturn( finalName ).atLeastOnce();
        }

        public void expectInterpolators()
        {
            expect( configSource.getCommandLinePropsInterpolator() ).andReturn(
                FixedStringSearchInterpolator.empty() ).anyTimes();
            expect( configSource.getEnvInterpolator() ).andReturn( FixedStringSearchInterpolator.empty() ).anyTimes();
            expect( configSource.getMainProjectInterpolator() ).andReturn(
                FixedStringSearchInterpolator.empty() ).anyTimes();
        }

        public void expectGetTemporaryRootDirectory( final File tempRoot )
        {
            expect( configSource.getTemporaryRootDirectory() ).andReturn( tempRoot ).atLeastOnce();
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
        final RepositoryAssembler repositoryAssembler;

        MockAndControlForRepositoryAssembler( final EasyMockSupport mockManager )
        {
            repositoryAssembler = mockManager.createMock( RepositoryAssembler.class );
        }

        public void expectAssemble()
        {
            try
            {
                repositoryAssembler.buildRemoteRepository( (File) anyObject(), (RepositoryInfo) anyObject(),
                                                           (RepositoryBuilderConfigSource) anyObject() );
                EasyMock.expectLastCall().atLeastOnce();
            }
            catch ( final RepositoryAssemblyException e )
            {
                Assert.fail( "Should never happen" );
            }

        }
    }

}
