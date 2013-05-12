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
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.i18n.I18N;

import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Modules report.
 *
 * @author ltheussl
 * @version $Id$
 * @since 2.2
 */
@Mojo( name = "modules" )
public class ModulesReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void executeReport( Locale locale )
    {
        new ModulesRenderer( getSink(), getProject(), getI18N( locale ), locale, siteTool ).render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "modules";
    }

    @Override
    protected String getI18Nsection()
    {
        return "modules";
    }

    @Override
    public boolean canGenerateReport()
    {
        return ( getProject().getModel().getModules() != null && !getProject().getModel().getModules().isEmpty() );
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    static class ModulesRenderer
        extends AbstractProjectInfoRenderer
    {
        protected MavenProject project;

        protected SiteTool siteTool;

        ModulesRenderer( Sink sink, MavenProject project, I18N i18n, Locale locale, SiteTool siteTool )
        {
            super( sink, i18n, locale );

            this.project = project;
            this.siteTool = siteTool;
        }

        @Override
        protected String getI18Nsection()
        {
            return "modules";
        }

        @Override
        public void renderBody()
        {
            startSection( getTitle() );

            paragraph( getI18nString( "intro" ) );

            startTable();

            String name = getI18nString( "header.name" );
            String description = getI18nString( "header.description" );
            tableHeader( new String[] {name, description} );

            final String baseURL = project.getUrl();
            
            // before MPIR-229 this was model.getModules(), which could have uninherited/unresolved values
            // @todo also include modules which are not part of reactor, e.g. caused by -pl 
            List<MavenProject> modules = project.getCollectedProjects();
            for ( MavenProject moduleProject : modules )
            {
                Model moduleModel = moduleProject.getModel();

                final String moduleName = moduleProject.getName();

                final String moduleHref = getRelativeLink( baseURL, moduleProject.getUrl(), moduleProject.getArtifactId() );

                tableRow( new String[] {linkedName( moduleName, moduleHref ), moduleModel.getDescription()} );
            }

            endTable();

            endSection();
        }

        // adapted from DefaultSiteTool#appendMenuItem
        private String getRelativeLink( String baseUrl, String href, String defaultHref )
        {
            String selectedHref = href;

            if ( selectedHref == null )
            {
                selectedHref = defaultHref;
            }

            if ( baseUrl != null )
            {
                selectedHref = siteTool.getRelativePath( selectedHref, baseUrl );
            }

            if ( selectedHref.endsWith( "/" ) )
            {
                selectedHref = selectedHref.concat( "index.html" );
            }
            else
            {
                selectedHref = selectedHref.concat( "/index.html" );
            }

            return selectedHref;
        }

        private String linkedName( String name, String link )
        {
            return "{" + name + ", ./" + link + "}";
        }
    }
}
