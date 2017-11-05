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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.doxia.util.HtmlTools;

import org.codehaus.plexus.util.StringUtils;

/**
 * A Doxia Sink that produces an aggregated FO model. The usage is similar to the following:
 * <p/>
 * <pre>
 * FoAggregateSink sink = new FoAggregateSink( writer );
 * sink.setDocumentModel( documentModel );
 * sink.beginDocument();
 * sink.coverPage();
 * sink.toc();
 * ...
 * sink.endDocument();
 * </pre>
 * <p/>
 * <b>Note</b>: the documentModel object contains several
 * <a href="http://maven.apache.org/doxia/doxia/doxia-core/document.html">document metadata</a>, but only a few
 * of them are used in this sink (i.e. author, confidential, date and title), the others are ignored.
 *
 * @author ltheussl
 * @version $Id: FoAggregateSink.java 1788693 2017-03-25 21:59:56Z hboutemy $
 * @since 1.1
 */
public class FoAggregateSink
    extends FoSink
{
    /**
     * No Table Of Content.
     *
     * @see #setDocumentModel(DocumentModel, int)
     */
    public static final int TOC_NONE = 0;

    /**
     * Table Of Content at the start of the document.
     *
     * @see #setDocumentModel(DocumentModel, int)
     */
    public static final int TOC_START = 1;

    /**
     * Table Of Content at the end of the document.
     *
     * @see #setDocumentModel(DocumentModel, int)
     */
    public static final int TOC_END = 2;

    // TODO: make configurable
    private static final String COVER_HEADER_HEIGHT = "1.5in";

    /**
     * The document model to be used by this sink.
     */
    private DocumentModel docModel;

    /**
     * Counts the current chapter level.
     */
    private int chapter = 0;

    /**
     * Name of the source file of the current document, relative to the source root.
     */
    private String docName;

    /**
     * Title of the chapter, used in the page header.
     */
    private String docTitle = "";

    /**
     * Content in head is ignored in aggregated documents.
     */
    private boolean ignoreText;

    /**
     * Current position of the TOC, see {@link #TOC_POSITION}
     */
    private int tocPosition;

    /**
     * expected DocumentRendererContext
     */
    private Object context = null;
    
    /**
     * Used to get the current position in the TOC.
     */
    private final Stack<NumberedListItem> tocStack = new Stack<NumberedListItem>();

    /**
     * Constructor.
     *
     * @param writer The writer for writing the result.
     */
    public FoAggregateSink( Writer writer )
    {
        super( writer );
    }

    public FoAggregateSink( Writer writer, Object context )  
    {
        this( writer );
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public void head()
    {
        head( null );
    }

    /**
     * {@inheritDoc}
     */
    public void head( SinkEventAttributes attributes )
    {
        init();

        ignoreText = true;
    }

    /**
     * {@inheritDoc}
     */
    public void head_()
    {
        ignoreText = false;
        writeEOL();
    }

    /**
     * {@inheritDoc}
     */
    public void title()
    {
        title( null );
    }

    /**
     * {@inheritDoc}
     */
    public void title( SinkEventAttributes attributes )
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void title_()
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void author()
    {
        author( null );
    }

    /**
     * {@inheritDoc}
     */
    public void author( SinkEventAttributes attributes )
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void author_()
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void date()
    {
        date( null );
    }

    /**
     * {@inheritDoc}
     */
    public void date( SinkEventAttributes attributes )
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void date_()
    {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void body()
    {
        body( null );
    }

    /**
     * {@inheritDoc}
     */
    public void body( SinkEventAttributes attributes )
    {
        chapter++;

        resetSectionCounter();

        startPageSequence( getChapterName(), getHeaderText(), getFooterText() );

        if ( docName == null )
        {
            getLog().warn( "No document root specified, local links will not be resolved correctly!" );
        }
        else
        {
            writeStartTag( BLOCK_TAG, "" );
        }

    }
    
    public void setContext( Object context ) 
    {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public void body_()
    {
        writeEOL();
        writeEndTag( BLOCK_TAG );
        writeEndTag( FLOW_TAG );
        writeEndTag( PAGE_SEQUENCE_TAG );

        // reset document name
        docName = null;
    }

    /**
     * Sets the title of the current document. This is used as a chapter title in the page header.
     *
     * @param title the title of the current document.
     */
    public void setDocumentTitle( String title )
    {
        this.docTitle = title;

        if ( title == null )
        {
            this.docTitle = "";
        }
    }

    /**
     * Sets the name of the current source document, relative to the source root.
     * Used to resolve links to other source documents.
     *
     * @param name the name for the current document.
     */
    public void setDocumentName( String name )
    {
        this.docName = getIdName( name );
    }

    /**
     * Sets the DocumentModel to be used by this sink. The DocumentModel provides all the meta-information
     * required to render a document, eg settings for the cover page, table of contents, etc.
     * <br/>
     * By default, a TOC will be added at the beginning of the document.
     *
     * @param model the DocumentModel.
     * @see #setDocumentModel(DocumentModel, String)
     * @see #TOC_START
     */
    public void setDocumentModel( DocumentModel model )
    {
        setDocumentModel( model, TOC_START );
    }

    /**
     * Sets the DocumentModel to be used by this sink. The DocumentModel provides all the meta-information
     * required to render a document, eg settings for the cover page, table of contents, etc.
     *
     * @param model  the DocumentModel, could be null.
     * @param tocPos should be one of these values: {@link #TOC_NONE}, {@link #TOC_START} and {@link #TOC_END}.
     * @since 1.1.2
     */
    public void setDocumentModel( DocumentModel model, int tocPos )
    {
        this.docModel = model;
        if ( !( tocPos == TOC_NONE || tocPos == TOC_START || tocPos == TOC_END ) )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Unrecognized value for tocPosition: " + tocPos + ", using no toc." );
            }
            tocPos = TOC_NONE;
        }
        this.tocPosition = tocPos;

        if ( this.docModel != null && this.docModel.getToc() != null && this.tocPosition != TOC_NONE )
        {
            DocumentTOCItem tocItem = new DocumentTOCItem();
            tocItem.setName( this.docModel.getToc().getName() );
            tocItem.setRef( "./toc" );
            List<DocumentTOCItem> items = new LinkedList<DocumentTOCItem>();
            if ( this.tocPosition == TOC_START )
            {
                items.add( tocItem );
            }
            items.addAll( this.docModel.getToc().getItems() );
            if ( this.tocPosition == TOC_END )
            {
                items.add( tocItem );
            }

            this.docModel.getToc().setItems( items );
        }
    }

    /**
     * Translates the given name to a usable id.
     * Prepends "./" and strips any extension.
     *
     * @param name the name for the current document.
     * @return String
     */
    private String getIdName( String name )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            getLog().warn( "Empty document reference, links will not be resolved correctly!" );
            return "";
        }

        String idName = name.replace( '\\', '/' );

        // prepend "./" and strip extension
        if ( !idName.startsWith( "./" ) )
        {
            idName = "./" + idName;
        }

        if ( idName.substring( 2 ).lastIndexOf( "." ) != -1 )
        {
            idName = idName.substring( 0, idName.lastIndexOf( "." ) );
        }

        while ( idName.indexOf( "//" ) != -1 )
        {
            idName = StringUtils.replace( idName, "//", "/" );
        }

        return idName;
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void figureGraphics( String name )
    {
        figureGraphics( name, null );
    }

    /**
     * {@inheritDoc}
     */
    public void figureGraphics( String src, SinkEventAttributes attributes )
    {
        String anchor = src;

        while ( anchor.startsWith( "./" ) )
        {
            anchor = anchor.substring( 2 );
        }

        if ( anchor.startsWith( "../" ) && docName != null )
        {
            anchor = resolveLinkRelativeToBase( anchor );
        }

        super.figureGraphics( anchor, attributes );
    }

    /**
     * {@inheritDoc}
     */
    public void anchor( String name )
    {
        anchor( name, null );
    }

    /**
     * {@inheritDoc}
     */
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

        anchor = "#" + anchor;

        if ( docName != null )
        {
            anchor = docName + anchor;
        }

        writeStartTag( INLINE_TAG, "id", anchor );
    }

    /**
     * {@inheritDoc}
     */
    public void link( String name )
    {
        link( name, null );
    }

    /**
     * {@inheritDoc}
     */
    public void link( String name, SinkEventAttributes attributes )
    {
        if ( name == null )
        {
            throw new NullPointerException( "Link name cannot be null!" );
        }

        if ( DoxiaUtils.isExternalLink( name ) )
        {
            // external links
            writeStartTag( BASIC_LINK_TAG, "external-destination", HtmlTools.escapeHTML( name ) );
            writeStartTag( INLINE_TAG, "href.external" );
            return;
        }

        while ( name.indexOf( "//" ) != -1 )
        {
            name = StringUtils.replace( name, "//", "/" );
        }

        if ( DoxiaUtils.isInternalLink( name ) )
        {
            // internal link (ie anchor is in the same source document)
            String anchor = name.substring( 1 );

            if ( !DoxiaUtils.isValidId( anchor ) )
            {
                String tmp = anchor;
                anchor = DoxiaUtils.encodeId( anchor, true );

                String msg = "Modified invalid anchor name: '" + tmp + "' to '" + anchor + "'";
                logMessage( "modifiedLink", msg );
            }

            if ( docName != null )
            {
                anchor = docName + "#" + anchor;
            }

            writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( anchor ) );
            writeStartTag( INLINE_TAG, "href.internal" );
        }
        else if ( name.startsWith( "../" ) )
        {
            // local link (ie anchor is not in the same source document)

            if ( docName == null )
            {
                // can't resolve link without base, fop will issue a warning
                writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( name ) );
                writeStartTag( INLINE_TAG, "href.internal" );

                return;
            }

            String anchor = resolveLinkRelativeToBase( chopExtension( name ) );

            writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( anchor ) );
            writeStartTag( INLINE_TAG, "href.internal" );
        }
        else
        {
            // local link (ie anchor is not in the same source document)

            String anchor = name;

            if ( anchor.startsWith( "./" ) )
            {
                this.link( anchor.substring( 2 ) );
                return;
            }

            anchor = chopExtension( anchor );

            String base = docName.substring( 0, docName.lastIndexOf( "/" ) );
            anchor = base + "/" + anchor;

            writeStartTag( BASIC_LINK_TAG, "internal-destination", HtmlTools.escapeHTML( anchor ) );
            writeStartTag( INLINE_TAG, "href.internal" );
        }
    }

    // only call this if docName != null !!!
    private String resolveLinkRelativeToBase( String name )
    {
        String anchor = name;

        String base = docName.substring( 0, docName.lastIndexOf( "/" ) );

        if ( base.indexOf( "/" ) != -1 )
        {
            while ( anchor.startsWith( "../" ) )
            {
                base = base.substring( 0, base.lastIndexOf( "/" ) );

                anchor = anchor.substring( 3 );

                if ( base.lastIndexOf( "/" ) == -1 )
                {
                    while ( anchor.startsWith( "../" ) )
                    {
                        anchor = anchor.substring( 3 );
                    }
                    break;
                }
            }
        }

        return base + "/" + anchor;
    }

    private String chopExtension( String name )
    {
        String anchor = name;

        int dot = anchor.lastIndexOf( "." );

        if ( dot != -1 && dot != anchor.length() && anchor.charAt( dot + 1 ) != '/' )
        {
            int hash = anchor.indexOf( "#", dot );

            if ( hash != -1 )
            {
                int dot2 = anchor.indexOf( ".", hash );

                if ( dot2 != -1 )
                {
                    anchor =
                        anchor.substring( 0, dot ) + "#" + HtmlTools.encodeId( anchor.substring( hash + 1, dot2 ) );
                }
                else
                {
                    anchor = anchor.substring( 0, dot ) + "#" + HtmlTools.encodeId(
                        anchor.substring( hash + 1, anchor.length() ) );
                }
            }
            else
            {
                anchor = anchor.substring( 0, dot );
            }
        }

        return anchor;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a start tag, prepending EOL.
     */
    protected void writeStartTag( Tag tag, String attributeId )
    {
        if ( !ignoreText )
        {
            super.writeStartTag( tag, attributeId );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a start tag, prepending EOL.
     */
    protected void writeStartTag( Tag tag, String id, String name )
    {
        if ( !ignoreText )
        {
            super.writeStartTag( tag, id, name );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes an end tag, appending EOL.
     */
    protected void writeEndTag( Tag t )
    {
        if ( !ignoreText )
        {
            super.writeEndTag( t );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a simple tag, appending EOL.
     */
    protected void writeEmptyTag( Tag tag, String attributeId )
    {
        if ( !ignoreText )
        {
            super.writeEmptyTag( tag, attributeId );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a text, swallowing any exceptions.
     */
    protected void write( String text )
    {
        if ( !ignoreText )
        {
            super.write( text );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a text, appending EOL.
     */
    protected void writeln( String text )
    {
        if ( !ignoreText )
        {
            super.writeln( text );
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes content, escaping special characters.
     */
    protected void content( String text )
    {
        if ( !ignoreText )
        {
            super.content( text );
        }
    }

    /**
     * Writes EOL.
     */
    protected void newline()
    {
        if ( !ignoreText )
        {
            writeEOL();
        }
    }

    /**
     * Starts a page sequence, depending on the current chapter.
     *
     * @param headerText The text to write in the header, if null, nothing is written.
     * @param footerText The text to write in the footer, if null, nothing is written.
     */
    protected void startPageSequence( String chapterName, String headerText, String footerText )
    {
        if ( chapter == 1 )
        {
            startPageSequence( "0", chapterName, headerText, footerText );
        }
        else
        {
            startPageSequence( "auto", chapterName, headerText, footerText );
        }
    }

    /**
     * Returns the text to write in the header of each page.
     *
     * @return String
     */
    protected String getHeaderText()
    {
        if ( context != null ) 
        {
            //developer commentary, this part uses reflection because the class DocumentRenderer
            //is not available in this classpath. 
            if ( context.getClass( ).getCanonicalName( ).
                    equals( "org.apache.maven.doxia.docrenderer.DocumentRendererContext" ) )
            {
                try 
                {
                    Method method = context.getClass( ).getMethod ( "get", String.class );
                    Method containsKey = context.getClass( ).getMethod ( "containsKey", Object.class );
                    boolean hasOverride = ( Boolean ) containsKey.invoke ( context, "pdf.header" );
                    if ( hasOverride ) 
                    {
                        return escaped( ( String ) method.invoke ( context, "pdf.header" ), false );
                    }
                } 
                catch ( NoSuchMethodException ex    ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( SecurityException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( IllegalAccessException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( IllegalArgumentException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( InvocationTargetException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                }
            }
        }
        
        return "";
    }
    
    protected String getChapterName(){
        return Integer.toString( chapter ) + "   " + docTitle;
    }

    /**
     * Returns the text to write in the footer of each page.
     *
     * @return String
     */
    protected String getFooterText()
    {
        if ( context != null ) 
        {
            //developer commentary, this part uses reflection because the class DocumentRenderer
            //is not available in this classpath. 
            if ( context.getClass( ).getCanonicalName( ).
                    equals( "org.apache.maven.doxia.docrenderer.DocumentRendererContext" ) )
            {
                try 
                {
                    Method method = context.getClass( ).getMethod ( "get", String.class );
                    Method containsKey = context.getClass( ).getMethod ( "containsKey", Object.class );
                    boolean hasOverride = ( Boolean ) containsKey.invoke ( context, "pdf.footer" );
                    if ( hasOverride ) 
                    {
                        return escaped( ( String ) method.invoke ( context, "pdf.footer" ), false );
                    }
                } 
                catch ( NoSuchMethodException ex    ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( SecurityException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( IllegalAccessException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( IllegalArgumentException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                } 
                catch ( InvocationTargetException ex ) 
                {
                    getLog().debug( "error trapped looking for footer override", ex );
                }
            }
        }
        
        int actualYear;
        String add = " &#8226; " + getBundle( Locale.US ).getString( "footer.rights" );
        String companyName = "";

        if ( docModel != null && docModel.getMeta() != null && docModel.getMeta().isConfidential() )
        {
            add = add + " &#8226; " + getBundle( Locale.US ).getString( "footer.confidential" );
        }

        if ( docModel != null && docModel.getCover() != null && docModel.getCover().getCompanyName() != null )
        {
            companyName = docModel.getCover().getCompanyName();
        }

        if ( docModel != null && docModel.getMeta() != null && docModel.getMeta().getDate() != null )
        {
            Calendar date = Calendar.getInstance();
            date.setTime( docModel.getMeta().getDate() );
            actualYear = date.get( Calendar.YEAR );
        }
        else
        {
            actualYear = Calendar.getInstance().get( Calendar.YEAR );
        }

        return "&#169;" + actualYear + ", " + escaped( companyName, false ) + add;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Returns the current chapter number as a string.
     */
    protected String getChapterString()
    {
        return Integer.toString( chapter ) + ".";
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a 'xsl-region-before' block.
     */
    @Override
    protected void regionBefore( String chapterName, String headerText )
    {
        writeStartTag( STATIC_CONTENT_TAG, "flow-name", "xsl-region-before" );
        writeln( "<fo:table table-layout=\"fixed\" width=\"100%\" >" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.1666in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.1666in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.1666in" );
        writeStartTag( TABLE_BODY_TAG, "" );
        writeStartTag( TABLE_ROW_TAG, "" );
        writeStartTag( TABLE_CELL_TAG, "" );
        writeStartTag( BLOCK_TAG, "header.style" );
        
        //
        
        if ( chapterName != null )
        {
            write( chapterName );
        }

        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        
        writeStartTag( TABLE_CELL_TAG, "" );
        writeStartTag( BLOCK_TAG, "header.style" );
        if ( headerText != null )
        {
            write( headerText );
        }
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );

        
        writeStartTag( TABLE_CELL_TAG, "" );
        writeStartTag( BLOCK_TAG, "page.number" );
        writeEmptyTag( PAGE_NUMBER_TAG, "" );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );
        writeEndTag( TABLE_BODY_TAG );
        writeEndTag( TABLE_TAG );
        writeEndTag( STATIC_CONTENT_TAG );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a 'xsl-region-after' block.
     */
    protected void regionAfter( String footerText )
    {
        writeStartTag( STATIC_CONTENT_TAG, "flow-name", "xsl-region-after" );
        writeStartTag( BLOCK_TAG, "footer.style" );
       
        if ( footerText != null )
        {
            write( footerText );
        }
        writeEndTag( BLOCK_TAG );
        writeEndTag( STATIC_CONTENT_TAG );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a chapter heading.
     */
    protected void chapterHeading( String headerText, boolean chapterNumber )
    {
        if ( docName == null )
        {
            getLog().warn( "No document root specified, local links will not be resolved correctly!" );
            writeStartTag( BLOCK_TAG, "" );
        }
        else
        {
            writeStartTag( BLOCK_TAG, "id", docName );
        }

        writeStartTag( LIST_BLOCK_TAG, "" );
        writeStartTag( LIST_ITEM_TAG, "" );
        writeln( "<fo:list-item-label end-indent=\"6.375in\" start-indent=\"-1in\">" );
        writeStartTag( BLOCK_TAG, "outdented.number.style" );

        if ( chapterNumber )
        {
            writeStartTag( BLOCK_TAG, "chapter.title" );
            write( Integer.toString( chapter ) );
            writeEndTag( BLOCK_TAG );
        }

        writeEndTag( BLOCK_TAG );
        writeEndTag( LIST_ITEM_LABEL_TAG );
        writeln( "<fo:list-item-body end-indent=\"1in\" start-indent=\"0in\">" );
        writeStartTag( BLOCK_TAG, "chapter.title" );

        if ( headerText == null )
        {
            text( docTitle );
        }
        else
        {
            text( headerText );
        }

        writeEndTag( BLOCK_TAG );
        writeEndTag( LIST_ITEM_BODY_TAG );
        writeEndTag( LIST_ITEM_TAG );
        writeEndTag( LIST_BLOCK_TAG );
        writeEndTag( BLOCK_TAG );
        writeStartTag( BLOCK_TAG, "space-after.optimum", "0em" );
        writeEmptyTag( LEADER_TAG, "chapter.rule" );
        writeEndTag( BLOCK_TAG );
    }

    /**
     * Writes a table of contents. The DocumentModel has to contain a DocumentTOC for this to work.
     */
    public void toc()
    {
        if ( docModel == null || docModel.getToc() == null || docModel.getToc().getItems() == null
            || this.tocPosition == TOC_NONE )
        {
            return;
        }

        DocumentTOC toc = docModel.getToc();

        writeln( "<fo:page-sequence master-reference=\"toc\" initial-page-number=\"1\" format=\"i\">" );
        regionBefore( toc.getName(), getHeaderText() );
        
        regionAfter( getFooterText() );
        writeStartTag( FLOW_TAG, "flow-name", "xsl-region-body" );
        writeStartTag( BLOCK_TAG, "id", "./toc" );
        chapterHeading( toc.getName(), false );
        writeln( "<fo:table table-layout=\"fixed\" width=\"100%\" >" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "0.45in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "0.4in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "0.4in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "5in" ); // TODO {$maxBodyWidth - 1.25}in
        writeStartTag( TABLE_BODY_TAG );

        writeTocItems( toc.getItems(), 1 );

        writeEndTag( TABLE_BODY_TAG );
        writeEndTag( TABLE_TAG );
        writeEndTag( BLOCK_TAG );
        writeEndTag( FLOW_TAG );
        writeEndTag( PAGE_SEQUENCE_TAG );
    }

    private void writeTocItems( List<DocumentTOCItem> tocItems, int level )
    {
        final int maxTocLevel = 4;

        if ( level < 1 || level > maxTocLevel )
        {
            return;
        }

        tocStack.push( new NumberedListItem( NUMBERING_DECIMAL ) );

        for ( DocumentTOCItem tocItem : tocItems )
        {
            String ref = getIdName( tocItem.getRef() );

            writeStartTag( TABLE_ROW_TAG, "keep-with-next", "auto" );

            if ( level > 2 )
            {
                for ( int i = 0; i < level - 2; i++ )
                {
                    writeStartTag( TABLE_CELL_TAG );
                    writeSimpleTag( BLOCK_TAG );
                    writeEndTag( TABLE_CELL_TAG );
                }
            }

            writeStartTag( TABLE_CELL_TAG, "toc.cell" );
            writeStartTag( BLOCK_TAG, "toc.number.style" );

            NumberedListItem current = tocStack.peek();
            current.next();
            write( currentTocNumber() );

            writeEndTag( BLOCK_TAG );
            writeEndTag( TABLE_CELL_TAG );

            String span = "3";

            if ( level > 2 )
            {
                span = Integer.toString( 5 - level );
            }

            writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", span, "toc.cell" );
            MutableAttributeSet atts = getFoConfiguration().getAttributeSet( "toc.h" + level + ".style" );
            atts.addAttribute( "text-align-last", "justify" );
            writeStartTag( BLOCK_TAG, atts );
            writeStartTag( BASIC_LINK_TAG, "internal-destination", ref );
            text( tocItem.getName() );
            writeEndTag( BASIC_LINK_TAG );
            writeEmptyTag( LEADER_TAG, "toc.leader.style" );
            writeStartTag( INLINE_TAG, "page.number" );
            writeEmptyTag( PAGE_NUMBER_CITATION_TAG, "ref-id", ref );
            writeEndTag( INLINE_TAG );
            writeEndTag( BLOCK_TAG );
            writeEndTag( TABLE_CELL_TAG );
            writeEndTag( TABLE_ROW_TAG );

            if ( tocItem.getItems() != null )
            {
                writeTocItems( tocItem.getItems(), level + 1 );
            }
        }

        tocStack.pop();
    }

    private String currentTocNumber()
    {
        StringBuilder ch = new StringBuilder( tocStack.get( 0 ).getListItemSymbol() );

        for ( int i = 1; i < tocStack.size(); i++ )
        {
            ch.append( "." + tocStack.get( i ).getListItemSymbol() );
        }

        return ch.toString();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Writes a fo:bookmark-tree. The DocumentModel has to contain a DocumentTOC for this to work.
     */
    protected void pdfBookmarks()
    {
        if ( docModel == null || docModel.getToc() == null )
        {
            return;
        }

        writeStartTag( BOOKMARK_TREE_TAG );

        renderBookmarkItems( docModel.getToc().getItems() );

        writeEndTag( BOOKMARK_TREE_TAG );
    }

    private void renderBookmarkItems( List<DocumentTOCItem> items )
    {
        for ( DocumentTOCItem tocItem : items )
        {
            String ref = getIdName( tocItem.getRef() );

            writeStartTag( BOOKMARK_TAG, "internal-destination", ref );
            writeStartTag( BOOKMARK_TITLE_TAG );
            text( tocItem.getName() );
            writeEndTag( BOOKMARK_TITLE_TAG );

            if ( tocItem.getItems() != null )
            {
                renderBookmarkItems( tocItem.getItems() );
            }

            writeEndTag( BOOKMARK_TAG );
        }
    }

    /**
     * Writes a cover page. The DocumentModel has to contain a DocumentMeta for this to work.
     */
    public void coverPage()
    {
        if ( this.docModel == null )
        {
            return;
        }

        DocumentCover cover = docModel.getCover();
        DocumentMeta meta = docModel.getMeta();

        if ( cover == null && meta == null )
        {
            return; // no information for cover page: ignore
        }

        // TODO: remove hard-coded settings

        writeStartTag( PAGE_SEQUENCE_TAG, "master-reference", "cover-page" );
        writeStartTag( FLOW_TAG, "flow-name", "xsl-region-body" );
        writeStartTag( BLOCK_TAG, "text-align", "center" );
        writeln( "<fo:table table-layout=\"fixed\" width=\"100%\" >" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "3.125in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "3.125in" );
        writeStartTag( TABLE_BODY_TAG );

        writeCoverHead( cover );
        writeCoverBody( cover, meta );
        writeCoverFooter( cover, meta );

        writeEndTag( TABLE_BODY_TAG );
        writeEndTag( TABLE_TAG );
        writeEndTag( BLOCK_TAG );
        writeEndTag( FLOW_TAG );
        writeEndTag( PAGE_SEQUENCE_TAG );
    }

    private void writeCoverHead( DocumentCover cover )
    {
        if ( cover == null )
        {
            return;
        }

        String compLogo = cover.getCompanyLogo();
        String projLogo = cover.getProjectLogo();

        writeStartTag( TABLE_ROW_TAG, "height", COVER_HEADER_HEIGHT );
        writeStartTag( TABLE_CELL_TAG );

        if ( StringUtils.isNotEmpty( compLogo ) )
        {
            SinkEventAttributeSet atts = new SinkEventAttributeSet();
            atts.addAttribute( "text-align", "left" );
            atts.addAttribute( "vertical-align", "top" );
            writeStartTag( BLOCK_TAG, atts );
            figureGraphics( compLogo, getGraphicsAttributes( compLogo ) );
            writeEndTag( BLOCK_TAG );
        }

        writeSimpleTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeStartTag( TABLE_CELL_TAG );

        if ( StringUtils.isNotEmpty( projLogo ) )
        {
            SinkEventAttributeSet atts = new SinkEventAttributeSet();
            atts.addAttribute( "text-align", "right" );
            atts.addAttribute( "vertical-align", "top" );
            writeStartTag( BLOCK_TAG, atts );
            figureGraphics( projLogo, getGraphicsAttributes( projLogo ) );
            writeEndTag( BLOCK_TAG );
        }

        writeSimpleTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );
    }

    private void writeCoverBody( DocumentCover cover, DocumentMeta meta )
    {
        if ( cover == null && meta == null )
        {
            return;
        }

        String subtitle = null;
        String title = null;
        String type = null;
        String version = null;
        if ( cover == null )
        {
            // aleady checked that meta != null
            getLog().debug( "The DocumentCover is not defined, using the DocumentMeta title as cover title." );
            title = meta.getTitle();
        }
        else
        {
            subtitle = cover.getCoverSubTitle();
            title = cover.getCoverTitle();
            type = cover.getCoverType();
            version = cover.getCoverVersion();
        }

        writeln( "<fo:table-row keep-with-previous=\"always\" height=\"0.014in\">" );
        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2" );
        writeStartTag( BLOCK_TAG, "line-height", "0.014in" );
        writeEmptyTag( LEADER_TAG, "chapter.rule" );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeStartTag( TABLE_ROW_TAG, "height", "7.447in" );
        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2" );
        writeln( "<fo:table table-layout=\"fixed\" width=\"100%\" >" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.083in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.083in" );
        writeEmptyTag( TABLE_COLUMN_TAG, "column-width", "2.083in" );

        writeStartTag( TABLE_BODY_TAG );

        writeStartTag( TABLE_ROW_TAG );
        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "3" );
        writeSimpleTag( BLOCK_TAG );
        writeEmptyTag( BLOCK_TAG, "space-before", "3.2235in" );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeStartTag( TABLE_ROW_TAG );
        writeStartTag( TABLE_CELL_TAG );
        writeEmptyTag( BLOCK_TAG, "space-after", "0.5in" );
        writeEndTag( TABLE_CELL_TAG );

        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2", "cover.border.left" );
        writeStartTag( BLOCK_TAG, "cover.title" );
        text( title == null ? "" : title );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeStartTag( TABLE_ROW_TAG );
        writeStartTag( TABLE_CELL_TAG );
        writeSimpleTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );

        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2", "cover.border.left.bottom" );
        writeStartTag( BLOCK_TAG, "cover.subtitle" );
        text( subtitle == null ? ( version == null ? "" : " v. " + version ) : subtitle );
        writeEndTag( BLOCK_TAG );
        writeStartTag( BLOCK_TAG, "cover.subtitle" );
        text( type == null ? "" : type );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeEndTag( TABLE_BODY_TAG );
        writeEndTag( TABLE_TAG );

        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeStartTag( TABLE_ROW_TAG, "height", "0.014in" );
        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2" );
        writeln( "<fo:block space-after=\"0.2in\" line-height=\"0.014in\">" );
        writeEmptyTag( LEADER_TAG, "chapter.rule" );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );

        writeStartTag( TABLE_ROW_TAG );
        writeStartTag( TABLE_CELL_TAG, "number-columns-spanned", "2" );
        writeSimpleTag( BLOCK_TAG );
        writeEmptyTag( BLOCK_TAG, "space-before", "0.2in" );
        writeEndTag( TABLE_CELL_TAG );
        writeEndTag( TABLE_ROW_TAG );
    }

    private void writeCoverFooter( DocumentCover cover, DocumentMeta meta )
    {
        if ( cover == null && meta == null )
        {
            return;
        }

        String date = null;
        String compName = null;
        if ( cover == null )
        {
            // aleady checked that meta != null
            getLog().debug( "The DocumentCover is not defined, using the DocumentMeta author as company name." );
            compName = meta.getAuthor();
        }
        else
        {
            compName = cover.getCompanyName();

            if ( cover.getCoverdate() == null )
            {
                cover.setCoverDate( new Date() );
                date = cover.getCoverdate();
                cover.setCoverDate( null );
            }
            else
            {
                date = cover.getCoverdate();
            }
        }

        writeStartTag( TABLE_ROW_TAG, "height", "0.3in" );

        writeStartTag( TABLE_CELL_TAG );
        MutableAttributeSet att = getFoConfiguration().getAttributeSet( "cover.subtitle" );
        att.addAttribute( "height", "0.3in" );
        att.addAttribute( "text-align", "left" );
        writeStartTag( BLOCK_TAG, att );
        text( compName == null ? ( cover.getAuthor() == null ? "" : cover.getAuthor() ) : compName );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );

        writeStartTag( TABLE_CELL_TAG );
        att = getFoConfiguration().getAttributeSet( "cover.subtitle" );
        att.addAttribute( "height", "0.3in" );
        att.addAttribute( "text-align", "right" );
        writeStartTag( BLOCK_TAG, att );
        text( date == null ? "" : date );
        writeEndTag( BLOCK_TAG );
        writeEndTag( TABLE_CELL_TAG );

        writeEndTag( TABLE_ROW_TAG );
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "doxia-fo", locale, this.getClass().getClassLoader() );
    }

    private SinkEventAttributeSet getGraphicsAttributes( String logo )
    {
        MutableAttributeSet atts = null;

        try
        {
            atts = DoxiaUtils.getImageAttributes( logo );
        }
        catch ( IOException e )
        {
            getLog().debug( e );
        }

        if ( atts == null )
        {
            return new SinkEventAttributeSet( new String[]{ SinkEventAttributes.HEIGHT, COVER_HEADER_HEIGHT } );
        }

        // FOP dpi: 72
        // Max width : 3.125 inch, table cell size, see #coverPage()
        final int maxWidth = 225; // 3.125 * 72

        if ( Integer.parseInt( atts.getAttribute( SinkEventAttributes.WIDTH ).toString() ) > maxWidth )
        {
            atts.addAttribute( "content-width", "3.125in" );
        }

        return new SinkEventAttributeSet( atts );
    }
}
