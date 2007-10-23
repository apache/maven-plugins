package org.apache.maven.plugin.assembly.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.plexus.util.IOUtil;

public final class TestUtils
{

    private TestUtils()
    {
    }

    /**
     * Write a text to a file using platform encoding.
     */
    public static void writeToFile( File file, String testStr )
        throws IOException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter( file ); // platform encoding
            fw.write( testStr );
        }
        finally
        {
            IOUtil.close( fw );
        }
    }

    /**
     * Read file content using platform encoding and converting line endings to \\n.
     */
    public static String readFile( File file ) throws IOException
    {
        StringBuffer buffer = new StringBuffer();

        BufferedReader reader = new BufferedReader( new FileReader( file ) ); // platform encoding

        String line = null;

        while( ( line = reader.readLine() ) != null )
        {
            if ( buffer.length() > 0 )
            {
                buffer.append( '\n' );
            }

            buffer.append( line );
        }

        return buffer.toString();
    }

    public static String toString( Throwable error )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw );

        error.printStackTrace( pw );

        return sw.toString();
    }
}
