package org.apache.maven.plugin.assembly.archive;

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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.classextension.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DefaultAssemblyArchiverTest
{

    private static final TestFileManager fileManager = new TestFileManager( "def-assy-archiver.test.", "" );

    private PlexusContainer container;

    @Before
    public void setup()
        throws PlexusContainerException
    {
        container = new DefaultPlexusContainer();
        container.initialize();
        container.start();
    }

    @AfterClass
    public static void tearDown()
        throws Exception
    {
        fileManager.cleanUp();
    }

    @Test( expected = InvalidAssemblerConfigurationException.class )
    public void failWhenAssemblyIdIsNull()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAssemblyArchiver macMgr = new MockAndControlForAssemblyArchiver( mm );

        final AssemblerConfigurationSource configSource = mm.createControl().createMock(
            AssemblerConfigurationSource.class );

        mm.replayAll();

        final DefaultAssemblyArchiver archiver = createSubject( macMgr, null, null );
        archiver.createArchive( new Assembly(), "full-name", "zip", configSource, false );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchive()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        IOException, DependencyResolutionException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAssemblyArchiver macMgr = new MockAndControlForAssemblyArchiver( mm );

        macMgr.expectGetArchiver( "zip", Archiver.class );
        macMgr.expectGetDestFile( new File( "test" ) );

        final AssemblyArchiverPhase phase = mm.createControl().createMock(AssemblyArchiverPhase.class  );

        phase.execute( (Assembly)anyObject(), (Archiver)anyObject(), (AssemblerConfigurationSource)anyObject() );

        final AssemblerConfigurationSource configSource =
            mm.createControl().createMock( AssemblerConfigurationSource.class );

        final File tempDir = fileManager.createTempDir();
        FileUtils.deleteDirectory( tempDir );

        expect(configSource.getTemporaryRootDirectory()).andReturn( tempDir ).anyTimes();
        expect( configSource.isDryRun()).andReturn( false ).anyTimes();
        expect( configSource.isIgnoreDirFormatExtensions()).andReturn( false ).anyTimes();

        final File outDir = fileManager.createTempDir();

        macMgr.archiver.setDestFile( new File( outDir, "full-name.zip" ) );

        try
        {
            macMgr.archiver.createArchive();
        }
        catch ( final ArchiverException e )
        {
            fail( "Should never happen" );
        }
        catch ( final IOException e )
        {
            fail( "Should never happen" );
        }

        expect(configSource.getOutputDirectory()).andReturn( outDir );
        expect( configSource.getFinalName() ).andReturn( "finalName" );
        expect( configSource.getArchiverConfig()).andReturn( null ).anyTimes();
        expect(    configSource.getWorkingDirectory()).andReturn( new File( "." )).anyTimes();
        expect( configSource.isUpdateOnly()).andReturn( false ).anyTimes();
        expect( configSource.isIgnorePermissions()).andReturn( false ).anyTimes();

        final Assembly assembly = new Assembly();
        assembly.setId( "id" );

       // try
       // {
   //         expect( macMgr.dependencyResolver.resolve( (Assembly) anyObject(), (AssemblerConfigurationSource) anyObject() )).andReturn( new HashSet<Artifact>(  ) );
//            macMgr.dependencyResolverControl.setMatcher( MockControl.ALWAYS_MATCHER );
   //     }
      //  catch ( final DependencyResolutionException e )
       // {
        //    fail( "Should never happen" );
       // }

        mm.replayAll();

        final DefaultAssemblyArchiver subject = createSubject( macMgr, Collections.singletonList( phase ), null );

        subject.createArchive( assembly, "full-name", "zip", configSource, false );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldConfigureArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        final TestArchiverWithConfig archiver = new TestArchiverWithConfig();

        macArchiverManager.expectGetArchiver( "dummy", archiver );

        final AssemblerConfigurationSource configSource = mm.createMock(  AssemblerConfigurationSource.class);

        final String simpleConfig = "value";

        expect( configSource.getArchiverConfig()).andReturn(
        "<configuration><simpleConfig>" + simpleConfig + "</simpleConfig></configuration>").anyTimes();

        final MavenProject project = new MavenProject( new Model() );

        expect(configSource.getProject()).andReturn( project ).anyTimes();

        expect(configSource.getMavenSession()).andReturn( null ).anyTimes();

        expect(configSource.isDryRun()).andReturn( false ).anyTimes();

        expect(configSource.getWorkingDirectory()).andReturn(  new File( "." )).anyTimes();

        expect(configSource.isUpdateOnly()).andReturn( false ).anyTimes();

        final ArtifactRepository lr = mm.createMock( ArtifactRepository.class );

        expect(lr.getBasedir()).andReturn(  "/path/to/local/repo" ).anyTimes();

        expect(configSource.getLocalRepository()).andReturn( lr ).anyTimes();
        expect(configSource.isIgnorePermissions()).andReturn( true );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "dummy", false, "finalName", configSource, null, false );

        assertEquals( simpleConfig, archiver.getSimpleConfig() );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldCreateTarArchiverWithNoCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        final AssemblerConfigurationSource configSource = mm.createMock( AssemblerConfigurationSource.class );

        expect( configSource.getTarLongFileMode()).andReturn( TarLongFileMode.fail.toString()).anyTimes();
        expect( configSource.isDryRun()).andReturn( false ).anyTimes();

        expect( configSource.getArchiverConfig()).andReturn( null ).anyTimes();

        expect( configSource.getProject()).andReturn( new MavenProject( new Model() ) ).anyTimes();

        expect( configSource.getJarArchiveConfiguration()).andReturn( null ).anyTimes();

        expect( configSource.getWorkingDirectory()).andReturn(  new File( "." ) ).anyTimes();

        expect( configSource.isUpdateOnly()).andReturn( false ).anyTimes();

        expect( configSource.isIgnorePermissions()).andReturn( true ).anyTimes();

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "tar", false, "finalName", configSource, null, false );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldCreateWarArchiverWithIgnoreWebxmlSetToFalse()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestWarArchiver twArchiver = new TestWarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        final AssemblerConfigurationSource configSource = mm.createMock( AssemblerConfigurationSource.class );

        expect( configSource.isDryRun()).andReturn( false ).anyTimes();
        expect( configSource.getArchiverConfig()).andReturn( null ).anyTimes();
        expect( configSource.getMavenSession()).andReturn( null ).anyTimes();
        expect( configSource.getProject()).andReturn( new MavenProject( new Model() ) ).anyTimes();
        expect( configSource.getJarArchiveConfiguration()).andReturn( null ).anyTimes();
        expect( configSource.getWorkingDirectory()).andReturn( new File( "." ) ).anyTimes();
        expect( configSource.isUpdateOnly()).andReturn( false ).anyTimes();
        expect( configSource.isIgnorePermissions()).andReturn( true ).anyTimes();

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "war", false, null, configSource, null, false );

        assertFalse( twArchiver.ignoreWebxml );
    }

    @Test
    public void testCreateArchiver_ShouldCreateZipArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final ZipArchiver archiver = new ZipArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "zip", archiver );

        final AssemblerConfigurationSource configSource = mm.createMock( AssemblerConfigurationSource.class );

        expect( configSource.isDryRun()).andReturn( false ).anyTimes();
        expect( configSource.getArchiverConfig()).andReturn( null ).anyTimes();
        expect( configSource.getWorkingDirectory()).andReturn( new File( "." ) ).anyTimes();
        expect( configSource.isUpdateOnly()).andReturn( false ).anyTimes();
        expect( configSource.getJarArchiveConfiguration()).andReturn( null ).anyTimes();
        expect( configSource.isIgnorePermissions()).andReturn( true ).anyTimes();

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "zip", false, null, configSource, null, false );
    }

    @Test
    public void testCreateWarArchiver_ShouldDisableIgnoreWebxmlOption()
        throws NoSuchArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestWarArchiver twArchiver = new TestWarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createWarArchiver();

        assertFalse( twArchiver.ignoreWebxml );
    }

    @Test
    public void testCreateTarArchiver_ShouldNotInitializeCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        subject.createTarArchiver( "tar", TarLongFileMode.fail );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    private DefaultAssemblyArchiver createSubject(EasyMockSupport mm,
            TestTarArchiver ttArchiver) throws NoSuchArchiverException {
        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        return createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );
    }

    @Test
    public void testCreateTarArchiver_TarGzFormat_ShouldInitializeGZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        subject.createTarArchiver( "tar.gz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.gzip, ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_TgzFormat_ShouldInitializeGZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        subject.createTarArchiver( "tgz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.gzip, ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_TarBz2Format_ShouldInitializeBZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        subject.createTarArchiver( "tar.bz2", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.bzip2, ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_Tbz2Format_ShouldInitializeBZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        subject.createTarArchiver( "tbz2", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.bzip2, ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_InvalidFormat_ShouldFailWithInvalidCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final DefaultAssemblyArchiver subject = createSubject(mm, ttArchiver);

        try
        {
            subject.createTarArchiver( "tar.Z", null );

            fail( "Invalid compression formats should throw an error." );
        }
        catch ( final IllegalArgumentException e )
        {
            // expected.
        }

        mm.verifyAll();
    }

    private DefaultAssemblyArchiver createSubject( final MockAndControlForAssemblyArchiver macMgr,
                                                   final List<AssemblyArchiverPhase> phases, Logger logger )
    {
        final DefaultAssemblyArchiver subject =
            new DefaultAssemblyArchiver( macMgr.archiverManager, phases );

        subject.setContainer( container );

        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        }

        subject.enableLogging( logger );

        return subject;
    }

    private final class MockAndControlForAssemblyArchiver
    {
        final ArchiverManager archiverManager;


        Archiver archiver;

        final DependencyResolver dependencyResolver;

        private final EasyMockSupport mm;

        public MockAndControlForAssemblyArchiver( final EasyMockSupport mm )
        {
            this.mm = mm;
            archiverManager = mm.createControl().createMock(  ArchiverManager.class );

            dependencyResolver = mm.createControl().createMock( DependencyResolver.class );

        }

        void expectGetDestFile( final File file )
        {
            expect(archiver.getDestFile()).andReturn( file ).anyTimes();
        }

        void createArchiver( final Class<? extends Archiver> archiverClass )
        {
            archiver = mm.createControl().createMock(archiverClass);

            archiver.setForced( anyBoolean() );
            expectLastCall().anyTimes();

            archiver.setIgnorePermissions( false );
            expectLastCall().anyTimes();
        }

        void expectGetArchiver( final String format, final Class<? extends Archiver> archiverClass )
        {
            createArchiver( archiverClass );

            try
            {
                expect(archiverManager.getArchiver( format )).andReturn( archiver );
            }
            catch ( final NoSuchArchiverException e )
            {
                Assert.fail( "should never happen" );
            }

        }

        void expectGetArchiver( final String format, final Archiver archiver )
            throws NoSuchArchiverException
        {
            expect(archiverManager.getArchiver( format )).andReturn( archiver );
        }
    }

    private static final class TestTarArchiver
        extends TarArchiver
    {

        TarCompressionMethod compressionMethod;

        TarLongFileMode longFileMode;

        @Override
        protected void execute()
            throws ArchiverException, IOException
        {
            super.createArchive();
        }

        @Override
        public void setCompression( final TarCompressionMethod mode )
        {
            compressionMethod = mode;
            super.setCompression( mode );
        }

        @Override
        public void setLongfile( final TarLongFileMode mode )
        {
            longFileMode = mode;
            super.setLongfile( mode );
        }

    }

    private static final class TestWarArchiver
        extends WarArchiver
    {

        boolean ignoreWebxml;

        @Override
        public void setIgnoreWebxml( final boolean ignore )
        {
            ignoreWebxml = ignore;
            super.setIgnoreWebxml( ignore );
        }

    }

    public static final class TestArchiverWithConfig
        extends NoOpArchiver
    {

        private String simpleConfig;

        public String getSimpleConfig()
        {
            return simpleConfig;
        }


        public String getDuplicateBehavior()
        {
            return Archiver.DUPLICATES_ADD;
        }
    }

}
