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
import java.io.StringReader;
import java.io.Writer;

import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.parser.XhtmlBaseParser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.impl.AbstractSinkTest;
import org.apache.maven.doxia.sink.impl.SinkTestDocument;

/**
 * <code>FO Sink</code> Test case.
 *
 * @version $Id: FoSinkTest.java 1726411 2016-01-23 16:34:09Z hboutemy $
 */
public class FoSinkTest
    extends AbstractSinkTest
{
    private FoConfiguration config;

    // ----------------------------------------------------------------------
    // Specific test methods
    // ----------------------------------------------------------------------

    @Override
    protected String wrapXml( String xmlFragment )
    {
        return "<fo:fo xmlns:fo=\"" + FoMarkup.FO_NAMESPACE + "\">" + xmlFragment + "</fo:fo>";
    }

    /**
     * Uses fop to generate a pdf from a test document.
     * @throws Exception If the conversion fails.
     */
    public void testConvertFO2PDF()
        throws Exception
    {
        String fileName = "test";
        // first create fo
        FoSink fosink = new FoSink( getTestWriter( fileName ) );
        fosink.beginDocument();
        SinkTestDocument.generate( fosink );
        fosink.endDocument();
        fosink.close();

        // then generate PDF
        fo2pdf( fileName );
    }

    /**
     * Uses fop to generate an aggregated pdf from two test documents.
     * @throws Exception If the conversion fails.
     */
    public void testAggregateMode()
        throws Exception
    {
        FoAggregateSink fosink = new FoAggregateSink( getTestWriter( "aggregate" ) );

        fosink.setDocumentModel( getModel() );

        fosink.beginDocument();

        fosink.coverPage();

        fosink.toc();

        fosink.setDocumentName( "doc1" );
        fosink.setDocumentTitle( "Document 1" );
        SinkTestDocument.generate( fosink );

        // re-use the same source
        fosink.setDocumentName( "doc2" );
        fosink.setDocumentTitle( "Document 2" );
        SinkTestDocument.generate( fosink );

        fosink.endDocument();

        // then generate PDF
        fo2pdf( "aggregate" );
    }

    private DocumentModel getModel()
    {
        DocumentModel model = new DocumentModel();
        model.setToc( getToc() );
        model.setMeta( getMeta() );
        return model;
    }

    private DocumentMeta getMeta()
    {
        DocumentMeta meta = new DocumentMeta();
        meta.setAuthor( "The Apache Maven Project" );
        meta.setTitle( "Doxia FO Sink" );
        return meta;
    }

    private DocumentTOC getToc()
    {
        DocumentTOCItem item1 = new DocumentTOCItem();
        item1.setName( "First document" );
        item1.setRef( "doc1.apt" );

        DocumentTOCItem item2 = new DocumentTOCItem();
        item2.setName( "Second document" );
        item2.setRef( "doc2.xml" );

        DocumentTOC toc = new DocumentTOC();
        toc.setName( "What's in here" );
        toc.addItem( item1 );
        toc.addItem( item2 );

        return toc;
    }

    // ----------------------------------------------------------------------
    // Abstract methods the individual SinkTests must provide
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected String outputExtension()
    {
        return "fo";
    }

    /** {@inheritDoc} */
    protected Sink createSink( Writer writer )
    {
        return new FoSink( writer );
    }

    /** {@inheritDoc} */
    protected boolean isXmlSink()
    {
        return true;
    }

    /** {@inheritDoc} */
    protected String getTitleBlock( String title )
    {
        String attribs = getConfig().getAttributeString( "doc.header.title" );
        return EOL + "<fo:block" + attribs + ">" + title + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getAuthorBlock( String author )
    {
        String attribs = getConfig().getAttributeString( "doc.header.author" );
        return EOL + "<fo:block" + attribs + ">" + author + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getDateBlock( String date )
    {
        String attribs = getConfig().getAttributeString( "doc.header.date" );
        return EOL + "<fo:block" + attribs + ">" + date + "</fo:block>" + EOL;
    }

    // TODO
    protected String getHeadBlock()
    {
        return "";
    }

    // TODO: remove
    public void testHead()
    {
        String expected = "";
        assertEquals( "Wrong head!", expected, getHeadBlock() );
    }

    /** {@inheritDoc} */
    protected String getBodyBlock()
    {
        return EOL + "</fo:flow>" + EOL + "</fo:page-sequence>" + EOL + "</fo:root>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getSectionTitleBlock( String title )
    {
        return title;
    }

    /** {@inheritDoc} */
    protected String getSection1Block( String title )
    {
        String attribs = getConfig().getAttributeString( "body.text" );
        String attrib2 = getConfig().getAttributeString( "body.h1" );
        return EOL + EOL + "<fo:block" + attribs + ">" + EOL + EOL + "<fo:block" + attrib2 + ">1   " + title
            + "</fo:block>" + EOL + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getSection2Block( String title )
    {
        String attribs = getConfig().getAttributeString( "body.text" );
        String attrib2 = getConfig().getAttributeString( "body.h2" );
        return EOL + EOL + "<fo:block" + attribs + ">" + EOL + EOL + "<fo:block" + attrib2 + ">0.1   " + title
            + "</fo:block>" + EOL + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getSection3Block( String title )
    {
        String attribs = getConfig().getAttributeString( "body.text" );
        String attrib2 = getConfig().getAttributeString( "body.h3" );
        return EOL + EOL + "<fo:block" + attribs + ">" + EOL + EOL + "<fo:block" + attrib2 + ">0.0.1   " + title
            + "</fo:block>" + EOL + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getSection4Block( String title )
    {
        String attribs = getConfig().getAttributeString( "body.text" );
        String attrib2 = getConfig().getAttributeString( "body.h4" );
        return EOL + EOL + "<fo:block" + attribs + ">" + EOL + EOL + "<fo:block" + attrib2 + ">" + title
            + "</fo:block>" + EOL + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getSection5Block( String title )
    {
        String attribs = getConfig().getAttributeString( "body.text" );
        String attrib2 = getConfig().getAttributeString( "body.h5" );
        return EOL + EOL + "<fo:block" + attribs + ">" + EOL + EOL + "<fo:block" + attrib2 + ">" + title
            + "</fo:block>" + EOL + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getListBlock( String item )
    {
        String attribs = getConfig().getAttributeString( "list" );
        String itemAttribs = getConfig().getAttributeString( "list.item" );
        return EOL + EOL + "<fo:list-block" + attribs + ">" + EOL + "<fo:list-item" + itemAttribs
            + "><fo:list-item-label><fo:block>&#8226;</fo:block></fo:list-item-label>" + EOL + EOL
            + "<fo:list-item-body" + itemAttribs + ">" + EOL + "<fo:block>" + item + "</fo:block>" + EOL
            + "</fo:list-item-body>" + EOL + "</fo:list-item>" + EOL + "</fo:list-block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getNumberedListBlock( String item )
    {
        String attribs = getConfig().getAttributeString( "list" );
        String itemAttribs = getConfig().getAttributeString( "list.item.numbered" );
        return EOL + EOL + "<fo:list-block" + attribs + ">" + EOL + "<fo:list-item" + itemAttribs + ">" + EOL
            + "<fo:list-item-label>" + EOL + "<fo:block>i.</fo:block>" + EOL + "</fo:list-item-label>" + EOL + EOL
            + "<fo:list-item-body" + itemAttribs + ">" + EOL + "<fo:block>" + item + "</fo:block>" + EOL
            + "</fo:list-item-body>" + EOL + "</fo:list-item>" + EOL + "</fo:list-block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getDefinitionListBlock( String definum, String definition )
    {
        String dlAtts = getConfig().getAttributeString( "dl" );
        String dtAtts = getConfig().getAttributeString( "dt" );
        String ddAtts = getConfig().getAttributeString( "dd" );
        return EOL + EOL + "<fo:block" + dlAtts + ">" + EOL + "<fo:block" + dtAtts + ">" + definum + "</fo:block>"
            + EOL + EOL + EOL + "<fo:block" + ddAtts + ">" + definition + "</fo:block>" + EOL + "</fo:block>"
            + EOL;
    }

    /** {@inheritDoc} */
    protected String getFigureBlock( String source, String caption )
    {
        String dlAtts = getConfig().getAttributeString( "figure.display" );
        String dtAtts = getConfig().getAttributeString( "figure.graphics" );
        String ddAtts = getConfig().getAttributeString( "figure.caption" );

        String figureBlock = EOL + EOL + "<fo:block" + dlAtts + "><fo:external-graphic" + " src=\"" + source + "\"" + dtAtts
            + "/>" + EOL;
        if ( caption != null )
        {
            figureBlock += EOL + "<fo:block" + ddAtts + ">" + caption + "</fo:block>" + EOL;
        }
        figureBlock +=  "</fo:block>" + EOL;
        
        
        return figureBlock;
    }

    /** {@inheritDoc} */
    protected String getTableBlock( String cell, String caption )
    {
        String dlAtts = getConfig().getAttributeString( "table.padding" );
        String dtAtts = getConfig().getAttributeString( "table.layout" );
        String ddAtts = getConfig().getAttributeString( "table.body.row" );
        // String deAtts = getConfig().getAttributeString( "table.body.cell" );

        return EOL + EOL + "<fo:block" + dlAtts + ">" + EOL + "<fo:table" + dtAtts + ">" + EOL
            + "<fo:table-column column-width=\"proportional-column-width(1)\"/>" + EOL + EOL + "<fo:table-body>"
            + EOL + "<fo:table-row" + ddAtts
            + "><fo:table-cell column-number=\"1\" padding-after=\"1.5pt\" padding-end=\"5pt\" "
            + "keep-together.within-column=\"always\" padding-start=\"2.5pt\" "
            + "background-color=\"#eeeeee\" padding-before=\"4pt\">" + EOL + "<fo:block line-height=\"1.2em\" "
            + "text-align=\"center\" font-family=\"Helvetica,sans-serif\" font-size=\"9pt\">" + EOL + cell
            + "</fo:block>" + EOL + "</fo:table-cell>" + EOL + "</fo:table-row>" + EOL + "</fo:table-body>" + EOL
            + "</fo:table>" + EOL + "</fo:block>" + EOL + EOL
            + "<fo:block white-space-collapse=\"true\" space-after=\"6pt\" space-before=\"3pt\" "
            + "font-family=\"Garamond,serif\" line-height=\"12pt\" text-align=\"center\" font-size=\"11pt\">"
            + "Table_caption</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getParagraphBlock( String text )
    {
        String attribs = getConfig().getAttributeString( "normal.paragraph" );
        return EOL + "<fo:block" + attribs + ">" + text + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getVerbatimBlock( String text )
    {
        String attribs = getConfig().getAttributeString( "body.source" );
        return EOL + "<fo:block" + attribs + ">" + text + "</fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getHorizontalRuleBlock()
    {
        String attribs = getConfig().getAttributeString( "body.rule" );
        return EOL + EOL + "<fo:block>" + EOL + "<fo:leader" + attribs + " /></fo:block>" + EOL;
    }

    /** {@inheritDoc} */
    protected String getPageBreakBlock()
    {
        return EOL + "<fo:block break-before=\"page\" />" + EOL;
    }

    /** {@inheritDoc} */
    protected String getAnchorBlock( String anchor )
    {
        // all anchors get '#' pre-pended
        return EOL + "<fo:inline id=\"#" + anchor + "\">" + anchor + "</fo:inline>";
    }

    /** {@inheritDoc} */
    protected String getLinkBlock( String link, String text )
    {
        String attribs = getConfig().getAttributeString( "href.internal" );
        return EOL + "<fo:basic-link internal-destination=\"" + link + "\">" + EOL + "<fo:inline" + attribs + ">"
            + text + "</fo:inline></fo:basic-link>";
    }

    /** {@inheritDoc} */
    protected String getItalicBlock( String text )
    {
        String attribs = getConfig().getAttributeString( "italic" );
        return EOL + "<fo:inline" + attribs + ">" + text + "</fo:inline>";
    }

    /** {@inheritDoc} */
    protected String getBoldBlock( String text )
    {
        String attribs = getConfig().getAttributeString( "bold" );
        return EOL + "<fo:inline" + attribs + ">" + text + "</fo:inline>";
    }

    /** {@inheritDoc} */
    protected String getMonospacedBlock( String text )
    {
        String attribs = getConfig().getAttributeString( "monospace" );
        return EOL + "<fo:inline" + attribs + ">" + text + "</fo:inline>";
    }

    /** {@inheritDoc} */
    protected String getLineBreakBlock()
    {
        return EOL + EOL + "<fo:block />";
    }

    /** {@inheritDoc} */
    protected String getNonBreakingSpaceBlock()
    {
        return "&#160;";
    }

    /** {@inheritDoc} */
    protected String getTextBlock( String text )
    {
        return FoSink.escaped( text, false );
    }

    /** {@inheritDoc} */
    protected String getRawTextBlock( String text )
    {
        return text;
    }

    // ----------------------------------------------------------------------
    // Auxiliary methods
    // ----------------------------------------------------------------------

    private void fo2pdf( String baseName )
        throws Exception
    {
        // File outputDirectory = new File( getBasedirFile(), getOutputDir() );
        File outputDirectory = new File( getBasedir(), outputBaseDir() + getOutputDir() );
        File resourceDirectory = new File( getBasedirFile(), "target/test-classes" );
        File foFile = new File( outputDirectory, baseName + "." + outputExtension() );
        File pdfFile = new File( outputDirectory, baseName + ".pdf" );
        FoUtils.convertFO2PDF( foFile, pdfFile, resourceDirectory.getCanonicalPath() );
    }

    private FoConfiguration getConfig()
    {
        if ( config == null )
        {
            config = ( (FoSink) getSink() ).getFoConfiguration();
        }

        return config;
    }

    /** {@inheritDoc} */
    protected String getCommentBlock( String text )
    {
        return "<!--" + toXmlComment( text ) + "-->";
    }

    /**
     * DOXIA-357
     *
     * @throws Exception if any
     */
    public void testTableCaption()
        throws Exception
    {
        StringBuilder html = new StringBuilder();
        html.append( "<table>" ).append( EOL );
        html.append( "<caption>caption table</caption>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>foo</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>bar</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "</table>" ).append( EOL );

        String fileName = "testTableCaption";

        // first create fo
        FoSink fosink = new FoSink( getTestWriter( fileName ) );
        fosink.beginDocument();
        SinkTestDocument.generateHead( fosink );

        fosink.body();
        XhtmlBaseParser parser = new XhtmlBaseParser();
        parser.parse( new StringReader( html.toString() ), fosink );
        fosink.body_();

        fosink.endDocument();
        fosink.close();

        // then generate PDF
        fo2pdf( fileName );
    }

    /**
     * @throws Exception if any
     */
    public void testNestedTables()
        throws Exception
    {
        StringBuilder html = new StringBuilder();
        html.append( "<table>" ).append( EOL );
        html.append( "<caption>first caption</caption>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>foo</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>" ).append( EOL );

        html.append( "<table>" ).append( EOL );
        html.append( "<caption>second caption</caption>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>foo</td>" ).append( EOL );
        html.append( "<td>bar</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>foo1</td>" ).append( EOL );
        html.append( "<td>" ).append( EOL );

        html.append( "<table>" ).append( EOL );
        html.append( "<caption>third caption</caption>" ).append( EOL );
        html.append( "<tr>" ).append( EOL );
        html.append( "<td>foo1</td>" ).append( EOL );
        html.append( "<td>bar1</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "</table>" ).append( EOL );
        html.append( "</td>" ).append( EOL );

        html.append( "</tr>" ).append( EOL );
        html.append( "</table>" ).append( EOL );

        html.append( "</td>" ).append( EOL );
        html.append( "</tr>" ).append( EOL );
        html.append( "</table>" ).append( EOL );

        String fileName = "testNestedTables";

        // first create fo
        FoSink fosink = new FoSink( getTestWriter( fileName ) );
        fosink.beginDocument();
        SinkTestDocument.generateHead( fosink );

        fosink.body();
        XhtmlBaseParser parser = new XhtmlBaseParser();
        parser.parse( new StringReader( html.toString() ), fosink );
        fosink.body_();

        fosink.endDocument();
        fosink.close();

        // then generate PDF
        fo2pdf( fileName );
    }
}
