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

package org.apache.maven.plugin.dependency.fromConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Goal that retrieves a list of artifacts from the repository and unpacks them in a defined location.
 *
 * @goal unpack
 *
 * @phase process-sources
 * @author brianf
 */
public final class UnpackMojo
    extends AbstractFromConfigurationMojo
{

    /**
     * Directory to store flag files after unpack
     * @parameter expression="${project.build.directory}/dependency-maven-plugin-markers" 
     */
    private File markersDirectory;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates through each one passing
     * it to unpackArtifact.
     * 
     * @throws MojoExecutionException 
     *              with a message if an error occurs.
     *              
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #unpackArtifact(ArtifactItem)
     */
    public void execute()
        throws MojoExecutionException
    {
        ArrayList artifactItems = getArtifactItems();
        Iterator iter = artifactItems.iterator();
        while ( iter.hasNext() )
        {
            ArtifactItem artifactItem = (ArtifactItem) iter.next();
            unpackArtifact( artifactItem );
        }
    }

    /**
     * This method gets the Artifact object and calls DependencyUtil.unpackFile.
     * @param artifactItem 
     *          containing the information about the Artifact to unpack.
     *          
     * @throws MojoExecutionException 
     *          with a message if an error occurs.
     *          
     * @see #getArtifact
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager, Log)
     */
    private void unpackArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        Artifact artifact = artifactItem.getArtifact();

        File location = artifactItem.getOutputDirectory();

        DependencyUtil.unpackFile( artifact, location, this.markersDirectory, this.archiverManager,
                                   this.getLog(), artifactItem.isDoOverWrite() );
    }
}
