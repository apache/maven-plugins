package org.apache.maven.plugins;

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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo( name="ear" )
public class EarMojo extends AbstractMojo
{
    @Component
    private MavenProject project;
    
    /**
     * Directory containing the generated EAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required=true )
    private File outputDirectory;

    /**
     * Name of the generated EAR.
     */
    @Parameter( alias = "earName", property = "ear.finalName", defaultValue = "${project.build.finalName}", required = true )
    private String finalName;

    public void execute() throws MojoExecutionException
    {
        File targetFile = new File( outputDirectory, finalName + ".ear" );
        
        try
        {
            targetFile.getParentFile().mkdirs();
            targetFile.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        
        project.getArtifact().setFile( targetFile );
    }
}
