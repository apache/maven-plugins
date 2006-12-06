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
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactItemFilter;
import org.apache.maven.plugin.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;

/**
 * Goal that retrieves a list of artifacts from the repository and unpacks them
 * in a defined location.
 * 
 * @since 1.0
 * @goal unpack
 * @phase process-sources
 * @author brianf
 */
public final class UnpackMojo
    extends AbstractFromConfigurationMojo
{

    /**
     * Directory to store flag files after unpack
     * 
     * @parameter expression="${project.build.directory}/dependency-maven-plugin-markers"
     */
    private File markersDirectory;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates
     * through each one passing it to unpackArtifact.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #unpackArtifact(ArtifactItem)
     */
    public void execute()
        throws MojoExecutionException
    {
        ArrayList processedItems = getArtifactItems( false );
        Iterator iter = processedItems.iterator();
        while ( iter.hasNext() )
        {
            ArtifactItem artifactItem = (ArtifactItem) iter.next();
            if ( artifactItem.isNeedsProcessing() )
            {
                unpackArtifact( artifactItem );
            }
            else
            {
                this.getLog().info( artifactItem.getArtifact().getFile().getName() + " already unpacked." );
            }
        }
    }

    /**
     * This method gets the Artifact object and calls DependencyUtil.unpackFile.
     * 
     * @param artifactItem
     *            containing the information about the Artifact to unpack.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see #getArtifact
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager,
     *      Log)
     */
    private void unpackArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        Artifact artifact = artifactItem.getArtifact();

        MarkerHandler handler = new DefaultFileMarkerHandler( artifact, this.markersDirectory );

        unpack( artifact.getFile(), artifactItem.getOutputDirectory() );
        handler.setMarker();

    }

    ArtifactItemFilter getMarkedArtifactFilter( ArtifactItem item )
    {
        MarkerHandler handler = new DefaultFileMarkerHandler( item.getArtifact(), this.markersDirectory );

        return new MarkerFileFilter( this.isOverWriteReleases(), this.isOverWriteSnapshots(),
                                     this.isOverWriteIfNewer(), handler );
    }

    /**
     * @return Returns the markersDirectory.
     */
    public File getMarkersDirectory()
    {
        return this.markersDirectory;
    }

    /**
     * @param theMarkersDirectory
     *            The markersDirectory to set.
     */
    public void setMarkersDirectory( File theMarkersDirectory )
    {
        this.markersDirectory = theMarkersDirectory;
    }
}
