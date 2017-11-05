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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.AbstractXmlSink;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.doxia.sink.impl.SinkUtils;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.doxia.util.HtmlTools;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;

/**
 * A Doxia Sink that produces a FO model. The usage is similar to the following:
 *
 * <pre>
 * FoSink sink = new FoSink( writer );
 * sink.beginDocument();
 * ...
 * sink.endDocument();
 * </pre>
 *
 * @author ltheussl
 * @version $Id: FoSink.java 1726411 2016-01-23 16:34:09Z hboutemy $
 * @since 1.1
 */
public class FoSink
    extends AbstractXmlSink
    implements FoMarkup
{
    /** For writing the result. */
    private final PrintWriter out;

    /** Used to get the current position in numbered lists. */
    private final Stack<NumberedListItem> listStack;

    /** Used to get attributes for a given FO element. */
    private final FoConfiguration config;

    /** Counts the current section level. */
    private int section = 0;

    /** Counts the current subsection level. */
    private int subsection = 0;

    /** Counts the current subsubsection level. */
    private int subsubsection = 0;

    /** Verbatim flag. */
    private boolean verbatim;

    /** figure flag. */
    private boolean inFigure;

    private final String encoding;

    private final String languageId;

    /** Stack of drawing borders on table cells. */
    private final LinkedList<Boolean> tableGridStack;

    /** Stack of alignment int[] of table cells. */
    private final LinkedList<int[]> cellJustifStack;

    /** Stack of justification of table cells. */
    private final LinkedList<Boolean> isCellJustifStack;

    /** Stack of current table cell. */
    private final LinkedList<Integer> cellCountStack;

    /** The stack of StringWriter to write the table result temporary, so we could play with the output and fix fo. */
    private final LinkedList<StringWriter> tableContentWriterStack;

    private final LinkedList<StringWriter> tableCaptionWriterStack;

    private final LinkedList<PrettyPrintXMLWriter> tableCaptionXMLWriterStack;

    /** The stack of table caption */
    private final LinkedList<String> tableCaptionStack;

    /** Map of warn messages with a String as key to describe the error type and a Set as value.
     * Using to reduce warn messages. */
    protected Map<String, Set<String>> warnMessages;

    /**
     * Constructor, initialize the Writer.
     *
     * @param writer not null writer to write the result. <b>Should</b> be an UTF-8 Writer.
     * You could use <code>newXmlWriter</code> methods from {@link org.codehaus.plexus.util.WriterFactory}.
     */
    protected FoSink( Writer writer )
    {
        this( writer, "UTF-8" );
    }

    /**
     * Constructor, initialize the Writer and tells which encoding is used.
     *
     * @param writer not null writer to write the result.
     * @param encoding the encoding used, that should be written to the generated HTML content
     * if not <code>null</code>.
     */
    protected FoSink( Writer writer, String encoding )
    {
        this( writer, encoding, null );
    }

    /**
     * Constructor, initialize the Writer and tells which encoding and languageId are used.
     *
     * @param writer not null writer to write the result.
     * @param encoding the encoding used, that should be written to the generated HTML content
     * if not <code>null</code>.
     * @param languageId language identifier for the root element as defined by
     * <a href="ftp://ftp.isi.edu/in-notes/bcp/bcp47.txt">IETF BCP 47</a>, Tags for the Identification of Languages;
     * in addition, the empty string may be specified.
     */
    protected FoSink( Writer writer, String encoding, String languageId )
    {
        if ( writer == null )
        {
            throw new NullPointerException( "Null writer in FO Sink!" );
        }

        this.out = new PrintWriter( writer );
        this.encoding = encoding;
        this.languageId = languageId;
        this.config = new FoConfiguration();

        this.listStack = new Stack<NumberedListItem>();
        this.tableGridStack = new LinkedList<Boolean>();
        this.cellJustifStack = new LinkedList<int[]>();
        this.isCellJustifStack = new LinkedList<Boolean>();
        this.cellCountStack = new LinkedList<Integer>();
        this.tableContentWriterStack = new LinkedList<StringWriter>();
        this.tableCaptionWriterStack = new LinkedList<StringWriter>();
        this.tableCaptionXMLWriterStack = new LinkedList<PrettyPrintXMLWriter>();
        this.tableCaptionStack = new LinkedList<String>();

        setNameSpace( "fo" );
    }

    // TODO add FOP compliance mode?

    /**
     * Load configuration parameters from a File.
     *
     * @param configFile the configuration file.
     *
     * @throws java.io.IOException if the File cannot be read
     *  or some error occurs when initializing the configuration parameters.
     *
     * @since 1.1.1
     */
    public void load( File configFile )
            throws IOException
    {
        config.load( configFile );
    }

    /** {@inheritDoc} */
    public void head( SinkEventAttributes attributes )
    {
        init();

        startPageSequence( "0", null, null, null );
    }

    /** {@inheritDoc} */
    public void head()
    {
        head( null );
    }

    /** {@inheritDoc} */
    public void head_()
    {
        writeEOL();
    }

    /** {@inheritDoc} */
    public void title( SinkEventAttributes attributes )
    {
        writeStartTag( BLOCK_TAG, "doc.header.title" );
    }

    /** {@inheritDoc} */
    public void title()
    {
        title( null );
    }

    /** {@inheritDoc} */
    public void title_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void author( SinkEventAttributes attributes )
    {
        writeStartTag( BLOCK_TAG, "doc.header.author" );
    }

    /** {@inheritDoc} */
    public void author()
    {
        author( null );
    }

    /** {@inheritDoc} */
    public void author_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void date( SinkEventAttributes attributes )
    {
        writeStartTag( BLOCK_TAG, "doc.header.date" );
    }

    /** {@inheritDoc} */
    public void date()
    {
        date( null );
    }

    /** {@inheritDoc} */
    public void date_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void body( SinkEventAttributes attributes )
    {
        // noop
    }

    /** {@inheritDoc} */
    public void body()
    {
        body( null );
    }

    /** {@inheritDoc} */
    public void body_()
    {
        writeEOL();
        writeEndTag( FLOW_TAG );
        writeEOL();
        writeEndTag( PAGE_SEQUENCE_TAG );
        writeEOL();
        endDocument();
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void sectionTitle()
    {
        // nop
    }

    /** {@inheritDoc} */
    public void sectionTitle_()
    {
        // nop
    }

    /** {@inheritDoc} */
    public void section( int level, SinkEventAttributes attributes )
    {
        if ( level == SECTION_LEVEL_1 )
        {
            section++;
            subsection = 0;
            subsubsection = 0;
        }
        else if ( level == SECTION_LEVEL_2 )
        {
            subsection++;
            subsubsection = 0;
        }
        else if ( level == SECTION_LEVEL_3 )
        {
            subsubsection++;
        }

        onSection();
    }

    /** {@inheritDoc} */
    public void section_( int level )
    {
        onSection_();
    }

    /** {@inheritDoc} */
    public void sectionTitle( int level, SinkEventAttributes attributes )
    {
        onSectionTitle( level );
    }

    /** {@inheritDoc} */
    public void sectionTitle_( int level )
    {
        onSectionTitle_();
    }

    /** {@inheritDoc} */
    public void section1()
    {
        section( SECTION_LEVEL_1, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle1()
    {
        sectionTitle( SECTION_LEVEL_1, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle1_()
    {
        sectionTitle_( SECTION_LEVEL_1 );
    }

    /** {@inheritDoc} */
    public void section1_()
    {
        section_( SECTION_LEVEL_1 );
    }

    /** {@inheritDoc} */
    public void section2()
    {
        section( SECTION_LEVEL_2, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle2()
    {
        sectionTitle( SECTION_LEVEL_2, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle2_()
    {
        sectionTitle_( SECTION_LEVEL_2 );
    }

    /** {@inheritDoc} */
    public void section2_()
    {
        section_( SECTION_LEVEL_2 );
    }

    /** {@inheritDoc} */
    public void section3()
    {
        section( SECTION_LEVEL_3, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle3()
    {
        sectionTitle( SECTION_LEVEL_3, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle3_()
    {
        sectionTitle_( SECTION_LEVEL_3 );
    }

    /** {@inheritDoc} */
    public void section3_()
    {
        section_( SECTION_LEVEL_3 );
    }

    /** {@inheritDoc} */
    public void section4()
    {
        section( SECTION_LEVEL_4, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle4()
    {
        sectionTitle( SECTION_LEVEL_4, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle4_()
    {
        sectionTitle_( SECTION_LEVEL_4 );
    }

    /** {@inheritDoc} */
    public void section4_()
    {
        section_( SECTION_LEVEL_4 );
    }

    /** {@inheritDoc} */
    public void section5()
    {
        section( SECTION_LEVEL_5, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle5()
    {
        sectionTitle( SECTION_LEVEL_5, null );
    }

    /** {@inheritDoc} */
    public void sectionTitle5_()
    {
        sectionTitle_( SECTION_LEVEL_5 );
    }

    /** {@inheritDoc} */
    public void section5_()
    {
        section_( SECTION_LEVEL_5 );
    }

    /** Starts a section/subsection. */
    private void onSection()
    {
        writeEOL();
        writeStartTag( BLOCK_TAG, "body.text" );
    }

    /**
     * Starts a section title.
     *
     * @param depth The section level.
     */
    private void onSectionTitle( int depth )
    {
        StringBuilder title = new StringBuilder( 16 );

        title.append( getChapterString() );

        writeEOL();
        if ( depth == SECTION_LEVEL_1 )
        {
            writeStartTag( BLOCK_TAG, "body.h1" );
            title.append( section ).append( "   " );
        }
        else if ( depth == SECTION_LEVEL_2 )
        {
            writeStartTag( BLOCK_TAG, "body.h2" );
            title.append( section ).append( "." );
            title.append( subsection ).append( "   " );
        }
        else if ( depth == SECTION_LEVEL_3 )
        {
            writeStartTag( BLOCK_TAG, "body.h3" );
            title.append( section ).append( "." );
            title.append( subsection ).append( "." );
            title.append( subsubsection ).append( "   " );
        }
        else if ( depth == SECTION_LEVEL_4 )
        {
            writeStartTag( BLOCK_TAG, "body.h4" );
        }
        else
        {
            writeStartTag( BLOCK_TAG, "body.h5" );
        }

        write( title.toString() );
    }

    /** Ends a section title. */
    private void onSectionTitle_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** Ends a section/subsection. */
    private void onSection_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /**
     * Resets the section counter to 0.
     * Only useful for overriding classes, like AggregateSink, the FoSink puts everything into one chapter.
     */
    protected void resetSectionCounter()
    {
        this.section = 0;
    }

    /**
     * Returns the current chapter number as a string.
     * By default does nothing, gets overridden by AggregateSink.
     *
     * @return an empty String.
     */
    protected String getChapterString()
    {
        return "";
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void list( SinkEventAttributes attributes )
    {
        writeEOL();
        writeStartTag( LIST_BLOCK_TAG, "list" );
    }

    /** {@inheritDoc} */
    public void list()
    {
        list( null );
    }

    /** {@inheritDoc} */
    public void list_()
    {
        writeEndTag( LIST_BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void listItem( SinkEventAttributes attributes )
    {
        writeStartTag( LIST_ITEM_TAG, "list.item" );
        writeStartTag( LIST_ITEM_LABEL_TAG );
        writeStartTag( BLOCK_TAG );
        write( "&#8226;" ); // TODO customize?
        writeEndTag( BLOCK_TAG );
        writeEndTag( LIST_ITEM_LABEL_TAG );
        writeEOL();
        writeStartTag( LIST_ITEM_BODY_TAG, "list.item" );
        writeEOL();
        writeStartTag( BLOCK_TAG );
    }

    /** {@inheritDoc} */
    public void listItem()
    {
        listItem( null );
    }

    /** {@inheritDoc} */
    public void listItem_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
        writeEndTag( LIST_ITEM_BODY_TAG );
        writeEOL();
        writeEndTag( LIST_ITEM_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void numberedList( int numbering, SinkEventAttributes attributes )
    {
        this.listStack.push( new NumberedListItem( numbering ) );
        writeEOL();
        writeStartTag( LIST_BLOCK_TAG, "list" );
    }

    /** {@inheritDoc} */
    public void numberedList( int numbering )
    {
        numberedList( numbering, null );
    }

    /** {@inheritDoc} */
    public void numberedList_()
    {
        this.listStack.pop();
        writeEndTag( LIST_BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void numberedListItem( SinkEventAttributes attributes )
    {
        NumberedListItem current = this.listStack.peek();
        current.next();

        writeStartTag( LIST_ITEM_TAG, "list.item.numbered" );

        writeEOL();
        writeStartTag( LIST_ITEM_LABEL_TAG );
        writeEOL();
        writeStartTag( BLOCK_TAG );
        write( current.getListItemSymbol() );
        writeEndTag( BLOCK_TAG );
        writeEOL();
        writeEndTag( LIST_ITEM_LABEL_TAG );
        writeEOL();

        writeStartTag( LIST_ITEM_BODY_TAG, "list.item.numbered" );
        writeEOL();
        writeStartTag( BLOCK_TAG );
    }

    /** {@inheritDoc} */
    public void numberedListItem()
    {
        numberedListItem( null );
    }

    /** {@inheritDoc} */
    public void numberedListItem_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
        writeEndTag( LIST_ITEM_BODY_TAG );
        writeEOL();
        writeEndTag( LIST_ITEM_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void definitionList( SinkEventAttributes attributes )
    {
        writeEOL();
        writeStartTag( BLOCK_TAG, "dl" );
    }

    /** {@inheritDoc} */
    public void definitionList()
    {
        definitionList( null );
    }

    /** {@inheritDoc} */
    public void definitionList_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void definitionListItem( SinkEventAttributes attributes )
    {
        // nop
    }

    /** {@inheritDoc} */
    public void definitionListItem()
    {
        definitionListItem( null );
    }

    /** {@inheritDoc} */
    public void definitionListItem_()
    {
        // nop
    }

    /** {@inheritDoc} */
    public void definedTerm( SinkEventAttributes attributes )
    {
        writeStartTag( BLOCK_TAG, "dt" );
    }

    /** {@inheritDoc} */
    public void definedTerm()
    {
        definedTerm( null );
    }

    /** {@inheritDoc} */
    public void definedTerm_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void definition( SinkEventAttributes attributes )
    {
        writeEOL();
        writeStartTag( BLOCK_TAG, "dd" );
    }

    /** {@inheritDoc} */
    public void definition()
    {
        definition( null );
    }

    /** {@inheritDoc} */
    public void definition_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void figure( SinkEventAttributes attributes )
    {
        this.inFigure = true;
        writeEOL();
        writeStartTag( BLOCK_TAG, "figure.display" );
    }

    /** {@inheritDoc} */
    public void figure()
    {
        figure( null );
    }

    /** {@inheritDoc} */
    public void figure_()
    {
        this.inFigure = false;
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void figureGraphics( String name )
    {
        figureGraphics( name, null );
    }

    /** {@inheritDoc} */
    public void figureGraphics( String src, SinkEventAttributes attributes )
    {
        MutableAttributeSet atts = config.getAttributeSet( "figure.graphics" );
        atts.addAttribute( Attribute.SRC.toString(), src );

        // http://xmlgraphics.apache.org/fop/graphics.html#resolution

        final String[] valids = new String[] {"content-height", "content-width", "height", "width"};
        final MutableAttributeSet filtered = SinkUtils.filterAttributes( attributes, valids );

        if ( filtered != null )
        {
            atts.addAttributes( filtered );
        }

        writeln( "<fo:external-graphic" + SinkUtils.getAttributeString( atts ) + "/>" );
    }

    /**
     * Flags if we are inside a figure.
     *
     * @return True if we are between {@link #figure()} and {@link #figure_()} calls.
     */
    protected boolean isFigure()
    {
        return this.inFigure;
    }

    /** {@inheritDoc} */
    public void figureCaption( SinkEventAttributes attributes )
    {
        writeStartTag( BLOCK_TAG, "figure.caption" );
    }

    /** {@inheritDoc} */
    public void figureCaption()
    {
        figureCaption( null );
    }

    /** {@inheritDoc} */
    public void figureCaption_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void paragraph()
    {
        paragraph( null );
    }

    /** {@inheritDoc} */
    public void paragraph( SinkEventAttributes attributes )
    {
        MutableAttributeSet atts = config.getAttributeSet( "normal.paragraph" );

        if ( attributes != null && attributes.isDefined( SinkEventAttributes.ALIGN ) )
        {
            atts.addAttribute( "text-align", attributes.getAttribute( SinkEventAttributes.ALIGN ) );
        }

        writeEOL();
        writeStartTag( BLOCK_TAG, atts );
    }

    /** {@inheritDoc} */
    public void paragraph_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void verbatim( SinkEventAttributes attributes )
    {
        this.verbatim = true;

        boolean boxed = false;

        if ( attributes != null && attributes.isDefined( SinkEventAttributes.DECORATION ) )
        {
            boxed =
                "boxed".equals( attributes.getAttribute( SinkEventAttributes.DECORATION ).toString() );
        }

        if ( boxed )
        {
            writeStartTag( BLOCK_TAG, "body.source" );
        }
        else
        {
            writeStartTag( BLOCK_TAG, "body.pre" );
        }
    }

    /** {@inheritDoc} */
    public void verbatim( boolean boxed )
    {
        verbatim( boxed ? SinkEventAttributeSet.BOXED : null );
    }

    /** {@inheritDoc} */
    public void verbatim_()
    {
        this.verbatim = false;
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void horizontalRule( SinkEventAttributes attributes )
    {
        writeEOL();
        writeEOL();
        writeStartTag( BLOCK_TAG );
        writeEmptyTag( LEADER_TAG, "body.rule" );
        writeEndTag( BLOCK_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void horizontalRule()
    {
        horizontalRule( null );
    }

    /** {@inheritDoc} */
    public void pageBreak()
    {
        writeEmptyTag( BLOCK_TAG, "break-before", "page" );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void table( SinkEventAttributes attributes )
    {
        writeEOL();
        writeStartTag( BLOCK_TAG, "table.padding" );

        // <fo:table-and-caption> is XSL-FO 1.0 standard but still not implemented in FOP 0.95
        //writeStartTag( TABLE_AND_CAPTION_TAG );

        this.tableContentWriterStack.addLast( new StringWriter() );
        writeStartTag( TABLE_TAG, "table.layout" );
    }

    /** {@inheritDoc} */
    public void table()
    {
        table( null );
    }

    /** {@inheritDoc} */
    public void table_()
    {
        String content = this.tableContentWriterStack.removeLast().toString();

        StringBuilder sb = new StringBuilder();
        int cellCount = Integer.parseInt( this.cellCountStack.removeLast().toString() );
        for ( int i = 0; i < cellCount; i++ )
        {
            sb.append( "<fo:table-column column-width=\"proportional-column-width(1)\"/>" );
            sb.append( EOL );
        }

        int index = content.indexOf( ">" ) + 1;
        writeln( content.substring( 0, index ) );
        write( sb.toString() );
        write( content.substring( index ) );

        writeEndTag( TABLE_TAG );
        writeEOL();

        // <fo:table-and-caption> is XSL-FO 1.0 standard but still not implemented in FOP 0.95
        //writeEndTag( TABLE_AND_CAPTION_TAG );

        writeEndTag( BLOCK_TAG );
        writeEOL();

        if ( !this.tableCaptionStack.isEmpty() && this.tableCaptionStack.getLast() != null )
        {
            paragraph( SinkEventAttributeSet.CENTER );
            write( this.tableCaptionStack.removeLast().toString() );
            paragraph_();
        }
    }

    /** {@inheritDoc} */
    public void tableRows( int[] justification, boolean grid )
    {
        this.tableGridStack.addLast( Boolean.valueOf( grid ) );
        this.cellJustifStack.addLast( justification );
        this.isCellJustifStack.addLast( Boolean.valueOf( true ) );
        this.cellCountStack.addLast( Integer.valueOf( 0 ) );
        writeEOL();
        writeStartTag( TABLE_BODY_TAG );
    }

    /** {@inheritDoc} */
    public void tableRows_()
    {
        this.tableGridStack.removeLast();
        this.cellJustifStack.removeLast();
        this.isCellJustifStack.removeLast();
        writeEndTag( TABLE_BODY_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void tableRow( SinkEventAttributes attributes )
    {
        // TODO spacer rows
        writeStartTag( TABLE_ROW_TAG, "table.body.row" );
        this.cellCountStack.removeLast();
        this.cellCountStack.addLast( Integer.valueOf( 0 ) );
    }

    /** {@inheritDoc} */
    public void tableRow()
    {
        tableRow( null );
    }

    /** {@inheritDoc} */
    public void tableRow_()
    {
        writeEndTag( TABLE_ROW_TAG );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void tableCell( SinkEventAttributes attributes )
    {
        tableCell( false, attributes );
    }

    /** {@inheritDoc} */
    public void tableCell()
    {
        tableCell( (SinkEventAttributes) null );
    }

    /** {@inheritDoc} */
    public void tableCell( String width )
    {
        // TODO: fop can't handle cell width
        tableCell();
    }

    /** {@inheritDoc} */
    public void tableHeaderCell( SinkEventAttributes attributes )
    {
        tableCell( true, attributes );
    }

    /** {@inheritDoc} */
    public void tableHeaderCell()
    {
        tableHeaderCell( (SinkEventAttributes) null );
    }

    /** {@inheritDoc} */
    public void tableHeaderCell( String width )
    {
        // TODO: fop can't handle cell width
        tableHeaderCell();
    }

    /**
     * Writes a table cell.
     *
     * @param headerRow true if this is a header cell.
     * @param attributes the cell attributes, could be null.
     */
    private void tableCell( boolean headerRow, SinkEventAttributes attributes )
    {
        MutableAttributeSet cellAtts = headerRow
                 ? config.getAttributeSet( "table.heading.cell" )
                 : config.getAttributeSet( "table.body.cell" );

        // the column-number is needed for the hack to center the table, see tableRows.
        int cellCount = Integer.parseInt( this.cellCountStack.getLast().toString() );
        cellAtts.addAttribute( "column-number", String.valueOf( cellCount + 1 ) );

        if ( this.tableGridStack.getLast().equals( Boolean.TRUE ) )
        {
            cellAtts.addAttributes( config.getAttributeSet( "table.body.cell.grid" ) );
        }

        MutableAttributeSet blockAtts = headerRow
                 ? config.getAttributeSet( "table.heading.block" )
                 : config.getAttributeSet( "table.body.block" );

        String justif = null;
        if ( attributes == null )
        {
            attributes = new SinkEventAttributeSet( 0 );
        }

        if ( attributes.isDefined( Attribute.ALIGN.toString() ) )
        {
            justif = attributes.getAttribute( Attribute.ALIGN.toString() ).toString();
        }

        int[] cellJustif = this.cellJustifStack.getLast();
        if ( justif == null && cellJustif != null && cellJustif.length > 0
            && this.isCellJustifStack.getLast().equals( Boolean.TRUE ) )
        {
            switch ( cellJustif[Math.min( cellCount, cellJustif.length - 1 )] )
            {
                case JUSTIFY_LEFT:
                    justif = "left";
                    break;
                case JUSTIFY_RIGHT:
                    justif = "right";
                    break;
                case JUSTIFY_CENTER:
                default:
                    justif = "center";
            }
        }

        if ( justif != null )
        {
            blockAtts.addAttribute( "text-align", justif );
        }

        writeStartTag( TABLE_CELL_TAG, cellAtts );
        writeEOL();
        writeStartTag( BLOCK_TAG, blockAtts );
        writeEOL();
    }

    /** {@inheritDoc} */
    public void tableCell_()
    {
        writeEndTag( BLOCK_TAG );
        writeEOL();
        writeEndTag( TABLE_CELL_TAG );
        writeEOL();

        if ( this.isCellJustifStack.getLast().equals( Boolean.TRUE ) )
        {
            int cellCount = Integer.parseInt( this.cellCountStack.removeLast().toString() );
            this.cellCountStack.addLast( Integer.valueOf( ++cellCount ) );
        }
    }

    /** {@inheritDoc} */
    public void tableHeaderCell_()
    {
        tableCell_();
    }

    /** {@inheritDoc} */
    public void tableCaption( SinkEventAttributes attributes )
    {
        StringWriter sw = new StringWriter();
        this.tableCaptionWriterStack.addLast( sw );
        this.tableCaptionXMLWriterStack.addLast( new PrettyPrintXMLWriter( sw ) );

        // <fo:table-caption> is XSL-FO 1.0 standard but not implemented in FOP 0.95
        //writeStartTag( TABLE_CAPTION_TAG );

        // TODO: how to implement this otherwise?
        // table-footer doesn't work because it has to be declared before table-body.
    }

    /** {@inheritDoc} */
    public void tableCaption()
    {
        tableCaption( null );
    }

    /** {@inheritDoc} */
    public void tableCaption_()
    {
        if ( !this.tableCaptionXMLWriterStack.isEmpty() && this.tableCaptionXMLWriterStack.getLast() != null )
        {
            this.tableCaptionStack.addLast( this.tableCaptionWriterStack.removeLast().toString() );
            this.tableCaptionXMLWriterStack.removeLast();
        }
        // <fo:table-caption> is XSL-FO 1.0 standard but not implemented in FOP 0.95
        //writeEndTag( TABLE_CAPTION_TAG );
    }

    /** {@inheritDoc} */
    public void anchor( String name, SinkEventAttributes attributes )
    {
        if ( name == null )
        {
            throw new NullPointerException( "Anchor name cannot be null!" );
        }

        String anchor = name;

        if ( !DoxiaUtils.isValidId( anchor ) )
        {
            anchor = DoxiaUtils.encodeId( name, true );

            String msg = "Modified invalid anchor name: '" + name + "' to '" + anchor + "'";
            logMessage( "modifiedLink", msg );
        }

        anchor = "#" + name;

        writeStartTag( INLINE_TAG, "id", anchor );
    }

    /** {@inheritDoc} */
    public void anchor( String name )
    {
        anchor( name, null );
    }

    /** {@inheritDoc} */
    public void anchor_()
    {
        writeEndTag( INLINE_TAG );
    }

    /** {@inheritDoc} */
    public void link( String name, SinkEventAttributes attributes )
    {
        if ( name == null )
        {
            throw new NullPointerException( "Link name cannot be null!" );
        }

        if ( DoxiaUtils.isExternalLink( name ) )
        {
            writeStartTag( BASIC_LINK_TAG, "external-destination", HtmlTools.escapeHTML( name ) );
            writeStartTag( INLINE_TAG, "href.external" );
        }
        else if ( DoxiaUtils.isInternalLink( name ) )
        {
            String anchor = name.substring( 1 );

            if ( !DoxiaUtils.isValidId( anchor ) )
            {
                anchor = DoxiaUtils.encodeId( anchor, true );

                String msg = "Modified invalid anchor name: '" + name + "' to '" + anchor + "'";
                logMessage( "modifiedLink", msg );
            }

            anchor = "#" + anchor;

            writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( anchor ) );
            writeStartTag( INLINE_TAG, "href.internal" );
        }
        else
        {
            // treat everything else as is
            String anchor = name;

            writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( anchor ) );
            writeStartTag( INLINE_TAG, "href.internal" );
        }
    }

    /** {@inheritDoc} */
    public void link( String name )
    {
        link( name, null );
    }

    /** {@inheritDoc} */
    public void link_()
    {
        writeEndTag( INLINE_TAG );
        writeEndTag( BASIC_LINK_TAG );
    }

    /** {@inheritDoc} */
    public void italic()
    {
        writeStartTag( INLINE_TAG, "italic" );
    }

    /** {@inheritDoc} */
    public void italic_()
    {
        writeEndTag( INLINE_TAG );
    }

    /** {@inheritDoc} */
    public void bold()
    {
        writeStartTag( INLINE_TAG, "bold" );
    }

    /** {@inheritDoc} */
    public void bold_()
    {
        writeEndTag( INLINE_TAG );
    }

    /** {@inheritDoc} */
    public void monospaced()
    {
        writeStartTag( INLINE_TAG, "monospace" );
    }

    /** {@inheritDoc} */
    public void monospaced_()
    {
        writeEndTag( INLINE_TAG );
    }

    /** {@inheritDoc} */
    public void lineBreak( SinkEventAttributes attributes )
    {
        writeEOL();
        writeEOL();
        writeSimpleTag( BLOCK_TAG );
    }

    /** {@inheritDoc} */
    public void lineBreak()
    {
        lineBreak( null );
    }

    /** {@inheritDoc} */
    public void nonBreakingSpace()
    {
        write( "&#160;" );
    }

    /** {@inheritDoc} */
    public void text( String text, SinkEventAttributes attributes )
    {
        content( text );
    }

    /** {@inheritDoc} */
    public void text( String text )
    {
        text( text, null );
    }

    /** {@inheritDoc} */
    public void rawText( String text )
    {
        write( text );
    }

    /** {@inheritDoc} */
    public void flush()
    {
        out.flush();
    }

    /** {@inheritDoc} */
    public void close()
    {
        out.close();

        if ( getLog().isWarnEnabled() && this.warnMessages != null )
        {
            for ( Map.Entry<String, Set<String>> entry : this.warnMessages.entrySet() )
            {
                for ( String msg : entry.getValue() )
                {
                    getLog().warn( msg );
                }
            }

            this.warnMessages = null;
        }

        init();
    }

    /**
     * {@inheritDoc}
     *
     * Unkown events just log a warning message but are ignored otherwise.
     * @see org.apache.maven.doxia.sink.Sink#unknown(String,Object[],SinkEventAttributes)
     */
    public void unknown( String name, Object[] requiredParams, SinkEventAttributes attributes )
    {
        String msg = "Unknown Sink event: '" + name + "', ignoring!";
        logMessage( "unknownEvent", msg );
    }

    /** {@inheritDoc} */
    public void comment( String comment )
    {
        if ( comment != null )
        {
            final String originalComment = comment;

            // http://www.w3.org/TR/2000/REC-xml-20001006#sec-comments
            while ( comment.contains( "--" ) )
            {
                comment = comment.replace( "--", "- -" );
            }

            if ( comment.endsWith( "-" ) )
            {
                comment += " ";
            }

            if ( !originalComment.equals( comment ) )
            {
                String msg = "Modified invalid comment: '" + originalComment + "' to '" + comment + "'";
                logMessage( "modifyComment", msg );
            }

            final StringBuilder buffer = new StringBuilder( comment.length() + 7 );

            buffer.append( LESS_THAN ).append( BANG ).append( MINUS ).append( MINUS );
            buffer.append( comment );
            buffer.append( MINUS ).append( MINUS ).append( GREATER_THAN );

            write( buffer.toString() );
        }
    }

    /**
     * Writes the beginning of a FO document.
     */
    public void beginDocument()
    {
        write( "<?xml version=\"1.0\"" );
        if ( encoding != null )
        {
            write( " encoding=\"" + encoding + "\"" );
        }
        write( "?>" );
        writeEOL();

        MutableAttributeSet atts = new SinkEventAttributeSet();
        atts.addAttribute( "xmlns:" + getNameSpace(), FO_NAMESPACE );

        if ( languageId != null )
        {
            atts.addAttribute( "language", languageId );
        }

        writeStartTag( ROOT_TAG, atts );

        writeStartTag( LAYOUT_MASTER_SET_TAG );

        writeStartTag( SIMPLE_PAGE_MASTER_TAG, "layout.master.set.cover-page" );
        writeEmptyTag( REGION_BODY_TAG, "layout.master.set.cover-page.region-body" );
        writeEndTag( SIMPLE_PAGE_MASTER_TAG );
        writeEOL();

        writeStartTag( SIMPLE_PAGE_MASTER_TAG, "layout.master.set.toc" );
        writeEmptyTag( REGION_BODY_TAG, "layout.master.set.toc.region-body" );
        writeEmptyTag( REGION_BEFORE_TAG, "layout.master.set.toc.region-before" );
        writeEmptyTag( REGION_AFTER_TAG, "layout.master.set.toc.region-after" );
        writeEndTag( SIMPLE_PAGE_MASTER_TAG );
        writeEOL();

        writeStartTag( SIMPLE_PAGE_MASTER_TAG, "layout.master.set.body" );
        writeEmptyTag( REGION_BODY_TAG, "layout.master.set.body.region-body" );
        writeEmptyTag( REGION_BEFORE_TAG, "layout.master.set.body.region-before" );
        writeEmptyTag( REGION_AFTER_TAG, "layout.master.set.body.region-after" );
        writeEndTag( SIMPLE_PAGE_MASTER_TAG );
        writeEOL();

        writeEndTag( LAYOUT_MASTER_SET_TAG );
        writeEOL();

        pdfBookmarks();
    }

    /**
     * Writes the end of a FO document, flushes and closes the stream.
     */
    public void endDocument()
    {
        writeEndTag( ROOT_TAG );
        writeEOL();

        flush();
        close();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Returns the configuration object of this sink.
     *
     * @return The configuration object of this sink.
     */
    protected FoConfiguration getFoConfiguration()
    {
        return config;
    }

    /**
     * Writes a start tag, prepending EOL.
     *
     * @param tag The tag.
     * @param attributeId An id identifying the attribute set.
     */
    protected void writeStartTag( Tag tag, String attributeId )
    {
        writeEOL();
        writeStartTag( tag, config.getAttributeSet( attributeId ) );
    }

    /**
     * Writes a start tag, prepending EOL.
     *
     * @param tag The tag.
     * @param id An id to add.
     * @param name The name (value) of the id.
     */
    protected void writeStartTag( Tag tag, String id, String name )
    {
        writeEOL();
        MutableAttributeSet att = new SinkEventAttributeSet( new String[] {id, name} );

        writeStartTag( tag, att );
    }

    /**
     * Writes a start tag, prepending EOL.
     *
     * @param tag The tag.
     * @param id An id to add.
     * @param name The name (value) of the id.
     * @param attributeId An id identifying the attribute set.
     */
    protected void writeStartTag( Tag tag, String id, String name, String attributeId )
    {
        MutableAttributeSet att = config.getAttributeSet( attributeId );

        // make sure we don't add it twice
        if ( att.isDefined( id ) )
        {
            att.removeAttribute( id );
        }

        att.addAttribute( id, name );

        writeEOL();
        writeStartTag( tag, att );
    }

    /**
     * Writes an empty tag, prepending EOL.
     *
     * @param tag The tag.
     * @param id An id to add.
     * @param name The name (value) of the id.
     */
    protected void writeEmptyTag( Tag tag, String id, String name )
    {
        MutableAttributeSet att = new SinkEventAttributeSet( new String[] {id, name} );

        writeEOL();
        writeSimpleTag( tag, att );
    }

    /**
     * Writes a simple tag, appending EOL.
     *
     * @param tag The tag name.
     * @param attributeId An id identifying the attribute set.
     */
    protected void writeEmptyTag( Tag tag, String attributeId )
    {
        writeEOL();
        writeSimpleTag( tag, config.getAttributeSet( attributeId ) );
    }

    /**
     * {@inheritDoc}
     *
     * Writes a text, swallowing any exceptions.
     */
    protected void write( String text )
    {
        if ( !this.tableCaptionXMLWriterStack.isEmpty() && this.tableCaptionXMLWriterStack.getLast() != null )
        {
            this.tableCaptionXMLWriterStack.getLast().writeText( unifyEOLs( text ) );
        }
        else if ( !this.tableContentWriterStack.isEmpty() && this.tableContentWriterStack.getLast() != null )
        {
            this.tableContentWriterStack.getLast().write( unifyEOLs( text ) );
        }
        else
        {
            out.write( unifyEOLs( text ) );
        }
    }

    /**
     * Writes a text, appending EOL.
     *
     * @param text The text to write.
     */
    protected void writeln( String text )
    {
        write( text );
        writeEOL();
    }

    /**
     * Writes content, escaping special characters.
     *
     * @param text The text to write.
     */
    protected void content( String text )
    {
        write( escaped( text, verbatim ) );
    }

    /**
     * Escapes special characters so that the text can be included in a fo file.
     *
     * @param text The text to process.
     * @param verb In verbatim mode, white space and newlines are escaped.
     * @return The text with special characters escaped.
     */
    public static String escaped( String text, boolean verb )
    {
        int length = text.length();
        StringBuilder buffer = new StringBuilder( length );

        for ( int i = 0; i < length; ++i )
        {
            char c = text.charAt( i );
            switch ( c )
            {
                case ' ':
                    if ( verb )
                    {
                        buffer.append( "&#160;" );
                    }
                    else
                    {
                        buffer.append( c );
                    }
                    break;
                case '<':
                    buffer.append( "&lt;" );
                    break;
                case '>':
                    buffer.append( "&gt;" );
                    break;
                case '&':
                    buffer.append( "&amp;" );
                    break;
                case '\n':
                    buffer.append( EOL );
                    if ( verb )
                    {
                        buffer.append( "<fo:block/>" + EOL );
                    }
                    break;
                default:
                    if ( needsSymbolFont( c ) )
                    {
                        // TODO: make font configurable?
                        buffer.append( "<fo:inline font-family=\"Symbol\">" ).append( c ).append( "</fo:inline>" );
                    }
                    else
                    {
                        buffer.append( c );
                    }
            }
        }

        return buffer.toString();
    }

    /** {@inheritDoc} */
    protected void writeStartTag( Tag t, MutableAttributeSet att, boolean isSimpleTag )
    {
        if ( this.tableCaptionXMLWriterStack.isEmpty() )
        {
            super.writeStartTag ( t, att, isSimpleTag );
        }
        else
        {
            String tag = ( getNameSpace() != null ? getNameSpace() + ":" : "" ) + t.toString();
            this.tableCaptionXMLWriterStack.getLast().startElement( tag );

            if ( att != null )
            {
                Enumeration<?> names = att.getAttributeNames();
                while ( names.hasMoreElements() )
                {
                    Object key = names.nextElement();
                    Object value = att.getAttribute( key );

                    this.tableCaptionXMLWriterStack.getLast().addAttribute( key.toString(), value.toString() );
                }
            }

            if ( isSimpleTag )
            {
                this.tableCaptionXMLWriterStack.getLast().endElement();
            }
        }
    }

    /** {@inheritDoc} */
    protected void writeEndTag( Tag t )
    {
        if ( this.tableCaptionXMLWriterStack.isEmpty() )
        {
            super.writeEndTag( t );
        }
        else
        {
            this.tableCaptionXMLWriterStack.getLast().endElement();
        }
    }

    private static final char UPPER_ALPHA = 0x391;
    private static final char PIV = 0x3d6;
    private static final char OLINE = 0x203e;
    private static final char DIAMS = 0x2666;
    private static final char EURO = 0x20ac;
    private static final char TRADE = 0x2122;
    private static final char PRIME = 0x2032;
    private static final char PPRIME = 0x2033;

    private static boolean needsSymbolFont( char c )
    {
        // greek characters and mathematical symbols, except the euro and trade symbols
        // symbols I couldn't get to display in any font:
        // zwnj (0x200C), zwj (0x200D), lrm (0x200E), rlm (0x200F), oline (0x203E),
        // lceil (0x2038), rceil (0x2039), lfloor (0x203A), rfloor (0x203B)
        return ( c >= UPPER_ALPHA && c <= PIV )
                || ( c == PRIME || c == PPRIME )
                || ( c >= OLINE && c <= DIAMS && c != EURO && c != TRADE );
    }

    /**
     * Starts a page sequence.
     *
     * @param initPageNumber The initial page number. Should be either "0" (for the first page) or "auto".
     * @param headerText The text to write in the header, if null, nothing is written.
     * @param footerText The text to write in the footer, if null, nothing is written.
     */
    protected void startPageSequence( String initPageNumber, String chapterName, String headerText, String footerText )
    {
        writeln( "<fo:page-sequence initial-page-number=\"" + initPageNumber + "\" master-reference=\"body\">" );
        regionBefore( chapterName, headerText );
        regionAfter( footerText );
        writeln( "<fo:flow flow-name=\"xsl-region-body\">" );
        chapterHeading( null, true );
    }

    /**
     * Writes a 'xsl-region-before' block.
     *
     * @param headerText The text to write in the header, if null, nothing is written.
     */
    protected void regionBefore( String chapterName, String headerText )
    {
        // do nothing, overridden by AggregateSink
    }

    /**
     * Writes a 'xsl-region-after' block. By default does nothing, gets overridden by AggregateSink.
     *
     * @param footerText The text to write in the footer, if null, nothing is written.
     */
    protected void regionAfter( String footerText )
    {
        // do nothing, overridden by AggregateSink
    }

    /**
     * Writes a chapter heading. By default does nothing, gets overridden by AggregateSink.
     *
     * @param headerText The text to write in the header, if null, the current document title is written.
     * @param chapterNumber True if the chapter number should be written in front of the text.
     */
    protected void chapterHeading( String headerText, boolean chapterNumber )
    {
        // do nothing, overridden by AggregateSink
    }

    /**
     * Writes a fo:bookmark-tree. By default does nothing, gets overridden by AggregateSink.
     */
    protected void pdfBookmarks()
    {
        // do nothing, overridden by AggregateSink
    }

    /**
     * If debug mode is enabled, log the <code>msg</code> as is, otherwise add unique msg in <code>warnMessages</code>.
     *
     * @param key not null
     * @param msg not null
     * @see #close()
     * @since 1.1.1
     */
    protected void logMessage( String key, String msg )
    {
        msg = "[FO Sink] " + msg;
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( msg );

            return;
        }

        if ( warnMessages == null )
        {
            warnMessages = new HashMap<String, Set<String>>();
        }

        Set<String> set = warnMessages.get( key );
        if ( set == null )
        {
            set = new TreeSet<String>();
        }
        set.add( msg );
        warnMessages.put( key, set );
    }

    /** {@inheritDoc} */
    protected void init()
    {
        super.init();

        this.listStack.clear();
        this.tableGridStack.clear();
        this.cellJustifStack.clear();
        this.isCellJustifStack.clear();
        this.cellCountStack.clear();
        this.tableContentWriterStack.clear();
        this.tableCaptionWriterStack.clear();
        this.tableCaptionXMLWriterStack.clear();
        this.tableCaptionStack.clear();

        this.section = 0;
        this.subsection = 0;
        this.subsubsection = 0;
        this.verbatim = false;
        this.inFigure = false;
        this.warnMessages = null;
    }
}