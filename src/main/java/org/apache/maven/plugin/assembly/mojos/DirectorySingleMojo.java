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
 * Assemble an application bundle or distribution from an assembly descriptor without
 * launching a parallel lifecycle build. Also, this mojo is not an aggregator, so it can be used
 * multiple times in a single multimodule build.
 *
 * @author <a href="mailto:gscokart@users.sourceforge.net">Gilles Scokart</a>
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
