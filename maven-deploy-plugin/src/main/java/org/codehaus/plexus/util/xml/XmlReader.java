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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.MessageFormat;

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
 * @deprecated use XmlStreamReader
 * @since 1.4.3
 * @deprecated TO BE REMOVED from here when plexus-utils is upgraded to 1.4.5+ (and prerequisite upgraded to Maven 2.0.6)
 */
public class XmlReader extends Reader
{
    private static final int BUFFER_SIZE = 4096;

    private static final String UTF_8 = "UTF-8";

    private static final String US_ASCII = "US-ASCII";

    private static final String UTF_16BE = "UTF-16BE";

    private static final String UTF_16LE = "UTF-16LE";

    private static final String UTF_16 = "UTF-16";

    private static final String EBCDIC = "CP1047";

    private static String _staticDefaultEncoding = null;

    private Reader _reader;

    private String _encoding;

    private String _defaultEncoding;

    /**
     * Sets the default encoding to use if none is set in HTTP content-type, XML prolog and the rules based on
     * content-type are not adequate. <p/> If it is set to NULL the content-type based rules are used. <p/> By default
     * it is NULL. <p/>
     * 
     * @param encoding
     *            charset encoding to default to.
     */
    public static void setDefaultEncoding( String encoding )
    {
        _staticDefaultEncoding = encoding;
    }

    /**
     * Returns the default encoding to use if none is set in HTTP content-type, XML prolog and the rules based on
     * content-type are not adequate. <p/> If it is NULL the content-type based rules are used. <p/>
     * 
     * @return the default encoding to use.
     */
    public static String getDefaultEncoding()
    {
        return _staticDefaultEncoding;
    }

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
    public XmlReader( File file ) throws IOException
    {
        this( new FileInputStream( file ) );
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
    public XmlReader( InputStream is ) throws IOException
    {
        this( is, true );
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
    public XmlReader( InputStream is, boolean lenient ) throws IOException, XmlStreamReaderException
    {
        _defaultEncoding = _staticDefaultEncoding;
        try
        {
            doRawStream( is, lenient );
        }
        catch ( XmlStreamReaderException ex )
        {
            if ( !lenient )
            {
                throw ex;
            }
            else
            {
                doLenientDetection( null, ex );
            }
        }
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
    public XmlReader( URL url ) throws IOException
    {
        this( url.openConnection() );
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
    public XmlReader( URLConnection conn ) throws IOException
    {
        _defaultEncoding = _staticDefaultEncoding;
        boolean lenient = true;
        if ( conn instanceof HttpURLConnection )
        {
            try
            {
                doHttpStream( conn.getInputStream(), conn.getContentType(), lenient );
            }
            catch ( XmlStreamReaderException ex )
            {
                doLenientDetection( conn.getContentType(), ex );
            }
        }
        else if ( conn.getContentType() != null )
        {
            try
            {
                doHttpStream( conn.getInputStream(), conn.getContentType(), lenient );
            }
            catch ( XmlStreamReaderException ex )
            {
                doLenientDetection( conn.getContentType(), ex );
            }
        }
        else
        {
            try
            {
                doRawStream( conn.getInputStream(), lenient );
            }
            catch ( XmlStreamReaderException ex )
            {
                doLenientDetection( null, ex );
            }
        }
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
    public XmlReader( InputStream is, String httpContentType ) throws IOException
    {
        this( is, httpContentType, true );
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
    public XmlReader( InputStream is, String httpContentType, boolean lenient, String defaultEncoding )
        throws IOException, XmlStreamReaderException
    {
        _defaultEncoding = ( defaultEncoding == null ) ? _staticDefaultEncoding : defaultEncoding;
        try
        {
            doHttpStream( is, httpContentType, lenient );
        }
        catch ( XmlStreamReaderException ex )
        {
            if ( !lenient )
            {
                throw ex;
            }
            else
            {
                doLenientDetection( httpContentType, ex );
            }
        }
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
    public XmlReader( InputStream is, String httpContentType, boolean lenient ) throws IOException, XmlStreamReaderException
    {
        this( is, httpContentType, lenient, null );
    }

    private void doLenientDetection( String httpContentType, XmlStreamReaderException ex ) throws IOException
    {
        if ( httpContentType != null )
        {
            if ( httpContentType.startsWith( "text/html" ) )
            {
                httpContentType = httpContentType.substring( "text/html".length() );
                httpContentType = "text/xml" + httpContentType;
                try
                {
                    doHttpStream( ex.getInputStream(), httpContentType, true );
                    ex = null;
                }
                catch ( XmlStreamReaderException ex2 )
                {
                    ex = ex2;
                }
            }
        }
        if ( ex != null )
        {
            String encoding = ex.getXmlEncoding();
            if ( encoding == null )
            {
                encoding = ex.getContentTypeEncoding();
            }
            if ( encoding == null )
            {
                encoding = ( _defaultEncoding == null ) ? UTF_8 : _defaultEncoding;
            }
            prepareReader( ex.getInputStream(), encoding );
        }
    }

    /**
     * Returns the charset encoding of the XmlReader.
     * <p>
     * 
     * @return charset encoding.
     * 
     */
    public String getEncoding()
    {
        return _encoding;
    }

    public int read( char[] buf, int offset, int len ) throws IOException
    {
        return _reader.read( buf, offset, len );
    }

    /**
     * Closes the XmlReader stream.
     * <p>
     * 
     * @throws IOException
     *             thrown if there was a problem closing the stream.
     * 
     */
    public void close() throws IOException
    {
        _reader.close();
    }

    private void doRawStream( InputStream is, boolean lenient ) throws IOException
    {
        BufferedInputStream pis = new BufferedInputStream( is, BUFFER_SIZE );
        String bomEnc = getBOMEncoding( pis );
        String xmlGuessEnc = getXMLGuessEncoding( pis );
        String xmlEnc = getXmlProlog( pis, xmlGuessEnc );
        String encoding = calculateRawEncoding( bomEnc, xmlGuessEnc, xmlEnc, pis );
        prepareReader( pis, encoding );
    }

    private void doHttpStream( InputStream is, String httpContentType, boolean lenient ) throws IOException
    {
        BufferedInputStream pis = new BufferedInputStream( is, BUFFER_SIZE );
        String cTMime = getContentTypeMime( httpContentType );
        String cTEnc = getContentTypeEncoding( httpContentType );
        String bomEnc = getBOMEncoding( pis );
        String xmlGuessEnc = getXMLGuessEncoding( pis );
        String xmlEnc = getXmlProlog( pis, xmlGuessEnc );
        String encoding = calculateHttpEncoding( cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, pis, lenient );
        prepareReader( pis, encoding );
    }

    private void prepareReader( InputStream is, String encoding ) throws IOException
    {
        _reader = new InputStreamReader( is, encoding );
        _encoding = encoding;
    }

    // InputStream is passed for XmlStreamReaderException creation only
    private String calculateRawEncoding( String bomEnc, String xmlGuessEnc, String xmlEnc, InputStream is )
        throws IOException
    {
        String encoding;
        if ( bomEnc == null )
        {
            if ( xmlGuessEnc == null || xmlEnc == null )
            {
                encoding = ( _defaultEncoding == null ) ? UTF_8 : _defaultEncoding;
            }
            else if ( xmlEnc.equals( UTF_16 ) && ( xmlGuessEnc.equals( UTF_16BE ) || xmlGuessEnc.equals( UTF_16LE ) ) )
            {
                encoding = xmlGuessEnc;
            }
            else
            {
                encoding = xmlEnc;
            }
        }
        else if ( bomEnc.equals( UTF_8 ) )
        {
            if ( xmlGuessEnc != null && !xmlGuessEnc.equals( UTF_8 ) )
            {
                throw new XmlStreamReaderException( RAW_EX_1.format( new Object[] { bomEnc, xmlGuessEnc, xmlEnc } ), bomEnc,
                                              xmlGuessEnc, xmlEnc, is );
            }
            if ( xmlEnc != null && !xmlEnc.equals( UTF_8 ) )
            {
                throw new XmlStreamReaderException( RAW_EX_1.format( new Object[] { bomEnc, xmlGuessEnc, xmlEnc } ), bomEnc,
                                              xmlGuessEnc, xmlEnc, is );
            }
            encoding = UTF_8;
        }
        else if ( bomEnc.equals( UTF_16BE ) || bomEnc.equals( UTF_16LE ) )
        {
            if ( xmlGuessEnc != null && !xmlGuessEnc.equals( bomEnc ) )
            {
                throw new IOException( RAW_EX_1.format( new Object[] { bomEnc, xmlGuessEnc, xmlEnc } ) );
            }
            if ( xmlEnc != null && !xmlEnc.equals( UTF_16 ) && !xmlEnc.equals( bomEnc ) )
            {
                throw new XmlStreamReaderException( RAW_EX_1.format( new Object[] { bomEnc, xmlGuessEnc, xmlEnc } ), bomEnc,
                                              xmlGuessEnc, xmlEnc, is );
            }
            encoding = bomEnc;
        }
        else
        {
            throw new XmlStreamReaderException( RAW_EX_2.format( new Object[] { bomEnc, xmlGuessEnc, xmlEnc } ), bomEnc,
                                          xmlGuessEnc, xmlEnc, is );
        }
        return encoding;
    }

    // InputStream is passed for XmlStreamReaderException creation only
    private String calculateHttpEncoding( String cTMime, String cTEnc, String bomEnc, String xmlGuessEnc,
                                          String xmlEnc, InputStream is, boolean lenient ) throws IOException
    {
        String encoding;
        if ( lenient & xmlEnc != null )
        {
            encoding = xmlEnc;
        }
        else
        {
            boolean appXml = isAppXml( cTMime );
            boolean textXml = isTextXml( cTMime );
            if ( appXml || textXml )
            {
                if ( cTEnc == null )
                {
                    if ( appXml )
                    {
                        encoding = calculateRawEncoding( bomEnc, xmlGuessEnc, xmlEnc, is );
                    }
                    else
                    {
                        encoding = ( _defaultEncoding == null ) ? US_ASCII : _defaultEncoding;
                    }
                }
                else if ( bomEnc != null && ( cTEnc.equals( UTF_16BE ) || cTEnc.equals( UTF_16LE ) ) )
                {
                    throw new XmlStreamReaderException( HTTP_EX_1.format( new Object[] { cTMime, cTEnc, bomEnc, xmlGuessEnc,
                        xmlEnc } ), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, is );
                }
                else if ( cTEnc.equals( UTF_16 ) )
                {
                    if ( bomEnc != null && bomEnc.startsWith( UTF_16 ) )
                    {
                        encoding = bomEnc;
                    }
                    else
                    {
                        throw new XmlStreamReaderException( HTTP_EX_2.format( new Object[] { cTMime, cTEnc, bomEnc,
                            xmlGuessEnc, xmlEnc } ), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, is );
                    }
                }
                else
                {
                    encoding = cTEnc;
                }
            }
            else
            {
                throw new XmlStreamReaderException( HTTP_EX_3.format( new Object[] { cTMime, cTEnc, bomEnc, xmlGuessEnc,
                    xmlEnc } ), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, is );
            }
        }
        return encoding;
    }

    // returns MIME type or NULL if httpContentType is NULL
    private static String getContentTypeMime( String httpContentType )
    {
        String mime = null;
        if ( httpContentType != null )
        {
            int i = httpContentType.indexOf( ";" );
            mime = ( ( i == -1 ) ? httpContentType : httpContentType.substring( 0, i ) ).trim();
        }
        return mime;
    }

    private static final Pattern CHARSET_PATTERN = Pattern.compile( "charset=([.[^; ]]*)" );

    // returns charset parameter value, NULL if not present, NULL if httpContentType is NULL
    private static String getContentTypeEncoding( String httpContentType )
    {
        String encoding = null;
        if ( httpContentType != null )
        {
            int i = httpContentType.indexOf( ";" );
            if ( i > -1 )
            {
                String postMime = httpContentType.substring( i + 1 );
                Matcher m = CHARSET_PATTERN.matcher( postMime );
                encoding = ( m.find() ) ? m.group( 1 ) : null;
                encoding = ( encoding != null ) ? encoding.toUpperCase() : null;
            }
        }
        return encoding;
    }

    // returns the BOM in the stream, NULL if not present,
    // if there was BOM the in the stream it is consumed
    private static String getBOMEncoding( BufferedInputStream is ) throws IOException
    {
        String encoding = null;
        int[] bytes = new int[3];
        is.mark( 3 );
        bytes[0] = is.read();
        bytes[1] = is.read();
        bytes[2] = is.read();

        if ( bytes[0] == 0xFE && bytes[1] == 0xFF )
        {
            encoding = UTF_16BE;
            is.reset();
            is.read();
            is.read();
        }
        else if ( bytes[0] == 0xFF && bytes[1] == 0xFE )
        {
            encoding = UTF_16LE;
            is.reset();
            is.read();
            is.read();
        }
        else if ( bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF )
        {
            encoding = UTF_8;
        }
        else
        {
            is.reset();
        }
        return encoding;
    }

    // returns the best guess for the encoding by looking the first bytes of the stream, '<?'
    private static String getXMLGuessEncoding( BufferedInputStream is ) throws IOException
    {
        String encoding = null;
        int[] bytes = new int[4];
        is.mark( 4 );
        bytes[0] = is.read();
        bytes[1] = is.read();
        bytes[2] = is.read();
        bytes[3] = is.read();
        is.reset();

        if ( bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F )
        {
            encoding = UTF_16BE;
        }
        else if ( bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00 )
        {
            encoding = UTF_16LE;
        }
        else if ( bytes[0] == 0x3C && bytes[1] == 0x3F && bytes[2] == 0x78 && bytes[3] == 0x6D )
        {
            encoding = UTF_8;
        }
        else if ( bytes[0] == 0x4C && bytes[1] == 0x6F && bytes[2] == 0xA7 && bytes[3] == 0x94 )
        {
            encoding = EBCDIC;
        }
        return encoding;
    }

    static final Pattern ENCODING_PATTERN =
        Pattern.compile( "<\\?xml.*encoding[\\s]*=[\\s]*((?:\".[^\"]*\")|(?:'.[^']*'))", Pattern.MULTILINE );

    // returns the encoding declared in the <?xml encoding=...?>, NULL if none
    private static String getXmlProlog( BufferedInputStream is, String guessedEnc ) throws IOException
    {
        String encoding = null;
        if ( guessedEnc != null )
        {
            byte[] bytes = new byte[BUFFER_SIZE];
            is.mark( BUFFER_SIZE );
            int offset = 0;
            int max = BUFFER_SIZE;
            int c = is.read( bytes, offset, max );
            int firstGT = -1;
            String xmlProlog = null;
            while ( c != -1 && firstGT == -1 && offset < BUFFER_SIZE )
            {
                offset += c;
                max -= c;
                c = is.read( bytes, offset, max );
                xmlProlog = new String( bytes, 0, offset, guessedEnc );
                firstGT = xmlProlog.indexOf( '>' );
            }
            if ( firstGT == -1 )
            {
                if ( c == -1 )
                {
                    throw new IOException( "Unexpected end of XML stream" );
                }
                else
                {
                    throw new IOException( "XML prolog or ROOT element not found on first " + offset + " bytes" );
                }
            }
            int bytesRead = offset;
            if ( bytesRead > 0 )
            {
                is.reset();
                BufferedReader bReader = new BufferedReader( new StringReader( xmlProlog.substring( 0, firstGT ) ) );
                StringBuffer prolog = new StringBuffer();
                String line = bReader.readLine();
                while ( line != null )
                {
                    prolog.append( line );
                    line = bReader.readLine();
                }
                Matcher m = ENCODING_PATTERN.matcher( prolog );
                if ( m.find() )
                {
                    encoding = m.group( 1 ).toUpperCase();
                    encoding = encoding.substring( 1, encoding.length() - 1 );
                }
            }
        }
        return encoding;
    }

    // indicates if the MIME type belongs to the APPLICATION XML family
    private static boolean isAppXml( String mime )
    {
        return mime != null
                        && ( mime.equals( "application/xml" ) || mime.equals( "application/xml-dtd" )
                                        || mime.equals( "application/xml-external-parsed-entity" ) || ( mime.startsWith( "application/" ) && mime.endsWith( "+xml" ) ) );
    }

    // indicates if the MIME type belongs to the TEXT XML family
    private static boolean isTextXml( String mime )
    {
        return mime != null
                        && ( mime.equals( "text/xml" ) || mime.equals( "text/xml-external-parsed-entity" ) || ( mime.startsWith( "text/" ) && mime.endsWith( "+xml" ) ) );
    }

    private static final MessageFormat RAW_EX_1 =
        new MessageFormat( "Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] encoding mismatch" );

    private static final MessageFormat RAW_EX_2 =
        new MessageFormat( "Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] unknown BOM" );

    private static final MessageFormat HTTP_EX_1 =
        new MessageFormat(
                           "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], BOM must be NULL" );

    private static final MessageFormat HTTP_EX_2 =
        new MessageFormat(
                           "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], encoding mismatch" );

    private static final MessageFormat HTTP_EX_3 =
        new MessageFormat(
                           "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], Invalid MIME" );

}
