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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.FinalizerEnabled;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

public class DefaultAssemblyArchiverTest
// extends PlexusTestCase
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

    public void shutdown()
    {
        container.dispose();
    }

    // @Override
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
        final MockManager mm = new MockManager();
        final MockAndControlForAssemblyArchiver macMgr = new MockAndControlForAssemblyArchiver( mm );

        final MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );

        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) csControl.getMock();

        mm.replayAll();

        final DefaultAssemblyArchiver archiver = createSubject( macMgr, null, null );
        archiver.createArchive( new Assembly(), "full-name", "zip", configSource );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchive()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        IOException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAssemblyArchiver macMgr = new MockAndControlForAssemblyArchiver( mm );

        macMgr.expectGetArchiver( "zip", Archiver.class );
        macMgr.expectGetDestFile( new File( "test" ) );

        final MockControl phaseControl = MockControl.createControl( AssemblyArchiverPhase.class );
        mm.add( phaseControl );

        final AssemblyArchiverPhase phase = (AssemblyArchiverPhase) phaseControl.getMock();

        phase.execute( null, null, null, null );
        phaseControl.setMatcher( MockControl.ALWAYS_MATCHER );

        final MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );

        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) csControl.getMock();

        final File tempDir = fileManager.createTempDir();
        FileUtils.deleteDirectory( tempDir );

        configSource.getTemporaryRootDirectory();
        csControl.setReturnValue( tempDir, MockControl.ZERO_OR_MORE );

        configSource.isDryRun();
        csControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnoreDirFormatExtensions();
        csControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

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

        configSource.getOutputDirectory();
        csControl.setReturnValue( outDir );

        configSource.getFinalName();
        csControl.setReturnValue( "finalName" );

        configSource.getArchiverConfig();
        csControl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getWorkingDirectory();
        csControl.setReturnValue( new File( "." ), MockControl.ZERO_OR_MORE );

        configSource.isUpdateOnly();
        csControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnorePermissions();
        csControl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        final Assembly assembly = new Assembly();
        assembly.setId( "id" );

        final AssemblyContext context = new DefaultAssemblyContext();

        try
        {
            macMgr.dependencyResolver.resolve( assembly, configSource, context );
            macMgr.dependencyResolverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }
        catch ( final DependencyResolutionException e )
        {
            fail( "Should never happen" );
        }

        mm.replayAll();

        final DefaultAssemblyArchiver subject = createSubject( macMgr, Collections.singletonList( phase ), null );

        subject.createArchive( assembly, "full-name", "zip", configSource );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldConfigureArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        final TestArchiverWithConfig archiver = new TestArchiverWithConfig();

        macArchiverManager.expectGetArchiver( "dummy", archiver );

        final MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        final String simpleConfig = "value";

        configSource.getArchiverConfig();
        configCtl.setReturnValue( "<configuration><simpleConfig>" + simpleConfig + "</simpleConfig></configuration>",
                                  MockControl.ZERO_OR_MORE );

        final MavenProject project = new MavenProject( new Model() );

        configSource.getProject();
        configCtl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.getMavenSession();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getWorkingDirectory();
        configCtl.setReturnValue( new File( "." ), MockControl.ZERO_OR_MORE );

        configSource.isUpdateOnly();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        final MockControl lrCtl = MockControl.createControl( ArtifactRepository.class );
        final ArtifactRepository lr = (ArtifactRepository) lrCtl.getMock();
        mm.add( lrCtl );

        lr.getBasedir();
        lrCtl.setReturnValue( "/path/to/local/repo", MockControl.ZERO_OR_MORE );

        configSource.getLocalRepository();
        configCtl.setReturnValue( lr, MockControl.ZERO_OR_MORE );

        configSource.isIgnorePermissions();
        configCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        mm.add( configCtl );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "dummy", false, "finalName", configSource, null );

        assertEquals( simpleConfig, archiver.getSimpleConfig() );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldCreateTarArchiverWithNoCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        final MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.getTarLongFileMode();
        configCtl.setReturnValue( TarLongFileMode.FAIL, MockControl.ZERO_OR_MORE );

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configCtl.setReturnValue( new MavenProject( new Model() ), MockControl.ZERO_OR_MORE );

        configSource.getJarArchiveConfiguration();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getWorkingDirectory();
        configCtl.setReturnValue( new File( "." ), MockControl.ZERO_OR_MORE );

        configSource.isUpdateOnly();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnorePermissions();
        configCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        mm.add( configCtl );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "tar", false, "finalName", configSource, null );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    @Test
    public void testCreateArchiver_ShouldCreateWarArchiverWithIgnoreWebxmlSetToFalse()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final TestWarArchiver twArchiver = new TestWarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        final MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configCtl.setReturnValue( new MavenProject( new Model() ), MockControl.ZERO_OR_MORE );

        configSource.getJarArchiveConfiguration();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getWorkingDirectory();
        configCtl.setReturnValue( new File( "." ), MockControl.ZERO_OR_MORE );

        configSource.isUpdateOnly();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnorePermissions();
        configCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        mm.add( configCtl );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "war", false, null, configSource, null );

        assertFalse( twArchiver.ignoreWebxml );
    }

    @Test
    public void testCreateArchiver_ShouldCreateZipArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final ZipArchiver archiver = new ZipArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "zip", archiver );

        final MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        final AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();

        configSource.isDryRun();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.getArchiverConfig();
        configCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        configSource.getWorkingDirectory();
        configCtl.setReturnValue( new File( "." ), MockControl.ZERO_OR_MORE );

        configSource.isUpdateOnly();
        configCtl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnorePermissions();
        configCtl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        mm.add( configCtl );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "zip", false, null, configSource, null );
    }

    // TODO: Re-implement these tests on the createArchiver(..) method. For now, they're no big loss.
    // public void testConfigureArchiverFinalizers_ShouldDoNothingWhenNotSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( Archiver.class );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFinalizers( macArchiverManager.archiver, "format", null, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFinalizers_ShouldAddManifestFinalizerWhenSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
    //
    // Set finalizerClasses = new HashSet();
    // finalizerClasses.add( ComponentsXmlArchiverFileFilter.class );
    // finalizerClasses.add( ManifestCreationFinalizer.class );
    //
    // macArchiverManager.expectSetArchiverFinalizers( finalizerClasses );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // MockControl configCtl = MockControl.createControl( AssemblerConfigurationSource.class );
    // AssemblerConfigurationSource configSource = (AssemblerConfigurationSource) configCtl.getMock();
    //
    // Model model = new Model();
    // model.setGroupId( "group" );
    // model.setArtifactId( "artifact" );
    // model.setVersion( "1" );
    //
    // configSource.getProject();
    // configCtl.setReturnValue( new MavenProject( model ), MockControl.ZERO_OR_MORE );
    //
    // configSource.getJarArchiveConfiguration();
    // configCtl.setReturnValue( new MavenArchiveConfiguration() );
    //
    // mm.add( configCtl );
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFinalizers( macArchiverManager.archiver, "jar", configSource, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFilters_ShouldDoNothingWhenNotSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( Archiver.class );
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFilters( macArchiverManager.archiver, filter );
    //
    // mm.verifyAll();
    // }
    //
    // public void testConfigureArchiverFilters_ShouldAddComponentsFilterWhenSupportedByArchiver()
    // {
    // MockManager mm = new MockManager();
    //
    // MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );
    //
    // macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
    // macArchiverManager.expectSetArchiverFilters();
    //
    // ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();
    //
    // mm.replayAll();
    //
    // DefaultAssemblyArchiver subject =
    // createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );
    //
    // subject.configureArchiverFilters( macArchiverManager.archiver, filter );
    //
    // mm.verifyAll();
    // }

    @Test
    public void testCreateWarArchiver_ShouldDisableIgnoreWebxmlOption()
        throws NoSuchArchiverException
    {
        final MockManager mm = new MockManager();

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
        final MockManager mm = new MockManager();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar", TarLongFileMode.FAIL );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_ShouldInitializeGZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar.gz", TarLongFileMode.FAIL );

        assertEquals( "gzip", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_ShouldInitializeBZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar.bz2", TarLongFileMode.FAIL );

        assertEquals( "bzip2", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    @Test
    public void testCreateTarArchiver_ShouldFailWithInvalidCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        final MockManager mm = new MockManager();

        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final MockAndControlForAssemblyArchiver macArchiverManager = new MockAndControlForAssemblyArchiver( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        final DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

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
            new DefaultAssemblyArchiver( macMgr.archiverManager, macMgr.dependencyResolver, phases );

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
        ArchiverManager archiverManager;

        MockControl control;

        MockControl archiverControl;

        Archiver archiver;

        MockControl dependencyResolverControl;

        DependencyResolver dependencyResolver;

        private final MockManager mm;

        public MockAndControlForAssemblyArchiver( final MockManager mm )
        {
            this.mm = mm;
            control = MockControl.createControl( ArchiverManager.class );
            mm.add( control );

            archiverManager = (ArchiverManager) control.getMock();

            dependencyResolverControl = MockControl.createControl( DependencyResolver.class );
            mm.add( dependencyResolverControl );

            dependencyResolver = (DependencyResolver) dependencyResolverControl.getMock();
        }

        void expectGetDestFile( final File file )
        {
            archiver.getDestFile();
            archiverControl.setReturnValue( file, MockControl.ZERO_OR_MORE );
        }

        void createArchiver( final Class<?> archiverClass )
        {
            archiverControl = MockControl.createControl( archiverClass );
            mm.add( archiverControl );

            archiver = (Archiver) archiverControl.getMock();

            archiver.setForced( false );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
            archiverControl.setVoidCallable( MockControl.ZERO_OR_MORE );

            archiver.setIgnorePermissions( false );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
            archiverControl.setVoidCallable( MockControl.ZERO_OR_MORE );
        }

        // void expectSetArchiverFilters()
        // {
        // ( ( FilterEnabled ) archiver ).setArchiveFilters( null );
        // archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        // }

        void expectGetArchiver( final String format, final Class<?> archiverClass )
        {
            createArchiver( archiverClass );

            try
            {
                archiverManager.getArchiver( format );
            }
            catch ( final NoSuchArchiverException e )
            {
                Assert.fail( "should never happen" );
            }

            control.setReturnValue( archiver );
        }

        void expectGetArchiver( final String format, final Archiver archiver )
            throws NoSuchArchiverException
        {
            archiverManager.getArchiver( format );
            control.setReturnValue( archiver );
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
        public TarOptions getOptions()
        {
            return super.getOptions();
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

        @Override
        public void setOptions( final TarOptions options )
        {
            super.setOptions( options );
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

    interface TestFinalizerFilteredArchiver
        extends Archiver, FinalizerEnabled
    {

    }

    public static final class TestArchiverWithConfig
        implements Archiver
    {

        private String simpleConfig;

        private boolean useJvmChmod;

        private boolean ignorePermissions;

        public void setSimpleConfig( final String simpleConfig )
        {
            this.simpleConfig = simpleConfig;
        }

        public String getSimpleConfig()
        {
            return simpleConfig;
        }

        public void addArchivedFileSet( final File arg0 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( final ArchivedFileSet arg0 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( final File arg0, final String arg1 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( final File arg0, final String[] arg1, final String[] arg2 )
            throws ArchiverException
        {
        }

        public void addArchivedFileSet( final File arg0, final String arg1, final String[] arg2, final String[] arg3 )
            throws ArchiverException
        {
        }

        public void addDirectory( final File arg0 )
            throws ArchiverException
        {
        }

        public void addDirectory( final File arg0, final String arg1 )
            throws ArchiverException
        {
        }

        public void addDirectory( final File arg0, final String[] arg1, final String[] arg2 )
            throws ArchiverException
        {
        }

        public void addDirectory( final File arg0, final String arg1, final String[] arg2, final String[] arg3 )
            throws ArchiverException
        {
        }

        public void addFile( final File arg0, final String arg1 )
            throws ArchiverException
        {
        }

        public void addFile( final File arg0, final String arg1, final int arg2 )
            throws ArchiverException
        {
        }

        public void addFileSet( final FileSet arg0 )
            throws ArchiverException
        {
        }

        public void addResource( final PlexusIoResource arg0, final String arg1, final int arg2 )
            throws ArchiverException
        {
        }

        public void addResources( final PlexusIoResourceCollection arg0 )
            throws ArchiverException
        {
        }

        public void createArchive()
            throws ArchiverException, IOException
        {
        }

        public int getDefaultDirectoryMode()
        {
            return 0;
        }

        public int getDefaultFileMode()
        {
            return 0;
        }

        public File getDestFile()
        {
            return null;
        }

        @SuppressWarnings( "rawtypes" )
        public Map getFiles()
        {
            return null;
        }

        public boolean getIncludeEmptyDirs()
        {
            return false;
        }

        public ResourceIterator getResources()
            throws ArchiverException
        {
            return null;
        }

        public boolean isForced()
        {
            return false;
        }

        public boolean isSupportingForced()
        {
            return false;
        }

        public void setDefaultDirectoryMode( final int arg0 )
        {
        }

        public void setDefaultFileMode( final int arg0 )
        {
        }

        public void setDestFile( final File arg0 )
        {
        }

        public void setDotFileDirectory( final File arg0 )
        {
        }

        public void setForced( final boolean arg0 )
        {
        }

        public void setIncludeEmptyDirs( final boolean arg0 )
        {
        }

        public String getDuplicateBehavior()
        {
            return Archiver.DUPLICATES_ADD;
        }

        public void setDuplicateBehavior( final String duplicate )
        {
        }

        public int getDirectoryMode()
        {
            return 0;
        }

        public int getFileMode()
        {
            return 0;
        }

        public int getOverrideDirectoryMode()
        {
            return 0;
        }

        public int getOverrideFileMode()
        {
            return 0;
        }

        public void setDirectoryMode( final int mode )
        {
        }

        public void setFileMode( final int mode )
        {
        }

        public boolean isUseJvmChmod()
        {
            return useJvmChmod;
        }

        public void setUseJvmChmod( final boolean useJvmChmod )
        {
            this.useJvmChmod = useJvmChmod;
        }

        public boolean isIgnorePermissions()
        {
            return ignorePermissions;
        }

        public void setIgnorePermissions( final boolean ignorePermissions )
        {
            this.ignorePermissions = ignorePermissions;
        }

    }

}
