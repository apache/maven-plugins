package org.apache.maven.plugin.assembly.testutils;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.io.File;
import java.util.List;

public class ConfigSourceStub
    implements AssemblerConfigurationSource
{

    private String archiverConfig;

    private MavenProject project;

    public File getArchiveBaseDirectory()
    {
        return null;
    }

    public File getBasedir()
    {
        return null;
    }

    public String getClassifier()
    {
        return null;
    }

    public String getDescriptor()
    {
        return null;
    }

    public String getDescriptorId()
    {
        return null;
    }

    public String[] getDescriptorReferences()
    {
        return null;
    }

    public File getDescriptorSourceDirectory()
    {
        return null;
    }

    public String[] getDescriptors()
    {
        return null;
    }

    public List<String> getFilters()
    {
        return null;
    }

    public String getFinalName()
    {
        return null;
    }

    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return null;
    }

    public ArtifactRepository getLocalRepository()
    {
        return null;
    }

    public MavenSession getMavenSession()
    {
        return null;
    }

    public File getOutputDirectory()
    {
        return null;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public List<MavenProject> getReactorProjects()
    {
        return null;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return null;
    }

    public File getSiteDirectory()
    {
        return null;
    }

    public String getTarLongFileMode()
    {
        return null;
    }

    public File getTemporaryRootDirectory()
    {
        return null;
    }

    public File getWorkingDirectory()
    {
        return null;
    }

    public boolean isAssemblyIdAppended()
    {
        return false;
    }

    public boolean isDryRun()
    {
        return false;
    }

    public boolean isIgnoreDirFormatExtensions()
    {
        return false;
    }

    public boolean isIgnoreMissingDescriptor()
    {
        return false;
    }

    public boolean isSiteIncluded()
    {
        return false;
    }

    public void setArchiverConfig( final String archiverConfig )
    {
        this.archiverConfig = archiverConfig;
    }

    public String getArchiverConfig()
    {
        return archiverConfig;
    }

    public MavenFileFilter getMavenFileFilter()
    {
        return null;
    }

    public void setProject( final MavenProject mavenProject )
    {
        project = mavenProject;
    }

    public boolean isUpdateOnly()
    {
        return false;
    }

    public boolean isIgnorePermissions()
    {
        return true;
    }

    public boolean isUseJvmChmod()
    {
        return true;
    }
    
    public String getEncoding() {
    	return null;
    }

}
