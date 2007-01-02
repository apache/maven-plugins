package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;
import org.apache.maven.plugin.dependency.utils.filters.DestFileFilter;

/**
 * Goal that copies the project dependencies from the repository to a defined
 * location.
 * 
 * @goal copy-dependencies
 * @requiresDependencyResolution test
 * @phase process-sources
 * @author brianf
 * @since 1.0
 */
public class CopyDependenciesMojo
    extends AbstractFromDependenciesMojo
{

    /**
     * Strip artifact version during copy
     * 
     * @optional
     * @parameter expression="${stripVersion}" default-value="false"
     * @parameter
     */
    protected boolean stripVersion = false;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * calling copyArtifact.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see #getDependencies
     * @see #copyArtifact(Artifact, boolean)
     */
    public void execute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( true );
        Set artifacts = dss.getResolvedDependencies();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            copyArtifact( (Artifact) i.next(), this.stripVersion );
        }

        artifacts = dss.getSkippedDependencies();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            getLog().info( artifact.getFile().getName() + " already exists in destination." );
        }
    }

    /**
     * Copies the Artifact after building the destination file name if
     * overridden. This method also checks if the classifier is set and adds it
     * to the destination file name if needed.
     * 
     * @param artifact
     *            representing the object to be copied.
     * @param removeVersion
     *            specifies if the version should be removed from the file name
     *            when copying.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see DependencyUtil#copyFile(File, File, Log)
     * @see DependencyUtil#getFormattedFileName(Artifact, boolean)
     */
    protected void copyArtifact( Artifact artifact, boolean removeVersion )
        throws MojoExecutionException
    {

        String destFileName = DependencyUtil.getFormattedFileName( artifact, removeVersion );

        File destDir = DependencyUtil.getFormattedOutputDirectory( this.useSubDirectoryPerType,
                                                                   this.useSubDirectoryPerArtifact,
                                                                   this.outputDirectory, artifact );
        File destFile = new File( destDir, destFileName );

        copyFile( artifact.getFile(), destFile );
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new DestFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                   this.useSubDirectoryPerArtifact, this.useSubDirectoryPerType, this.stripVersion,
                                   this.outputDirectory );
    }
}
