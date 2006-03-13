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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.i18n.I18N;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the project information reports summary.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal project-info-summary
 * @todo [IMPORTANT] this is the same for each category, should juts be one renderer. Maybe put it in Doxia instead, and instantiate directly from the site plugin?
 * @todo [IMPORTANT] need to have a key prefix - report.information (Project Infos) vs report.project (Project Reports)
 * @todo [IMPORTANT] have not moved keys correctly in translations yet
 */
public class ProjectInfoSummaryReport
    extends AbstractMavenReport
{
    /**
     * Report output directory.
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

    private List reports;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.menu.projectinformation" );
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
        return "";
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
        SummaryRenderer r = new SummaryRenderer( reports, getSink(), i18n, locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "project-info";
    }

    private static class SummaryRenderer
        extends AbstractMavenReportRenderer
    {
        private I18N i18n;

        private Locale locale;

        private List reports;

        SummaryRenderer( List reports, Sink sink, I18N i18n, Locale locale )
        {
            super( sink );

            this.i18n = i18n;

            this.locale = locale;

            this.reports = reports;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.information.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( getTitle() );

            sink.paragraph();
            sink.text( i18n.getString( "project-info-report", locale, "report.information.description1" ) + " " );
            sink.link( "http://maven.apache.org" );
            sink.text( "Maven" );
            sink.link_();
            sink.text( " " + i18n.getString( "project-info-report", locale, "report.information.description2" ) );
            sink.paragraph_();

            startSection( i18n.getString( "project-info-report", locale, "report.information.sectionTitle" ) );

            startTable();

            String name = i18n.getString( "project-info-report", locale, "report.information.column.document" );
            String description =
                i18n.getString( "project-info-report", locale, "report.information.column.description" );
            tableHeader( new String[]{name, description} );

            if ( reports != null )
            {
                for ( Iterator i = reports.iterator(); i.hasNext(); )
                {
                    MavenReport report = (MavenReport) i.next();

                    sink.tableRow();
                    sink.tableCell();
                    sink.link( report.getOutputName() + ".html" );
                    sink.text( report.getName( locale ) );
                    sink.link_();
                    sink.tableCell_();
                    sink.tableCell();
                    sink.text( report.getDescription( locale ) );
                    sink.tableCell_();
                    sink.tableRow_();
                }
            }

            endTable();

            endSection();

            endSection();
        }
    }

}
