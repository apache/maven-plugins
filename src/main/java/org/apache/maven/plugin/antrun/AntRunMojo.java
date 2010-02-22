package org.apache.maven.plugin.antrun;

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

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Target;

/**
 * Maven AntRun Mojo.
 *
 * This plugin provides the capability of calling Ant tasks
 * from a POM by running the nested ant tasks inside the &lt;tasks/&gt;
 * parameter. It is encouraged to move the actual tasks to
 * a separate build.xml file and call that file with an
 * &lt;ant/&gt; task.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @configurator override
 * @goal run
 * @requiresDependencyResolution test
 */
public class AntRunMojo
    extends AbstractAntMojo
{
    /**
     * The plugin dependencies.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginArtifacts;

    /**
     * The XML for the Ant task. You can add anything you can add
     * between &lt;target&gt; and &lt;/target&gt; in a build.xml.
     *
     * @parameter expression="${tasks}"
     */
    private Target tasks;

    /**
     * This folder is added to the list of those folders
     * containing source to be compiled. Use this if your
     * ant script generates source code.
     *
     * @parameter expression="${sourceRoot}"
     * @deprecated Use the build-helper-maven-plugin to bind source directories
     */
    private File sourceRoot;

    /**
     * This folder is added to the list of those folders
     * containing source to be compiled for testing. Use this if your
     * ant script generates test source code.
     *
     * @parameter expression="${testSourceRoot}"
     * @deprecated Use the build-helper-maven-plugin to bind test source directories
     */
    private File testSourceRoot;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        executeTasks( tasks, getMavenProject(), pluginArtifacts );

        if ( sourceRoot != null )
        {
            getLog().info( "Registering compile source root " + sourceRoot );
            getMavenProject().addCompileSourceRoot( sourceRoot.toString() );
        }

        if ( testSourceRoot != null )
        {
            getLog().info( "Registering compile test source root " + testSourceRoot );
            getMavenProject().addTestCompileSourceRoot( testSourceRoot.toString() );
        }
    }
    
}
