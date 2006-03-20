package org.apache.maven.plugin.assembly;

import org.apache.maven.project.MavenProject;

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



/**
 * Assemble an application bundle or distribution from an assembly descriptor without
 * launching a parallel lifecycle build.
 *
 * @author <a href="mailto:gscokart@users.sourceforge.net">Gilles Scokart</a>
 *
 * @goal directory-inline
 * @requiresDependencyResolution test
 * @aggregator
 * 
 */
public class DirectoryInlineMojo
    extends AbstractDirectoryMojo
{
    protected MavenProject getExecutedProject()
    {
        return project;
    }

}
