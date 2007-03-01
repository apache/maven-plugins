package org.apache.maven.plugin.jira;

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

import org.apache.maven.doxia.sink.Sink;

/**
 * Generates a JIRA report.
 *
 * @version $Id$
 */
public class JiraReportGenerator
{
    private JiraXML jira;

    public JiraReportGenerator()
    {

    }

    public JiraReportGenerator( String xmlPath )
    {
        jira = new JiraXML( xmlPath );
    }

    public void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sinkBeginReport( sink, bundle );

        sink.text( "An error occured that made it impossible to generate this report." );

        sinkEndReport( sink );
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink )
    {
        List issueList = jira.getIssueList();

        sinkBeginReport( sink, bundle );

        constructHeaderRow( sink, issueList, bundle );

        constructDetailRows( sink, issueList );

        sinkEndReport( sink );
    }

    private void constructHeaderRow( Sink sink, List issueList, ResourceBundle bundle )
    {
        if ( issueList == null )
        {
            return;
        }

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.jira.label.key" ) );

        sinkHeader( sink, bundle.getString( "report.jira.label.summary" ) );

        sinkHeader( sink, bundle.getString( "report.jira.label.status" ) );

        sinkHeader( sink, bundle.getString( "report.jira.label.resolution" ) );

        sinkHeader( sink, bundle.getString( "report.jira.label.by" ) );

        sink.tableRow_();
    }

    private void constructDetailRows( Sink sink, List issueList )
    {
        if ( issueList == null )
        {
            return;
        }

        for ( int idx = 0; idx < issueList.size(); idx++ )
        {
            JiraIssue issue = (JiraIssue) issueList.get( idx );

            sink.tableRow();

            sink.tableCell();

            sink.link( issue.getLink() );

            sink.text( issue.getKey() );

            sink.link_();

            sink.tableCell_();

            sinkCell( sink, issue.getSummary() );

            sinkCell( sink, issue.getStatus() );

            sinkCell( sink, issue.getResolution() );

            sinkCell( sink, issue.getAssignee() );

            sink.tableRow_();
        }

        sink.table_();
    }

    private void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        sink.text( bundle.getString( "report.jira.header" ) );

        sink.head_();

        sink.body();

        sinkSectionTitle1( sink, bundle.getString( "report.jira.header" ) );

    }

    private void sinkEndReport( Sink sink )
    {
        sink.body_();

        sink.flush();

        sink.close();
    }

    private void sinkFigure( Sink sink, String image )
    {
        sink.figure();

        sink.figureGraphics( image );

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();

        sink.text( header );

        sink.tableHeaderCell_();
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();

        if ( text != null )
        {
            sink.rawText( text );
        }
        else
        {
            sink.rawText( "&nbsp;" );
        }

        sink.tableCell_();
    }

    private void sinkSectionTitle1( Sink sink, String text )
    {
        sink.sectionTitle1();

        sink.text( text );

        sink.sectionTitle1_();
    }
}
