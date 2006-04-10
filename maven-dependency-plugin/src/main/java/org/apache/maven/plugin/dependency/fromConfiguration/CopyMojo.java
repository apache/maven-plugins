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
import org.apache.maven.plugin.dependency.utils.DependencyUtil;

/**
 * Goal that copies a list of artifacts from the repository to defined locations.
 *
 * @goal copy
 *
 * @phase process-sources
 * @author brianf
 */
public class CopyMojo
    extends AbstractFromConfigurationMojo
{

    /**
     * Strip artifact version during copy
     * @parameter expression="${stripVersion}" default-value="false" 
     * @parameter
     */
    private boolean stripVersion = false;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates through each one passing
     * it to copyArtifact.
     
     * @throws MojoExecutionException 
     *          with a message if an error occurs.
     * 
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #copyArtifact(ArtifactItem)
     */
    public void execute()
        throws MojoExecutionException
    {
        ArrayList artifactItems = getArtifactItems();
        Iterator iter = artifactItems.iterator();
        while ( iter.hasNext() )
        {
            ArtifactItem artifactItem = (ArtifactItem) iter.next();
            copyArtifact( artifactItem, this.stripVersion );
        }
    }

    /**
     * Resolves the artifact from the repository and copies it to the specified location.
     * @param artifactItem 
     *          containing the information about the Artifact to copy.
     * @param removeVersion 
     *          specifies if the version should be removed from the file name when copying.      
     * @throws MojoExecutionException 
     *          with a message if an error occurs.
     * 
     * @see DependencyUtil#copyFile(File, File, Log)
     * @see DependencyUtil#getFormattedFileName(Artifact, boolean)
     */
    protected void copyArtifact( ArtifactItem artifactItem, boolean removeVersion )
        throws MojoExecutionException
    {
        Artifact artifact = artifactItem.getArtifact();

        String destFileName = null;
        if ( artifactItem.getDestFileName() != null )
        {
            destFileName = artifactItem.getDestFileName();
        }
        else
        {
            destFileName = DependencyUtil.getFormattedFileName( artifact, removeVersion );
        }

        File destFile = new File( artifactItem.getOutputDirectory(), destFileName );

        DependencyUtil.copyFile( artifact.getFile(), destFile, this.getLog(), artifactItem.isDoOverWrite() );
    }

}
