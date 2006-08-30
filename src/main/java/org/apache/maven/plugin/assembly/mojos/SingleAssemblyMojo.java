package org.apache.maven.plugin.assembly.mojos;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.project.MavenProject;

/**
 * Assemble an application bundle or distribution from an assembly descriptor.
 * Do not specify a phase, so make it usable in a reactor environment where forking would create issues.
 * Do not specify it as an aggregator, so it is only for a single project.
 * Both cases aid it in working around issues with the Maven lifecycle that should be addressed in Maven 2.1.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal single
 */
public class SingleAssemblyMojo
    extends AbstractAssemblyMojo
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
