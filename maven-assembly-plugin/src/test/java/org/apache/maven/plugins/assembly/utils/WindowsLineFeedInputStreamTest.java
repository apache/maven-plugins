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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.codehaus.plexus.util.IOUtil;

public class WindowsLineFeedInputStreamTest
    extends TestCase
{

    public void testSimpleString()
        throws Exception
    {
        assertEquals( "abc\r\n", roundtrip( "abc" ) );
    }

    public void testInTheMiddleOfTheLine()
        throws Exception
    {
        assertEquals( "a\r\nbc\r\n", roundtrip( "a\r\nbc" ) );
    }

    public void testMultipleBlankLines()
        throws Exception
    {
        assertEquals( "a\r\n\r\nbc\r\n", roundtrip( "a\r\n\r\nbc" ) );
    }

    public void testTwoLinesAtEnd()
        throws Exception
    {
        assertEquals( "a\r\n\r\n", roundtrip( "a\r\n\r\n" ) );
    }

    public void testLinuxLinefeeds()
        throws Exception
    {
        final String roundtrip = roundtrip( "ab\nc", false );
        assertEquals( "ab\r\nc", roundtrip );
    }


    public void testMalformed()
        throws Exception
    {
        assertEquals( "a\rbc", roundtrip( "a\rbc", false ) );
    }

    public void testRetainLineFeed()
        throws Exception
    {
        assertEquals( "a\r\n\r\n", roundtrip( "a\r\n\r\n", false ) );
        assertEquals( "a", roundtrip( "a", false ) );
    }

    private String roundtrip( String msg )
        throws IOException
    {
        return roundtrip( msg, true );
    }

    private String roundtrip( String msg, boolean ensure )
        throws IOException
    {
        ByteArrayInputStream baos = new ByteArrayInputStream( msg.getBytes() );
        
        WindowsLineFeedInputStream lf = null;
        try
        {
            lf = new WindowsLineFeedInputStream( baos, ensure );
            byte[] buf = new byte[ 100 ];
            final int read = lf.read( buf );
            final String string = new String( buf, 0, read );
            lf.close();
            lf = null;
            return string;
        }
        finally
        {
            IOUtil.close( lf );
        }
    }
}