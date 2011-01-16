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

import java.io.File;
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
import org.apache.maven.settings.Settings;

/**
 * Goal which downloads issues from the Issue Tracking System and generates a report.
 *
 * @goal jira-report
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class JiraMojo
    extends AbstractChangesReport
{
    /**
     * Valid JIRA columns.
     */
    private static Map JIRA_COLUMNS = new HashMap();

    static
    {
        JIRA_COLUMNS.put( "Assignee", new Integer( IssuesReportHelper.COLUMN_ASSIGNEE ) );
        JIRA_COLUMNS.put( "Component", new Integer( IssuesReportHelper.COLUMN_COMPONENT ) );
        JIRA_COLUMNS.put( "Created", new Integer( IssuesReportHelper.COLUMN_CREATED ) );
        JIRA_COLUMNS.put( "Fix Version", new Integer( IssuesReportHelper.COLUMN_FIX_VERSION ) );
        JIRA_COLUMNS.put( "Id", new Integer( IssuesReportHelper.COLUMN_ID ) );
        JIRA_COLUMNS.put( "Key", new Integer( IssuesReportHelper.COLUMN_KEY ) );
        JIRA_COLUMNS.put( "Priority", new Integer( IssuesReportHelper.COLUMN_PRIORITY ) );
        JIRA_COLUMNS.put( "Reporter", new Integer( IssuesReportHelper.COLUMN_REPORTER ) );
        JIRA_COLUMNS.put( "Resolution", new Integer( IssuesReportHelper.COLUMN_RESOLUTION ) );
        JIRA_COLUMNS.put( "Status", new Integer( IssuesReportHelper.COLUMN_STATUS ) );
        JIRA_COLUMNS.put( "Summary", new Integer( IssuesReportHelper.COLUMN_SUMMARY ) );
        JIRA_COLUMNS.put( "Type", new Integer( IssuesReportHelper.COLUMN_TYPE ) );
        JIRA_COLUMNS.put( "Updated", new Integer( IssuesReportHelper.COLUMN_UPDATED ) );
        JIRA_COLUMNS.put( "Version", new Integer( IssuesReportHelper.COLUMN_VERSION ) );
    }

    /**
     * Path to the JIRA XML file, which will be parsed.
     *
     * @parameter expression="${project.build.directory}/jira-results.xml"
     * @required
     * @readonly
     */
    private File jiraXmlPath;

    /**
     * The encoding used in the JIRA XML file. You only need to change this if
     * your JIRA server is returning responses in an encoding other than UTF-8.
     *
     * @parameter default-value="UTF-8" expression="${changes.jiraXmlEncoding}"
     * @since 2.4
     */
    private String jiraXmlEncoding;

    /**
     * Settings XML configuration.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Maximum number of entries to be fetched from JIRA.
     *
     * @parameter default-value=100
     *
     */
    private int maxEntries;

    /**
     * Defines the filter parameters to restrict which issues are retrieved
     * from JIRA. The filter parameter uses the same format of url
     * parameters that is used in a JIRA search.
     *
     * @parameter default-value=""
     */
    private String filter;

    /**
     * Sets the fix version id(s) that you want to limit your report to include.
     * These are JIRA's internal version ids, <b>NOT</b> the human readable display ones.
     * Multiple fix versions can be separated by commas.
     * If this is set to empty - that means all fix versions will be included.
     *
     * @parameter default-value=""
     * @since 2.0
     */
    private String fixVersionIds;

    /**
     * Sets the status(es) that you want to fetch from JIRA.
     * Valid statuses are: <code>Open</code>, <code>In Progress</code>,
     * <code>Reopened</code>, <code>Resolved</code> and <code>Closed</code>.
     * Multiple values can be separated by commas.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter had no
     * default value.
     * </p>
     *
     * @parameter default-value="Closed"
     */
    private String statusIds;

    /**
     * Sets the resolution(s) that you want to fetch from JIRA.
     * Valid resolutions are: <code>Unresolved</code>, <code>Fixed</code>,
     * <code>Won't Fix</code>, <code>Duplicate</code>, <code>Incomplete</code>
     * and <code>Cannot Reproduce</code>.
     * Multiple values can be separated by commas.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter had no
     * default value.
     * </p>
     *
     * @parameter default-value="Fixed"
     */
    private String resolutionIds;

    /**
     * Sets the priority(s) that you want to limit your report to include.
     * Valid statuses are <code>Blocker</code>, <code>Critical</code>,
     * <code>Major</code>, <code>Minor</code> and <code>Trivial</code>.
     * Multiple values can be separated by commas.
     * If this is set to empty - that means all priorities will be included.
     *
     * @parameter default-value=""
     */
    private String priorityIds;

    /**
     * Sets the component(s) that you want to limit your report to include.
     * Multiple values can be separated by commas (such as 10011,10012).
     * If this is set to empty - that means all components will be included.
     *
     * @parameter default-value=""
     */
    private String component;

    /**
     * Sets the types(s) that you want to limit your report to include.
     * Valid types are: <code>Bug</code>, <code>New Feature</code>,
     * <code>Task</code>, <code>Improvement</code>, <code>Wish</code>,
     * <code>Test</code> and <code>Sub-task</code>.
     * Multiple values can be separated by commas.
     * If this is set to empty - that means all types will be included.
     *
     * @parameter default-value=""
     * @since 2.0
     */
    private String typeIds;

    /**
     * Sets the names of the columns that you want in the report. The columns
     * will appear in the report in the same order as you specify them here.
     * Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Component</code>,
     * <code>Created</code>, <code>Fix Version</code>, <code>Id</code>,
     * <code>Key</code>, <code>Priority</code>, <code>Reporter</code>,
     * <code>Resolution</code>, <code>Status</code>, <code>Summary</code>,
     * <code>Type</code>, <code>Updated</code> and <code>Version</code>.
     * </p>
     *
     * @parameter default-value="Key,Summary,Status,Resolution,Assignee"
     * @since 2.0
     */
    private String columnNames;

    /**
     * Sets the column names that you want to sort the report by. Add
     * <code>DESC</code> following the column name
     * to specify <i>descending</i> sequence. For
     * example <code>Fix Version DESC, Type</code> sorts first by
     * the Fix Version in descending order and then by Type in
     * ascending order. By default sorting is done in ascending order, but is
     * possible to specify <code>ASC</code> for consistency. The previous
     * example would then become <code>Fix Version DESC, Type ASC</code>. 
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Component</code>,
     * <code>Created</code>, <code>Fix Version</code>, <code>Id</code>,
     * <code>Key</code>, <code>Priority</code>, <code>Reporter</code>,
     * <code>Resolution</code>, <code>Status</code>, <code>Summary</code>,
     * <code>Type</code>, <code>Updated</code> and <code>Version</code>.
     * </p>
     *
     * @parameter default-value="Priority DESC, Created DESC"
     * @since 2.0
     */
    private String sortColumnNames;

    /**
     * Defines the JIRA username for authentication into a private JIRA installation.
     *
     * @parameter default-value=""
     */
    private String jiraUser;

    /**
     * Defines the JIRA password for authentication into a private JIRA installation.
     *
     * @parameter default-value=""
     */
    private String jiraPassword;

    /**
     * Defines the http user for basic authentication into the JIRA webserver.
     *
     * @parameter default-value=""
     */
    private String webUser;

    /**
     * Defines the http password for basic authentication into the JIRA webserver.
     *
     * @parameter default-value=""
     */
    private String webPassword;

    /**
     * If you only want to show issues for the current version in the report.
     * The current version being used is <code>${project.version}</code> minus
     * any "-SNAPSHOT" suffix.
     *
     * @parameter default-value="false"
     * @since 2.0
     */
    private boolean onlyCurrentVersion;

    /**
     * The pattern used by dates in the JIRA XML-file. This is used to parse
     * the Created and Updated fields.
     *
     * @parameter default-value="EEE, d MMM yyyy HH:mm:ss Z"
     * @since 2.4
     */
    private String jiraDatePattern;

    /**
     * The prefix used when naming versions in JIRA.
     * <p>
     * If you have a project in JIRA with several components that have different
     * release cycles, it is an often used pattern to prefix the version with
     * the name of the component, e.g. maven-filtering-1.0 etc. To fetch issues
     * from JIRA for a release of the "maven-filtering" component you would need
     * to set this parameter to "maven-filtering-".
     * </p>
     *
     * @parameter default-value=""
     * @since 2.4
     */
    private String versionPrefix;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return ProjectUtils.validateIfIssueManagementComplete( project, "JIRA", "JIRA Report", getLog() );
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        // Validate parameters
        List columnIds = IssuesReportHelper.getColumnIds( columnNames, JIRA_COLUMNS );
        if ( columnIds.size() == 0 )
        {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException(
                "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
        }

        try
        {
            // Download issues
            JiraDownloader issueDownloader = new JiraDownloader();
            configureIssueDownloader( issueDownloader );
            issueDownloader.doExecute();

            List issueList = issueDownloader.getIssueList();

            if ( onlyCurrentVersion )
            {
                String version = ( versionPrefix == null ? "" : versionPrefix ) + project.getVersion();
                issueList = JiraHelper.getIssuesForVersion( issueList, version );
                getLog().info( "The JIRA Report will contain issues only for the current version." );
            }

            // Generate the report
            IssuesReportGenerator report = new IssuesReportGenerator( IssuesReportHelper.toIntArray( columnIds ) );

            if ( issueList.isEmpty() )
            {
                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
            }
            else
            {
                report.doGenerateReport( getBundle( locale ), getSink(), issueList );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.description" );
    }

    public String getOutputName()
    {
        return "jira-report";
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "jira-report", locale, this.getClass().getClassLoader() );
    }

    private void configureIssueDownloader( JiraDownloader issueDownloader )
    {
        issueDownloader.setLog( getLog() );

        issueDownloader.setMavenProject( project );

        issueDownloader.setOutput( jiraXmlPath );

        issueDownloader.setNbEntries( maxEntries );

        issueDownloader.setComponent( component );

        issueDownloader.setFixVersionIds( fixVersionIds );

        issueDownloader.setStatusIds( statusIds );

        issueDownloader.setResolutionIds( resolutionIds );

        issueDownloader.setPriorityIds( priorityIds );

        issueDownloader.setSortColumnNames( sortColumnNames );

        issueDownloader.setFilter( filter );

        issueDownloader.setJiraDatePattern( jiraDatePattern );

        issueDownloader.setJiraUser( jiraUser );

        issueDownloader.setJiraPassword( jiraPassword );

        issueDownloader.setJiraXmlEncoding( jiraXmlEncoding );

        issueDownloader.setTypeIds( typeIds );

        issueDownloader.setWebUser( webUser );

        issueDownloader.setWebPassword( webPassword );

        issueDownloader.setSettings( settings );
    }
}
