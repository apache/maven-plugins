package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public final class AssemblyFileUtils
{

    private AssemblyFileUtils()
    {
    }

    /**
     * NOTE: It is the responsibility of the caller to close the source Reader instance.
     * @param lineEndings This is the result of the getLineEndingChars(..) method in this utility class; the actual
     *   line-ending characters.
     */
    public static void convertLineEndings( Reader source, File dest, String lineEndings )
        throws IOException
    {
        BufferedWriter out = null;
        BufferedReader bufferedSource = null;
        try
        {
            if ( source instanceof BufferedReader )
            {
                bufferedSource = (BufferedReader) source;
            }
            else
            {
                bufferedSource = new BufferedReader( source );
            }
            
            out = new BufferedWriter( new FileWriter( dest ) );

            String line;

            do
            {
                line = bufferedSource.readLine();
                if ( line != null )
                {
                    out.write( line );
                    out.write( lineEndings );
                }
            } while ( line != null );

            out.flush();
        }
        finally
        {
            IOUtil.close( out );
        }
    }

    public static String getLineEndingCharacters( String lineEnding )
        throws AssemblyFormattingException
    {
        String value = lineEnding;
        if ( lineEnding != null )
        {
            if ( "keep".equals( lineEnding ) )
            {
                value = null;
            }
            else if ( "dos".equals( lineEnding ) || "crlf".equals( lineEnding ) )
            {
                value = "\r\n";
            }
            else if ( "unix".equals( lineEnding ) || "lf".equals( lineEnding ) )
            {
                value = "\n";
            }
            else
            {
                throw new AssemblyFormattingException( "Illlegal lineEnding specified: '" + lineEnding + "'" );
            }
        }

        return value;
    }
}
