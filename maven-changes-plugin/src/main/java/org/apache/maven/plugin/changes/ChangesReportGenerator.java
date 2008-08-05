package org.apache.maven.plugin.changes;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.DueTo;
import org.apache.maven.plugins.changes.model.FixedIssue;
import org.apache.maven.plugins.changes.model.Release;

/**
 * Generates a changes report.
 *
 * @version $Id$
 */
public class ChangesReportGenerator
{

    /**
     * The token in {@link #issueLink} denoting the base URL for the issue management.
     */
    private static final String URL_TOKEN = "%URL%";

    /**
     * The token in {@link #issueLink} denoting the issue ID.
     */
    private static final String ISSUE_TOKEN = "%ISSUE%";

    private static final String DEFAULT_ISSUE_SYSTEM_KEY = "default";

    private ChangesXML report;

    private String url;

    private Map issueLinksPerSystem;

    private boolean addActionDate;

    public ChangesReportGenerator()
    {
        issueLinksPerSystem = new HashMap();
    }

    public ChangesReportGenerator( File xmlPath, Log log )
    {
        this();
        report = new ChangesXML( xmlPath, log );
    }

    /**
     * @deprecated
     */
    public void setIssueLink( String issueLink )
    {
        if ( this.issueLinksPerSystem == null )
        {
            this.issueLinksPerSystem = new HashMap();
        }
        this.issueLinksPerSystem.put( DEFAULT_ISSUE_SYSTEM_KEY, issueLink );
    }

    /**
     * @deprecated
     */
    public String getIssueLink()
    {
        return (String) issueLinksPerSystem.get( DEFAULT_ISSUE_SYSTEM_KEY );
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public Map getIssueLinksPerSystem()
    {
        return issueLinksPerSystem;
    }

    public void setIssueLinksPerSystem( Map issueLinksPerSystem )
    {
        if ( this.issueLinksPerSystem != null && issueLinksPerSystem == null )
        {
            return;
        }
        this.issueLinksPerSystem = issueLinksPerSystem;
    }

    public boolean isAddActionDate()
    {
        return addActionDate;
    }

    public void setAddActionDate( boolean addActionDate )
    {
        this.addActionDate = addActionDate;
    }

    /**
     * Checks whether links to the issues can be generated.
     *
     * @return <code>true</code> if issue links can be generated, <code>false</code> otherwise.
     */
    public boolean canGenerateIssueLinks( String system )
    {
        if ( !this.issueLinksPerSystem.containsKey( system ) )
        {
            return false;
        }
        String issueLink = (String) this.issueLinksPerSystem.get( system );
        return !StringUtils.isBlank( issueLink )
            && ( !StringUtils.isBlank( getUrl() ) || issueLink.indexOf( URL_TOKEN ) < 0 );
    }

    public boolean canGenerateIssueLinks()
    {
        if ( this.issueLinksPerSystem == null || this.issueLinksPerSystem.isEmpty() )
        {
            return false;
        }
        return this.issueLinksPerSystem.containsKey( DEFAULT_ISSUE_SYSTEM_KEY );
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

        if ( this.isAddActionDate() )
        {
            sinkHeader( sink, bundle.getString( "report.changes.label.date" ) );
        }
        sink.tableRow_();

        for ( int idx = 0; idx < actionList.size(); idx++ )
        {
            Action action = (Action) actionList.get( idx );

            sink.tableRow();

            sinkShowTypeIcon( sink, action.getType() );

            sink.tableCell();

            sink.rawText( action.getAction() );

            // no null check needed classes from modello return a new ArrayList
            if ( StringUtils.isNotEmpty( action.getIssue() ) || ( !action.getFixedIssues().isEmpty() ) )
            {
                sink.text( " " + bundle.getString( "report.changes.text.fixes" ) + " " );

                String system = action.getSystem();
                system = StringUtils.isEmpty( system ) ? DEFAULT_ISSUE_SYSTEM_KEY : system;
                if ( !canGenerateIssueLinks( system ) )
                {
                    constructIssueText( action.getIssue(), sink, action.getFixedIssues() );
                }
                else
                {
                    constructIssueLink( action.getIssue(), system, sink, action.getFixedIssues() );
                }
                sink.text( "." );
            }

            if ( StringUtils.isNotEmpty( action.getDueTo() ) || ( !action.getDueTos().isEmpty() ) )
            {
                constructDueTo( sink, action, bundle, action.getDueTos() );
            }

            sink.tableCell_();

            sinkCellLink( sink, action.getDev(), "team-list.html#" + action.getDev() );

            if ( this.isAddActionDate() )
            {
                sinkCell( sink, action.getDate() );
            }

            sink.tableRow_();
        }

        sink.table_();
    }

    private void constructReleaseHistory( Sink sink, ResourceBundle bundle )
    {
        sink.section2();

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

        // @todo Temporarily commented out until MCHANGES-46 is completely solved
        //        sink.rawText( bundle.getString( "report.changes.text.rssfeed" ) );
        //        sink.text( " " );
        //        sink.link( "changes.rss" );
        //        sinkFigure( "images/rss.png", sink );
        //        sink.link_();
        //
        //        sink.lineBreak();

        sink.section2_();
    }

    private void constructReleases( Sink sink, ResourceBundle bundle )
    {
        List releaseList = report.getReleaseList();

        for ( int idx = 0; idx < releaseList.size(); idx++ )
        {
            Release release = (Release) releaseList.get( idx );

            sink.section2();

            sinkSectionTitle2Anchor( sink, bundle.getString( "report.changes.label.release" ) + " "
                + release.getVersion() + " - " + release.getDateRelease(), HtmlTools.encodeId( release.getVersion() ) );

            constructActions( sink, release.getActions(), bundle );

            sink.section2_();
        }
    }

    private String parseIssueLink( String issue, String system )
    {
        String parseLink;
        String issueLink = (String) this.issueLinksPerSystem.get( system );
        parseLink = issueLink.replaceFirst( ISSUE_TOKEN, issue );

        if ( parseLink.indexOf( URL_TOKEN ) >= 0 )
        {
            String url = this.url.substring( 0, this.url.lastIndexOf( "/" ) );
            parseLink = parseLink.replaceFirst( URL_TOKEN, url );
        }

        return parseLink;
    }

    private void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        sink.title();
        sink.text( bundle.getString( "report.changes.header" ) );
        sink.title_();

        if ( StringUtils.isNotEmpty( report.getAuthor() ) )
        {
            sink.author();
            sink.text( report.getAuthor() );
            sink.author_();
        }

        sink.head_();

        sink.body();

        sink.section1();

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
        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();
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

    /**
     * @param issue The issue specified by attributes
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueLink( String issue, String system, Sink sink, List fixes )
    {
        if ( StringUtils.isNotEmpty( issue ) )
        {
            sink.link( parseIssueLink( issue, system ) );

            sink.text( issue );

            sink.link_();

            if ( !fixes.isEmpty() )
            {
                sink.text( ", " );
            }
        }

        for ( Iterator iterator = fixes.iterator(); iterator.hasNext(); )
        {
            FixedIssue fixedIssue = (FixedIssue) iterator.next();
            String currentIssueId = fixedIssue.getIssue();
            if ( StringUtils.isNotEmpty( currentIssueId ) )
            {
                sink.link( parseIssueLink( currentIssueId, system ) );

                sink.text( currentIssueId );

                sink.link_();
            }

            if ( iterator.hasNext() )
            {
                sink.text( ", " );
            }
        }
    }

    /**
     * @param issue The issue specified by attributes
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueText( String issue, Sink sink, List fixes )
    {
        if ( StringUtils.isNotEmpty( issue ) )
        {
            sink.text( issue );

            if ( !fixes.isEmpty() )
            {
                sink.text( ", " );
            }
        }

        for ( Iterator iterator = fixes.iterator(); iterator.hasNext(); )
        {
            FixedIssue fixedIssue = (FixedIssue) iterator.next();

            String currentIssueId = fixedIssue.getIssue();
            if ( StringUtils.isNotEmpty( currentIssueId ) )
            {
                sink.text( currentIssueId );
            }

            if ( iterator.hasNext() )
            {
                sink.text( ", " );
            }
        }
    }

    /**
     *
     * @param sink
     * @param action
     * @param bundle
     */
    private void constructDueTo( Sink sink, Action action, ResourceBundle bundle, List dueTos )
    {

        // Create a Map with key : dueTo name, value : dueTo email
        Map namesEmailMap = new LinkedHashMap();

        // Only add the dueTo specified as attributes, if it has either a dueTo or a dueToEmail
        if ( StringUtils.isNotEmpty( action.getDueTo() ) || StringUtils.isNotEmpty( action.getDueToEmail() ) )
        {
            namesEmailMap.put( action.getDueTo(), action.getDueToEmail() );
        }

        for (Iterator iterator = dueTos.iterator();iterator.hasNext();)
        {
            DueTo dueTo = (DueTo) iterator.next();
            namesEmailMap.put( dueTo.getName(), dueTo.getEmail() );
        }

        if (namesEmailMap.isEmpty())
        {
            return;
        }

        sink.text( " " + bundle.getString( "report.changes.text.thanx" ) + " " );
        int i = 0;
        for (Iterator iterator = namesEmailMap.keySet().iterator(); iterator.hasNext();)
        {
            String currentDueTo = (String) iterator.next();
            String currentDueToEmail = (String) namesEmailMap.get( currentDueTo );
            i++;

            if ( StringUtils.isNotEmpty( currentDueToEmail ) )
            {
                sinkLink( sink, currentDueTo, "mailto:" + currentDueToEmail );
            }
            else if ( StringUtils.isNotEmpty( currentDueTo ) )
            {
                sink.text( currentDueTo );
            }

            if ( i < namesEmailMap.size() )
            {
                sink.text( ", " );
            }
        }

        sink.text( "." );
    }

}
