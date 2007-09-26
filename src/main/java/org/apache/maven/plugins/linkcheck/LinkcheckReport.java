package org.apache.maven.plugins.linkcheck;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.linkcheck.HttpBean;
import org.apache.maven.doxia.linkcheck.LinkCheck;
import org.apache.maven.doxia.linkcheck.model.LinkcheckFile;
import org.apache.maven.doxia.linkcheck.model.LinkcheckFileResult;
import org.apache.maven.doxia.linkcheck.model.LinkcheckModel;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generates a link check report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal linkcheck
 */
public class LinkcheckReport
    extends AbstractMavenReport
{
    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Settings.
     *
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * The settings offline paramater.
     *
     * @parameter expression="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean offline;

    /**
     * If online, the HTTP method should automatically follow HTTP redirects,
     * <tt>false</tt> otherwise.
     *
     * @parameter default-value="false"
     */
    protected boolean httpFollowRedirect;

    /**
     * Report output directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The location of the linkcheck cache.
     *
     * @parameter expression="${project.build.directory}/linkcheck/linkcheck.cache"
     * @required
     */
    private String linkcheckCache;

    /**
     * The location of the linkcheck report.
     *
     * @parameter expression="${project.build.directory}/linkcheck/linkcheck.xml"
     * @required
     */
    private String linkcheckOutput;

    /**
     * The current report level. Defaults to {@link LinkCheckResult#WARNING}.
     *
     * @parameter default-value="2"
     */
    protected int reportLevel;

    /**
     * The HTTP method to use. Currently supported are "GET" and "HEAD".
     * <dl>
     * <dt>HTTP GET</dt>
     * <dd>
     * The HTTP GET method is defined in section 9.3 of
     * <a HREF="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>:
     * <blockquote>
     * The GET method means retrieve whatever information (in the form of an
     * entity) is identified by the Request-URI.
     * </blockquote>
     * </dd>
     * <dt>HTTP HEAD</dt>
     * <dd>
     * The HTTP HEAD method is defined in section 9.4 of
     * <a HREF="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>:
     * <blockquote>
     * The HEAD method is identical to GET except that the server MUST NOT
     * return a message-body in the response.
     * </blockquote>
     * </dd>
     * </dl>
     *
     * @parameter default-value="head"
     */
    protected String httpMethod;

    /**
     * The list of links to exclude.
     *
     * @parameter
     */
    protected String[] exludedLinks;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "linkcheck-report", locale, "report.linkcheck.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "linkcheck-report", locale, "report.linkcheck.name" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "linkcheck";
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
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return outputDirectory.exists();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !canGenerateReport() )
        {
            return;
        }

        // Wrap linkcheck
        LinkCheck lc = new LinkCheck();
        lc.setOnline( !offline );
        lc.setBasedir( outputDirectory );
        lc.setReportOutput( new File( linkcheckOutput ) );
        lc.setLinkCheckCache( new File( linkcheckCache ) );
        lc.setExcludedLinks( exludedLinks );
        lc.setReportLevel( reportLevel );

        HttpBean bean = new HttpBean();
        bean.setMethod( httpMethod );
        bean.setFollowRedirects( httpFollowRedirect );

        Proxy proxy = settings.getActiveProxy();
        if ( proxy != null )
        {
            bean.setProxyHost( proxy.getHost() );
            bean.setProxyPort( proxy.getPort() );
            bean.setProxyUser( proxy.getUsername() );
            bean.setProxyPassword( proxy.getPassword() );
        }
        lc.setHttp( bean );

        lc.doExecute();

        // Render report
        LinkcheckRenderer r = new LinkcheckRenderer( getSink(), i18n, locale, lc.getModel() );
        r.render();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private static class LinkcheckRenderer
        extends AbstractMavenReportRenderer
    {
        private I18N i18n;

        private Locale locale;

        private LinkcheckModel linkcheckModel;

        LinkcheckRenderer( Sink sink, I18N i18n, Locale locale, LinkcheckModel linkcheckModel )
        {
            super( sink );

            this.i18n = i18n;

            this.locale = locale;

            this.linkcheckModel = linkcheckModel;
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "linkcheck-report", locale, "report.linkcheck.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            if ( linkcheckModel == null )
            {
                startSection( getTitle() );

                paragraph( i18n.getString( "linkcheck-report", locale, "report.linkcheck.empty" ) );

                endSection();

                return;
            }

            // Overview
            startSection( getTitle() );

            paragraph(  i18n.getString( "linkcheck-report", locale, "report.linkcheck.overview" ) );

            //Statistics
            generateSummarySection();

            //Statistics
            generateDetailsSection();

            endSection();
        }

        private void generateSummarySection()
        {
            // Calculus
            List linkcheckFiles = linkcheckModel.getFiles();

            int totalFiles = linkcheckFiles.size();

            int totalLinks = 0;
            int totalValidLinks = 0;
            int totalErrorLinks = 0;
            int totalWarningLinks = 0;
            for ( Iterator it = linkcheckFiles.iterator(); it.hasNext(); )
            {
                LinkcheckFile linkcheckFile = (LinkcheckFile) it.next();

                totalLinks += linkcheckFile.getNumberOfLinks();
                totalValidLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.VALID_LEVEL );
                totalErrorLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.ERROR_LEVEL );
                totalWarningLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.WARNING_LEVEL );
            }

            startSection( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary" ) );

            paragraph(  i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.overview" ) );

            startTable();

            //Header
            generateTableHeader( false );

            //Content
            sink.tableRow();

            sink.tableCell();
            sink.bold();
            sink.text( totalFiles + "" );
            sink.bold_();
            sink.tableCell_();
            sink.tableCell();
            sink.bold();
            sink.text( totalLinks + "" );
            sink.bold_();
            sink.tableCell_();
            sink.tableCell();
            sink.bold();
            sink.text( String.valueOf( totalValidLinks ) );
            sink.bold_();
            sink.tableCell_();
            sink.tableCell();
            sink.bold();
            sink.text( String.valueOf( totalWarningLinks ) );
            sink.bold_();
            sink.tableCell_();
            sink.tableCell();
            sink.bold();
            sink.text( String.valueOf( totalErrorLinks ) );
            sink.bold_();
            sink.tableCell_();

            sink.tableRow_();

            endTable();

            endSection();
        }

        private void generateDetailsSection()
        {
            startSection( i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail" ) );

            paragraph(  i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail.overview" ) );

            startTable();

            //Header
            generateTableHeader( true );

            // Content
            List linkcheckFiles = linkcheckModel.getFiles();
            for ( Iterator it = linkcheckFiles.iterator(); it.hasNext(); )
            {
                LinkcheckFile linkcheckFile = (LinkcheckFile) it.next();

                sink.tableRow();

                sink.tableCell();
                if ( linkcheckFile.getUnsuccessful() == 0 )
                {
                    iconValid();
                }
                else
                {
                    iconError();
                }
                sink.tableCell_();

                tableCell( createLinkPatternedText( linkcheckFile.getRelativePath(), "./"
                    + linkcheckFile.getRelativePath() ) );
                tableCell( String.valueOf( linkcheckFile.getNumberOfLinks() ) );
                tableCell( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.VALID_LEVEL ) ) );
                tableCell( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.WARNING_LEVEL ) ) );
                tableCell( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.ERROR_LEVEL ) ) );

                sink.tableRow_();

                // Detail error
                if ( linkcheckFile.getUnsuccessful() != 0 )
                {
                    sink.tableRow();

                    sink.tableCell();
                    sink.text( "" );
                    sink.tableCell_();

                    sink.tableCell( "5" );

                    startTable();

                    for ( Iterator it2 = linkcheckFile.getResults().iterator(); it2.hasNext(); )
                    {
                        LinkcheckFileResult linkcheckFileResult = (LinkcheckFileResult) it2.next();

                        if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.VALID_LEVEL )
                        {
                            continue;
                        }

                        sink.tableRow();

                        sink.tableCell();
                        if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.WARNING_LEVEL )
                        {
                            iconWarning();
                        }
                        else if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.ERROR_LEVEL )
                        {
                            iconError();
                        }
                        sink.tableCell_();

                        sink.tableCell();
                        sink.italic();
                        sink.link( linkcheckFileResult.getTarget() );
                        sink.text( linkcheckFileResult.getTarget() );
                        sink.link_();
                        sink.text( ": " );
                        sink.text( linkcheckFileResult.getErrorMessage() );
                        sink.italic_();
                        sink.tableCell_();

                        sink.tableRow_();
                    }

                    endTable();

                    sink.tableCell_();

                    sink.tableRow_();
                }
            }

            sink.tableRow();

            endTable();

            endSection();
        }

        private void generateTableHeader( boolean withImage )
        {
            sink.tableRow();
            if ( withImage )
            {
                sink.rawText( "<th rowspan=\"2\">" );
                sink.text( "" );
                sink.tableHeaderCell_();
            }
            sink.rawText( "<th rowspan=\"2\">" );
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.documents" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell( "4" );
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.links" ) );
            sink.tableHeaderCell_();
            sink.tableRow_();

            sink.tableRow();
            tableHeaderCell( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.totalLinks" ) );
            sink.tableHeaderCell();
            iconValid();
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            iconWarning();
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            iconError();
            sink.tableHeaderCell_();
            sink.tableRow_();
        }

        private void iconError()
        {
            sink.figure();
            sink.figureCaption();
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.error" ) );
            sink.figureCaption_();
            sink.figureGraphics( "images/icon_error_sml.gif" );
            sink.figure_();
        }

        private void iconValid()
        {
            sink.figure();
            sink.figureCaption();
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.valid" ) );
            sink.figureCaption_();
            sink.figureGraphics( "images/icon_success_sml.gif" );
            sink.figure_();
        }

        private void iconWarning()
        {
            sink.figure();
            sink.figureCaption();
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.warning" ) );
            sink.figureCaption_();
            sink.figureGraphics( "images/icon_warning_sml.gif" );
            sink.figure_();
        }
    }
}
