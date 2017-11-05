package org.apache.maven.doxia.module.fo;

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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.TransformerException;

import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.markup.Markup;
import org.codehaus.plexus.util.WriterFactory;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXParseException;

import junit.framework.TestCase;

/**
 * Test FoAggregateSink.
 *
 * @author ltheussl
 * @version $Id: FoAggregateSinkTest.java 1563713 2014-02-02 20:55:04Z rfscholte $
 */
public class FoAggregateSinkTest
    extends TestCase
{
    private FoAggregateSink sink;

    private Writer writer;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        writer = new StringWriter();
    }
    
    protected String wrapXml( String xmlFragment )
    {
        return "<fo:fo xmlns:fo=\"" + FoMarkup.FO_NAMESPACE+ "\">" + xmlFragment + "</fo:fo>";
    }


    /**
     * Test of body method, of class FoAggregateSink.
     */
    public void testBody()
    {
        try
        {
            sink = new FoAggregateSink( writer );

            sink.setDocumentName( "folder/documentName.apt" );
            sink.setDocumentTitle( "documentTitle" );
            sink.body();
            sink.body_();
        }
        finally
        {
            sink.close();
        }

        assertTrue( writer.toString().indexOf( "<fo:block id=\"./folder/documentName\">" ) != -1 );
    }

    /**
     * Test of setDocumentName method, of class FoAggregateSink.
     */
    public void testSetDocumentName()
    {
        try
        {
            sink = new FoAggregateSink( writer );

            sink.setDocumentName( "folder\\documentName.boo" );
            sink.body();
        }
        finally
        {
            sink.close();
        }

        assertTrue( writer.toString().indexOf( "<fo:block id=\"./folder/documentName\">" ) != -1 );
    }
    
    /**
     * Test the FO PDF generation with some special characters in company name.
     */
    public void testSpecialCharacters()
        throws IOException, TransformerException
    {
        DocumentModel model = new DocumentModel();
        DocumentCover cover = new DocumentCover();

        cover.setCompanyName( "Partner & Friends" );
        cover.setCoverTitle( "A Masterpice in Encoding Theory <>&" );
        cover.setCoverSubTitle( "Some nice Encodings & <METHODS>" );
        cover.setProjectName( "A Masterpice in Encoding Theory <>&" );
        cover.setAuthor( "Partner & Friends" );
        model.setCover( cover );

        File foFile = File.createTempFile( "fo-test", ".fo" );
        File pdfFile = File.createTempFile( "fo-test", ".pdf" );
        try
        {

            sink = new FoAggregateSink( WriterFactory.newXmlWriter( foFile ) );

            sink.setDocumentModel( model );
            sink.setDocumentTitle( "A Masterpice in Encoding Theory <>&" );
            sink.beginDocument();
            sink.coverPage();
            // sink.toc();
            sink.endDocument();
        }
        finally
        {
            sink.close();
        }

        try
        {
            FoUtils.convertFO2PDF( foFile, pdfFile, null, model );
        }
        catch ( TransformerException e )
        {
            if ( ( e.getCause() != null ) && ( e.getCause() instanceof SAXParseException ) )
            {
                SAXParseException sax = (SAXParseException) e.getCause();

                StringBuffer sb = new StringBuffer();
                sb.append( "Error creating PDF from " ).append( foFile.getAbsolutePath() ).append( ":" ).append( sax.getLineNumber() ).append( ":" ).append( sax.getColumnNumber() ).append( "\n" );
                sb.append( e.getMessage() );

                throw new RuntimeException( sb.toString() );
            }

            throw new TransformerException( "Error creating PDF from " + foFile + ": " + e.getMessage() );
        }
    }

    /**
     * Test of figureGraphics method, of class FoAggregateSink.
     */
    public void testFigureGraphics() throws Exception
    {
        try
        {
            sink = new FoAggregateSink( writer );
            sink.setDocumentName( "./folder\\docName.xml" );
            sink.figureGraphics( "./../images/fig.png", null );
        }
        finally
        {
            sink.close();
        }

        String expected = "<fo:external-graphic src=\"./images/fig.png\" "
                        + "content-width=\"scale-down-to-fit\" "
                        + "content-height=\"scale-down-to-fit\" "
                        + "height=\"100%\" "
                        + "width=\"100%\"/>" + Markup.EOL;
        String actual = writer.toString();

        Diff diff = XMLUnit.compareXML( wrapXml( expected ), wrapXml( actual ) );
        assertTrue( "Wrong figure!", diff.identical() );
    }

    /**
     * Test of anchor method, of class FoAggregateSink.
     */
    public void testAnchor()
    {
        try
        {
            sink = new FoAggregateSink( writer );
            sink.anchor( "invalid Anchor" );
            sink.setDocumentName( "./folder\\docName.xml" );
            sink.anchor( "validAnchor" );
        }
        finally
        {
            sink.close();
        }

        assertTrue( writer.toString().indexOf( "<fo:inline id=\"#invalid_Anchor\">" ) != -1 );
        assertTrue( writer.toString().indexOf( "<fo:inline id=\"./folder/docName#validAnchor\">" ) != -1 );
    }

    /**
     * Test of link method, of class FoAggregateSink.
     */
    public void testLink()
    {
        try
        {
            sink = new FoAggregateSink( writer );
            sink.link( "http://www.example.com/" );
            sink.text( "http://www.example.com/" );
            sink.link_();
            sink.setDocumentName( "./folder\\docName.xml" );
            sink.link( "#anchor" );
            sink.text( "#anchor" );
            sink.link_();
            sink.link( "./././index.html" );
            sink.text( "./././index.html" );
            sink.link_();
            sink.link( "./../download.html" );
            sink.text( "./../download.html" );
            sink.link_();
            sink.link( ".///test.html" );
            sink.text( "./test.html" );
            sink.link_();
            sink.link( "./whatsnew-1.1.html" );
            sink.text( "./whatsnew-1.1.html" );
            sink.link_();
            sink.setDocumentName( ".///whatsnew-1.1.html" );
            sink.body();
            sink.body_();
        }
        finally
        {
            sink.close();
        }

        String result = writer.toString();

        assertTrue( result.indexOf( "<fo:basic-link external-destination=\"http://www.example.com/\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./folder/docName#anchor\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./folder/index\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./download\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./folder/test\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./folder/whatsnew-1.1\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:block id=\"./whatsnew-1.1\">" ) != -1 );

        writer = new StringWriter();
        try
        {
            sink = new FoAggregateSink( writer );
            sink.setDocumentName( "./subdir/dir/index.html" );
            sink.link( "../../root.html" );
            sink.text( "../../root.html" );
            sink.link_();
            sink.link( "../../../outside.html" );
            sink.text( "../../../outside.html" );
            sink.link_();
            sink.body();
            sink.body_();
        }
        finally
        {
            sink.close();
        }

        result = writer.toString();

        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./root\">" ) != -1 );
        assertTrue( result.indexOf( "<fo:basic-link internal-destination=\"./outside\">" ) != -1 );
    }
}
