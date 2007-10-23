package org.apache.maven.plugin.assembly.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.maven.plugin.assembly.archive.ArchiveExpansionException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.IOUtil;
import org.easymock.MockControl;

public class AssemblyFileUtilsTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "file-utils.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testUnpack_ShouldSetSourceAndDestinationAndCallExtract()
        throws IOException, ArchiveExpansionException, NoSuchArchiverException
    {
        MockManager mockManager = new MockManager();

        File source = fileManager.createTempFile();
        File destDir = fileManager.createTempDir();

        MockControl unarchiverCtl = MockControl.createControl( UnArchiver.class );
        mockManager.add( unarchiverCtl );

        UnArchiver unarchiver = (UnArchiver) unarchiverCtl.getMock();

        MockControl archiverManagerCtl = MockControl.createControl( ArchiverManager.class );
        mockManager.add( archiverManagerCtl );

        ArchiverManager archiverManager = (ArchiverManager) archiverManagerCtl.getMock();

        try
        {
            archiverManager.getUnArchiver( source );
            archiverManagerCtl.setReturnValue( unarchiver );
        }
        catch ( NoSuchArchiverException e )
        {
            fail( "Should never happen." );
        }

        unarchiver.setSourceFile( source );
        unarchiver.setDestDirectory( destDir );

        try
        {
            unarchiver.extract();
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AssemblyFileUtils.unpack( source, destDir, archiverManager );

        mockManager.verifyAll();
    }

    public void testGetLineEndingChars_ShouldReturnDosLineEnding()
        throws AssemblyFormattingException
    {
        assertEquals( "\r\n", AssemblyFileUtils.getLineEndingCharacters( "dos" ) );
        assertEquals( "\r\n", AssemblyFileUtils.getLineEndingCharacters( "crlf" ) );
    }

    public void testGetLineEndingChars_ShouldReturnUnixLineEnding()
        throws AssemblyFormattingException
    {
        assertEquals( "\n", AssemblyFileUtils.getLineEndingCharacters( "unix" ) );
        assertEquals( "\n", AssemblyFileUtils.getLineEndingCharacters( "lf" ) );
    }

    public void testGetLineEndingChars_ShouldReturnNullLineEnding()
        throws AssemblyFormattingException
    {
        assertNull( AssemblyFileUtils.getLineEndingCharacters( "keep" ) );
    }

    public void testGetLineEndingChars_ShouldThrowFormattingExceptionWithInvalidHint()
    {
        try
        {
            AssemblyFileUtils.getLineEndingCharacters( "invalid" );

            fail( "Invalid line-ending hint should throw a formatting exception." );
        }
        catch ( AssemblyFormattingException e )
        {
        }
    }

    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceLFWithCRLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, "\r\n" );
    }

    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceCRLFWithLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.\n";

        testConversion( test, check, "\n" );
    }

    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceLFWithLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \ntest.\n";

        testConversion( test, check, "\n" );
    }

    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceCRLFWithCRLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, "\r\n" );
    }

    private void testConversion( String test, String check, String lineEndingChars )
        throws IOException
    {
        File dest = File.createTempFile( "line-conversion-test.", "" );
        dest.deleteOnExit();

        AssemblyFileUtils.convertLineEndings( new StringReader( test ), dest, lineEndingChars );

        FileReader reader = null;
        StringWriter writer = new StringWriter();

        try
        {
            reader = new FileReader( dest );

            IOUtil.copy( reader, writer );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertEquals( check, writer.toString() );
    }

}
