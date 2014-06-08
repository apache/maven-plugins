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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.archive.ArchiveExpansionException;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * Unpack project dependencies. Currently supports dependencies of type jar and zip.
 *
 * @version $Id$
 * @deprecated Use org.apache.maven.plugins:maven-dependency-plugin goal: unpack or unpack-dependencies instead.
 */
@Mojo( name = "unpack", requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false )
@Deprecated
public class UnpackMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    private MavenProject project;

    /**
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * Directory to unpack JARs into if needed
     */
    @Parameter( defaultValue = "${project.build.directory}/assembly/work", required = true )
    protected File workDirectory;

    /**
     * Unpacks the archive file.
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final Set<Artifact> dependencies = new LinkedHashSet<Artifact>();

        if ( project.getArtifact() != null && project.getArtifact().getFile() != null )
        {
            dependencies.add( project.getArtifact() );
        }

        @SuppressWarnings( "unchecked" )
        final Set<Artifact> projectArtifacts = project.getArtifacts();
        if ( projectArtifacts != null )
        {
            dependencies.addAll( projectArtifacts );
        }

        for (final Artifact artifact : dependencies) {
            final String name = artifact.getFile().getName();

            final File tempLocation = new File(workDirectory, name.substring(0, name.lastIndexOf('.')));
            boolean process = false;
            if (!tempLocation.exists()) {
                tempLocation.mkdirs();
                process = true;
            } else if (artifact.getFile().lastModified() > tempLocation.lastModified()) {
                process = true;
            }

            if (process) {
                final File file = artifact.getFile();
                try {
                    AssemblyFileUtils.unpack(file, tempLocation, archiverManager);
                } catch (final NoSuchArchiverException e) {
                    getLog().info("Skip unpacking dependency file with unknown extension: " + file.getPath());
                } catch (final ArchiveExpansionException e) {
                    throw new MojoExecutionException("Error unpacking dependency file: " + file, e);
                }
            }
        }
    }

}