package org.apache.maven.plugins.assembly.utils;

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

import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LineEndingsUtilsTest
{

    private static final String CRLF = "\r\n";

    private static final String LF = "\n";

    @Test
    public void shouldWorkCauseWeTestJdkEnumConversion()
    {
        LineEndings lineEnding = LineEndings.valueOf( "windows" );
        assertEquals( CRLF, lineEnding.getLineEndingCharacters() );
    }

    @Test
    public void shouldReturnDosLineEnding()
    {
        assertEquals( CRLF, LineEndings.windows.getLineEndingCharacters() );
        assertEquals( CRLF, LineEndings.dos.getLineEndingCharacters() );
        assertEquals( CRLF, LineEndings.crlf.getLineEndingCharacters() );
    }

    @Test
    public void shouldReturnUnixLineEnding()
    {
        assertEquals( LF, LineEndings.unix.getLineEndingCharacters() );
        assertEquals( LF, LineEndings.lf.getLineEndingCharacters() );
    }

    @Test
    public void shouldReturnNullAsLineEndingForKeep()
    {
        assertEquals( null, LineEndings.keep.getLineEndingCharacters() );
    }

    @Test
    public void testGetLineEndingChars_ShouldReturnDosLineEnding()
        throws AssemblyFormattingException
    {
        assertEquals( "\r\n", LineEndingsUtils.getLineEndingCharacters( "windows" ) );
        assertEquals( "\r\n", LineEndingsUtils.getLineEndingCharacters( "dos" ) );
        assertEquals( "\r\n", LineEndingsUtils.getLineEndingCharacters( "crlf" ) );
    }

    @Test
    public void testGetLineEndingChars_ShouldReturnUnixLineEnding()
        throws AssemblyFormattingException
    {
        assertEquals( "\n", LineEndingsUtils.getLineEndingCharacters( "unix" ) );
        assertEquals( "\n", LineEndingsUtils.getLineEndingCharacters( "lf" ) );
    }

    @Test
    public void testGetLineEndingChars_ShouldReturnNullLineEnding()
        throws AssemblyFormattingException
    {
        assertNull( LineEndingsUtils.getLineEndingCharacters( "keep" ) );
    }

    @Test( expected = AssemblyFormattingException.class )
    public void testGetLineEndingChars_ShouldThrowFormattingExceptionWithInvalidHint()
        throws AssemblyFormattingException
    {
        LineEndingsUtils.getLineEndingCharacters( "invalid" );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceLFWithCRLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.";

        testConversion( test, check, LineEndings.crlf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceLFWithCRLFAtEOF()
        throws IOException
    {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, LineEndings.crlf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceCRLFWithLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.";

        testConversion( test, check, LineEndings.lf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceCRLFWithLFAtEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.\n";

        testConversion( test, check, LineEndings.lf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceLFWithLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \ntest.";

        testConversion( test, check, LineEndings.lf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceLFWithLFAtEOF()
        throws IOException
    {
        String test = "This is a \ntest.\n";
        String check = "This is a \ntest.\n";

        testConversion( test, check, LineEndings.lf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceCRLFWithCRLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \r\ntest.";

        testConversion( test, check, LineEndings.crlf, null );
    }

    @Test
    public void testConvertLineEndings_ShouldReplaceCRLFWithCRLFAtEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, LineEndings.crlf, null );
    }

    @Test
    public void testConvertLineEndings_LFToCRLFNoEOFForceEOF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, LineEndings.crlf, true );
    }

    @Test
    public void testConvertLineEndings_LFToCRLFWithEOFForceEOF()
        throws IOException
    {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion( test, check, LineEndings.crlf, true );
    }

    @Test
    public void testConvertLineEndings_LFToCRLFNoEOFStripEOF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.";

        testConversion( test, check, LineEndings.crlf, false );
    }

    @Test
    public void testConvertLineEndings_LFToCRLFWithEOFStripEOF()
        throws IOException
    {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.";

        testConversion( test, check, LineEndings.crlf, false );
    }

    @Test
    public void testConvertLineEndings_CRLFToLFNoEOFForceEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.\n";

        testConversion( test, check, LineEndings.lf, true );
    }

    @Test
    public void testConvertLineEndings_CRLFToLFWithEOFForceEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.\n";

        testConversion( test, check, LineEndings.lf, true );
    }

    @Test
    public void testConvertLineEndings_CRLFToLFNoEOFStripEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.";

        testConversion( test, check, LineEndings.lf, false );
    }

    @Test
    public void testConvertLineEndings_CRLFToLFWithEOFStripEOF()
        throws IOException
    {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.";

        testConversion( test, check, LineEndings.lf, false );
    }

    private void testConversion( String test, String check, LineEndings lineEndingChars, Boolean eof )
        throws IOException
    {
        File source = File.createTempFile( "line-conversion-test-in.", "" );
        source.deleteOnExit();
        File dest = File.createTempFile( "line-conversion-test-out.", "" );
        dest.deleteOnExit();

        FileWriter sourceWriter = null;
        StringReader sourceReader = new StringReader( test );
        try
        {
            sourceWriter = new FileWriter( source );

            IOUtil.copy( sourceReader, sourceWriter );

            sourceWriter.close();
            sourceWriter = null;
        }
        finally
        {
            IOUtil.close( sourceWriter );
        }

        // Using platform encoding for the conversion tests in this class is OK
        LineEndingsUtils.convertLineEndings( source, dest, lineEndingChars, eof, null );

        FileReader destReader = null;
        StringWriter destWriter = new StringWriter();
        try
        {
            destReader = new FileReader( dest );

            IOUtil.copy( destReader, destWriter );

            assertEquals( check, destWriter.toString() );

            destWriter.close();
            destWriter = null;
            destReader.close();
            destReader = null;
        }
        finally
        {
            IOUtil.close( destWriter );
            IOUtil.close( destReader );
        }
    }

}
