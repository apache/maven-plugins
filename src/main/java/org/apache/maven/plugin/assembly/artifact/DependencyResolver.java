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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.ModuleSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convenience component that aids in the resolution of dependency artifacts, according to various configurations such
 * as transitivity flag and scope.
 *
 * @version $Id$
 */
public interface DependencyResolver
{

    public Map<DependencySet, Set<Artifact>> resolveDependencySets( final Assembly assembly, ModuleSet moduleSet,
                                                                    final AssemblerConfigurationSource configSource,
                                                                    List<DependencySet> dependencySets )
        throws DependencyResolutionException
    ;
    public Map<DependencySet, Set<Artifact>> resolveDependencySets( final Assembly assembly,
                                                                    final AssemblerConfigurationSource configSource,
                                                                    List<DependencySet> dependencySets )
        throws DependencyResolutionException
        ;

}