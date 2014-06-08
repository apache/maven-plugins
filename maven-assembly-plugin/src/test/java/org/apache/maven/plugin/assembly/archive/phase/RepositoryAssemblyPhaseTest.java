package org.apache.maven.plugin.assembly.archive.phase;

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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;

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
        final MockManager mm = new MockManager();

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
                                                                                                             macCS.configSource,
                                                                                                             new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testExecute_ShouldIncludeOneRepository()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );
        macCS.expectGetProject( new MavenProject( new Model() ) );
        macCS.expectGetFinalName( "final-name" );

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

        macRepo.expectAssemble( outDir, repo, macCS.configSource );

        assembly.addRepository( repo );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ).execute( assembly,
                                                                                                             macArchiver.archiver,
                                                                                                             macCS.configSource,
                                                                                                             new DefaultAssemblyContext() );

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
        Archiver archiver;

        MockControl control;

        public MockAndControlForArchiver( final MockManager mockManager )
        {
            control = MockControl.createControl( Archiver.class );
            mockManager.add( control );

            archiver = (Archiver) control.getMock();
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

                archiver.addFileSet( fs );
            }
            catch ( final ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }

            control.setMatcher( new AbstractMatcher()
            {

                @Override
                protected boolean argumentMatches( final Object expected, final Object actual )
                {
                    final FileSet e = (FileSet) expected;
                    final FileSet a = (FileSet) actual;

                    if ( !eq( e.getDirectory(), a.getDirectory() ) )
                    {
                        System.out.println( "FileSet directory expected: " + e.getDirectory() + "\nActual: "
                                        + a.getDirectory() );

                        return false;
                    }

                    if ( !eq( e.getPrefix(), a.getPrefix() ) )
                    {
                        System.out.println( "FileSet prefix expected: " + e.getPrefix() + "\nActual: " + a.getPrefix() );

                        return false;
                    }

                    if ( !areq( e.getIncludes(), a.getIncludes() ) )
                    {
                        System.out.println( "FileSet includes expected: " + arToStr( e.getIncludes() ) + "\nActual: "
                                        + arToStr( a.getIncludes() ) );

                        return false;
                    }

                    if ( !areq( e.getExcludes(), a.getExcludes() ) )
                    {
                        System.out.println( "FileSet excludes expected: " + arToStr( e.getExcludes() ) + "\nActual: "
                                        + arToStr( a.getExcludes() ) );

                        return false;
                    }

                    return true;
                }

                @Override
                protected String argumentToString( final Object argument )
                {
                    final FileSet a = (FileSet) argument;

                    return argument == null ? "Null FileSet" : "FileSet:[dir=" + a.getDirectory() + ", prefix: "
                                    + a.getPrefix() + "\nincludes:\n" + arToStr( a.getIncludes() ) + "\nexcludes:\n"
                                    + arToStr( a.getExcludes() ) + "]";
                }

                private String arToStr( final String[] array )
                {
                    return array == null ? "-EMPTY-" : StringUtils.join( array, "\n\t" );
                }

                private boolean areq( final String[] first, final String[] second )
                {
                    if ( ( first == null || first.length == 0 ) && ( second == null || second.length == 0 ) )
                    {
                        return true;
                    }
                    else if ( first == null && second != null )
                    {
                        return false;
                    }
                    else if ( first != null && second == null )
                    {
                        return false;
                    }
                    else
                    {
                        return Arrays.equals( first, second );
                    }
                }

                private boolean eq( final Object first, final Object second )
                {
                    if ( first == null && second == null )
                    {
                        return true;
                    }
                    else if ( first == null && second != null )
                    {
                        return false;
                    }
                    else if ( first != null && second == null )
                    {
                        return false;
                    }
                    else
                    {
                        return first.equals( second );
                    }
                }

            } );

            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }

        void expectModeChange( final int defaultDirMode, final int defaultFileMode, final int dirMode,
                               final int fileMode, final boolean expectTwoSets )
        {
            archiver.getOverrideDirectoryMode();
            control.setReturnValue( defaultDirMode );

            archiver.getOverrideFileMode();
            control.setReturnValue( defaultFileMode );

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
        AssemblerConfigurationSource configSource;

        MockControl control;

        public MockAndControlForConfigSource( final MockManager mockManager )
        {
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = (AssemblerConfigurationSource) control.getMock();

            configSource.getMavenSession();
            control.setReturnValue( null, MockControl.ZERO_OR_MORE );
        }

        public void expectGetProject( final MavenProject project )
        {
            configSource.getProject();
            control.setReturnValue( project, MockControl.ONE_OR_MORE );
        }

        public void expectGetFinalName( final String finalName )
        {
            configSource.getFinalName();
            control.setReturnValue( finalName, MockControl.ONE_OR_MORE );
        }

        public void expectGetTemporaryRootDirectory( final File tempRoot )
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

        MockAndControlForRepositoryAssembler( final MockManager mockManager )
        {
            control = MockControl.createControl( RepositoryAssembler.class );
            mockManager.add( control );

            repositoryAssembler = (RepositoryAssembler) control.getMock();
        }

        public void expectAssemble( final File dir, final Repository repo,
                                    final AssemblerConfigurationSource configSource )
        {
            try
            {
                repositoryAssembler.buildRemoteRepository( dir, new RepoInfoWrapper( repo ),
                                                           new RepoBuilderConfigSourceWrapper( configSource ) );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
            }
            catch ( final RepositoryAssemblyException e )
            {
                Assert.fail( "Should never happen" );
            }

            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }
    }

}
