package org.apache.maven.plugin.assembly.io;

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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.model.Assembly;

import java.io.File;
import java.util.List;

/**
 * @version $Id$
 */
public interface AssemblyReader
{

    public List<Assembly> readAssemblies( AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public Assembly getAssemblyForDescriptorReference( String ref, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public Assembly getAssemblyFromDescriptorFile( File file, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public void includeSiteInAssembly( Assembly assembly, AssemblerConfigurationSource configSource )
        throws MojoFailureException, InvalidAssemblerConfigurationException;

}
