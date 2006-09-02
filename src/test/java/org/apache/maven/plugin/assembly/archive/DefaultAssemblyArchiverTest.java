package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FilterEnabled;
import org.codehaus.plexus.archiver.FinalizerEnabled;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.Logger;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DefaultAssemblyArchiverTest
    extends TestCase
{
    
    private TestFileManager fileManager = new TestFileManager( "def-assy-archiver.test.", "" );
    
    public void tearDown() throws IOException
    {
        fileManager.cleanUp();
    }

    public void testCreateArchive()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiverManager macMgr = new MockAndControlForArchiverManager( mm );
        
        macMgr.expectGetArchiver( "zip", Archiver.class );

        MockControl phaseControl = MockControl.createControl( AssemblyArchiverPhase.class );
        mm.add( phaseControl );

        AssemblyArchiverPhase phase = ( AssemblyArchiverPhase ) phaseControl.getMock();

        phase.execute( null, null, null );
        phaseControl.setMatcher( MockControl.ALWAYS_MATCHER );
        
        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );
        
        AssemblerConfigurationSource configSource = ( AssemblerConfigurationSource ) csControl.getMock();
        
        File outDir = fileManager.createTempDir();
        
        macMgr.archiver.setDestFile( new File( outDir, "full-name.zip" ) );
        
        try
        {
            macMgr.archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen" );
        }
        catch ( IOException e )
        {
            fail( "Should never happen" );
        }
        
        configSource.getOutputDirectory();
        csControl.setReturnValue( outDir );
        
        configSource.getTarLongFileMode();
        csControl.setReturnValue( null );
        
        configSource.getFinalName();
        csControl.setReturnValue( "finalName" );
        
        Assembly assembly = new Assembly();
        
        mm.replayAll();
        
        DefaultAssemblyArchiver subject = createSubject( macMgr.archiverManager, Collections.singletonList( phase ), null );
        
        subject.createArchive( assembly, "full-name", "zip", configSource );
        
        mm.verifyAll();
    }

    public void testCreateArchiver_ShouldCreateTarArchiverWithNoCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "tar", false, "finalName", TarLongFileMode.FAIL, filter );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateArchiver_ShouldCreateWarArchiverWithIgnoreWebxmlSetToFalse()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestWarArchiver twArchiver = new TestWarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "war", false, null, null, filter );

        assertFalse( twArchiver.ignoreWebxml );
    }

    public void testCreateArchiver_ShouldCreateZipArchiver()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        ZipArchiver archiver = new ZipArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "zip", archiver );

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createArchiver( "zip", false, null, null, filter );
    }

    public void testConfigureArchiverFinalizers_ShouldDoNothingWhenNotSupportedByArchiver()
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.createArchiver( Archiver.class );

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.configureArchiverFinalizers( macArchiverManager.archiver, filter );

        mm.verifyAll();
    }

    public void testConfigureArchiverFinalizers_ShouldAddManifestFinalizerWhenSupportedByArchiver()
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
        macArchiverManager.expectSetArchiverFinalizers();

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.configureArchiverFinalizers( macArchiverManager.archiver, filter );

        mm.verifyAll();
    }

    public void testConfigureArchiverFilters_ShouldDoNothingWhenNotSupportedByArchiver()
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.createArchiver( Archiver.class );

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.configureArchiverFilters( macArchiverManager.archiver, filter );

        mm.verifyAll();
    }

    public void testConfigureArchiverFilters_ShouldAddComponentsFilterWhenSupportedByArchiver()
    {
        MockManager mm = new MockManager();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.createArchiver( TestFinalizerFilteredArchiver.class );
        macArchiverManager.expectSetArchiverFilters();

        ComponentsXmlArchiverFileFilter filter = new ComponentsXmlArchiverFileFilter();

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.configureArchiverFilters( macArchiverManager.archiver, filter );

        mm.verifyAll();
    }

    public void testCreateWarArchiver_ShouldDisableIgnoreWebxmlOption()
        throws NoSuchArchiverException
    {
        MockManager mm = new MockManager();

        TestWarArchiver twArchiver = new TestWarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "war", twArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createWarArchiver();

        assertFalse( twArchiver.ignoreWebxml );
    }

    public void testCreateTarArchiver_ShouldNotInitializeCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar", TarLongFileMode.FAIL );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldInitializeGZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar.gz", TarLongFileMode.FAIL );

        assertEquals( "gzip", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldInitializeBZipCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        subject.createTarArchiver( "tar.bz2", TarLongFileMode.FAIL );

        assertEquals( "bzip2", ttArchiver.compressionMethod.getValue() );
        assertEquals( TarLongFileMode.FAIL, ttArchiver.longFileMode.getValue() );

        mm.verifyAll();
    }

    public void testCreateTarArchiver_ShouldFailWithInvalidCompression()
        throws NoSuchArchiverException, ArchiverException
    {
        MockManager mm = new MockManager();

        TestTarArchiver ttArchiver = new TestTarArchiver();

        MockAndControlForArchiverManager macArchiverManager = new MockAndControlForArchiverManager( mm );

        macArchiverManager.expectGetArchiver( "tar", ttArchiver );

        mm.replayAll();

        DefaultAssemblyArchiver subject =
            createSubject( macArchiverManager.archiverManager, Collections.EMPTY_LIST, null );

        try
        {
            subject.createTarArchiver( "tar.Z", null );

            fail( "Invalid compression formats should throw an error." );
        }
        catch ( IllegalArgumentException e )
        {
            // expected.
        }

        mm.verifyAll();
    }

    private DefaultAssemblyArchiver createSubject( ArchiverManager archiverManager, List phases, Logger logger )
    {
        DefaultAssemblyArchiver subject = new DefaultAssemblyArchiver( archiverManager, phases );

        if ( logger != null )
        {
            subject.enableLogging( logger );
        }

        return subject;
    }

    private final class MockAndControlForArchiverManager
    {
        ArchiverManager archiverManager;

        MockControl control;

        MockControl archiverControl;

        Archiver archiver;

        private final MockManager mm;

        public MockAndControlForArchiverManager( MockManager mm )
        {
            this.mm = mm;
            control = MockControl.createControl( ArchiverManager.class );
            mm.add( control );

            archiverManager = ( ArchiverManager ) control.getMock();
        }

        void createArchiver( Class archiverClass )
        {
            archiverControl = MockControl.createControl( archiverClass );
            mm.add( archiverControl );

            archiver = ( Archiver ) archiverControl.getMock();
        }

        void expectSetArchiverFinalizers()
        {
            ( ( FinalizerEnabled ) archiver ).setArchiveFinalizers( null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }

        void expectSetArchiverFilters()
        {
            ( ( FilterEnabled ) archiver ).setArchiveFilters( null );
            archiverControl.setMatcher( MockControl.ALWAYS_MATCHER );
        }

        void expectGetArchiver( String format, Class archiverClass )
        {
            createArchiver( archiverClass );
            
            try
            {
                archiverManager.getArchiver( format );
            }
            catch ( NoSuchArchiverException e )
            {
                Assert.fail( "should never happen" );
            }
            
            control.setReturnValue( archiver );
        }

        void expectGetArchiver( String format, Archiver archiver )
            throws NoSuchArchiverException
        {
            archiverManager.getArchiver( format );
            control.setReturnValue( archiver );
        }
    }

    private static final class TestTarArchiver
        extends TarArchiver
    {

        boolean archiveCreated;

        boolean optionsRetrieved;

        TarCompressionMethod compressionMethod;

        int defaultDirMode;

        int defaultFileMode;

        TarLongFileMode longFileMode;

        TarOptions options;

        public void createArchive()
            throws ArchiverException, IOException
        {
            archiveCreated = true;
            super.createArchive();
        }

        public TarOptions getOptions()
        {
            optionsRetrieved = true;
            return super.getOptions();
        }

        public void setCompression( TarCompressionMethod mode )
        {
            compressionMethod = mode;
            super.setCompression( mode );
        }

        public void setDefaultDirectoryMode( int mode )
        {
            defaultDirMode = mode;
            super.setDefaultDirectoryMode( mode );
        }

        public void setDefaultFileMode( int mode )
        {
            defaultFileMode = mode;
            super.setDefaultFileMode( mode );
        }

        public void setLongfile( TarLongFileMode mode )
        {
            longFileMode = mode;
            super.setLongfile( mode );
        }

        public void setOptions( TarOptions options )
        {
            this.options = options;
            super.setOptions( options );
        }

    }

    private static final class TestWarArchiver
        extends WarArchiver
    {

        boolean ignoreWebxml;

        public void setIgnoreWebxml( boolean ignore )
        {
            ignoreWebxml = ignore;
            super.setIgnoreWebxml( ignore );
        }

    }

    interface TestFinalizerFilteredArchiver
        extends Archiver, FilterEnabled, FinalizerEnabled
    {

    }

}
