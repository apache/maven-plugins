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
import java.nio.channels.FileChannel;

import org.apache.maven.plugin.assembly.archive.ArchiveExpansionException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.IOUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @version $Id$
 */
public final class AssemblyFileUtils
{

    public static final String LINE_ENDING_KEEP = "keep";
    public static final String LINE_ENDING_DOS = "dos";
    public static final String LINE_ENDING_WINDOWS = "windows";
    public static final String LINE_ENDING_UNIX = "unix";
    public static final String LINE_ENDING_CRLF = "crlf";
    public static final String LINE_ENDING_LF = "lf";

    private AssemblyFileUtils()
    {
    }
    
    public static String makePathRelativeTo( String path, final File basedir )
    {
        if ( basedir == null )
        {
            return path;
        }

        if ( path == null )
        {
            return null;
        }

        path = path.trim();

        String base = basedir.getAbsolutePath();
        if ( path.startsWith( base ) )
        {
            path = path.substring( base.length() );
            if ( path.length() > 0 )
            {
                if ( path.startsWith( "/" ) || path.startsWith( "\\" ) )
                {
                    path = path.substring( 1 );
                }
            }
            
            if ( path.length() == 0 )
            {
                path = ".";
            }
        }

        if ( !new File( path ).isAbsolute() )
        {
            path = path.replace( '\\', '/' );
        }

        return path;
    }

    public static void verifyTempDirectoryAvailability( @Nonnull final File tempDir )
    {
        if ( !tempDir.exists() )
        {
            //noinspection ResultOfMethodCallIgnored
            tempDir.mkdirs();
        }
    }

    /**
     * Unpacks the archive file.
     *
     * @param source
     *            File to be unpacked.
     * @param destDir
     *            Location where to put the unpacked files.
     */
    public static void unpack( File source, File destDir, ArchiverManager archiverManager )
        throws ArchiveExpansionException, NoSuchArchiverException
    {
        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( source );

            unArchiver.setSourceFile( source );

            unArchiver.setDestDirectory( destDir );

            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + source + "to: " + destDir, e );
        }
    }

    /**
     * Converts the line endings of a file, writing a new file.
     * The encoding of reading and writing can be specified.
     * 
     * @param source The source file, not null
     * @param dest The destination file, not null
     * @param lineEndings This is the result of the getLineEndingChars(..) method in this utility class; the actual
     *   line-ending characters, not null.
     * @param atEndOfFile The end-of-file line ending,
     *  if true then the resulting file will have a new line at the end even if the input didn't have one,
     *  if false then the resulting file will have no new line at the end even if the input did have one,
     *  null to determine whether to have a new line at the end of the file based on the input file
     * @param encoding The encoding to use, null for platform encoding
     */
    public static void convertLineEndings( @Nonnull File source, @Nonnull File dest, String lineEndings, Boolean atEndOfFile, String encoding )
        throws IOException
    {
        // MASSEMBLY-637, MASSEMBLY-96
        // find characters at the end of the file
        // needed to preserve the last line ending
        // only check for LF (as CRLF also ends in LF)
        String eofChars = "";
        if ( atEndOfFile == null) {
            RandomAccessFile raf = null;
            try {
                if ( source.length() >= 1 ) {
                    raf = new RandomAccessFile( source, "r" );
                    raf.seek( source.length() - 1 );
                    byte last = raf.readByte();
                    if ( last == '\n' ) {
                      eofChars = lineEndings;
                    }
                }
            }
            finally
            {
                if ( raf != null ) {
                    try {
                        raf.close();
                    }
                    catch ( IOException ex )
                    {
                        // ignore
                    }
                }
            }
        } else if ( atEndOfFile.booleanValue() == true ) {
            eofChars = lineEndings;
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
                    out.write( lineEndings );
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

    @Nullable public static String getLineEndingCharacters( @Nullable String lineEnding )
        throws AssemblyFormattingException
    {
        String value = lineEnding;
        if ( lineEnding != null )
        {
            if ( LINE_ENDING_KEEP.equals( lineEnding ) )
            {
                value = null;
            }
            else if ( LINE_ENDING_DOS.equals( lineEnding ) || LINE_ENDING_WINDOWS.equals( lineEnding ) || LINE_ENDING_CRLF.equals( lineEnding ) )
            {
                value = "\r\n";
            }
            else if ( LINE_ENDING_UNIX.equals( lineEnding ) || LINE_ENDING_LF.equals( lineEnding ) )
            {
                value = "\n";
            }
            else
            {
                throw new AssemblyFormattingException( "Illegal lineEnding specified: '" + lineEnding + "'" );
            }
        }

        return value;
    }

    public static void copyFile( File src, File dst )
        throws IOException
    {
        FileChannel c1 = new RandomAccessFile( src, "r" ).getChannel();
        FileChannel c2 = new RandomAccessFile( dst, "rw" ).getChannel();

        long tCount = 0, size = c1.size();
        //noinspection StatementWithEmptyBody
        while ( ( tCount += c2.transferFrom( c1, 0, size - tCount ) ) < size )
            ;

        c1.close();
        c2.force( true );
        c2.close();
    }

    @Nonnull public static String normalizePath( @Nonnull String path )
    {
        return path.replace( '\\', '/' );
    }

}
