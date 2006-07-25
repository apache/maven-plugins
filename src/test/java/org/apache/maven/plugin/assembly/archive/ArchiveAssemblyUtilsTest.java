package org.apache.maven.plugin.assembly.archive;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.easymock.MockControl;

public class ArchiveAssemblyUtilsTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private ArchiverManager archiverManager;

    private MockControl archiverManagerCtl;

    public void setUp()
    {
        fileManager = new TestFileManager( "ArchiveAssemblyUtils.test.", "" );

        mockManager = new MockManager();

        archiverManagerCtl = MockControl.createControl( ArchiverManager.class );
        mockManager.add( archiverManagerCtl );

        archiverManager = (ArchiverManager) archiverManagerCtl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testUnpack_ShouldSetSourceAndDestinationAndCallExtract()
        throws IOException, ArchiveExpansionException, NoSuchArchiverException
    {
        File source = fileManager.createTempFile();
        File destDir = fileManager.createTempDir();

        MockAndControlForUnarchiver mac = new MockAndControlForUnarchiver();

        try
        {
            archiverManager.getUnArchiver( source );
            archiverManagerCtl.setReturnValue( mac.unarchiver );
        }
        catch ( NoSuchArchiverException e )
        {
            fail( "Should never happen." );
        }

        mac.unarchiver.setSourceFile( source );
        mac.unarchiver.setDestDirectory( destDir );

        try
        {
            mac.unarchiver.extract();
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        ArchiveAssemblyUtils.unpack( source, destDir, archiverManager );

        mockManager.verifyAll();
    }

    public void testAddDirectory_ShouldNotAddDirectoryIfNonExistent()
        throws ArchiveCreationException
    {
        File dir = new File( System.getProperty( "java.io.tmpdir" ), "non-existent." + System.currentTimeMillis() );

        MockAndControlForArchiver mac = new MockAndControlForArchiver();

        mockManager.replayAll();

        ArchiveAssemblyUtils.addDirectory( mac.archiver, dir, null, null, null );

        mockManager.verifyAll();
    }

//    public void testAddDirectory_ShouldAddDirectoryWithoutHandlingComponentsXml()
//        throws ArchiveCreationException
//    {
//        File dir = fileManager.createTempDir();
//
//        TestFilter filter = new TestFilter();
//
//        MockAndControlForArchiver mac = new MockAndControlForArchiver();
//
//        try
//        {
//            mac.archiver.addDirectory( null, null, null, null );
//            mac.archiverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
//        }
//        catch ( ArchiverException e )
//        {
//            fail( "Should never happen." );
//        }
//
//        mockManager.replayAll();
//
//        ArchiveAssemblyUtils.addDirectory( mac.archiver, dir, "dir", null, null, filter );
//
//        assertNull( filter.getComponents() );
//
//        mockManager.verifyAll();
//    }
//
//    public void testAddDirectory_ShouldAddDirectoryContainingComponentsXmlAndHaveOneComponentsXmlFiltered()
//        throws ArchiveCreationException, IOException
//    {
//        File dir = fileManager.createTempDir();
//
//        fileManager.createFile( dir, ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH, "<component-set><components>"
//            + "<component><role>role</role>" + "<implementation>impl</implementation></component>"
//            + "</components></component-set>" );
//
//        TestFilter filter = new TestFilter();
//
//        MockAndControlForArchiver mac = new MockAndControlForArchiver();
//
//        try
//        {
//            mac.archiver.addDirectory( null, null, null, null );
//            mac.archiverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
//        }
//        catch ( ArchiverException e )
//        {
//            fail( "Should never happen." );
//        }
//
//        mockManager.replayAll();
//
//        ArchiveAssemblyUtils.addDirectory( mac.archiver, dir, "dir", null, null, filter );
//
//        Map components = filter.getComponents();
//        
//        assertNotNull( components );
//        assertNotNull( components.get( "role" ) );
//
//        mockManager.verifyAll();
//    }
//
//    private final class TestFilter
//        extends ComponentsXmlArchiverFileFilter
//    {
//        Map getComponents()
//        {
//            return components;
//        }
//    }

    private final class MockAndControlForArchiver
    {
        Archiver archiver;

        MockControl archiverCtl;

        public MockAndControlForArchiver()
        {
            archiverCtl = MockControl.createControl( Archiver.class );
            mockManager.add( archiverCtl );

            archiver = (Archiver) archiverCtl.getMock();
        }
    }

    private final class MockAndControlForUnarchiver
    {
        UnArchiver unarchiver;

        MockControl unarchiverCtl;

        public MockAndControlForUnarchiver()
        {
            unarchiverCtl = MockControl.createControl( UnArchiver.class );
            mockManager.add( unarchiverCtl );

            unarchiver = (UnArchiver) unarchiverCtl.getMock();
        }
    }

}
