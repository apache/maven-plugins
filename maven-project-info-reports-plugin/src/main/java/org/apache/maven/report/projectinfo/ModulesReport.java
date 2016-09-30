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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.i18n.I18N;

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
    public boolean canGenerateReport()
    {
        boolean result = super.canGenerateReport();
        if ( result && skipEmptyReport )
        {
            result = !isEmpty( getProject().getModel().getModules() );
        }

        return result;
    }

    @Override
    public void executeReport( Locale locale )
    {
        new ModulesRenderer( getSink(), getProject(), getReactorProjects(), mavenProjectBuilder, localRepository,
                             getI18N( locale ), locale, getLog(), siteTool ).render();
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

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    static class ModulesRenderer
        extends AbstractProjectInfoRenderer
    {

        protected final Log log;

        protected MavenProject project;

        protected List<MavenProject> reactorProjects;

        protected MavenProjectBuilder mavenProjectBuilder;

        protected ArtifactRepository localRepository;

        protected SiteTool siteTool;

        ModulesRenderer( Sink sink, MavenProject project, List<MavenProject> reactorProjects,
                         MavenProjectBuilder mavenProjectBuilder, ArtifactRepository localRepository, I18N i18n,
                         Locale locale, Log log, SiteTool siteTool )
        {
            super( sink, i18n, locale );

            this.project = project;
            this.reactorProjects = reactorProjects;
            this.mavenProjectBuilder = mavenProjectBuilder;
            this.localRepository = localRepository;
            this.siteTool = siteTool;
            this.log = log;
        }

        @Override
        protected String getI18Nsection()
        {
            return "modules";
        }

        @Override
        public void renderBody()
        {
            List<String> modules = project.getModel().getModules();

            if ( modules == null || modules.isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getI18nString( "nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            paragraph( getI18nString( "intro" ) );

            startTable();

            String name = getI18nString( "header.name" );
            String description = getI18nString( "header.description" );
            tableHeader( new String[] { name, description } );

            final String baseUrl = getDistMgmntSiteUrl( project );

            for ( String module : modules )
            {
                MavenProject moduleProject = getModuleFromReactor( project, reactorProjects, module );

                if ( moduleProject == null )
                {
                    log.warn( "Module " + module + " not found in reactor: loading locally" );

                    File f = new File( project.getBasedir(), module + "/pom.xml" );
                    if ( f.exists() )
                    {
                        try
                        {
                            moduleProject = mavenProjectBuilder.build( f, localRepository, null );
                        }
                        catch ( ProjectBuildingException e )
                        {
                            throw new IllegalStateException( "Unable to read local module POM", e );
                        }
                    }
                    else
                    {
                        moduleProject = new MavenProject();
                        moduleProject.setName( module );
                        moduleProject.setDistributionManagement( new DistributionManagement() );
                        moduleProject.getDistributionManagement().setSite( new Site() );
                        moduleProject.getDistributionManagement().getSite().setUrl( module );
                    }
                }
                final String moduleName =
                    ( moduleProject.getName() == null ) ? moduleProject.getArtifactId() : moduleProject.getName();
                final String moduleHref =
                    getRelativeLink( baseUrl, getDistMgmntSiteUrl( moduleProject ), moduleProject.getArtifactId() );

                tableRow( new String[] { linkedName( moduleName, moduleHref ), moduleProject.getDescription() } );
            }

            endTable();

            endSection();
        }

        private MavenProject getModuleFromReactor( MavenProject project, List<MavenProject> reactorProjects,
                                                   String module )
        {
            // Mainly case of unit test
            if ( reactorProjects == null )
            {
                return null;
            }
            try
            {
                File moduleBasedir = new File( project.getBasedir(), module ).getCanonicalFile();

                for ( MavenProject reactorProject : reactorProjects )
                {
                    if ( moduleBasedir.equals( reactorProject.getBasedir() ) )
                    {
                        return reactorProject;
                    }
                }
            }
            catch ( IOException e )
            {
                log.error( "Error while populating modules menu: " + e.getMessage(), e );
            }
            // module not found in reactor
            return null;
        }

        /**
         * Return distributionManagement.site.url if defined, null otherwise.
         *
         * @param project not null
         * @return could be null
         */
        private static String getDistMgmntSiteUrl( MavenProject project )
        {
            return getDistMgmntSiteUrl( project.getDistributionManagement() );
        }

        private static String getDistMgmntSiteUrl( DistributionManagement distMgmnt )
        {
            if ( distMgmnt != null && distMgmnt.getSite() != null && distMgmnt.getSite().getUrl() != null )
            {
                return urlEncode( distMgmnt.getSite().getUrl() );
            }

            return null;
        }

        private static String urlEncode( final String url )
        {
            if ( url == null )
            {
                return null;
            }

            try
            {
                return new File( url ).toURI().toURL().toExternalForm();
            }
            catch ( MalformedURLException ex )
            {
                return url; // this will then throw somewhere else
            }
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
