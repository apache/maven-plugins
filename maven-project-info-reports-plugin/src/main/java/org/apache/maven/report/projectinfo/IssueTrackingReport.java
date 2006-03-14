package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

/**
 * Generates the Project Issue Tracking report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal issue-tracking
 */
public class IssueTrackingReport
    extends AbstractMavenReport
{
    /**
     * Report outpur directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @component
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
     * Internationalization.
     *
     * @component
     */
    private I18N i18n;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.issuetracking.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.issuetracking.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        IssueTrackingRenderer r = new IssueTrackingRenderer( getSink(), getProject().getModel(), i18n, locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "issue-tracking";
    }

    static class IssueTrackingRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        public IssueTrackingRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink );

            this.model = model;

            this.i18n = i18n;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.issuetracking.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            IssueManagement issueManagement = model.getIssueManagement();
            if ( issueManagement == null )
            {
                startSection( getTitle() );

                paragraph( i18n.getString( "project-info-report", locale, "report.issuetracking.noissueManagement" ) );

                endSection();

                return;
            }

            String system = issueManagement.getSystem();
            String url = issueManagement.getUrl();

            // Overview
            startSection( i18n.getString( "project-info-report", locale, "report.issuetracking.overview.title" ) );

            if ( isIssueManagementSystem( system, "jira" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.issuetracking.jira.intro" ) );
            }
            else if ( isIssueManagementSystem( system, "bugzilla" ) )
            {
                linkPatternedText(
                    i18n.getString( "project-info-report", locale, "report.issuetracking.bugzilla.intro" ) );
            }
            else if ( isIssueManagementSystem( system, "scarab" ) )
            {
                linkPatternedText(
                    i18n.getString( "project-info-report", locale, "report.issuetracking.scarab.intro" ) );
            }
            else if ( ( system == null ) || ( system.trim().equals( "" ) ) )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.issuetracking.general.intro" ) );
            }
            else
            {
                paragraph(
                    i18n.getString( "project-info-report", locale, "report.issuetracking.custom.intro" ).replaceFirst(
                        "%issueManagementSystem%", system ) );
            }

            endSection();

            // Connection
            startSection( getTitle() );

            paragraph( i18n.getString( "project-info-report", locale, "report.issuetracking.intro" ) );

            verbatimLink( url, url );

            endSection();
        }

        /**
         * Checks if a issue management system is Jira, bugzilla...
         *
         * @return true if the issue management system is Jira, bugzilla, false
         *         otherwise.
         */
        private boolean isIssueManagementSystem( String system, String im )
        {
            if ( StringUtils.isEmpty( system ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( im ) )
            {
                return false;
            }

            if ( system.toLowerCase().startsWith( im.toLowerCase() ) )
            {
                return true;
            }

            return false;
        }
    }
}
