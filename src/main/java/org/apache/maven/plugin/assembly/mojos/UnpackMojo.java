package org.apache.maven.plugin.assembly.mojos;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.archive.ArchiveExpansionException;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * Unpack project dependencies.  Currently supports dependencies of type jar and zip.
 *
 * @version $Id$
 * @goal unpack
 * @requiresDependencyResolution test
 */
public class UnpackMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * Directory to unpack JARs into if needed
     *
     * @parameter expression="${project.build.directory}/assembly/work"
     * @required
     */
    protected File workDirectory;

    /**
     * Unpacks the archive file.
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Set dependencies = new HashSet();

        if ( project.getArtifact() != null && project.getArtifact().getFile() != null )
        {
            dependencies.add( project.getArtifact() );
        }

        Set projectArtifacts = project.getArtifacts();
        if ( projectArtifacts != null )
        {
            dependencies.addAll( projectArtifacts );
        }

        for ( Iterator j = dependencies.iterator(); j.hasNext(); )
        {
            Artifact artifact = (Artifact) j.next();

            String name = artifact.getFile().getName();

            File tempLocation = new File( workDirectory, name.substring( 0, name.lastIndexOf( '.' ) ) );
            boolean process = false;
            if ( !tempLocation.exists() )
            {
                tempLocation.mkdirs();
                process = true;
            }
            else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
            {
                process = true;
            }

            if ( process )
            {
                File file = artifact.getFile();
                try
                {
                    AssemblyFileUtils.unpack( file, tempLocation, archiverManager );
                }
                catch ( NoSuchArchiverException e )
                {
                    this.getLog().info( "Skip unpacking dependency file with unknown extension: " + file.getPath() );
                }
                catch ( ArchiveExpansionException e )
                {
                    throw new MojoExecutionException( "Error unpacking dependency file: " + file, e );
                }
            }
        }
    }


}