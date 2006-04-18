package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Continuous Integration System report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal cim
 */
public class CimReport
    extends AbstractMavenReport
{
    // ----------------------------------------------------------------------
    // Parameters
    // ----------------------------------------------------------------------

    /**
     * Report output directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * Doxia SiteRender.
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

    // ----------------------------------------------------------------------
    // MavenReport Implementation
    // ----------------------------------------------------------------------

    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.cim.name" );
    }

    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.cim.description" );
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    public void executeReport( Locale locale )
    {
        CimRenderer r = new CimRenderer( getSink(), getProject().getModel(), i18n, locale );

        r.render();
    }

    public String getOutputName()
    {
        return "integration";
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    static class CimRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        public CimRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink );

            this.model = model;

            this.i18n = i18n;

            this.locale = locale;
        }

        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.cim.title" );
        }

        public void renderBody()
        {
            CiManagement cim = model.getCiManagement();
            if ( cim == null )
            {
                startSection( getTitle() );

                paragraph( i18n.getString( "project-info-report", locale, "report.cim.nocim" ) );

                endSection();

                return;
            }

            String system = cim.getSystem();
            String url = cim.getUrl();
            List notifiers = cim.getNotifiers();

            // Overview
            startSection( i18n.getString( "project-info-report", locale, "report.cim.overview.title" ) );

            if ( isCimSystem( system, "continuum" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.cim.continuum.intro" ) );
            }
            else if ( isCimSystem( system, "bugzilla" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.cim.bugzilla.intro" ) );
            }
            else
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.cim.general.intro" ) );
            }

            endSection();

            // Access
            startSection( i18n.getString( "project-info-report", locale, "report.cim.access" ) );

            if ( !StringUtils.isEmpty( url ) )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.cim.url" ) );

                verbatimLink( url, url );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.cim.nourl" ) );
            }

            endSection();

            // Notifiers
            startSection( i18n.getString( "project-info-report", locale, "report.cim.notifiers.title" ) );

            if ( ( notifiers == null ) || ( notifiers.isEmpty() ) )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.cim.notifiers.nolist" ) );
            }
            else
            {
                startTable();

                tableCaption( i18n.getString( "project-info-report", locale, "report.cim.notifiers.intro" ) );

                String type = i18n.getString( "project-info-report", locale, "report.cim.notifiers.column.type" );
                String address = i18n.getString( "project-info-report", locale, "report.cim.notifiers.column.address" );
                String configuration =
                    i18n.getString( "project-info-report", locale, "report.cim.notifiers.column.configuration" );

                tableHeader( new String[]{type, address, configuration} );

                for ( Iterator i = notifiers.iterator(); i.hasNext(); )
                {
                    Notifier notifier = (Notifier) i.next();

                    tableRow( new String[]{notifier.getType(),
                        createLinkPatternedText( notifier.getAddress(), notifier.getAddress() ),
                        propertiesToString( notifier.getConfiguration() )} );
                }

                endTable();
            }

            endSection();
        }

        /**
         * Checks if a CIM system is bugzilla, continium...
         *
         * @return true if the CIM system is bugzilla, continium..., false
         *         otherwise.
         */
        private boolean isCimSystem( String connection, String cim )
        {
            if ( StringUtils.isEmpty( connection ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( cim ) )
            {
                return false;
            }

            if ( connection.toLowerCase().startsWith( cim.toLowerCase() ) )
            {
                return true;
            }

            return false;
        }
    }
}
