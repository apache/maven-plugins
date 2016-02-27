package org.apache.maven.report.projectinfo;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

/**
 * Generates the Project Issue Management report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "issue-tracking" )
public class IssueManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = getProject().getModel().getIssueManagement() != null;
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        IssueManagementRenderer r =
            new IssueManagementRenderer( getSink(), getProject().getModel(), getI18N( locale ), locale );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "issue-tracking";
    }

    @Override
    protected String getI18Nsection()
    {
        return "issue-management";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class IssueManagementRenderer
        extends AbstractProjectInfoRenderer
    {
        private Model model;

        IssueManagementRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.model = model;
        }

        @Override
        protected String getI18Nsection()
        {
            return "issue-management";
        }

        @Override
        public void renderBody()
        {
            IssueManagement issueManagement = model.getIssueManagement();
            if ( issueManagement == null )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "noissueManagement" ) );

                endSection();

                return;
            }

            String system = issueManagement.getSystem();
            String url = issueManagement.getUrl();

            // Overview
            startSection( getI18nString( "overview.title" ) );

            if ( isIssueManagementSystem( system, "jira" ) )
            {
                sink.paragraph();
                linkPatternedText( getI18nString( "jira.intro" ) );
                sink.paragraph_();
            }
            else if ( isIssueManagementSystem( system, "bugzilla" ) )
            {
                sink.paragraph();
                linkPatternedText( getI18nString( "bugzilla.intro" ) );
                sink.paragraph_();
            }
            else if ( isIssueManagementSystem( system, "scarab" ) )
            {
                sink.paragraph();
                linkPatternedText( getI18nString( "scarab.intro" ) );
                sink.paragraph_();
            }
            else if ( system == null || "".equals( system.trim() ) )
            {
                paragraph( getI18nString( "general.intro" ) );
            }
            else
            {
                paragraph( getI18nString( "custom.intro" ).replaceFirst( "%issueManagementSystem%", system ) );
            }

            endSection();

            // Connection
            startSection( getTitle() );

            paragraph( getI18nString( "intro" ) );

            verbatimLink( url, url );

            endSection();
        }

        /**
         * Checks if a issue management system is Jira, bugzilla...
         *
         * @param system
         * @param actual
         * @return true if the issue management system is Jira, bugzilla, false otherwise.
         */
        private boolean isIssueManagementSystem( String system, String actual )
        {
            if ( StringUtils.isEmpty( system ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( actual ) )
            {
                return false;
            }

            return system.toLowerCase( Locale.ENGLISH ).startsWith( actual.toLowerCase( Locale.ENGLISH ) );
        }
    }
}
