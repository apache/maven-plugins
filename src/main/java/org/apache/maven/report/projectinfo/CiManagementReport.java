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
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Continuous Integration Management report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "cim" )
public class CiManagementReport
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
             CiManagement cim = getProject().getModel().getCiManagement();
             result = cim != null ;
         }

         return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        CiManagementRenderer r = new CiManagementRenderer( getSink(), getProject().getModel(),
                                                              getI18N( locale ), locale );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "integration";
    }

    @Override
    protected String getI18Nsection()
    {
        return "ci-management";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class CiManagementRenderer
        extends AbstractProjectInfoRenderer
    {
        private Model model;

        CiManagementRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.model = model;
        }

        @Override
        protected String getI18Nsection()
        {
            return "ci-management";
        }

        @Override
        public void renderBody()
        {
            CiManagement cim = model.getCiManagement();
            if ( cim == null )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nocim" ) );

                endSection();

                return;
            }

            String system = cim.getSystem();
            String url = cim.getUrl();
            List<Notifier> notifiers = cim.getNotifiers();

            // Overview
            startSection( getI18nString( "overview.title" ) );

            sink.paragraph();
            if ( isCiManagementSystem( system, "anthill" ) )
            {
                linkPatternedText( getI18nString( "anthill.intro" ) );
            }
            else if ( isCiManagementSystem( system, "bamboo" ) )
            {
                linkPatternedText( getI18nString( "bamboo.intro" ) );
            }
            else if ( isCiManagementSystem( system, "buildforge" ) )
            {
                linkPatternedText( getI18nString( "buildforge.intro" ) );
            }
            else if ( isCiManagementSystem( system, "continuum" ) )
            {
                linkPatternedText( getI18nString( "continuum.intro" ) );
            }
            else if ( isCiManagementSystem( system, "cruisecontrol" ) )
            {
                linkPatternedText( getI18nString( "cruisecontrol.intro" ) );
            }
            else if ( isCiManagementSystem( system, "hudson" ) )
            {
                linkPatternedText( getI18nString( "hudson.intro" ) );
            }
            else if ( isCiManagementSystem( system, "jenkins" ) )
            {
                linkPatternedText( getI18nString( "jenkins.intro" ) );
            }
            else if ( isCiManagementSystem( system, "luntbuild" ) )
            {
                linkPatternedText( getI18nString( "luntbuild.intro" ) );
            }
            else if ( isCiManagementSystem( system, "travis" ) )
            {
                linkPatternedText( getI18nString( "travis.intro" ) );
            }
            else
            {
                linkPatternedText( getI18nString( "general.intro" ) );
            }
            sink.paragraph_();

            endSection();

            // Access
            startSection( getI18nString( "access" ) );

            if ( !StringUtils.isEmpty( url ) )
            {
                paragraph( getI18nString( "url" ) );

                verbatimLink( url, url );
            }
            else
            {
                paragraph( getI18nString( "nourl" ) );
            }

            endSection();

            // Notifiers
            startSection( getI18nString( "notifiers.title" ) );

            if ( notifiers == null || notifiers.isEmpty() )
            {
                paragraph( getI18nString( "notifiers.nolist" ) );
            }
            else
            {
                sink.paragraph();
                sink.text( getI18nString( "notifiers.intro" ) );
                sink.paragraph_();

                startTable();

                String type = getI18nString( "notifiers.column.type" );
                String address = getI18nString( "notifiers.column.address" );
                String configuration = getI18nString( "notifiers.column.configuration" );

                tableHeader( new String[]{type, address, configuration} );

                for ( Notifier notifier : notifiers )
                {
                    tableRow( new String[]{notifier.getType(),
                        createLinkPatternedText( notifier.getAddress(), notifier.getAddress() ),
                        propertiesToString( notifier.getConfiguration() )} );
                }

                endTable();
            }

            endSection();
        }

        /**
         * Checks if a CI management system is bugzilla, continuum...
         *
         * @return true if the CI management system is bugzilla, continuum..., false otherwise.
         */
        private boolean isCiManagementSystem( String system, String actual )
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
