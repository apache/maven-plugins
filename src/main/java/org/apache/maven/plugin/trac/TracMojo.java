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

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.changes.AbstractChangesReport;
import org.apache.maven.plugin.changes.ProjectUtils;
import org.apache.maven.plugin.issues.IssuesReportGenerator;
import org.apache.maven.plugin.issues.IssuesReportHelper;
import org.apache.maven.project.MavenProject;
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
 */
public class TracMojo
    extends AbstractChangesReport
{
    /**
     * Valid Trac columns.
     */
    private static Map TRAC_COLUMNS = new HashMap();

    static
    {
        TRAC_COLUMNS.put( "changed", new Integer( IssuesReportHelper.COLUMN_UPDATED ) );
        TRAC_COLUMNS.put( "component", new Integer( IssuesReportHelper.COLUMN_COMPONENT ) );
        TRAC_COLUMNS.put( "created", new Integer( IssuesReportHelper.COLUMN_CREATED ) );
        TRAC_COLUMNS.put( "id", new Integer( IssuesReportHelper.COLUMN_ID ) );
        TRAC_COLUMNS.put( "milestone", new Integer( IssuesReportHelper.COLUMN_FIX_VERSION ) );
        TRAC_COLUMNS.put( "owner", new Integer( IssuesReportHelper.COLUMN_ASSIGNEE ) );
        TRAC_COLUMNS.put( "priority", new Integer( IssuesReportHelper.COLUMN_PRIORITY ) );
        TRAC_COLUMNS.put( "reporter", new Integer( IssuesReportHelper.COLUMN_REPORTER ) );
        TRAC_COLUMNS.put( "resolution", new Integer( IssuesReportHelper.COLUMN_RESOLUTION ) );
        TRAC_COLUMNS.put( "status", new Integer( IssuesReportHelper.COLUMN_STATUS ) );
        TRAC_COLUMNS.put( "summary", new Integer( IssuesReportHelper.COLUMN_SUMMARY ) );
        TRAC_COLUMNS.put( "type", new Integer( IssuesReportHelper.COLUMN_TYPE ) );
    }

    /**
     * Defines the Trac username for authentication into a private Trac
     * installation.
     *
     * @parameter default-value=""
     */
    private String tracUser;

    /**
     * Defines the Trac password for authentication into a private Trac
     * installation.
     *
     * @parameter default-value=""
     */
    private String tracPassword;

    /**
     * Defines the Trac query for searching ticket.
     *
     * @parameter default-value="order=id"
     */
    private String query;

    /**
     * Sets the column names that you want to show in the report. The columns
     * will appear in the report in the same order as you specify them here.
     * Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>id</code>, <code>type</code>,
     * <code>summary</code>, <code>status</code>, <code>resolution</code>,
     * <code>milestone</code>, <code>owner</code>, <code>priority</code>,
     * <code>reporter</code>, <code>component</code>, <code>created</code>,
     * <code>changed</code>.
     * </p>
     *
     * @parameter default-value="id,type,summary,owner,reporter,priority,status,resolution,created,changed"
     * @since 2.2
     */
    private String columnNames;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return ProjectUtils.validateIfIssueManagementComplete( project, "Trac", "Trac Report", getLog() );
    }

    private void configureIssueDownloader( TracDownloader issueDownloader )
    {
        issueDownloader.setProject( project );

        issueDownloader.setQuery( query );

        issueDownloader.setTracPassword( tracPassword );

        issueDownloader.setTracUser( tracUser );
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        TracDownloader issueDownloader = new TracDownloader();
        configureIssueDownloader( issueDownloader );

        try
        {
            List issueList = issueDownloader.getIssueList();

            List columnIds = IssuesReportHelper.getColumnIds( columnNames, TRAC_COLUMNS );
            if ( columnIds.size() == 0 )
            {
                // This can happen if the user has configured column names and they are all invalid
                throw new MavenReportException(
                    "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
            }

            // Generate the report
            IssuesReportGenerator report = new IssuesReportGenerator( IssuesReportHelper.toIntArray( columnIds ) );

            if ( issueList.isEmpty() )
            {
                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
                getLog().warn( "No ticket has matched." );
            }
            else
            {
                try
                {
                    report.doGenerateReport( getBundle( locale ), getSink(), issueList );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
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
    }

    public String getName( Locale locale )
    {
        return "Trac Report";
    }

    public String getDescription( Locale locale )
    {
        return "Report on Ticket from the Trac.";
    }

    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    public String getOutputName()
    {
        return "trac-report";
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "trac-report", locale, this.getClass().getClassLoader() );
    }
}
