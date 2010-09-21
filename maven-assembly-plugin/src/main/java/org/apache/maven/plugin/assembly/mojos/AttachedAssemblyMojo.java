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
 * Assemble an application bundle or distribution from an assembly descriptor, WITHOUT first forcing Maven to build all
 * POMs to the <code>package</code> phase (as is required by the <code>assembly:assembly</code> goal). <br/>
 * 
 * <b>NOTE:</b> This goal should ONLY be run from the command line, and if building a multimodule project it should be
 * used from the root POM. Use the <code>assembly:single</code> goal for binding your assembly to the lifecycle.
 * 
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * 
 * @goal attached
 * @aggregator
 * @inheritByDefault false
 * @deprecated Use assembly:single instead! The assembly:attached mojo leads to non-standard builds.
 */
@Deprecated
public class AttachedAssemblyMojo
    extends AbstractAssemblyMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    @Override
    public MavenProject getProject()
    {
        return project;
    }
}
