package org.apache.maven.plugin.idea;

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
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File iprFile = getIdeaFile( ".ipr" );
        deleteFile( iprFile );

        File imlFile = getIdeaFile( ".iml" );
        deleteFile( imlFile );

        File iwsFile = getIdeaFile( ".iws" );
        deleteFile( iwsFile );
    }

    private File getIdeaFile( String extension )
    {
        return new File( project.getBasedir(), project.getArtifactId() + extension );
    }

    private void deleteFile( File file )
    {
        if ( file.exists() )
        {
            if ( !file.isDirectory() )
            {
                FileUtils.fileDelete( file.getAbsolutePath() );
            }
        }
    }
}
