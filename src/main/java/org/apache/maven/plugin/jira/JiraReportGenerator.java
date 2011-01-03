package org.apache.maven.plugin.jira;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugin.issues.IssuesReportGenerator;
import org.apache.maven.reporting.MavenReportException;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Generates a JIRA report.
 *
 * @version $Id$
 */
public class JiraReportGenerator
{
    /**
     * Holds the id:s for the columns to include in the report, in the order
     * that they should appear in the report.
     */
    private int[] columns;

    /**
     * @param includedColumns The id:s of the columns to include in the report
     */
    public JiraReportGenerator( int[] includedColumns )
        throws MavenReportException
    {
        this.columns = includedColumns;
    }

    public void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sinkBeginReport( sink, bundle );

        sink.paragraph();

        sink.text( bundle.getString( "report.jira.error" ) );

        sink.paragraph_();

        sinkEndReport( sink );
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink, List issueList )
    {
        sinkBeginReport( sink, bundle );

        constructHeaderRow( sink, issueList, bundle );

        constructDetailRows( sink, issueList, bundle );

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

        for ( int columnIndex = 0; columnIndex < columns.length; columnIndex++ )
        {
            switch ( columns[columnIndex] )
            {
                case JiraMojo.COLUMN_KEY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.key" ) );
                    break;

                case JiraMojo.COLUMN_SUMMARY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.summary" ) );
                    break;

                case JiraMojo.COLUMN_STATUS:
                    sinkHeader( sink, bundle.getString( "report.jira.label.status" ) );
                    break;

                case JiraMojo.COLUMN_RESOLUTION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.resolution" ) );
                    break;

                case JiraMojo.COLUMN_ASSIGNEE:
                    sinkHeader( sink, bundle.getString( "report.jira.label.by" ) );
                    break;

                case JiraMojo.COLUMN_REPORTER:
                    sinkHeader( sink, bundle.getString( "report.jira.label.reporter" ) );
                    break;

                case JiraMojo.COLUMN_TYPE:
                    sinkHeader( sink, bundle.getString( "report.jira.label.type" ) );
                    break;

                case JiraMojo.COLUMN_PRIORITY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.priority" ) );
                    break;

                case JiraMojo.COLUMN_VERSION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.version" ) );
                    break;

                case JiraMojo.COLUMN_FIX_VERSION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.fixVersion" ) );
                    break;

                case JiraMojo.COLUMN_COMPONENT:
                    sinkHeader( sink, bundle.getString( "report.jira.label.component" ) );
                    break;

                default:
                    // Do not add a header for this column
                    break;
            }
        }

        sink.tableRow_();
    }

    private void constructDetailRows( Sink sink, List issueList, ResourceBundle bundle )
    {
        if ( issueList == null )
        {
            return;
        }

        for ( int idx = 0; idx < issueList.size(); idx++ )
        {
            Issue issue = (Issue) issueList.get( idx );

            sink.tableRow();

            for ( int columnIndex = 0; columnIndex < columns.length; columnIndex++ )
            {
                switch ( columns[columnIndex] )
                {
                    case JiraMojo.COLUMN_KEY:
                        sink.tableCell();
                        sink.link( issue.getLink() );
                        sink.text( issue.getKey() );
                        sink.link_();
                        sink.tableCell_();
                        break;

                    case JiraMojo.COLUMN_SUMMARY:
                        sinkCell( sink, issue.getSummary() );
                        break;

                    case JiraMojo.COLUMN_STATUS:
                        sinkCell( sink, issue.getStatus() );
                        break;

                    case JiraMojo.COLUMN_RESOLUTION:
                        sinkCell( sink, issue.getResolution() );
                        break;

                    case JiraMojo.COLUMN_ASSIGNEE:
                        sinkCell( sink, issue.getAssignee() );
                        break;

                    case JiraMojo.COLUMN_REPORTER:
                        sinkCell( sink, issue.getReporter() );
                        break;

                    case JiraMojo.COLUMN_TYPE:
                        sinkCell( sink, issue.getType() );
                        break;

                    case JiraMojo.COLUMN_PRIORITY:
                        sinkCell( sink, issue.getPriority() );
                        break;

                    case JiraMojo.COLUMN_VERSION:
                        sinkCell( sink, issue.getVersion() );
                        break;

                    case JiraMojo.COLUMN_FIX_VERSION:
                        sinkCell( sink, IssuesReportGenerator.printValues( issue.getFixVersions() ) );
                        break;

                    case JiraMojo.COLUMN_COMPONENT:
                        sinkCell( sink, IssuesReportGenerator.printValues( issue.getComponents() ) );
                        break;

                    default:
                        // Do not add this column
                        break;
                }
            }

            sink.tableRow_();
        }

        sink.table_();
    }

    private void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        sink.title();
        sink.text( bundle.getString( "report.jira.header" ) );
        sink.title_();

        sink.head_();

        sink.body();

        sink.section1();

        sinkSectionTitle1( sink, bundle.getString( "report.jira.header" ) );
    }

    private void sinkEndReport( Sink sink )
    {
        sink.section1_();

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
            sink.text( text );
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
