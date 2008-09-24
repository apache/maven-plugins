package org.apache.maven.plugin.assembly.artifact;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * Convenience component that aids in the resolution of dependency artifacts,
 * according to various configurations such as transitivity flag and scope.
 * 
 * @version $Id$
 */
public interface DependencyResolver
{

    /**
     * Resolve the project dependencies, according to the supplied configuration.
     * 
     * @param project The project whose dependencies should be resolved
     * @param scope The dependency scope to resolve
     * @param managedVersions The map of managed versions, which allows 
     *                        dependency version conflict resolution to happen 
     *                        once for the entire assembly process.
     * @param localRepository The local repository which acts as a local cache 
     *                        for remote artifact repositories
     * @param remoteRepositories The list of remote {@link ArtifactRepository} 
     *                           instances to use during resolution, in addition 
     *                           to those defined in the supplied 
     *                           {@link MavenProject} instance.
     * @param resolveTransitively If true, resolve project dependencies 
     *                            transitively; if false, only resolve the 
     *                            project's direct dependencies.
     * @return The set of resolved {@link Artifact} instances for the project
     */
    Set resolveDependencies( MavenProject project, String scope, Map managedVersions, ArtifactRepository localRepository,
                                             List remoteRepositories, boolean resolveTransitively )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Traverse the assembly descriptor to determine which scopes, modules, etc.
     * will require dependency resolution. Once we have a complete picture of
     * what will be required in the way of resolution, feed all of this into the
     * {@link ArtifactCollector} to discover/resolve versions for all 
     * dependencies in the mix, then save these versions in a mapping of:
     * <br/>
     * {@link Artifact#getDependencyConflictId()} -&gt; {@link Artifact}
     * <br/>
     * This allows dependency conflict resolution to happen a single time, then
     * be reused multiple times during the construction of the assembly archive.
     */
    Map buildManagedVersionMap( Assembly assembly, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, InvalidVersionSpecificationException, InvalidDependencyVersionException,
        ArtifactResolutionException;

}