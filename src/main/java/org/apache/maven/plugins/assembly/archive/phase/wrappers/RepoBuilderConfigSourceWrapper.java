package org.apache.maven.plugins.assembly.archive.phase.wrappers;

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

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.repository.RepositoryBuilderConfigSource;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

/**
 * @version $Id$
 */
public class RepoBuilderConfigSourceWrapper
    implements RepositoryBuilderConfigSource
{

    private final AssemblerConfigurationSource configSource;

    public RepoBuilderConfigSourceWrapper( AssemblerConfigurationSource configSource )
    {
        this.configSource = configSource;
    }

    @Override
    public MavenProject getProject()
    {
        return configSource.getProject();
    }

    @Override
    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        ProjectBuildingRequest pbr =
            new DefaultProjectBuildingRequest( configSource.getMavenSession().getProjectBuildingRequest() );
        pbr.setRemoteRepositories( getProject().getRemoteArtifactRepositories() );
        return pbr;
    }
}
