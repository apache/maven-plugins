package org.apache.maven.plugin.assembly.testutils;

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
        StringBuilder buffer = new StringBuilder();

        BufferedReader reader = new BufferedReader( new FileReader( file ) ); // platform encoding

        String line;

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
