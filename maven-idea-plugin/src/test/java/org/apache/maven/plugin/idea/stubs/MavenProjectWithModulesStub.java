package org.apache.maven.plugin.idea.stubs;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class MavenProjectWithModulesStub
    extends SimpleMavenProjectStub
{
    public List getCollectedProjects()
    {
        List projects = new ArrayList();

        projects.add( createModule( "module-1" ) );
        projects.add( createModule( "module-2" ) );
        projects.add( createModule( "module-3" ) );

        return projects;
    }

    private MavenProject createModule( String artifactId )
    {
        return new MavenProjectModuleStub( artifactId, new File( getBasedir(), artifactId ) );
    }
}
