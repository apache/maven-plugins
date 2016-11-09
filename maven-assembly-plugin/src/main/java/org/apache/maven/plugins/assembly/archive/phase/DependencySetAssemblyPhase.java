package org.apache.maven.plugins.assembly.archive.phase;

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
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Handles the top-level &lt;dependencySets/&gt; section of the assembly descriptor.
 *
 * @version $Id$
 */
@Component( role = AssemblyArchiverPhase.class, hint = "dependency-sets" )
public class DependencySetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase, PhaseOrder
{

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private ArchiverManager archiverManager;

    @Requirement
    private DependencyResolver dependencyResolver;


    /**
     * Default constructor.
     */
    public DependencySetAssemblyPhase()
    {
        // used for plexus init
    }

    /**
     * @param projectBuilder The Maven Project Builder.
     * @param logger         The Logger.
     */
    public DependencySetAssemblyPhase( final ProjectBuilder projectBuilder, DependencyResolver dependencyResolver,
                                       final Logger logger )
    {
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
        enableLogging( logger );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        DependencyResolutionException
    {

        Map<DependencySet, Set<Artifact>> resolved =
            dependencyResolver.resolveDependencySets( assembly, configSource, assembly.getDependencySets() );
        for ( Map.Entry<DependencySet, Set<Artifact>> dependencySetSetEntry : resolved.entrySet() )
        {
            final AddDependencySetsTask task =
                new AddDependencySetsTask( Collections.singletonList( dependencySetSetEntry.getKey() ),
                                           dependencySetSetEntry.getValue(), configSource.getProject(), projectBuilder,
                                           getLogger() );

            task.execute( archiver, configSource );
        }
    }

    @Override
    public int order()
    {
        // CHECKSTYLE_OFF: MagicNumber
        return 40;
        // CHECKSTYLE_ON: MagicNumber
    }
}
