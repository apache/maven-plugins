package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

public class AssemblyFileUtilsTest
    extends TestCase
{

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
    public void testConvertLineEndings_ShouldReplaceLFWithCRLF() throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.\r\n";
        
        testConversion( test, check, "\r\n" );
    }
    
    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceCRLFWithLF() throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.\n";
        
        testConversion( test, check, "\n" );
    }
    
    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceLFWithLF() throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \ntest.\n";
        
        testConversion( test, check, "\n" );
    }
    
    // TODO: Fix the end-of-document problem with line-ending conversions.
    public void testConvertLineEndings_ShouldReplaceCRLFWithCRLF() throws IOException
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
