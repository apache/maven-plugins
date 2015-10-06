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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.Os;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.classextension.EasyMockSupport;

import java.io.File;
import java.io.IOException;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class FileItemAssemblyPhaseTest
    extends TestCase
{

    private final TestFileManager fileManager = new TestFileManager( "file-item-phase.test.", "" );

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testExecute_ShouldAddNothingWhenNoFileItemsArePresent()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        macCS.expectGetBasedir( basedir );

        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, null, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldAddAbsoluteFileNoFilterNoLineEndingConversion()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        final File file = fileManager.createFile( basedir, "file.txt", "This is a test file." );

        macCS.expectGetBasedir( basedir );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();

        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileItem fi = new FileItem();
        fi.setSource( file.getAbsolutePath() );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        macArchiver.expectAddFile( file,
                                   "file.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        assembly.addFile( fi );
        
        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_ShouldAddRelativeFileNoFilterNoLineEndingConversion()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        final File file = fileManager.createFile( basedir, "file.txt", "This is a test file." );

        macCS.expectGetBasedir( basedir );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();

        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileItem fi = new FileItem();
        fi.setSource( "file.txt" );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        macArchiver.expectAddFile( file,
                                   "file.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        assembly.addFile( fi );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_WithOutputDirectory()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        final File readmeFile = fileManager.createFile( basedir, "README.txt", "This is a test file for README.txt." );
        final File licenseFile =
            fileManager.createFile( basedir, "LICENSE.txt", "This is a test file for LICENSE.txt." );
        final File configFile =
            fileManager.createFile( basedir, "config/config.txt", "This is a test file for config/config.txt" );

        macCS.expectGetBasedir( basedir );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();

        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            macLogger.logger.error( "OS=Windows and the assembly descriptor contains a *nix-specific "
                + "root-relative-reference (starting with slash) /" );
        }
        else
        {
            macLogger.logger.warn( (String)anyObject() );
        }

        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( true );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setOutputDirectory( "" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setOutputDirectory( "/" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        macArchiver.expectAddFile( readmeFile,
                                   "README.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( licenseFile,
                                   "LICENSE.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( configFile,
                                   "config/config.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_WithOutputDirectoryAndDestName()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        final File readmeFile = fileManager.createFile( basedir, "README.txt", "This is a test file for README.txt." );
        final File licenseFile =
            fileManager.createFile( basedir, "LICENSE.txt", "This is a test file for LICENSE.txt." );
        final File configFile =
            fileManager.createFile( basedir, "config/config.txt", "This is a test file for config/config.txt" );

        macCS.expectGetBasedir( basedir );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();


        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            macLogger.logger.error( "OS=Windows and the assembly descriptor contains a *nix-specific "
                + "root-relative-reference (starting with slash) /" );
        }
        else
        {
            macLogger.logger.warn( (String)anyObject() );
        }

        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( true );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setOutputDirectory( "" );
        readmeFileItem.setDestName( "README_renamed.txt" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setOutputDirectory( "/" );
        licenseFileItem.setDestName( "LICENSE_renamed.txt" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setDestName( "config_renamed.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        macArchiver.expectAddFile( readmeFile,
                                   "README_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( licenseFile,
                                   "LICENSE_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( configFile,
                                   "config/config_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    public void testExecute_WithOutputDirectoryAndDestNameAndIncludeBaseDirectoryFalse()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        final File basedir = fileManager.createTempDir();

        final File readmeFile = fileManager.createFile( basedir, "README.txt", "This is a test file for README.txt." );
        final File licenseFile =
            fileManager.createFile( basedir, "LICENSE.txt", "This is a test file for LICENSE.txt." );
        final File configFile =
            fileManager.createFile( basedir, "config/config.txt", "This is a test file for config/config.txt" );

        macCS.expectGetBasedir( basedir );

        macCS.expectGetProject( new MavenProject( new Model() ) );

        macCS.expectGetFinalName( "final-name" );
        macCS.expectInterpolators();

        final MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        final MockAndControlForArchiver macArchiver = new MockAndControlForArchiver( mm );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setDestName( "README_renamed.txt" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setDestName( "LICENSE_renamed.txt" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setDestName( "config_renamed.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        macArchiver.expectAddFile( readmeFile,
                                   "README_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( licenseFile,
                                   "LICENSE_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macArchiver.expectAddFile( configFile,
                                   "config/config_renamed.txt",
                                   TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        mm.replayAll();

        createPhase( macLogger.logger ).execute( assembly, macArchiver.archiver, macCS.configSource );

        mm.verifyAll();
    }

    private FileItemAssemblyPhase createPhase( final Logger logger )
    {
        final FileItemAssemblyPhase phase = new FileItemAssemblyPhase();
        phase.enableLogging( logger );

        return phase;
    }

    private final class MockAndControlForArchiver
    {
        final Archiver archiver;

        public MockAndControlForArchiver( final EasyMockSupport mockManager )
        {
            archiver = mockManager.createMock(Archiver.class);
        }

        public void expectAddFile( final File file, final String outputLocation, final int fileMode )
        {
            try
            {
                archiver.addResource( ( PlexusIoResource) anyObject(), (String) anyObject(), anyInt() );
            }
            catch ( final ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
        }
    }

    private final class MockAndControlForConfigSource
    {
        final AssemblerConfigurationSource configSource;

        public MockAndControlForConfigSource( final EasyMockSupport mockManager )
        {

            configSource = mockManager.createMock(AssemblerConfigurationSource.class);

            expect(configSource.getMavenSession()).andReturn( null ).anyTimes();
        }

        public void expectGetProject( final MavenProject project )
        {
            expect(configSource.getProject()).andReturn( project ).atLeastOnce();
        }

        public void expectGetFinalName( final String finalName )
        {
            expect( configSource.getFinalName()).andReturn( finalName ).atLeastOnce();
        }

        public void expectInterpolators( )
        {
            expect(configSource.getCommandLinePropsInterpolator()).andReturn( FixedStringSearchInterpolator.empty() ).anyTimes();
            expect(configSource.getEnvInterpolator()).andReturn( FixedStringSearchInterpolator.empty() ).anyTimes();
            expect(configSource.getMainProjectInterpolator()).andReturn( FixedStringSearchInterpolator.empty() ).anyTimes();
        }


        public void expectGetTemporaryRootDirectory( final File tempRoot )
        {
            expect( configSource.getTemporaryRootDirectory()).andReturn( tempRoot ).atLeastOnce();
        }

        public void expectGetBasedir( final File basedir )
        {
            expect( configSource.getBasedir()).andReturn( basedir ).atLeastOnce();
        }

        public void expectGetEncoding( )
        {
            expect( configSource.getEncoding()).andReturn( "UTF-8").atLeastOnce();
        }
}

    private final class MockAndControlForLogger
    {
        final Logger logger;

        public MockAndControlForLogger( final EasyMockSupport mockManager )
        {

            logger = mockManager.createMock (Logger.class);
        }
    }

}
