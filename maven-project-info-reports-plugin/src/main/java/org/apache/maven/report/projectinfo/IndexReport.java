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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.i18n.I18N;

import java.util.List;
import java.util.Locale;

/**
 * Generates the Project Index report.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "index" )
public class IndexReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public String getName( Locale locale )
    {
        return getI18nString( locale, "title" );
    }

    @Override
    public String getDescription( Locale locale )
    {
        String desc;
        if ( project.getDescription() != null )
        {
            // TODO How to handle i18n?
            desc = project.getDescription();
        }
        else
        {
            return getI18nString( locale, "nodescription" );
        }
        return desc;
    }

    @Override
    public void executeReport( Locale locale )
    {
        ProjectIndexRenderer r = new ProjectIndexRenderer( project, getReactorProjects(), mavenProjectBuilder,
                                                           localRepository, getName( locale ), getDescription( locale ),
                                                           getSink(), getI18N( locale ), locale, getLog(), siteTool );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "index";
    }

    @Override
    protected String getI18Nsection()
    {
        return "index";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class ProjectIndexRenderer
        extends ModulesReport.ModulesRenderer
    {
        private final String title;

        private final String description;

        private boolean modules = false;

        ProjectIndexRenderer( MavenProject project, List<MavenProject> reactorProjects,
                              MavenProjectBuilder mavenProjectBuilder, ArtifactRepository localRepository, String title,
                              String description, Sink sink, I18N i18n, Locale locale, Log log, SiteTool siteTool )
        {
            super( sink, project, reactorProjects, mavenProjectBuilder, localRepository, i18n, locale, log, siteTool );

            this.title = title;

            this.description = description;
        }

        @Override
        public String getTitle()
        {
            return modules ? super.getTitle() : title;
        }

        @Override
        public void renderBody()
        {
            startSection( title.trim() + " " + project.getName() );

            paragraph( description );

            if ( !project.getModel().getModules().isEmpty() )
            {
                modules = true;
                super.renderBody();
            }

            endSection();
        }
    }
}
