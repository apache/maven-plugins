package org.apache.maven.plugin.assembly.archive.phase;

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

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;

/**
 * Handles one top-level section of the assembly descriptor, to determine which files to include in the assembly archive
 * for that section.
 * 
 * @version $Id$
 */
public interface AssemblyArchiverPhase
{
    String ROLE = AssemblyArchiverPhase.class.getName();

    /**
     * Handle the associated section of the assembly descriptor.
     * 
     * @param assembly
     *            The assembly descriptor to use
     * @param archiver
     *            The archiver used to create the assembly archive, to which files/directories/artifacts are added
     * @param configSource
     *            The configuration for this assembly build, normally derived from the plugin that launched the assembly
     *            process.
     * @param context
     */
    void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource,
                  AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException;
}
