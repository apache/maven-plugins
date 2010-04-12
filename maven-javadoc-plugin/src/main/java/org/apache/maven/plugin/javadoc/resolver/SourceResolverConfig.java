package org.apache.maven.plugin.javadoc.resolver;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

import java.io.File;
import java.util.List;

public class SourceResolverConfig
{

    private final MavenProject project;

    private ArtifactFilter filter;

    private List<MavenProject> reactorProjects;

    private final File outputBasedir;

    private boolean compileSourceIncluded;

    private boolean testSourceIncluded;

    private final ArtifactRepository localRepository;

    private final ArtifactResolver artifactResolver;

    private final ArtifactMetadataSource artifactMetadataSource;

    private final ArchiverManager archiverManager;

    private final ArtifactFactory artifactFactory;

    private final Log log;

    public SourceResolverConfig( final Log log, final MavenProject project, final ArtifactRepository localRepository,
                                 final File outputBasedir, final ArtifactResolver artifactResolver,
                                 final ArtifactFactory artifactFactory,
                                 final ArtifactMetadataSource artifactMetadataSource,
                                 final ArchiverManager archiverManager )
    {
        this.log = log;
        this.project = project;
        this.localRepository = localRepository;
        this.outputBasedir = outputBasedir;
        this.artifactResolver = artifactResolver;
        this.artifactFactory = artifactFactory;
        this.artifactMetadataSource = artifactMetadataSource;
        this.archiverManager = archiverManager;
    }

    public SourceResolverConfig withFilter( final ArtifactFilter filter )
    {
        this.filter = filter;
        return this;
    }

    public SourceResolverConfig withReactorProjects( final List<MavenProject> reactorProjects )
    {
        this.reactorProjects = reactorProjects;
        return this;
    }

    public SourceResolverConfig withCompileSources()
    {
        compileSourceIncluded = true;
        return this;
    }

    public SourceResolverConfig withoutCompileSources()
    {
        compileSourceIncluded = false;
        return this;
    }

    public SourceResolverConfig withTestSources()
    {
        testSourceIncluded = true;
        return this;
    }

    public SourceResolverConfig withoutTestSources()
    {
        testSourceIncluded = false;
        return this;
    }

    public MavenProject project()
    {
        return project;
    }

    public ArtifactRepository localRepository()
    {
        return localRepository;
    }

    public ArtifactFilter filter()
    {
        return filter;
    }

    public List<MavenProject> reactorProjects()
    {
        return reactorProjects;
    }

    public File outputBasedir()
    {
        return outputBasedir;
    }

    public boolean includeCompileSources()
    {
        return compileSourceIncluded;
    }

    public boolean includeTestSources()
    {
        return testSourceIncluded;
    }

    public ArtifactResolver artifactResolver()
    {
        return artifactResolver;
    }

    public ArtifactMetadataSource artifactMetadataSource()
    {
        return artifactMetadataSource;
    }

    public ArchiverManager archiverManager()
    {
        return archiverManager;
    }

    public ArtifactFactory artifactFactory()
    {
        return artifactFactory;
    }
    
    public Log log()
    {
        return log;
    }

}
