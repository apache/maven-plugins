package org.apache.maven.plugin.antrun;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.ant.AntProjectPopulator;
import org.apache.tools.ant.Project;

import java.util.List;

/**
 * Abstract class for the Antrun plugin
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractAntMojo
    extends AbstractMojo
{
    /**
     * The plugin dependencies.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List artifacts;

    protected void populateAntProjectWithMavenInformation( MavenProject mavenProject, Project antProject )
        throws MojoExecutionException
    {
        AntProjectPopulator app = new AntProjectPopulator();

        app.populateAntProjectWithMavenInformation( mavenProject, antProject, artifacts, getLog() );
    }
}
