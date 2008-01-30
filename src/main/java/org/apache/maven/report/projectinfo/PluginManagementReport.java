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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.report.projectinfo.dependencies.renderer.PluginManagementRenderer;

/**
 * Generates the Project Plugin Management report.
 *
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 * @goal plugin-management
 * @requiresDependencyResolution test
 */
public class PluginManagementReport
    extends AbstractProjectInfoReport
{
    /**
     * Maven Project Builder component.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Maven Artifact Factory component.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.pluginManagement.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.pluginManagement.description" );
    }

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        PluginManagementRenderer r =
            new PluginManagementRenderer( getSink(), locale, i18n, project.getPluginManagement().getPlugins(), project,
                                          mavenProjectBuilder, artifactFactory, localRepository );

        r.setLog( getLog() );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "plugin-management";
    }

    public boolean canGenerateReport()
    {
        return project.getPluginManagement() != null;
    }
}
