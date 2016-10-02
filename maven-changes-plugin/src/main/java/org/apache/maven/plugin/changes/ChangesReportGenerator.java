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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.plugin.issues.AbstractIssuesReportGenerator;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Component;
import org.apache.maven.plugins.changes.model.DueTo;
import org.apache.maven.plugins.changes.model.FixedIssue;
import org.apache.maven.plugins.changes.model.Release;

/**
 * Generates a changes report.
 *
 * @version $Id$
 */
public class ChangesReportGenerator
    extends AbstractIssuesReportGenerator
{

    /**
     * The token in {@link #issueLinksPerSystem} denoting the base URL for the issue management.
     */
    private static final String URL_TOKEN = "%URL%";

    /**
     * The token in {@link #issueLinksPerSystem} denoting the issue ID.
     */
    private static final String ISSUE_TOKEN = "%ISSUE%";

    static final String DEFAULT_ISSUE_SYSTEM_KEY = "default";

    private static final String NO_TEAMLIST = "none";

    /**
     * The issue management system to use, for actions that do not specify a system.
     *
     * @since 2.4
     */
    private String system;

    private String teamlist;

    private String url;

    private Map<String, String> issueLinksPerSystem;

    private boolean addActionDate;

    private boolean linkToFeed;

    /**
     * @since 2.4
     */
    private boolean escapeHTML;

    /**
     * @since 2.4
     */
    private List<Release> releaseList;

    public ChangesReportGenerator()
    {
        issueLinksPerSystem = new HashMap<String, String>();
    }

    public ChangesReportGenerator( List<Release> releaseList )
    {
        this();
        this.releaseList = releaseList;
    }

    /**
     * @since 2.4
     */
    public boolean isEscapeHTML()
    {
        return escapeHTML;
    }

    /**
     * @since 2.4
     */
    public void setEscapeHTML( boolean escapeHTML )
    {
        this.escapeHTML = escapeHTML;
    }

    /**
     * @since 2.4
     */
    public String getSystem()
    {
        return system;
    }

    /**
     * @since 2.4
     */
    public void setSystem( String system )
    {
        this.system = system;
    }

    public void setTeamlist( final String teamlist )
    {
        this.teamlist = teamlist;
    }

    public String getTeamlist()
    {
        return teamlist;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public Map<String, String> getIssueLinksPerSystem()
    {
        return issueLinksPerSystem;
    }

    public void setIssueLinksPerSystem( Map<String, String> issueLinksPerSystem )
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

    public boolean isLinkToFeed()
    {
        return linkToFeed;
    }

    public void setLinkToFeed( boolean generateLinkTofeed )
    {
        this.linkToFeed = generateLinkTofeed;
    }

    /**
     * Checks whether links to the issues can be generated for the given system.
     *
     * @param system The issue management system
     * @return <code>true</code> if issue links can be generated, <code>false</code> otherwise.
     */
    public boolean canGenerateIssueLinks( String system )
    {
        if ( !this.issueLinksPerSystem.containsKey( system ) )
        {
            return false;
        }
        String issueLink = this.issueLinksPerSystem.get( system );

        // If the issue link entry is blank then no links are possible
        if ( StringUtils.isBlank( issueLink ) )
        {
            return false;
        }

        // If the %URL% token is used then the issue management system URL must be set.
        if ( issueLink.contains( URL_TOKEN ) && StringUtils.isBlank( getUrl() ) )
        {
            return false;
        }
        return true;
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

        constructReleaseHistory( sink, bundle, releaseList );

        constructReleases( sink, bundle, releaseList );

        sinkEndReport( sink );
    }

    /**
     * Constructs table row for specified action with all calculated content (e.g. issue link).
     *
     * @param sink Sink
     * @param bundle Resource bundle
     * @param action Action to generate content for
     */
    private void constructAction( Sink sink, ResourceBundle bundle, Action action )
    {
        sink.tableRow();

        sinkShowTypeIcon( sink, action.getType() );

        sink.tableCell();

        if ( escapeHTML )
        {
            sink.text( action.getAction() );
        }
        else
        {
            sink.rawText( action.getAction() );
        }

        // no null check needed classes from modello return a new ArrayList
        if ( StringUtils.isNotEmpty( action.getIssue() ) || ( !action.getFixedIssues().isEmpty() ) )
        {
            if ( StringUtils.isNotEmpty( action.getAction() ) )
            {
                sink.text( "." );
            }
            sink.text( " " + bundle.getString( "report.changes.text.fixes" ) + " " );

            // Try to get the issue management system specified in the changes.xml file
            String system = action.getSystem();
            // Try to get the issue management system configured in the POM
            if ( StringUtils.isEmpty( system ) )
            {
                system = this.system;
            }
            // Use the default issue management system
            if ( StringUtils.isEmpty( system ) )
            {
                system = DEFAULT_ISSUE_SYSTEM_KEY;
            }
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

        if ( NO_TEAMLIST.equals( teamlist ) )
        {
            sinkCell( sink, action.getDev() );
        }
        else
        {
            sinkCellLink( sink, action.getDev(), teamlist + "#" + action.getDev() );
        }

        if ( this.isAddActionDate() )
        {
            sinkCell( sink, action.getDate() );
        }

        sink.tableRow_();
    }

    /**
     * Construct a text or link that mention the people that helped with an action.
     *
     * @param sink The sink
     * @param action The action that was done
     * @param bundle A resource bundle for i18n
     * @param dueTos Other people that helped with an action
     */
    private void constructDueTo( Sink sink, Action action, ResourceBundle bundle, List<DueTo> dueTos )
    {

        // Create a Map with key : dueTo name, value : dueTo email
        Map<String, String> namesEmailMap = new LinkedHashMap<String, String>();

        // Only add the dueTo specified as attributes, if it has either a dueTo or a dueToEmail
        if ( StringUtils.isNotEmpty( action.getDueTo() ) || StringUtils.isNotEmpty( action.getDueToEmail() ) )
        {
            namesEmailMap.put( action.getDueTo(), action.getDueToEmail() );
        }

        for ( DueTo dueTo : dueTos )
        {
            namesEmailMap.put( dueTo.getName(), dueTo.getEmail() );
        }

        if ( namesEmailMap.isEmpty() )
        {
            return;
        }

        sink.text( " " + bundle.getString( "report.changes.text.thanx" ) + " " );
        int i = 0;
        for ( String currentDueTo : namesEmailMap.keySet() )
        {
            String currentDueToEmail = namesEmailMap.get( currentDueTo );
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

    /**
     * Construct links to the issues that were solved by an action.
     *
     * @param issue The issue specified by attributes
     * @param system The issue management system
     * @param sink The sink
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueLink( String issue, String system, Sink sink, List<FixedIssue> fixes )
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

        for ( Iterator<FixedIssue> iterator = fixes.iterator(); iterator.hasNext(); )
        {
            FixedIssue fixedIssue = iterator.next();
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
     * Construct a text that references (but does not link to) the issues that were solved by an action.
     *
     * @param issue The issue specified by attributes
     * @param sink The sink
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueText( String issue, Sink sink, List<FixedIssue> fixes )
    {
        if ( StringUtils.isNotEmpty( issue ) )
        {
            sink.text( issue );

            if ( !fixes.isEmpty() )
            {
                sink.text( ", " );
            }
        }

        for ( Iterator<FixedIssue> iterator = fixes.iterator(); iterator.hasNext(); )
        {
            FixedIssue fixedIssue = iterator.next();

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

    private void constructReleaseHistory( Sink sink, ResourceBundle bundle, List<Release> releaseList )
    {
        sink.section2();

        sink.sectionTitle2();
        sink.text( bundle.getString( "report.changes.label.releasehistory" ) );
        sink.sectionTitle2_();

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.issues.label.fixVersion" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.releaseDate" ) );

        sinkHeader( sink, bundle.getString( "report.changes.label.releaseDescription" ) );

        sink.tableRow_();

        for ( Release release : releaseList )
        {
            sink.tableRow();

            sinkCellLink( sink, release.getVersion(), "#" + HtmlTools.encodeId( release.getVersion() ) );

            sinkCell( sink, release.getDateRelease() );

            sinkCell( sink, release.getDescription() );

            sink.tableRow_();
        }

        sink.table_();

        // MCHANGES-46
        if ( linkToFeed )
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.changes.text.rssfeed" ) );
            sink.nonBreakingSpace();
            sink.link( "changes.rss" );
            sinkFigure( sink, "images/rss.png", "rss feed" );
            sink.link_();
            sink.paragraph_();
        }

        sink.section2_();
    }

    /**
     * Constructs document sections for each of specified releases.
     *
     * @param sink Sink
     * @param bundle Resource bundle
     * @param releaseList Releases to create content for
     */
    private void constructReleases( Sink sink, ResourceBundle bundle, List<Release> releaseList )
    {
        for ( Release release : releaseList )
        {
            constructRelease( sink, bundle, release );
        }
    }

    /**
     * Constructs document section for specified release.
     *
     * @param sink Sink
     * @param bundle Resource bundle
     * @param release Release to create document section for
     */
    private void constructRelease( Sink sink, ResourceBundle bundle, Release release )
    {
        sink.section2();

        final String date = ( release.getDateRelease() == null ) ? "" : " \u2013 " + release.getDateRelease();

        SinkEventAttributes attrs = new SinkEventAttributeSet();
        attrs.addAttribute( SinkEventAttributes.ID, HtmlTools.encodeId( release.getVersion() ) );
        sink.sectionTitle( Sink.SECTION_LEVEL_2, attrs );
        sink.text( bundle.getString( "report.changes.label.release" ) + " " + release.getVersion() + date );
        sink.sectionTitle_( Sink.SECTION_LEVEL_2 );

        if ( isReleaseEmpty( release ) )
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.changes.text.no.changes" ) );
            sink.paragraph_();
        }
        else
        {
            sink.table();

            sink.tableRow();
            sinkHeader( sink, bundle.getString( "report.issues.label.type" ) );
            sinkHeader( sink, bundle.getString( "report.issues.label.summary" ) );
            sinkHeader( sink, bundle.getString( "report.issues.label.assignee" ) );
            if ( this.isAddActionDate() )
            {
                sinkHeader( sink, bundle.getString( "report.issues.label.updated" ) );
            }
            sink.tableRow_();

            for ( Action action : release.getActions() )
            {
                constructAction( sink, bundle, action );
            }

            for ( Object o : release.getComponents() )
            {
                Component component = (Component) o;
                constructComponent( sink, bundle, component );
            }

            sink.table_();
        }

        sink.section2_();
    }

    /**
     * Constructs table rows for specified release component. It will create header row for component name and action
     * rows for all component issues.
     *
     * @param sink Sink
     * @param bundle Resource bundle
     * @param component Release component to generate content for.
     */
    private void constructComponent( Sink sink, ResourceBundle bundle, Component component )
    {
        if ( !component.getActions().isEmpty() )
        {
            sink.tableRow();

            sink.tableHeaderCell();
            sink.tableHeaderCell_();

            sink.tableHeaderCell();
            sink.text( component.getName() );
            sink.tableHeaderCell_();

            sink.tableHeaderCell();
            sink.tableHeaderCell_();

            if ( isAddActionDate() )
            {
                sink.tableHeaderCell();
                sink.tableHeaderCell_();
            }

            sink.tableRow_();

            for ( Action action : component.getActions() )
            {
                constructAction( sink, bundle, action );
            }
        }
    }

    /**
     * Checks if specified release contains own issues or issues inside the child components.
     *
     * @param release Release to check
     * @return <code>true</code> if release doesn't contain any issues, <code>false</code> otherwise
     */
    private boolean isReleaseEmpty( Release release )
    {
        if ( !release.getActions().isEmpty() )
        {
            return false;
        }

        for ( Object o : release.getComponents() )
        {
            Component component = (Component) o;
            if ( !component.getActions().isEmpty() )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Replace tokens in the issue link template with the real values.
     *
     * @param issue The issue identifier
     * @param system The issue management system
     * @return An interpolated issue link
     */
    private String parseIssueLink( String issue, String system )
    {
        String parseLink;
        String issueLink = (String) this.issueLinksPerSystem.get( system );
        parseLink = issueLink.replaceFirst( ISSUE_TOKEN, issue );
        if ( parseLink.contains( URL_TOKEN ) )
        {
            String url = this.url.substring( 0, this.url.lastIndexOf( "/" ) );
            parseLink = parseLink.replaceFirst( URL_TOKEN, url );
        }

        return parseLink;
    }

}
