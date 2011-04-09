package org.apache.maven.reporting.exec;

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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * Bean which contains necessary informations to build {@link MavenReportExecution} 
 * with {@link MavenReportExecutor}.
 * The rationale is to store some informations regarding the current Maven execution.
 *
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 3.0-beta-1
 * @version $Id$
 */
public class MavenReportExecutorRequest
{
    private ArtifactRepository localRepository;

    private MavenSession mavenSession;

    private MavenProject project;

    private ReportPlugin[] reportPlugins;

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public MavenSession getMavenSession()
    {
        return mavenSession;
    }

    public void setMavenSession( MavenSession mavenSession )
    {
        this.mavenSession = mavenSession;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public ReportPlugin[] getReportPlugins()
    {
        return reportPlugins;
    }

    public void setReportPlugins( ReportPlugin[] reportPlugins )
    {
        this.reportPlugins = reportPlugins;
    }

}
