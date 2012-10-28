package org.apache.maven.plugin.assembly.utils;

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

    public void testConvertLineEndings_ShouldReplaceLFWithCRLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.";

        testConversion( test, check, "\r\n" );
    }

    public void testConvertLineEndings_ShouldReplaceCRLFWithLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.";

        testConversion( test, check, "\n" );
    }

    public void testConvertLineEndings_ShouldReplaceLFWithLF()
        throws IOException
    {
        String test = "This is a \ntest.";
        String check = "This is a \ntest.";

        testConversion( test, check, "\n" );
    }

    public void testConvertLineEndings_ShouldReplaceCRLFWithCRLF()
        throws IOException
    {
        String test = "This is a \r\ntest.";
        String check = "This is a \r\ntest.";

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
