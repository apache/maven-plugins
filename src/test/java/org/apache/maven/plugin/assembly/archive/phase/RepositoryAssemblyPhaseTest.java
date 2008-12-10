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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
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

    private TestFileManager fileManager = new TestFileManager( "repository-phase.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testExecute_ShouldNotIncludeRepositoryIfNonSpecifiedInAssembly()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        MockAndControlForDependencyResolver macResolver = new MockAndControlForDependencyResolver( mm );
        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );

        Assembly assembly = new Assembly();

        assembly.setId( "test" );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, macResolver.dependencyResolver,
                     new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ).execute( assembly, macArchiver.archiver,
                                                                                macCS.configSource,
                                                                                new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testExecute_ShouldIncludeOneRepository()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForRepositoryAssembler macRepo = new MockAndControlForRepositoryAssembler( mm );
        MockAndControlForDependencyResolver macResolver = new MockAndControlForDependencyResolver( mm );
        MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        File tempRoot = fileManager.createTempDir();

        macCS.expectGetTemporaryRootDirectory( tempRoot );
        macCS.expectGetProject( new MavenProject( new Model() ) );
        macCS.expectGetFinalName( "final-name" );
        macCS.expectGetLocalRepository();
        macCS.expectGetRemoteRepositories( Collections.EMPTY_LIST );

        macResolver.expectResolveDependencies( Collections.EMPTY_SET );

        Assembly assembly = new Assembly();

        assembly.setId( "test" );

        Repository repo = new Repository();

        repo.setOutputDirectory( "out" );
        repo.setDirectoryMode( "777" );
        repo.setFileMode( "777" );

        int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        File outDir = new File( tempRoot, "out" );

        macArchiver.expectModeChange( -1, -1, mode, mode, true );
        macArchiver.expectAddDirectory( outDir, "out/", null, null );

        macRepo.expectAssemble( outDir, repo, macCS.configSource );

        assembly.addRepository( repo );

        mm.replayAll();

        createPhase( macRepo.repositoryAssembler, macResolver.dependencyResolver,
                     new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ).execute( assembly, macArchiver.archiver,
                                                                                macCS.configSource,
                                                                                new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    private RepositoryAssemblyPhase createPhase( RepositoryAssembler repositoryAssembler, DependencyResolver resolver,
                                                 Logger logger )
    {
        RepositoryAssemblyPhase phase = new RepositoryAssemblyPhase( repositoryAssembler, resolver );
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

            archiver = (Archiver) control.getMock();
        }

        public void expectAddDirectory( File outDir, String location, String[] includes, String[] excludes )
        {
            try
            {
                DefaultFileSet fs = new DefaultFileSet();
                fs.setDirectory( outDir );
                fs.setPrefix( location );
                fs.setIncludes( includes );
                fs.setExcludes( excludes );
                
                archiver.addFileSet( fs );
            }
            catch ( ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }

            control.setMatcher( new AbstractMatcher()
            {

                protected boolean argumentMatches( Object expected, Object actual )
                {
                    FileSet e = (FileSet) expected;
                    FileSet a = (FileSet) actual;
                    
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

                protected String argumentToString( Object argument )
                {
                    FileSet a = (FileSet) argument;
                    
                    return argument == null ? "Null FileSet" : "FileSet:[dir=" + a.getDirectory() + ", prefix: "
                        + a.getPrefix() + "\nincludes:\n" + arToStr( a.getIncludes() ) + "\nexcludes:\n"
                        + arToStr( a.getExcludes() ) + "]";
                }
                
                private String arToStr( String[] array )
                {
                    return array == null ? "-EMPTY-" : StringUtils.join( array, "\n\t" );
                }

                private boolean areq( String[] first, String[] second )
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

                private boolean eq( Object first, Object second )
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
                
            });
            
            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }

        void expectModeChange( int defaultDirMode, int defaultFileMode, int dirMode, int fileMode, boolean expectTwoSets )
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

        ArtifactRepository localRepo;

        MockControl localRepoCtl;

        final MockManager mockManager;

        public MockAndControlForConfigSource( MockManager mockManager )
        {
            this.mockManager = mockManager;

            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = (AssemblerConfigurationSource) control.getMock();
            
            configSource.getMavenSession();
            control.setReturnValue( null, MockControl.ZERO_OR_MORE );
        }

        public void expectGetRemoteRepositories( List remoteRepos )
        {
            configSource.getRemoteRepositories();
            control.setReturnValue( remoteRepos, MockControl.ONE_OR_MORE );
        }

        public void expectGetLocalRepository()
        {
            localRepoCtl = MockControl.createControl( ArtifactRepository.class );
            mockManager.add( localRepoCtl );

            localRepo = (ArtifactRepository) localRepoCtl.getMock();

            configSource.getLocalRepository();
            control.setReturnValue( localRepo, MockControl.ONE_OR_MORE );
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

    private final class MockAndControlForDependencyResolver
    {

        MockControl dependencyResolverCtl;

        DependencyResolver dependencyResolver;

        public MockAndControlForDependencyResolver( MockManager mm )
        {
            dependencyResolverCtl = MockControl.createControl( DependencyResolver.class );
            mm.add( dependencyResolverCtl );

            dependencyResolver = (DependencyResolver) dependencyResolverCtl.getMock();
        }

        public void expectResolveDependencies( Set resolvedArtifacts )
        {
            try
            {
                dependencyResolver.resolveDependencies( null, null, null, null, null, true );
                dependencyResolverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
                dependencyResolverCtl.setReturnValue( resolvedArtifacts );
            }
            catch ( ArtifactResolutionException e )
            {
                Assert.fail( "Should never happen!" );
            }
            catch ( ArtifactNotFoundException e )
            {
                Assert.fail( "Should never happen!" );
            }
            catch ( InvalidDependencyVersionException e )
            {
                Assert.fail( "Should never happen!" );
            }
        }
    }

    private final class MockAndControlForRepositoryAssembler
    {
        RepositoryAssembler repositoryAssembler;

        MockControl control;

        MockAndControlForRepositoryAssembler( MockManager mockManager )
        {
            control = MockControl.createControl( RepositoryAssembler.class );
            mockManager.add( control );

            repositoryAssembler = (RepositoryAssembler) control.getMock();
        }

        public void expectAssemble( File dir, Repository repo, AssemblerConfigurationSource configSource )
        {
            try
            {
                repositoryAssembler.buildRemoteRepository( dir, new RepoInfoWrapper( repo ),
                                                           new RepoBuilderConfigSourceWrapper( configSource ) );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
            }
            catch ( RepositoryAssemblyException e )
            {
                Assert.fail( "Should never happen" );
            }

            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }
    }

}
