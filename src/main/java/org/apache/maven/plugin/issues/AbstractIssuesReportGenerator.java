package org.apache.maven.plugin.issues;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.HtmlTools;
import org.codehaus.plexus.util.StringUtils;

import java.util.ResourceBundle;

/**
 * An abstract super class that helps when generating a report on issues.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public abstract class AbstractIssuesReportGenerator
{
    protected String author;

    protected String title;

    public AbstractIssuesReportGenerator()
    {
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor( String author )
    {
        this.author = author;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    protected void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        String title = null;
        if ( this.title != null )
        {
            title = this.title;
        }
        else
        {
            title = bundle.getString( "report.issues.header" );
        }
        sink.title();
        sink.text( title );
        sink.title_();

        if ( StringUtils.isNotEmpty( author ) )
        {
            sink.author();
            sink.text( author );
            sink.author_();
        }

        sink.head_();

        sink.body();

        sink.section1();

        sinkSectionTitle1Anchor( sink, title, title );
    }

    protected void sinkCell( Sink sink, String text )
    {
        sink.tableCell();

        if ( text != null )
        {
            sink.text( text );
        }
        else
        {
            sink.nonBreakingSpace();
        }

        sink.tableCell_();
    }

    protected void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();

        sinkLink( sink, text, link );

        sink.tableCell_();
    }

    protected void sinkEndReport( Sink sink )
    {
        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();
    }

    protected void sinkFigure( Sink sink, String image, String altText )
    {
        sink.figure();

        sink.figureGraphics( image );

        sink.figureCaption();

        sink.text( altText );

        sink.figureCaption_();

        sink.figure_();
    }

    protected void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();

        sink.text( header );

        sink.tableHeaderCell_();
    }

    protected void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );

        sink.text( text );

        sink.link_();
    }

    protected void sinkSectionTitle1Anchor( Sink sink, String text, String anchor )
    {
        sink.sectionTitle1();

        sink.text( text );

        sink.sectionTitle1_();

        sink.anchor( HtmlTools.encodeId( anchor ) );
        sink.anchor_();
    }

    protected void sinkSectionTitle2Anchor( Sink sink, String text, String anchor )
    {
        sink.sectionTitle2();
        sink.text( text );
        sink.sectionTitle2_();

        sink.anchor( HtmlTools.encodeId( anchor ) );
        sink.anchor_();
    }

    protected void sinkShowTypeIcon( Sink sink, String type )
    {
        String image = "";
        String altText = "";

        if ( type == null )
        {
            image = "images/icon_help_sml.gif";
            altText = "?";
        }
        else if ( type.equals( "fix" ) )
        {
            image = "images/fix.gif";
            altText = "fix";
        }
        else if ( type.equals( "update" ) )
        {
            image = "images/update.gif";
            altText = "update";
        }
        else if ( type.equals( "add" ) )
        {
            image = "images/add.gif";
            altText = "add";
        }
        else if ( type.equals( "remove" ) )
        {
            image = "images/remove.gif";
            altText = "remove";
        }

        sink.tableCell();

        sinkFigure( sink, image, altText );

        sink.tableCell_();
    }
}