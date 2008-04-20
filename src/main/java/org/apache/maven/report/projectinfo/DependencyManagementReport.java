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

import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependencyManagementRenderer;


/**
 * Generates the Project Dependency Management report.
 *
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 * @goal dependency-management
 * @requiresDependencyResolution test
 */
public class DependencyManagementReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencyManagement.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencyManagement.description" );
    }

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {

        ManagementDependencies dependencies =
            new ManagementDependencies( project.getDependencyManagement().getDependencies() );

        DependencyManagementRenderer r = new DependencyManagementRenderer( getSink(), locale, i18n, dependencies );

        r.setLog( getLog() );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "dependency-management";
    }

    public boolean canGenerateReport()
    {

        return project.getDependencyManagement() != null;
    }
}
