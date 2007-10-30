package org.codehaus.plexus.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.codehaus.plexus.util.xml.XmlStreamWriter;

/**
 * Utility to create Writers, with explicit encoding choice: platform default,
 * XML, or specified.
 * 
 * @author <a href="mailto:hboutemy@codehaus.org">Herve Boutemy</a>
 * @see Charset
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">Supported encodings</a>
 * @version $Id$
 * @since 1.4.4
 * @deprecated TO BE REMOVED from here when plexus-utils is upgraded to 1.4.5+ (and prerequisite upgraded to Maven 2.0.6)
 */
public class WriterFactory
{
    /**
     * ISO Latin Alphabet #1, also known as ISO-LATIN-1.
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String ISO_8859_1 = "ISO-8859-1";

    /**
     * Seven-bit ASCII, also known as ISO646-US, also known as the Basic Latin block of the Unicode character set.
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String US_ASCII = "US-ASCII";

    /**
     * Sixteen-bit Unicode Transformation Format, byte order specified by a mandatory initial byte-order mark (either
     * order accepted on input, big-endian used on output).
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String UTF_16 = "UTF-16";

    /**
     * Sixteen-bit Unicode Transformation Format, big-endian byte order.
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String UTF_16BE = "UTF-16BE";

    /**
     * Sixteen-bit Unicode Transformation Format, little-endian byte order.
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String UTF_16LE = "UTF-16LE";

    /**
     * Eight-bit Unicode Transformation Format.
     * Every implementation of the Java platform is required to support this character encoding.
     * @see Charset
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * The <code>file.encoding</code> System Property.
     */
    public static final String FILE_ENCODING = System.getProperty( "file.encoding" );
    
    /**
     * Create a new Writer with XML encoding detection rules.
     * @see XmlStreamWriter
     */
    public static XmlStreamWriter newXmlWriter( OutputStream out )
    throws IOException
    {
        return new XmlStreamWriter( out );
    }

    /**
     * Create a new Writer with XML encoding detection rules.
     * @see XmlStreamWriter
     */
    public static XmlStreamWriter newXmlWriter( File file )
    throws IOException
    {
        return new XmlStreamWriter( file );
    }

    /**
     * Create a new Writer with default plaform encoding.
     */
    public static Writer newPlatformWriter( OutputStream out )
    {
        return new OutputStreamWriter( out );
    }

    /**
     * Create a new Writer with default plaform encoding.
     */
    public static Writer newPlatformWriter( File file )
    throws IOException
    {
        return new FileWriter( file );
    }

    /**
     * Create a new Writer with specified encoding.
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">Supported encodings</a>
     */
    public static Writer newWriter( OutputStream out, String encoding )
    throws UnsupportedEncodingException
    {
        return new OutputStreamWriter( out, encoding );
    }

    /**
     * Create a new Writer with specified encoding.
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">Supported encodings</a>
     */
    public static Writer newWriter( File file, String encoding )
    throws UnsupportedEncodingException, FileNotFoundException
    {
        return newWriter( new FileOutputStream(file), encoding );
    }
}
