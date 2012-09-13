package org.apache.maven.plugin.github;

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

import org.apache.maven.plugin.changes.AbstractChangesReport;
import org.apache.maven.plugin.changes.ProjectUtils;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugin.issues.IssueUtils;
import org.apache.maven.plugin.issues.IssuesReportGenerator;
import org.apache.maven.plugin.issues.IssuesReportHelper;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Goal which downloads issues from GitHub and generates a
 * report.
 *
 * @author Bryan Baugher
 * @since 2.8
 */
@Mojo (name = "github-report", threadSafe = true)
public class GitHubMojo
    extends AbstractChangesReport
{

    /**
     * Valid Github columns.
     */
    private static Map<String, Integer> GITHUB_COLUMNS = new HashMap<String, Integer>();

    static
    {
        GITHUB_COLUMNS.put( "Assignee", Integer.valueOf( IssuesReportHelper.COLUMN_ASSIGNEE ) );
        GITHUB_COLUMNS.put( "Created", Integer.valueOf( IssuesReportHelper.COLUMN_CREATED ) );
        GITHUB_COLUMNS.put( "Fix Version", Integer.valueOf( IssuesReportHelper.COLUMN_FIX_VERSION ) );
        GITHUB_COLUMNS.put( "Id", Integer.valueOf( IssuesReportHelper.COLUMN_ID ) );
        GITHUB_COLUMNS.put( "Reporter", Integer.valueOf( IssuesReportHelper.COLUMN_REPORTER ) );
        GITHUB_COLUMNS.put( "Status", Integer.valueOf( IssuesReportHelper.COLUMN_STATUS ) );
        GITHUB_COLUMNS.put( "Summary", Integer.valueOf( IssuesReportHelper.COLUMN_SUMMARY ) );
        GITHUB_COLUMNS.put( "Type", Integer.valueOf( IssuesReportHelper.COLUMN_TYPE ) );
        GITHUB_COLUMNS.put( "Updated", Integer.valueOf( IssuesReportHelper.COLUMN_UPDATED ) );
    }

    /**
     * Sets the column names that you want to show in the report. The columns
     * will appear in the report in the same order as you specify them here.
     * Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Created</code>,
     * <code>Fix Version</code>, <code>Id</code>, <code>Reporter</code>,
     * <code>Status</code>, <code>Summary</code>, <code>Type</code> and
     * <code>Updated</code>.
     * </p>
     */
    @Parameter (defaultValue = "Id,Type,Summary,Assignee,Reporter,Status,Created,Updated,Fix Version")
    private String columnNames;

    /**
     * The scheme of your github api domain. Only use if using github enterprise.
     */
    @Parameter (defaultValue = "http")
    private String githubAPIScheme;

    /**
     * The port of your github api domain. Only use if using github enterprise.
     */
    @Parameter (defaultValue = "80")
    private int githubAPIPort;

    /**
     * Boolean which says if we should include open issues in the report.
     */
    @Parameter (defaultValue = "true")
    private boolean includeOpenIssues;

    /**
     * Boolean which says if we should include only issues with milestones.
     */
    @Parameter (defaultValue = "true")
    private boolean onlyMilestoneIssues;

    /**
     * If you only want to show issues for the current version in the report.
     * The current version being used is <code>${project.version}</code> minus
     * any "-SNAPSHOT" suffix.
     */
    @Parameter (defaultValue = "false")
    private boolean onlyCurrentVersion;

    public String getOutputName()
    {
        return "github-report";
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.description" );
    }

    /* --------------------------------------------------------------------- */
    /* Public methods                                                        */
    /* --------------------------------------------------------------------- */

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return ProjectUtils.validateIfIssueManagementComplete( project, "GitHub", "GitHub Report", getLog() );
    }

    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {

        // Validate parameters
        List<Integer> columnIds = IssuesReportHelper.getColumnIds( columnNames, GITHUB_COLUMNS );
        if ( columnIds.size() == 0 )
        {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException(
                "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
        }

        try
        {
            // Download issues
            GitHubDownloader issueDownloader =
                new GitHubDownloader( project, githubAPIScheme, githubAPIPort, includeOpenIssues, onlyMilestoneIssues );

            List<Issue> issueList = issueDownloader.getIssueList();

            if ( onlyCurrentVersion )
            {
                issueList = IssueUtils.getIssuesForVersion( issueList, project.getVersion() );
                getLog().info( "The GitHub Report will contain issues only for the current version." );
            }

            // Generate the report
            IssuesReportGenerator report = new IssuesReportGenerator( IssuesReportHelper.toIntArray( columnIds ) );

            if ( issueList.isEmpty() )
            {
                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
                getLog().warn( "No issue was matched." );
            }
            else
            {
                report.doGenerateReport( getBundle( locale ), getSink(), issueList );
            }
        }
        catch ( MalformedURLException e )
        {
            // Rethrow this error so that the build fails
            throw new MavenReportException( "The Github URL is incorrect." );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Private methods                                                       */
    /* --------------------------------------------------------------------- */

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "github-report", locale, this.getClass().getClassLoader() );
    }

}
