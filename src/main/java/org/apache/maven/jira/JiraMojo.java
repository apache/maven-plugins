package org.apache.maven.jira;

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

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

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
     * Output Directory of the report.
     * 
     * @parameter expression="${project.build.directory}/site "
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * Path of the JIRA XML file to be parsed.
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
     * Defines the filter parameters to restrict the result issues from JIRA.
     * The filter parameters property must use the same format of url parameters from the JIRA search.
     *
     * @parameter default-value=""
     */
    private String filter;

    /**
     * Sets the status(es) of the project you want to limit your report to.
     * Valid statuses are: Open, In Progress, Reopened, Resolved and Closed. Multiple values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String statusIds;

    /**
     * Sets the resolution(s) of the project you want to limit your report to.
     * Valid statuses are: Unresolved, Fixed, Won't Fix, Duplicate, Incomplete, Cannot Reproduce.
     * Multiple values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String resolutionIds;

    /**
     * Sets the priority(s) of the project you want to limit your report to.
     * Valid statuses are: Blocker, Critical, Major, Minor, Trivial. Multiple values can be separated by commas.
     *
     * @parameter default-value=""
     */
    private String priorityIds;

    /**
     * Sets the component(s) of the project you want to limit your report to.
     * Multiple components can be separated by commas (such as 10011,10012).
     * Default-value -  empty, meaning all components.
     *
     * @parameter default-value=""
     */
    private String component;

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
        JiraDownloader2 jira = new JiraDownloader2();

        setJiraDownloaderParameter( jira );

        JiraReportGenerator report;

        try
        {
            jira.doExecute();

            if ( !( new File( jiraXmlPath ).exists() ) )
            {
                return;
            }

            report = new JiraReportGenerator( jiraXmlPath );

            report.doGenerateReport( getBundle( locale ), getSink() );
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

    private void setJiraDownloaderParameter( JiraDownloader2 jira )
    {
        jira.setLog( getLog() );

        jira.setMavenProject( project );

        jira.setOutput( new File( jiraXmlPath ) );

        jira.setNbEntries( maxEntries );

        jira.setComponent( component );

        jira.setStatusIds( statusIds );

        jira.setResolutionIds( resolutionIds );

        jira.setPriorityIds( priorityIds );

        jira.setFilter( filter );

        jira.setJiraUser( jiraUser );

        jira.setJiraPassword( jiraPassword );

        jira.setWebUser( webUser );

        jira.setWebPassword( webPassword );

        jira.setSettings( settings );
    }

    private boolean validateIfIssueManagementComplete()
    {
        if ( project.getIssueManagement() == null )
        {
            getLog().error( "No Issue Management set. JIRA Report will not be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getUrl() == null )
            || ( project.getIssueManagement().getUrl().trim().equals( "" ) ) )
        {
            getLog().error( "No URL set in Issue Management. JIRA Report will not be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getSystem() != null )
            && !( project.getIssueManagement().getSystem().equalsIgnoreCase( "jira" ) ) )
        {
            getLog().error( "JIRA Report only supports JIRA.  JIRA Report will not be generated." );

            return false;
        }
        return true;
    }
}
