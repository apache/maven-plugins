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
 * Like the <code>assembly:attached</code> goal, assemble an application bundle 
 * or distribution from an assembly descriptor. This goal is suitable either for 
 * binding to the lifecycle or calling directly from the command line (provided 
 * all required files are available before the build starts, or are produced 
 * by another goal specified before this one on the command line). 
 * <br/>
 * 
 * This goal differs from <code>assembly:single</code> in that it ignores the &lt;formats/&gt;
 * section of the assembly descriptor, and forces the assembly to be created as
 * a directory in the project's build-output directory (usually <code>./target</code>).
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:gscokart@users.sourceforge.net">Gilles Scokart</a>
 * @version $Id$
 *
 * @goal directory-single
 */
public class DirectorySingleMojo
    extends AbstractDirectoryMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public MavenProject getProject()
    {
        return project;
    }

}
