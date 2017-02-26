package org.apache.maven.plugins.github;

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

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.AbstractChangesReport;
import org.apache.maven.plugins.changes.ProjectUtils;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.plugins.issues.IssueUtils;
import org.apache.maven.plugins.issues.IssuesReportGenerator;
import org.apache.maven.plugins.issues.IssuesReportHelper;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

/**
 * Goal which downloads issues from GitHub and generates a report.
 *
 * @author Bryan Baugher
 * @since 2.8
 */
@Mojo( name = "github-report", threadSafe = true )
public class GitHubMojo
    extends AbstractChangesReport
{

    /**
     * Valid Github columns.
     */
    private static Map<String, Integer> githubColumns = new HashMap<String, Integer>();

    static
    {
        githubColumns.put( "Assignee", IssuesReportHelper.COLUMN_ASSIGNEE );
        githubColumns.put( "Created", IssuesReportHelper.COLUMN_CREATED );
        githubColumns.put( "Fix Version", IssuesReportHelper.COLUMN_FIX_VERSION );
        githubColumns.put( "Id", IssuesReportHelper.COLUMN_ID );
        githubColumns.put( "Reporter", IssuesReportHelper.COLUMN_REPORTER );
        githubColumns.put( "Status", IssuesReportHelper.COLUMN_STATUS );
        githubColumns.put( "Summary", IssuesReportHelper.COLUMN_SUMMARY );
        githubColumns.put( "Type", IssuesReportHelper.COLUMN_TYPE );
        githubColumns.put( "Updated", IssuesReportHelper.COLUMN_UPDATED );
    }

    /**
     * Component used to decrypt server information.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    /**
     * Sets the column names that you want to show in the report. The columns will appear in the report in the same
     * order as you specify them here. Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Created</code>, <code>Fix Version</code>, <code>Id</code>,
     * <code>Reporter</code>, <code>Status</code>, <code>Summary</code>, <code>Type</code> and <code>Updated</code>.
     * </p>
     */
    @Parameter( defaultValue = "Id,Type,Summary,Assignee,Reporter,Status,Created,Updated,Fix Version" )
    private String columnNames;

    /**
     * The scheme of your github api domain. Only use if using github enterprise.
     */
    @Parameter( defaultValue = "http" )
    private String githubAPIScheme;

    /**
     * The port of your github api domain. Only use if using github enterprise.
     */
    @Parameter( defaultValue = "80" )
    private int githubAPIPort;

    /**
     * The settings.xml server id to be used to authenticate into github api domain. Only use if using github
     * enterprise.
     */
    @Parameter( defaultValue = "github" )
    private String githubAPIServerId;

    /**
     * Settings XML configuration.
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    /**
     * Boolean which says if we should include open issues in the report.
     */
    @Parameter( defaultValue = "true" )
    private boolean includeOpenIssues;

    /**
     * Boolean which says if we should include only issues with milestones.
     */
    @Parameter( defaultValue = "true" )
    private boolean onlyMilestoneIssues;

    /**
     * If you only want to show issues for the current version in the report. The current version being used is
     * <code>${project.version}</code> minus any "-SNAPSHOT" suffix.
     */
    @Parameter( defaultValue = "false" )
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
    /* Public methods */
    /* --------------------------------------------------------------------- */

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the GitHub Report in this project because it's not the Execution Root" );
            return false;
        }
        String message = ProjectUtils.validateIssueManagement( project, "GitHub", "GitHub Report" );
        if ( message != null )
        {
            getLog().warn( message );
        }
        return message == null;
    }

    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {

        // Validate parameters
        List<Integer> columnIds = IssuesReportHelper.getColumnIds( columnNames, githubColumns );
        if ( columnIds.size() == 0 )
        {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException( "maven-changes-plugin: None of the configured columnNames '" + columnNames
                + "' are valid." );
        }

        try
        {
            // Download issues
            GitHubDownloader issueDownloader =
                new GitHubDownloader( project, githubAPIScheme, githubAPIPort, includeOpenIssues, onlyMilestoneIssues );

            issueDownloader.configureAuthentication( settingsDecrypter, githubAPIServerId, settings, getLog() );

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
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    /* --------------------------------------------------------------------- */
    /* Private methods */
    /* --------------------------------------------------------------------- */

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "github-report", locale, this.getClass().getClassLoader() );
    }

}
