package org.apache.maven.plugin.assembly;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.io.File;
import java.util.List;

/**
 * @version $Id$
 */
public interface AssemblerConfigurationSource
{

    String getDescriptor();

    String getDescriptorId();

    String[] getDescriptors();

    String[] getDescriptorReferences();

    File getDescriptorSourceDirectory();

    File getBasedir();

    MavenProject getProject();

    boolean isSiteIncluded();

    File getSiteDirectory();

    String getFinalName();

    boolean isAssemblyIdAppended();

    String getClassifier();

    String getTarLongFileMode();

    File getOutputDirectory();

    File getWorkingDirectory();

    MavenArchiveConfiguration getJarArchiveConfiguration();

    ArtifactRepository getLocalRepository();

    File getTemporaryRootDirectory();

    File getArchiveBaseDirectory();

    List getFilters();

    List getReactorProjects();

    List getRemoteRepositories();

    boolean isDryRun();

    boolean isIgnoreDirFormatExtensions();

    boolean isIgnoreMissingDescriptor();
    
    MavenSession getMavenSession();
    
    String getArchiverConfig();

    MavenFileFilter getMavenFileFilter();
}
