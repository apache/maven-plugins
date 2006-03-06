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

import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the Mailing List report.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal mailing-list
 */
public class MailingListsReport
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
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

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
        return i18n.getString( "project-info-report", locale, "report.mailing-lists.name" );
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
        return i18n.getString( "project-info-report", locale, "report.mailing-lists.description" );
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
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        MailingListsRenderer r = new MailingListsRenderer( getSink(), getProject().getModel(), i18n, locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "mail-lists";
    }

    static class MailingListsRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        public MailingListsRenderer( Sink sink, Model model, I18N i18n, Locale locale )
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
            return i18n.getString( "project-info-report", locale, "report.mailing-lists.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            List mailingLists = model.getMailingLists();

            if ( ( mailingLists == null ) || ( mailingLists.isEmpty() ) )
            {
                startSection( getTitle() );

                // TODO: should the report just be excluded?
                paragraph( i18n.getString( "project-info-report", locale, "report.mailing-lists.nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            paragraph( i18n.getString( "project-info-report", locale, "report.mailing-lists.intro" ) );

            startTable();

            // To beautify the display with other archives
            boolean otherArchives = false;
            for ( Iterator i = mailingLists.iterator(); i.hasNext(); )
            {
                MailingList m = (MailingList) i.next();

                if ( ( ( m.getOtherArchives() != null ) ) && ( !m.getOtherArchives().isEmpty() ) )
                {
                    otherArchives = true;
                }
            }

            String name = i18n.getString( "project-info-report", locale, "report.mailing-lists.column.name" );
            String subscribe = i18n.getString( "project-info-report", locale, "report.mailing-lists.column.subscribe" );
            String unsubscribe =
                i18n.getString( "project-info-report", locale, "report.mailing-lists.column.unsubscribe" );
            String post = i18n.getString( "project-info-report", locale, "report.mailing-lists.column.post" );
            String archive = i18n.getString( "project-info-report", locale, "report.mailing-lists.column.archive" );
            String archivesOther =
                i18n.getString( "project-info-report", locale, "report.mailing-lists.column.otherArchives" );

            if ( otherArchives )
            {
                tableHeader( new String[]{name, subscribe, unsubscribe, post, archive, archivesOther} );
            }
            else
            {
                tableHeader( new String[]{name, subscribe, unsubscribe, post, archive} );
            }

            for ( Iterator i = model.getMailingLists().iterator(); i.hasNext(); )
            {
                MailingList mailingList = (MailingList) i.next();

                List textRow = new ArrayList();

                // Validate here subsribe/unsubsribe lists and archives?
                textRow.add( mailingList.getName() );

                textRow.add( createLinkPatternedText( subscribe, mailingList.getSubscribe() ) );

                textRow.add( createLinkPatternedText( unsubscribe, mailingList.getUnsubscribe() ) );

                if ( mailingList.getPost() != null && mailingList.getPost().length() > 0 )
                {
                    textRow.add( createLinkPatternedText( post, mailingList.getPost() ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( mailingList.getArchive() != null && mailingList.getArchive().length() > 0 )
                {
                    textRow.add( createLinkPatternedText( getArchiveServer( mailingList.getArchive() ),
                                                          mailingList.getArchive() ) );
                }
                else
                {
                    textRow.add( "-" );
                }

                if ( ( ( mailingList.getOtherArchives() != null ) ) && ( !mailingList.getOtherArchives().isEmpty() ) )
                {
                    // For the first line
                    Iterator it = mailingList.getOtherArchives().iterator();
                    String otherArchive = it.next().toString();

                    textRow.add( createLinkPatternedText( getArchiveServer( otherArchive ), otherArchive ) );

                    tableRow( (String[]) textRow.toArray( new String[0] ) );

                    // Other lines...
                    while ( it.hasNext() )
                    {
                        otherArchive = (String) it.next();

                        // Reinit the list to beautify the display
                        textRow = new ArrayList();

                        // Name
                        textRow.add( " " );

                        // Subscribe
                        textRow.add( " " );

                        // UnSubscribe
                        textRow.add( " " );

                        // Post
                        textRow.add( " " );

                        // Archive
                        textRow.add( " " );

                        textRow.add( createLinkPatternedText( getArchiveServer( otherArchive ), otherArchive ) );

                        tableRow( (String[]) textRow.toArray( new String[0] ) );
                    }
                }
                else
                {
                    if ( otherArchives )
                    {
                        textRow.add( null );
                    }

                    tableRow( (String[]) textRow.toArray( new String[0] ) );
                }
            }

            endTable();

            endSection();
        }
    }

    /**
     * Convenience method to return the name of a web-based mailing list archive
     * server. <br>
     * For instance, if the archive uri is
     * <code>http://www.mail-archive.com/dev@maven.apache.org</code>, this
     * method return <code>www.mail-archive.com</code>
     *
     * @param uri
     * @return the server name of a web-based mailing list archive server
     */
    private static String getArchiveServer( String uri )
    {
        if ( StringUtils.isEmpty( uri ) )
        {
            return "???UNKNOWN???";
        }

        int at = uri.indexOf( "//" );
        int from = uri.indexOf( "/", at >= 0 ? ( uri.lastIndexOf( "/", at - 1 ) >= 0 ? 0 : at + 2 ) : 0 );

        return uri.substring( at + 2, from );
    }
}