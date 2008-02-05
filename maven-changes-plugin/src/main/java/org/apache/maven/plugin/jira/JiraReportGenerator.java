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
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Generates a JIRA report.
 *
 * @version $Id$
 */
public class JiraReportGenerator
{
    private static final int COLUMN_KEY = 0;
    private static final int COLUMN_SUMMARY = 1;
    private static final int COLUMN_STATUS = 2;
    private static final int COLUMN_RESOLUTION = 3;
    private static final int COLUMN_ASSIGNEE = 4;
    private static final int COLUMN_REPORTER = 5;
    private static final int COLUMN_TYPE = 6;
    private static final int COLUMN_PRIORITY = 7;
    private static final int COLUMN_VERSION = 8;
    private static final int COLUMN_FIX_VERSION = 9;
    private static final int COLUMN_COMPONENT = 10;

    private static final String[] JIRA_COLUMNS = new String[] {
        /* 0  */ "Key",
        /* 1  */ "Summary",
        /* 2  */ "Status",
        /* 3  */ "Resolution",
        /* 4  */ "Assignee",
        /* 5  */ "Reporter",
        /* 6  */ "Type",
        /* 7  */ "Priority",
        /* 8  */ "Version",
        /* 9  */ "Fix Version",
        /* 10 */ "Component"
    };

    private int[] columnOrder;

    private JiraXML jira;

    public JiraReportGenerator()
    {

    }

    public JiraReportGenerator( File xmlPath, String columnNames )
        throws MavenReportException
    {
        jira = new JiraXML( xmlPath );

        String[] columnNamesArray = columnNames.split( "," );
        int validColumnNames = 0;
        columnOrder = new int[columnNamesArray.length];
        for ( int i = 0; i < columnOrder.length; i++ )
        {
            // Default to -1, indicating that the column should not be included in the report
            columnOrder[i] = -1;
            for ( int columnIndex = 0; columnIndex < JIRA_COLUMNS.length; columnIndex++ )
            {
                String columnName = columnNamesArray[i].trim();
                if ( JIRA_COLUMNS[columnIndex].equalsIgnoreCase( columnName ) )
                {
                    // Found a valid column name - add it
                    columnOrder[i] = columnIndex;
                    validColumnNames++;
                    break;
                }
            }
        }
        if ( validColumnNames == 0 )
        {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException(
                "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
        }
    }

    public void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sinkBeginReport( sink, bundle );

        sink.text( bundle.getString( "report.jira.error" ) );

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

        for ( int columnIndex = 0; columnIndex < columnOrder.length; columnIndex++ )
        {
            switch ( columnOrder[columnIndex] )
            {
                case COLUMN_KEY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.key" ) );
                    break;

                case COLUMN_SUMMARY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.summary" ) );
                    break;

                case COLUMN_STATUS:
                    sinkHeader( sink, bundle.getString( "report.jira.label.status" ) );
                    break;

                case COLUMN_RESOLUTION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.resolution" ) );
                    break;

                case COLUMN_ASSIGNEE:
                    sinkHeader( sink, bundle.getString( "report.jira.label.by" ) );
                    break;

                case COLUMN_REPORTER:
                    sinkHeader( sink, bundle.getString( "report.jira.label.reporter" ) );
                    break;

                case COLUMN_TYPE:
                    sinkHeader( sink, bundle.getString( "report.jira.label.type" ) );
                    break;

                case COLUMN_PRIORITY:
                    sinkHeader( sink, bundle.getString( "report.jira.label.priority" ) );
                    break;

                case COLUMN_VERSION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.version" ) );
                    break;

                case COLUMN_FIX_VERSION:
                    sinkHeader( sink, bundle.getString( "report.jira.label.fixVersion" ) );
                    break;

                case COLUMN_COMPONENT:
                    sinkHeader( sink, bundle.getString( "report.jira.label.component" ) );
                    break;

                default:
                    // Do not add a header for this column
                    break;
            }

        }

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

            for ( int columnIndex = 0; columnIndex < columnOrder.length; columnIndex++ )
            {
                switch ( columnOrder[columnIndex] )
                {
                    case COLUMN_KEY:
                        sink.tableCell();
                        sink.link( issue.getLink() );
                        sink.text( issue.getKey() );
                        sink.link_();
                        sink.tableCell_();
                        break;

                    case COLUMN_SUMMARY:
                        sinkCell( sink, issue.getSummary() );
                        break;

                    case COLUMN_STATUS:
                        sinkCell( sink, issue.getStatus() );
                        break;

                    case COLUMN_RESOLUTION:
                        sinkCell( sink, issue.getResolution() );
                        break;

                    case COLUMN_ASSIGNEE:
                        sinkCell( sink, issue.getAssignee() );
                        break;

                    case COLUMN_REPORTER:
                        sinkCell( sink, issue.getReporter() );
                        break;

                    case COLUMN_TYPE:
                        sinkCell( sink, issue.getType() );
                        break;

                    case COLUMN_PRIORITY:
                        sinkCell( sink, issue.getPriority() );
                        break;

                    case COLUMN_VERSION:
                        sinkCell( sink, issue.getVersion() );
                        break;

                    case COLUMN_FIX_VERSION:
                        sinkCell( sink, issue.getFixVersion() );
                        break;

                    case COLUMN_COMPONENT:
                        sinkCell( sink, issue.getComponent() );
                        break;

                    default:
                        // Do not add a cell for this column
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
