package org.codehaus.plexus.util.xml;

import java.io.InputStream;

/**
 * The XmlStreamReaderException is thrown by the XmlStreamReader constructors if the charset encoding can not be determined
 * according to the XML 1.0 specification and RFC 3023.
 * <p>
 * The exception returns the unconsumed InputStream to allow the application to do an alternate processing with the
 * stream. Note that the original InputStream given to the XmlStreamReader cannot be used as that one has been already read.
 * <p>
 * 
 * @author Alejandro Abdelnur
 * @version revision 1.1 taken on 26/06/2007 from Rome (see https://rome.dev.java.net/source/browse/rome/src/java/com/sun/syndication/io/XmlReaderException.java)
 * @deprecated TO BE REMOVED from here when plexus-utils is upgraded to 1.4.5+ (and prerequisite upgraded to Maven 2.0.6)
 */
public class XmlStreamReaderException extends XmlReaderException
{
    /**
     * Creates an exception instance if the charset encoding could not be determined.
     * <p>
     * Instances of this exception are thrown by the XmlReader.
     * <p>
     * 
     * @param msg
     *            message describing the reason for the exception.
     * @param bomEnc
     *            BOM encoding.
     * @param xmlGuessEnc
     *            XML guess encoding.
     * @param xmlEnc
     *            XML prolog encoding.
     * @param is
     *            the unconsumed InputStream.
     * 
     */
    public XmlStreamReaderException( String msg, String bomEnc, String xmlGuessEnc, String xmlEnc, InputStream is )
    {
        super( msg, bomEnc, xmlGuessEnc, xmlEnc, is );
    }

    /**
     * Creates an exception instance if the charset encoding could not be determined.
     * <p>
     * Instances of this exception are thrown by the XmlReader.
     * <p>
     * 
     * @param msg
     *            message describing the reason for the exception.
     * @param ctMime
     *            MIME type in the content-type.
     * @param ctEnc
     *            encoding in the content-type.
     * @param bomEnc
     *            BOM encoding.
     * @param xmlGuessEnc
     *            XML guess encoding.
     * @param xmlEnc
     *            XML prolog encoding.
     * @param is
     *            the unconsumed InputStream.
     * 
     */
    public XmlStreamReaderException( String msg, String ctMime, String ctEnc, String bomEnc, String xmlGuessEnc,
                               String xmlEnc, InputStream is )
    {
        super( msg, ctMime, ctEnc, bomEnc, xmlGuessEnc, xmlEnc, is );
    }
}
