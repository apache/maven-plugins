package org.apache.maven.plugin.assembly.mojos;

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

import org.apache.maven.project.MavenProject;

/**
 * Like the <code>assembly:attached</code> goal, assemble an application bundle or distribution using an assembly
 * descriptor from the command line. This goal will force Maven to build all included POMs up to the
 * <code>package</code> phase BEFORE the assembly is processed. This goal differs from <code>assembly:assembly</code> in
 * that it ignores the &lt;formats/&gt; section of the assembly descriptor, and forces the assembly to be created as a
 * directory in the project's build-output directory (usually <code>./target</code>). <br/>
 * 
 * This goal is also functionally equivalent to using the <code>assembly:assembly</code> goal in conjunction with the
 * <code>dir</code> assembly format. <br/>
 * 
 * <b>NOTE:</b> This goal should ONLY be run from the command line, and if building a multimodule project it should be
 * used from the root POM. Use the <code>assembly:directory-single</code> goal for binding your assembly to the
 * lifecycle.
 * 
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @version $Id$
 * @goal directory
 * @execute phase="package"
 * @aggregator
 * @inheritByDefault false
 * @deprecated Use assembly:single and an assembly with format == dir instead! This mojo is redundant.
 */
@Deprecated
public class DirectoryMojo
    extends AbstractDirectoryMojo
{
    /**
     * Get the executed project from the forked lifecycle.
     * 
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }
}
