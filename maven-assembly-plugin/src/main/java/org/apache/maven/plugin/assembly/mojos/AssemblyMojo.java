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
 * Assemble an application bundle or distribution using an assembly descriptor from the command line. This goal will
 * force Maven to build all included POMs up to the <code>package</code> phase BEFORE the assembly is processed. <br/>
 * 
 * <b>NOTE:</b> This goal should ONLY be run from the command line, and if building a multimodule project it should be
 * used from the root POM. Use the <code>assembly:single</code> goal for binding your assembly to the lifecycle. <br/>
 * 
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * 
 * @goal assembly
 * @execute phase="package"
 * @aggregator
 * @inheritByDefault false
 * @deprecated Use assembly:single instead! The assembly:assembly mojo leads to non-standard builds.
 */
@Deprecated
public class AssemblyMojo
    extends AbstractAssemblyMojo
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
