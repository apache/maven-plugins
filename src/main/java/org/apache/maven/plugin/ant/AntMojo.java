package org.apache.maven.plugin.ant;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.IOException;

/**
 * Generate Ant build files.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal ant
 * @requiresDependencyResolution test
 * @todo change this to use the artifact ant tasks instead of :get
 */
public class AntMojo
    extends AbstractMojo
{
    /**
     * The project to create a build for.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Overwrite or not the <code>build.xml</code>
     *
     * @parameter expression="${overwrite}" default-value="false"
     */
    private boolean overwrite;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        AntBuildWriter antBuildWriter = new AntBuildWriter( project, new File( localRepository.getBasedir() ),
                                                            settings, overwrite );

        try
        {
            antBuildWriter.writeBuildXmls();
            antBuildWriter.writeBuildProperties();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error building Ant script: " + e.getMessage(), e );
        }

        getLog().info(
                       "Wrote Ant project for " + project.getArtifactId() + " to "
                           + project.getBasedir().getAbsolutePath() );
    }
}
