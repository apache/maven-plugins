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
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
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
    extends AbstractMavenReport
{
    /**
     * Output directory where the report will be placed.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * Path to the JIRA XML file, which will be parsed.
     *
     * @parameter expression="${project.build.directory}/jira-results.xml "
     * @required
     * @readonly
     */
    private String jiraXmlPath;

    /**
     * Doxia Site Renderer.
     *
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Settings XML configuration.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Maximum number of entries to be displayed by the JIRA Report.
     *
     * @parameter default-value=100
     *
     */
    private int maxEntries;

    /**
     * Defines the filter parameters to restrict which issues are retrieved
     * from JIRA. The filter parameter must use the same format of url
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
     * @since 2.0-beta-4
     */
    private String fixVersionIds;

    /**
     * Sets the status(es) that you want to limit your report to include.
     * Valid statuses are: Open, In Progress, Reopened, Resolved and Closed.
     * Multiple values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String statusIds;

    /**
     * Sets the resolution(s) that you want to limit your report to include.
     * Valid statuses are: Unresolved, Fixed, Won't Fix, Duplicate, Incomplete,
     * Cannot Reproduce. Multiple values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String resolutionIds;

    /**
     * Sets the priority(s) that you want to limit your report to include.
     * Valid statuses are: Blocker, Critical, Major, Minor, Trivial. Multiple
     * values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String priorityIds;

    /**
     * Sets the component(s) that you want to limit your report to include.
     * Multiple components can be separated by commas (such as 10011,10012).
     * If this is set to empty - that means all components.
     *
     * @parameter default-value=""
     */
    private String component;

    /**
     * Sets the types(s) that you want to limit your report to include.
     * Valid types are: <code>Bug</code>, <code>New Feature</code>,
     * <code>Task</code>, <code>Improvement</code>, <code>Wish</code>,
     * <code>Test</code> and <code>Sub-task</code>. Multiple
     * values can be separated by commas.
     *
     * @parameter default-value=""
     * @since 2.0-beta-4
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
     * @since 2.0-beta-4
     */
    private String columnNames;

    /**
     * Sets the column names that you want to sort the report by. Add
     * <code>DESC</code> following the column name
     * to specify <i>descending</i> sequence. For
     * example <code>Fix Version DESC, Type</code> sorts first by
     * the Fix Version in descending order and then by Type in
     * ascending order.
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
     * @since 2.0-beta-4
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
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return validateIfIssueManagementComplete();
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        JiraDownloader jiraDownloader = new JiraDownloader();

        setJiraDownloaderParameters( jiraDownloader );

        JiraReportGenerator report;

        try
        {
            jiraDownloader.doExecute();

            if ( new File( jiraXmlPath ).exists() )
            {
                report = new JiraReportGenerator( jiraXmlPath, columnNames );

                report.doGenerateReport( getBundle( locale ), getSink() );
            }
            else
            {
                report = new JiraReportGenerator();

                report.doGenerateEmptyReport( getBundle( locale ), getSink() );
            }
        }
        catch ( MavenReportException mre )
        {
            // Rethrow this error from JiraReportGenerator( String, String )
            // so that the build fails
            throw mre;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.jira.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.jira.description" );
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
        return "jira-report";
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "jira-report", locale, this.getClass().getClassLoader() );
    }

    private void setJiraDownloaderParameters( JiraDownloader jira )
    {
        jira.setLog( getLog() );

        jira.setMavenProject( project );

        jira.setOutput( new File( jiraXmlPath ) );

        jira.setNbEntries( maxEntries );

        jira.setComponent( component );

        jira.setFixVersionIds( fixVersionIds );

        jira.setStatusIds( statusIds );

        jira.setResolutionIds( resolutionIds );

        jira.setPriorityIds( priorityIds );

        jira.setSortColumnNames( sortColumnNames );

        jira.setFilter( filter );

        jira.setJiraUser( jiraUser );

        jira.setJiraPassword( jiraPassword );

        jira.setTypeIds( typeIds );

        jira.setWebUser( webUser );

        jira.setWebPassword( webPassword );

        jira.setSettings( settings );
    }

    private boolean validateIfIssueManagementComplete()
    {
        if ( project.getIssueManagement() == null )
        {
            getLog().error( "No Issue Management set. No JIRA Report will be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getUrl() == null )
            || ( project.getIssueManagement().getUrl().trim().equals( "" ) ) )
        {
            getLog().error( "No URL set in Issue Management. No JIRA Report will be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getSystem() != null )
            && !( project.getIssueManagement().getSystem().equalsIgnoreCase( "jira" ) ) )
        {
            getLog().error( "The JIRA Report only supports JIRA.  No JIRA Report will be generated." );

            return false;
        }
        return true;
    }
}
