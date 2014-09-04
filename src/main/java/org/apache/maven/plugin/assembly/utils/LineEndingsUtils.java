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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Line Ending class which contains convenience methods to change line endings.
 */
public final class LineEndingsUtils
{

    private LineEndingsUtils()
    {
        // prevent creations of instances.
    }

    /**
     * Converts the line endings of a file, writing a new file. The encoding of reading and writing can be specified.
     *
     * @param source The source file, not null
     * @param dest The destination file, not null
     * @param lineEndings This is the result of the getLineEndingChars(..) method in this utility class; the actual
     *            line-ending characters, not null.
     * @param atEndOfFile The end-of-file line ending, if true then the resulting file will have a new line at the end
     *            even if the input didn't have one, if false then the resulting file will have no new line at the end
     *            even if the input did have one, null to determine whether to have a new line at the end of the file
     *            based on the input file
     * @param encoding The encoding to use, null for platform encoding
     */
    public static void convertLineEndings( @Nonnull File source, @Nonnull File dest, LineEndings lineEndings,
                                           Boolean atEndOfFile, String encoding )
        throws IOException
    {
        // MASSEMBLY-637, MASSEMBLY-96
        // find characters at the end of the file
        // needed to preserve the last line ending
        // only check for LF (as CRLF also ends in LF)
        String eofChars = "";
        if ( atEndOfFile == null )
        {
            RandomAccessFile raf = null;
            try
            {
                if ( source.length() >= 1 )
                {
                    raf = new RandomAccessFile( source, "r" );
                    raf.seek( source.length() - 1 );
                    byte last = raf.readByte();
                    if ( last == '\n' )
                    {
                        eofChars = lineEndings.getLineEndingCharacters();
                    }
                }
            }
            finally
            {
                if ( raf != null )
                {
                    try
                    {
                        raf.close();
                    }
                    catch ( IOException ex )
                    {
                        // ignore
                    }
                }
            }
        }
        else if ( atEndOfFile == true )
        {
            eofChars = lineEndings.getLineEndingCharacters();
        }

        BufferedReader in = null;
        BufferedWriter out = null;
        try
        {
            if ( encoding == null )
            {
                // platform encoding
                in = new BufferedReader( new InputStreamReader( new FileInputStream( source ) ) );
                out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( dest ) ) );
            }
            else
            {
                // MASSEMBLY-371
                in = new BufferedReader( new InputStreamReader( new FileInputStream( source ), encoding ) );
                out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( dest ), encoding ) );
            }

            String line;

            line = in.readLine();
            while ( line != null )
            {
                out.write( line );
                line = in.readLine();
                if ( line != null )
                {
                    out.write( lineEndings.getLineEndingCharacters() );
                }
                else
                {
                    out.write( eofChars );
                }
            }

            out.flush();
        }
        finally
        {
            IOUtil.close( in );
            IOUtil.close( out );
        }
    }

    public static LineEndings getLineEnding( @Nullable String lineEnding )
        throws AssemblyFormattingException
    {
        LineEndings result = LineEndings.keep;
        if ( lineEnding != null )
        {
            try
            {
                result = LineEndings.valueOf( lineEnding );
            }
            catch ( IllegalArgumentException e )
            {
                throw new AssemblyFormattingException( "Illegal lineEnding specified: '" + lineEnding + "'", e );
            }
        }
        return result;
    }

    @Nullable
    public static String getLineEndingCharacters( @Nullable String lineEnding )
        throws AssemblyFormattingException
    {

        String value = lineEnding;

        if ( lineEnding != null )
        {

            try
            {
                value = LineEndings.valueOf( lineEnding ).getLineEndingCharacters();
            }
            catch ( IllegalArgumentException e )
            {
                throw new AssemblyFormattingException( "Illegal lineEnding specified: '" + lineEnding + "'", e );
            }
        }

        return value;
    }

}
