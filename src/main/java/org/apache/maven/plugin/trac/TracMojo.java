package org.apache.maven.plugin.trac;

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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.plugin.changes.AbstractChangesReport;
import org.apache.maven.plugin.changes.ProjectUtils;
import org.apache.maven.plugin.issues.IssuesReportGenerator;
import org.apache.maven.plugin.issues.IssuesReportHelper;
import org.apache.maven.reporting.MavenReportException;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Goal which downloads issues from the Issue Tracking System and generates a
 * report.
 *
 * @goal trac-report
 * @author Noriko Kinugasa
 * @version $Id$
 * @since 2.1
 * @threadSafe
 */
public class TracMojo
    extends AbstractChangesReport
{
    /**
     * Deprecated Trac columns.
     */
    private static Map DEPRECATED_TRAC_COLUMNS = new HashMap();

    /**
     * Valid Trac columns.
     */
    private static Map TRAC_COLUMNS = new HashMap();

    static
    {
        DEPRECATED_TRAC_COLUMNS.put( "changed", new Integer( IssuesReportHelper.COLUMN_UPDATED ) );
        DEPRECATED_TRAC_COLUMNS.put( "component", new Integer( IssuesReportHelper.COLUMN_COMPONENT ) );
        DEPRECATED_TRAC_COLUMNS.put( "created", new Integer( IssuesReportHelper.COLUMN_CREATED ) );
        DEPRECATED_TRAC_COLUMNS.put( "id", new Integer( IssuesReportHelper.COLUMN_ID ) );
        DEPRECATED_TRAC_COLUMNS.put( "milestone", new Integer( IssuesReportHelper.COLUMN_FIX_VERSION ) );
        DEPRECATED_TRAC_COLUMNS.put( "owner", new Integer( IssuesReportHelper.COLUMN_ASSIGNEE ) );
        DEPRECATED_TRAC_COLUMNS.put( "priority", new Integer( IssuesReportHelper.COLUMN_PRIORITY ) );
        DEPRECATED_TRAC_COLUMNS.put( "reporter", new Integer( IssuesReportHelper.COLUMN_REPORTER ) );
        DEPRECATED_TRAC_COLUMNS.put( "resolution", new Integer( IssuesReportHelper.COLUMN_RESOLUTION ) );
        DEPRECATED_TRAC_COLUMNS.put( "status", new Integer( IssuesReportHelper.COLUMN_STATUS ) );
        DEPRECATED_TRAC_COLUMNS.put( "summary", new Integer( IssuesReportHelper.COLUMN_SUMMARY ) );
        DEPRECATED_TRAC_COLUMNS.put( "type", new Integer( IssuesReportHelper.COLUMN_TYPE ) );

        TRAC_COLUMNS.put( "Assignee", new Integer( IssuesReportHelper.COLUMN_ASSIGNEE ) );
        TRAC_COLUMNS.put( "Component", new Integer( IssuesReportHelper.COLUMN_COMPONENT ) );
        TRAC_COLUMNS.put( "Created", new Integer( IssuesReportHelper.COLUMN_CREATED ) );
        TRAC_COLUMNS.put( "Fix Version", new Integer( IssuesReportHelper.COLUMN_FIX_VERSION ) );
        TRAC_COLUMNS.put( "Id", new Integer( IssuesReportHelper.COLUMN_ID ) );
        TRAC_COLUMNS.put( "Priority", new Integer( IssuesReportHelper.COLUMN_PRIORITY ) );
        TRAC_COLUMNS.put( "Reporter", new Integer( IssuesReportHelper.COLUMN_REPORTER ) );
        TRAC_COLUMNS.put( "Resolution", new Integer( IssuesReportHelper.COLUMN_RESOLUTION ) );
        TRAC_COLUMNS.put( "Status", new Integer( IssuesReportHelper.COLUMN_STATUS ) );
        TRAC_COLUMNS.put( "Summary", new Integer( IssuesReportHelper.COLUMN_SUMMARY ) );
        TRAC_COLUMNS.put( "Type", new Integer( IssuesReportHelper.COLUMN_TYPE ) );
        TRAC_COLUMNS.put( "Updated", new Integer( IssuesReportHelper.COLUMN_UPDATED ) );
    }

    /**
     * Sets the column names that you want to show in the report. The columns
     * will appear in the report in the same order as you specify them here.
     * Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Component</code>,
     * <code>Created</code>, <code>Fix Version</code>, <code>Id</code>,
     * <code>Priority</code>, <code>Reporter</code>, <code>Resolution</code>,
     * <code>Status</code>, <code>Summary</code>, <code>Type</code> and
     * <code>Updated</code>.
     * </p>
     *
     * @parameter default-value="Id,Type,Summary,Assignee,Reporter,Priority,Status,Resolution,Created,Updated"
     * @since 2.2
     */
    private String columnNames;

    /**
     * Defines the Trac query for searching ticket.
     *
     * @parameter default-value="order=id"
     */
    private String query;

    /**
     * Defines the Trac password for authentication into a private Trac
     * installation.
     *
     * @parameter default-value=""
     */
    private String tracPassword;

    /**
     * Defines the Trac username for authentication into a private Trac
     * installation.
     *
     * @parameter default-value=""
     */
    private String tracUser;

    /* --------------------------------------------------------------------- */
    /* Public methods                                                        */
    /* --------------------------------------------------------------------- */

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return ProjectUtils.validateIfIssueManagementComplete( project, "Trac", "Trac Report", getLog() );
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        // Validate parameters
        List columnIds = IssuesReportHelper.getColumnIds( columnNames, TRAC_COLUMNS, DEPRECATED_TRAC_COLUMNS, getLog() );
        if ( columnIds.size() == 0 )
        {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException(
                "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
        }

        try
        {
            // Download issues
            TracDownloader issueDownloader = new TracDownloader();
            configureIssueDownloader( issueDownloader );

            List issueList = issueDownloader.getIssueList();

            // Generate the report
            IssuesReportGenerator report = new IssuesReportGenerator( IssuesReportHelper.toIntArray( columnIds ) );

            if ( issueList.isEmpty() )
            {
                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
                getLog().warn( "No ticket has matched." );
            }
            else
            {
                report.doGenerateReport( getBundle( locale ), getSink(), issueList );
            }
        }
        catch ( MalformedURLException e )
        {
            // Rethrow this error so that the build fails
            throw new MavenReportException( "The Trac URL is incorrect." );
        }
        catch ( XmlRpcException e )
        {
            // Rethrow this error so that the build fails
            throw new MavenReportException( "XmlRpc Error.", e );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.description" );
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.name" );
    }

    public String getOutputName()
    {
        return "trac-report";
    }

    /* --------------------------------------------------------------------- */
    /* Private methods                                                       */
    /* --------------------------------------------------------------------- */

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "trac-report", locale, this.getClass().getClassLoader() );
    }

    private void configureIssueDownloader( TracDownloader issueDownloader )
    {
        issueDownloader.setProject( project );

        issueDownloader.setQuery( query );

        issueDownloader.setTracPassword( tracPassword );

        issueDownloader.setTracUser( tracUser );
    }
}
