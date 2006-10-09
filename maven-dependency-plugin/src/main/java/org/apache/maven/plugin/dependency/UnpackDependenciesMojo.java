/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com) 
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

package org.apache.maven.plugin.dependency;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;
import org.apache.maven.plugin.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Goal that unpacks the project dependencies from the repository to a defined
 * location.
 * 
 * @goal unpack-dependencies
 * @requiresDependencyResolution compile
 * @phase process-sources
 * @author brianf
 */
public class UnpackDependenciesMojo
    extends AbstractFromDependenciesMojo
{

    /**
     * Main entry into mojo. This method gets the dependencies and iterates
     * through each one passing it to DependencyUtil.unpackFile().
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see #getDependencies
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager,
     *      Log)
     */
    public void execute()
        throws MojoExecutionException
    {
        Set artifacts = getDependencies(true);

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            File destDir = DependencyUtil.getFormattedOutputDirectory( this.useSubDirectoryPerType,
                                                                       this.useSubDirectoryPerArtifact,
                                                                       this.outputDirectory, artifact );
//          force overwrite for now. The filters should have removed anything from the list that shouldn't 
            //be overwritten.
            DependencyUtil.unpackFile( artifact, destDir, this.markersDirectory, this.archiverManager, this.getLog(),
                                       true );
        }
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new MarkerFileFilter(this.overWriteReleases,this.overWriteSnapshots,this.overWriteIfNewer,new DefaultFileMarkerHandler(this.markersDirectory));
    }
}
