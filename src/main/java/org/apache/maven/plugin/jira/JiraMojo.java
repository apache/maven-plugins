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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.plugin.changes.AbstractChangesReport;
import org.apache.maven.plugin.changes.ProjectUtils;
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
    static final int COLUMN_KEY = 0;
    static final int COLUMN_SUMMARY = 1;
    static final int COLUMN_STATUS = 2;
    static final int COLUMN_RESOLUTION = 3;
    static final int COLUMN_ASSIGNEE = 4;
    static final int COLUMN_REPORTER = 5;
    static final int COLUMN_TYPE = 6;
    static final int COLUMN_PRIORITY = 7;
    static final int COLUMN_VERSION = 8;
    static final int COLUMN_FIX_VERSION = 9;
    static final int COLUMN_COMPONENT = 10;

    /**
     * Valid JIRA columns.
     */
    private static Map JIRA_COLUMNS = new HashMap();

    static
    {
        JIRA_COLUMNS.put( "Key", new Integer( COLUMN_KEY ) );
        JIRA_COLUMNS.put( "Summary", new Integer( COLUMN_SUMMARY ) );
        JIRA_COLUMNS.put( "Status", new Integer( COLUMN_STATUS ) );
        JIRA_COLUMNS.put( "Resolution", new Integer( COLUMN_RESOLUTION ) );
        JIRA_COLUMNS.put( "Assignee", new Integer( COLUMN_ASSIGNEE ) );
        JIRA_COLUMNS.put( "Reporter", new Integer( COLUMN_REPORTER ) );
        JIRA_COLUMNS.put( "Type", new Integer( COLUMN_TYPE ) );
        JIRA_COLUMNS.put( "Priority", new Integer( COLUMN_PRIORITY ) );
        JIRA_COLUMNS.put( "Version", new Integer( COLUMN_VERSION ) );
        JIRA_COLUMNS.put( "Fix Version", new Integer( COLUMN_FIX_VERSION ) );
        JIRA_COLUMNS.put( "Component", new Integer( COLUMN_COMPONENT ) );
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
     * Sets the column names that you want to show in the report. The columns
     * will appear in the report in the same order as you specify them here.
     * Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Key</code>, <code>Summary</code>,
     * <code>Status</code>, <code>Resolution</code>, <code>Assignee</code>,
     * <code>Reporter</code>, <code>Type</code>, <code>Priority</code>,
     * <code>Version</code>, <code>Fix Version</code> and
     * <code>Component</code>.
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
     * Valid columns are: <code>Key</code>, <code>Summary</code>,
     * <code>Status</code>, <code>Resolution</code>, <code>Assignee</code>,
     * <code>Reporter</code>, <code>Type</code>, <code>Priority</code>,
     * <code>Version</code>, <code>Fix Version</code>,
     * <code>Component</code>, <code>Created</code> and
     * <code>Updated</code>.
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
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return ProjectUtils.validateIfIssueManagementComplete( project, "JIRA", "JIRA Report", getLog() );
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        JiraDownloader issueDownloader = new JiraDownloader();

        configureIssueDownloader( issueDownloader );

        JiraReportGenerator report;

        try
        {
            issueDownloader.doExecute();

            if ( jiraXmlPath.isFile() )
            {
                JiraXML jira = new JiraXML( jiraXmlPath, jiraXmlEncoding );
                List issueList = jira.getIssueList();

                List columnIds = getColumnIds( columnNames, JIRA_COLUMNS );
                if ( columnIds.size() == 0 )
                {
                    // This can happen if the user has configured column names and they are all invalid
                    throw new MavenReportException(
                        "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid." );
                }
                report = new JiraReportGenerator( toIntArray( columnIds ) );

                if ( onlyCurrentVersion )
                {
                    issueList = JiraHelper.getIssuesForVersion( issueList, project.getVersion() );
                    getLog().info( "The JIRA Report will contain issues only for the current version." );
                }

                report.doGenerateReport( getBundle( locale ), getSink(), issueList );
            }
            else
            {
                report = new JiraReportGenerator( null );

                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
            }
        }
        catch ( MavenReportException mre )
        {
            // Rethrow this error from JiraReportGenerator( String )
            // so that the build fails
            throw mre;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Get a list of id:s for the columns that are to be included in the report.
     *
     * @param columnNames The names of the columns
     * @param allColumns A mapping from column name to column id
     * @return A List of column id:s
     * @todo Move to helper class
     */
    public static List getColumnIds( String columnNames, Map allColumns )
    {
        List columnIds = new ArrayList();
        String[] columnNamesArray = columnNames.split( "," );
        // Loop through the names of the columns, to validate each of them and add their id to the list
        for ( int i = 0; i < columnNamesArray.length; i++ )
        {
            String columnName = columnNamesArray[i].trim();
            if ( allColumns.containsKey( columnName ) )
            {
                columnIds.add( (Integer) allColumns.get( columnName ) );
            }
        }
        return columnIds;
    }

    /**
     * Convert a List of Integers to an int array.
     *
     * @param list The List to convert
     * @return An in array
     * @todo Move to helper class
     */
    public static int[] toIntArray( List list )
    {
        int[] intArray = new int[list.size()];
        for ( int j = 0; j < intArray.length; j++ )
        {
            intArray[j] = ( (Integer) list.get( j ) ).intValue();
        }
        return intArray;
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

        issueDownloader.setJiraUser( jiraUser );

        issueDownloader.setJiraPassword( jiraPassword );

        issueDownloader.setTypeIds( typeIds );

        issueDownloader.setWebUser( webUser );

        issueDownloader.setWebPassword( webPassword );

        issueDownloader.setSettings( settings );
    }
}
