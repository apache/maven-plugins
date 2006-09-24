package org.apache.maven.plugin.ant;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Clean all Ant build files.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal clean
 */
public class AntCleanMojo
    extends AbstractMojo
{
    /**
     * The working project.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        File buildXml = new File( project.getBasedir(), AntBuildWriter.DEFAULT_BUILD_FILENAME );
        if ( buildXml.exists() && !buildXml.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + buildXml.getAbsolutePath() );
        }

        File mavenBuildXml = new File( project.getBasedir(), AntBuildWriter.DEFAULT_MAVEN_BUILD_FILENAME );
        if ( mavenBuildXml.exists() && !mavenBuildXml.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + mavenBuildXml.getAbsolutePath() );
        }

        File mavenBuildProperties = new File( project.getBasedir(), AntBuildWriter.DEFAULT_MAVEN_PROPERTIES_FILENAME );
        if ( mavenBuildProperties.exists() && !mavenBuildProperties.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + mavenBuildProperties.getAbsolutePath() );
        }

        getLog().info(
                       "Deleted Ant project for " + project.getArtifactId() + " in "
                           + project.getBasedir().getAbsolutePath() );
    }
}
