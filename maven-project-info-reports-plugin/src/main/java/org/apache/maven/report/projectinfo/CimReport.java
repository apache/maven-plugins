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
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Continuous Integration System report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal cim
 */
public class CimReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        CimRenderer r = new CimRenderer( getSink(), getProject().getModel(), i18n, locale );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "integration";
    }

    protected String getI18Nsection()
    {
        return "cim";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class CimRenderer
        extends AbstractProjectInfoRenderer
    {
        private Model model;

        CimRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.model = model;
        }

        protected String getI18Nsection()
        {
            return "cim";
        }

        /** {@inheritDoc} */
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
            if ( isCimSystem( system, "anthill" ) )
            {
                linkPatternedText( getI18nString( "anthill.intro" ) );
            }
            else if ( isCimSystem( system, "buildforge" ) )
            {
                linkPatternedText( getI18nString( "buildforge.intro" ) );
            }
            else if ( isCimSystem( system, "continuum" ) )
            {
                linkPatternedText( getI18nString( "continuum.intro" ) );
            }
            else if ( isCimSystem( system, "cruisecontrol" ) )
            {
                linkPatternedText( getI18nString( "cruisecontrol.intro" ) );
            }
            else if ( isCimSystem( system, "hudson" ) )
            {
                linkPatternedText( getI18nString( "hudson.intro" ) );
            }
            else if ( isCimSystem( system, "luntbuild" ) )
            {
                linkPatternedText( getI18nString( "luntbuild.intro" ) );
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
         * Checks if a CIM system is bugzilla, continuum...
         *
         * @param connection
         * @param cim
         * @return true if the CIM system is bugzilla, continuum..., false otherwise.
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

            return connection.toLowerCase( Locale.ENGLISH ).startsWith( cim.toLowerCase( Locale.ENGLISH ) );
        }
    }
}
