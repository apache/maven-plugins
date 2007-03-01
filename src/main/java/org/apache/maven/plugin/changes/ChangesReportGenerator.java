package org.apache.maven.plugin.changes;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.doxia.module.HtmlTools;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;

import org.apache.commons.lang.StringUtils;

/**
 * Generates a changes report.
 *
 * @version $Id$
 */
public class ChangesReportGenerator
{
    private ChangesXML report;

    private String issueLink;

    private String url;

    public ChangesReportGenerator()
    {
    }

    public ChangesReportGenerator( String xmlPath, Log log )
    {
        report = new ChangesXML( xmlPath, log );
    }

    public void setIssueLink( String issueLink )
    {
        this.issueLink = issueLink;
    }

    public String getIssueLink()
    {
        return issueLink;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void doGenerateEmptyReport( ResourceBundle bundle, Sink sink, String message )
    {
        sinkBeginReport( sink, bundle );

        sink.text( message );

        sinkEndReport( sink );
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink )
    {
        sinkBeginReport( sink, bundle );

        constructReleaseHistory( sink, bundle );

        constructReleases( sink, bundle );

        sinkEndReport( sink );
    }

    private void constructActions( Sink sink, List actionList, ResourceBundle bundle )
    {
        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.changes.label.type" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.changes" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.by" ) );

        sink.tableRow_();

        for ( int idx = 0; idx < actionList.size(); idx++ )
        {
            Action action = (Action) actionList.get( idx );

            sink.tableRow();

            sinkShowTypeIcon( sink, action.getType() );

            sink.tableCell();

            sink.rawText( action.getAction() );

            if ( StringUtils.isNotEmpty( action.getIssue() ) )
            {
                sink.text( " " + bundle.getString( "report.changes.text.fixes" ) + " " );

                if ( StringUtils.isEmpty( url ) )
                {
                    sink.text( action.getIssue() );

                }
                else
                {
                    sink.link( parseIssueLink( action.getIssue() ) );

                    sink.text( action.getIssue() );

                    sink.link_();

                }
                sink.text( "." );
            }

            if ( StringUtils.isNotEmpty( action.getDueTo() ) )
            {
                sink.text( " " + bundle.getString( "report.changes.text.thanx" ) + " " );

                if ( StringUtils.isNotEmpty( action.getDueToEmail() ) )
                {
                    sinkLink( sink, action.getDueTo(), "mailto:" + action.getDueToEmail() );
                }
                else
                {
                    sink.text( action.getDueTo() );
                }

                sink.text( "." );
            }

            sink.tableCell_();

            sinkCellLink( sink, action.getDev(), "team-list.html#" + action.getDev() );

            sink.tableRow_();
        }

        sink.table_();
    }

    private void constructReleaseHistory( Sink sink, ResourceBundle bundle )
    {
        sinkSectionTitle2Anchor( sink, bundle.getString( "report.changes.label.releasehistory" ), bundle
            .getString( "report.changes.label.releasehistory" ) );

        List releaseList = report.getReleaseList();

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.changes.label.version" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.date" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.description" ) );

        sink.tableRow_();

        for ( int idx = 0; idx < releaseList.size(); idx++ )
        {
            Release release = (Release) releaseList.get( idx );

            sink.tableRow();

            sinkCellLink( sink, release.getVersion(), "#" + HtmlTools.encodeId( release.getVersion() ) );

            sinkCell( sink, release.getDateRelease() );

            sinkCell( sink, release.getDescription() );

            sink.tableRow_();
        }

        sink.table_();

        sink.lineBreak();

        // @todo Temporarily commented out until MCHANGES-46 is completely solved
        //        sink.rawText( bundle.getString( "report.changes.text.rssfeed" ) );
        //        sink.text( " " );
        //        sink.link( "changes.rss" );
        //        sinkFigure( "images/rss.png", sink );
        //        sink.link_();
        //
        //        sink.lineBreak();

        sink.lineBreak();
    }

    private void constructReleases( Sink sink, ResourceBundle bundle )
    {
        List releaseList = report.getReleaseList();

        for ( int idx = 0; idx < releaseList.size(); idx++ )
        {
            Release release = (Release) releaseList.get( idx );

            sinkSectionTitle2Anchor( sink, bundle.getString( "report.changes.label.release" ) + " "
                + release.getVersion() + " - " + release.getDateRelease(), release.getVersion() );

            constructActions( sink, release.getAction(), bundle );
        }
    }

    private String parseIssueLink( String issue )
    {
        String parseLink = "";

        String url = this.url.substring( 0, this.url.lastIndexOf( "/" ) );

        parseLink = this.issueLink.replaceFirst( "%ISSUE%", issue );

        parseLink = parseLink.replaceFirst( "%URL%", url );

        return parseLink;
    }

    private void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        sink.text( bundle.getString( "report.changes.header" ) );

        sink.head_();

        sink.body();

        sinkSectionTitle1Anchor( sink, bundle.getString( "report.changes.header" ), bundle
            .getString( "report.changes.header" ) );
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();

        sink.text( text );

        sink.tableCell_();
    }

    private void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();

        sinkLink( sink, text, link );

        sink.tableCell_();
    }

    private void sinkEndReport( Sink sink )
    {
        sink.body_();

        sink.flush();

        sink.close();
    }

    private void sinkFigure( String image, Sink sink )
    {
        sink.figure();

        sink.figureGraphics( image );

        sink.figure_();
    }

    private void sinkFigure( String image, Sink sink, String altText )
    {
        sink.figure();

        sink.figureGraphics( image );

        sink.figureCaption();

        sink.text( altText );

        sink.figureCaption_();

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();

        sink.text( header );

        sink.tableHeaderCell_();
    }

    private void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );

        sink.text( text );

        sink.link_();
    }

    private void sinkSectionTitle1Anchor( Sink sink, String text, String anchor )
    {
        sink.sectionTitle1();
        sink.anchor( anchor );
        sink.anchor_();
        sink.text( text );
        sink.sectionTitle1_();
    }

    private void sinkSectionTitle2Anchor( Sink sink, String text, String anchor )
    {
        sink.sectionTitle2();
        sink.anchor( anchor );
        sink.anchor_();
        sink.text( text );
        sink.sectionTitle2_();
    }

    private void sinkShowTypeIcon( Sink sink, String type )
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

        sinkFigure( image, sink, altText );

        sink.tableCell_();
    }

}
