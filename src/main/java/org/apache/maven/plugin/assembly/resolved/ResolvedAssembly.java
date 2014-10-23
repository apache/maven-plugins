package org.apache.maven.plugin.assembly.resolved;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.resolved.functions.ResolvedModuleSetConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResolvedAssembly
{

    private final Assembly assembly;

    private final List<ResolvedModuleSet> resolvedModuleSets;

    private final Set<Artifact> dependencySetArtifacts;

    private ResolvedAssembly( Assembly assembly, List<ResolvedModuleSet> resolvedModuleSets,
                              Set<Artifact> dependencySetArtifacts )
    {
        this.assembly = assembly;
        this.resolvedModuleSets = resolvedModuleSets;
        this.dependencySetArtifacts = dependencySetArtifacts;
    }

    public static ResolvedAssembly create( Assembly assembly )
    {
        return new ResolvedAssembly( assembly, null, null );
    }

    public ResolvedAssembly withResolvedModuleSets( Iterable<ResolvedModuleSet> resolvedModuleSets )
    {
        List<ResolvedModuleSet> resolvedModuleSets1 = new ArrayList<ResolvedModuleSet>();
        for ( ResolvedModuleSet resolvedModuleSet : resolvedModuleSets )
        {
            resolvedModuleSets1.add( resolvedModuleSet );
        }
        return new ResolvedAssembly( assembly, resolvedModuleSets1, dependencySetArtifacts );
    }

    public void forEachResolvedModule( ResolvedModuleSetConsumer resolvedModuleSetConsumer )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( resolvedModuleSets == null )
            return;
        for ( ResolvedModuleSet resolvedModuleSet : resolvedModuleSets )
        {
            resolvedModuleSetConsumer.accept( resolvedModuleSet );
        }
    }

    public Set<Artifact> getResolvedDependencySetArtifacts()
    {
        return dependencySetArtifacts;
    }

    public List<DependencySet> getDependencySets()
    {
        return assembly.getDependencySets();
    }

    public List<FileItem> getFiles()
    {
        return assembly.getFiles();
    }

    public List<FileSet> getFileSets()
    {
        return assembly.getFileSets();
    }

    public List<Repository> getRepositories()
    {
        return assembly.getRepositories();
    }

    public ResolvedAssembly withDependencySetArtifacts( final Set<Artifact> dependencySetArtifacts )
    {
        return new ResolvedAssembly( assembly, resolvedModuleSets, dependencySetArtifacts );
    }
}
