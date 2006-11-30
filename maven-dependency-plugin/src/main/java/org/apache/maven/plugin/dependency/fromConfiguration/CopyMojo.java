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

package org.apache.maven.plugin.dependency.fromConfiguration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.logging.Log;

/**
 * Goal that copies a list of artifacts from the repository to defined
 * locations.
 * 
 * @goal copy
 * @since 1.0
 * @phase process-sources
 * @author brianf
 */
public class CopyMojo
    extends AbstractFromConfigurationMojo
{

    /**
     * Strip artifact version during copy
     * 
     * @parameter expression="${stripVersion}" default-value="false"
     * @parameter
     */
    private boolean stripVersion = false;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates
     * through each one passing it to copyArtifact.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #copyArtifact(ArtifactItem)
     */
    public void execute()
        throws MojoExecutionException
    {
        ArrayList theArtifactItems = getArtifactItems( this.stripVersion );
        Iterator iter = theArtifactItems.iterator();
        while ( iter.hasNext() )
        {
            ArtifactItem artifactItem = (ArtifactItem) iter.next();
            copyArtifact( artifactItem);
        }
    }

    /**
     * Resolves the artifact from the repository and copies it to the specified
     * location.
     * 
     * @param artifactItem
     *            containing the information about the Artifact to copy.
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see DependencyUtil#copyFile(File, File, Log)
     * @see DependencyUtil#getFormattedFileName(Artifact, boolean)
     */
    protected void copyArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        Artifact artifact = artifactItem.getArtifact();

        File destFile = new File( artifactItem.getOutputDirectory(), artifactItem.getDestFileName() );

        // TODO: refactor this to use the filters.
        if ( !artifactItem.isDoOverWrite() )
        {
            if ( artifactItem.getArtifact().isSnapshot() )
            {
                artifactItem.setDoOverWrite( this.overWriteSnapshots );
            }
            else
            {
                artifactItem.setDoOverWrite( this.overWriteReleases );
            }
        }

        File artifactFile = artifact.getFile();
        if ( artifactItem.isDoOverWrite()
            || ( !destFile.exists() || ( overWriteIfNewer && artifactFile.lastModified() < destFile.lastModified() ) ) )
        {
            copyFile( artifactFile, destFile );
        }
        else
        {
            this.getLog().info( artifactFile + " already exists." );
        }
    }

    /**
     * @return Returns the stripVersion.
     */
    public boolean isStripVersion()
    {
        return this.stripVersion;
    }

    /**
     * @param stripVersion The stripVersion to set.
     */
    public void setStripVersion( boolean stripVersion )
    {
        this.stripVersion = stripVersion;
    }

}
