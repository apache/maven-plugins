package org.apache.maven.plugin.idea;

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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Plugin to remove existing idea files on the project
 *
 * @goal clean
 * @author Edwin Punzalan
 */
public class IdeaCleanMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void initParams( MavenProject project )
    {
        this.project = project;
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File files[] = project.getBasedir().listFiles();

        for ( int idx = 0; idx < files.length; idx++ )
        {
            File file = files[ idx ];

            if ( file.getName().endsWith( ".ipr" ) ||
                 file.getName().endsWith( ".iml" ) ||
                 file.getName().endsWith( ".iws" ) )
            {
                getLog().debug( "Deleting " + file.getAbsolutePath() + "...");
                FileUtils.fileDelete( file.getAbsolutePath() );
            }
        }
    }
}
