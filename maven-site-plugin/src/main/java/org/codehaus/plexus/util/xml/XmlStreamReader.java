/*
 * Copyright 2004 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.plexus.util.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Character stream that handles (or at least attemtps to) all the necessary Voodo to figure out the charset encoding of
 * the XML document within the stream.
 * <p>
 * IMPORTANT: This class is not related in any way to the org.xml.sax.XMLReader. This one IS a character stream.
 * <p>
 * All this has to be done without consuming characters from the stream, if not the XML parser will not recognized the
 * document as a valid XML. This is not 100% true, but it's close enough (UTF-8 BOM is not handled by all parsers right
 * now, XmlReader handles it and things work in all parsers).
 * <p>
 * The XmlReader class handles the charset encoding of XML documents in Files, raw streams and HTTP streams by offering
 * a wide set of constructors.
 * <P>
 * By default the charset encoding detection is lenient, the constructor with the lenient flag can be used for an script
 * (following HTTP MIME and XML specifications). All this is nicely explained by Mark Pilgrim in his blog, <a
 * href="http://diveintomark.org/archives/2004/02/13/xml-media-types"> Determining the character encoding of a feed</a>.
 * <p>
 * 
 * @author Alejandro Abdelnur
 * @version revision 1.17 taken on 26/06/2007 from Rome (see https://rome.dev.java.net/source/browse/rome/src/java/com/sun/syndication/io/XmlReader.java)
 * @since 1.4.4
 * @deprecated TO BE REMOVED from here when plexus-utils is upgraded to 1.4.5+ (and prerequisite upgraded to Maven 2.0.6)
 */
public class XmlStreamReader
extends XmlReader
{
    /**
     * Creates a Reader for a File.
     * <p>
     * It looks for the UTF-8 BOM first, if none sniffs the XML prolog charset, if this is also missing defaults to
     * UTF-8.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     * <p>
     * 
     * @param file
     *            File to create a Reader from.
     * @throws IOException
     *             thrown if there is a problem reading the file.
     * 
     */
    public XmlStreamReader( File file ) throws IOException
    {
        super( file );
    }

    /**
     * Creates a Reader for a raw InputStream.
     * <p>
     * It follows the same logic used for files.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     * <p>
     * 
     * @param is
     *            InputStream to create a Reader from.
     * @throws IOException
     *             thrown if there is a problem reading the stream.
     * 
     */
    public XmlStreamReader( InputStream is ) throws IOException
    {
        super( is );
    }

    /**
     * Creates a Reader for a raw InputStream.
     * <p>
     * It follows the same logic used for files.
     * <p>
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     * <p>
     * 
     * @param is
     *            InputStream to create a Reader from.
     * @param lenient
     *            indicates if the charset encoding detection should be relaxed.
     * @throws IOException
     *             thrown if there is a problem reading the stream.
     * @throws XmlStreamReaderException
     *             thrown if the charset encoding could not be determined according to the specs.
     * 
     */
    public XmlStreamReader( InputStream is, boolean lenient ) throws IOException, XmlStreamReaderException
    {
        super( is, lenient );
    }

    /**
     * Creates a Reader using the InputStream of a URL.
     * <p>
     * If the URL is not of type HTTP and there is not 'content-type' header in the fetched data it uses the same logic
     * used for Files.
     * <p>
     * If the URL is a HTTP Url or there is a 'content-type' header in the fetched data it uses the same logic used for
     * an InputStream with content-type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     * <p>
     * 
     * @param url
     *            URL to create a Reader from.
     * @throws IOException
     *             thrown if there is a problem reading the stream of the URL.
     * 
     */
    public XmlStreamReader( URL url ) throws IOException
    {
        super( url );
    }

    /**
     * Creates a Reader using the InputStream of a URLConnection.
     * <p>
     * If the URLConnection is not of type HttpURLConnection and there is not 'content-type' header in the fetched data
     * it uses the same logic used for files.
     * <p>
     * If the URLConnection is a HTTP Url or there is a 'content-type' header in the fetched data it uses the same logic
     * used for an InputStream with content-type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     * <p>
     * 
     * @param conn
     *            URLConnection to create a Reader from.
     * @throws IOException
     *             thrown if there is a problem reading the stream of the URLConnection.
     * 
     */
    public XmlStreamReader( URLConnection conn ) throws IOException
    {
        super( conn );
    }

    /**
     * Creates a Reader using an InputStream an the associated content-type header.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     * <p>
     * 
     * @param is
     *            InputStream to create the reader from.
     * @param httpContentType
     *            content-type header to use for the resolution of the charset encoding.
     * @throws IOException
     *             thrown if there is a problem reading the file.
     * 
     */
    public XmlStreamReader( InputStream is, String httpContentType ) throws IOException
    {
        super( is, httpContentType );
    }

    /**
     * Creates a Reader using an InputStream an the associated content-type header. This constructor is lenient
     * regarding the encoding detection.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     * <p>
     * 
     * @param is
     *            InputStream to create the reader from.
     * @param httpContentType
     *            content-type header to use for the resolution of the charset encoding.
     * @param lenient
     *            indicates if the charset encoding detection should be relaxed.
     * @throws IOException
     *             thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException
     *             thrown if the charset encoding could not be determined according to the specs.
     * 
     */
    public XmlStreamReader( InputStream is, String httpContentType, boolean lenient, String defaultEncoding )
        throws IOException, XmlStreamReaderException
    {
        super( is, httpContentType, lenient, defaultEncoding );
    }

    /**
     * Creates a Reader using an InputStream an the associated content-type header. This constructor is lenient
     * regarding the encoding detection.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     * <p>
     * 
     * @param is
     *            InputStream to create the reader from.
     * @param httpContentType
     *            content-type header to use for the resolution of the charset encoding.
     * @param lenient
     *            indicates if the charset encoding detection should be relaxed.
     * @throws IOException
     *             thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException
     *             thrown if the charset encoding could not be determined according to the specs.
     * 
     */
    public XmlStreamReader( InputStream is, String httpContentType, boolean lenient ) throws IOException, XmlStreamReaderException
    {
        super( is, httpContentType, lenient );
    }
}
