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

import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the Project Distribution Management report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.3
 * @goal distribution-management
 */
public class DistributionManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        DistributionManagementRenderer r =
            new DistributionManagementRenderer( getSink(), getProject(), getI18N( locale ), locale );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "distribution-management";
    }

    @Override
    protected String getI18Nsection()
    {
        return "distributionManagement";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class DistributionManagementRenderer
        extends AbstractProjectInfoRenderer
    {
        private final MavenProject project;

        DistributionManagementRenderer( Sink sink, MavenProject project, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.project = project;
        }

        @Override
        protected String getI18Nsection()
        {
            return "distributionManagement";
        }

        @Override
        public void renderBody()
        {
            DistributionManagement distributionManagement = project.getDistributionManagement();
            if ( distributionManagement == null )
            {
                startSection( getI18nString( "overview.title" ) );

                paragraph( getI18nString( "nodistributionmanagement" ) );

                endSection();

                return;
            }

            startSection( getI18nString( "overview.title" ) );
            paragraph( getI18nString( "overview.intro" ) );

            if ( StringUtils.isNotEmpty( distributionManagement.getDownloadUrl() ) )
            {
                startSection( getI18nString( "downloadURL" ) );
                internalLink( distributionManagement.getDownloadUrl() );
                endSection();
            }

            if ( distributionManagement.getRelocation() != null )
            {
                startSection( getI18nString( "relocation" ) );
                startTable();
                tableHeader( new String[] { getI18nString( "field" ), getI18nString( "value" ) } );
                tableRow( new String[] { getI18nString( "relocation.groupid" ),
                    distributionManagement.getRelocation().getGroupId() } );
                tableRow( new String[] { getI18nString( "relocation.artifactid" ),
                    distributionManagement.getRelocation().getArtifactId() } );
                tableRow( new String[] { getI18nString( "relocation.version" ),
                    distributionManagement.getRelocation().getVersion() } );
                tableRow( new String[] { getI18nString( "relocation.message" ),
                    distributionManagement.getRelocation().getMessage() } );
                endTable();
                endSection();
            }

            if ( distributionManagement.getRepository() != null
                && StringUtils.isNotEmpty( distributionManagement.getRepository().getUrl() ) )
            {
                startSection( getI18nString( "repository" )
                    + getRepoName( distributionManagement.getRepository().getId() ) );
                internalLink( distributionManagement.getRepository().getUrl() );
                endSection();
            }

            if ( distributionManagement.getSnapshotRepository() != null
                && StringUtils.isNotEmpty( distributionManagement.getSnapshotRepository().getUrl() ) )
            {
                startSection( getI18nString( "snapshotRepository" )
                    + getRepoName( distributionManagement.getSnapshotRepository().getId() ) );
                internalLink( distributionManagement.getSnapshotRepository().getUrl() );
                endSection();
            }

            if ( distributionManagement.getSite() != null
                && StringUtils.isNotEmpty( distributionManagement.getSite().getUrl() ) )
            {
                startSection( getI18nString( "site" ) + getRepoName( distributionManagement.getSite().getId() ) );
                internalLink( distributionManagement.getSite().getUrl() );
                endSection();
            }

            endSection();
        }

        private void internalLink( String url )
        {
            if ( StringUtils.isEmpty( url ) )
            {
                return;
            }

            String urlLowerCase = url.trim().toLowerCase( Locale.ENGLISH );
            if ( urlLowerCase.startsWith( "http" ) || urlLowerCase.startsWith( "https" )
                || urlLowerCase.startsWith( "ftp" ) )
            {
                link( url, url );
            }
            else
            {
                paragraph( url );
            }
        }

        private String getRepoName( String name )
        {
            if ( StringUtils.isNotEmpty( name ) )
            {
                return " - " + name;
            }

            return "";
        }
    }
}
