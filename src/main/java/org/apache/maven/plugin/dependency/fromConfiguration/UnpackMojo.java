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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactItemFilter;
import org.apache.maven.plugin.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;
import org.apache.maven.plugin.dependency.utils.markers.UnpackFileMarkerHandler;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal that retrieves a list of artifacts from the repository and unpacks them in a defined location.
 * 
 * @since 1.0
 * @goal unpack
 * @phase process-sources
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
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
     * A comma separated list of file patterns to include when unpacking the artifact. i.e. **\/*.xml,**\/*.properties
     * NOTE: Excludes patterns override the includes. (component code = return isIncluded( name ) AND !isExcluded( name
     * );)
     * 
     * @since 2.0-alpha-5
     * @parameter expression="${mdep.unpack.includes}"
     */
    private String includes;

    /**
     * A comma separated list of file patterns to exclude when unpacking the artifact. i.e. **\/*.xml,**\/*.properties
     * NOTE: Excludes patterns override the includes. (component code = return isIncluded( name ) AND !isExcluded( name
     * );)
     * 
     * @since 2.0-alpha-5
     * @parameter expression="${mdep.unpack.excludes}"
     */
    private String excludes;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates through each one passing it to
     * unpackArtifact.
     * 
     * @throws MojoExecutionException with a message if an error occurs.
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #unpackArtifact(ArtifactItem)
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( !isSkip() )
        {
            List<ArtifactItem> processedItems = getProcessedArtifactItems( false );
            for ( ArtifactItem artifactItem : processedItems )
            {
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
    }

    /**
     * This method gets the Artifact object and calls DependencyUtil.unpackFile.
     * 
     * @param artifactItem containing the information about the Artifact to unpack.
     * @throws MojoExecutionException with a message if an error occurs.
     * @see #getArtifact
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager, Log)
     */
    private void unpackArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        MarkerHandler handler = new UnpackFileMarkerHandler( artifactItem, this.markersDirectory );

        unpack( artifactItem.getArtifact().getFile(), artifactItem.getOutputDirectory(), artifactItem.getIncludes(),
                artifactItem.getExcludes() );
        handler.setMarker();

    }

    ArtifactItemFilter getMarkedArtifactFilter( ArtifactItem item )
    {
        MarkerHandler handler = new UnpackFileMarkerHandler( item, this.markersDirectory );

        return new MarkerFileFilter( this.isOverWriteReleases(), this.isOverWriteSnapshots(),
                                     this.isOverWriteIfNewer(), handler );
    }

    protected List<ArtifactItem> getProcessedArtifactItems( boolean removeVersion )
        throws MojoExecutionException
    {
        List<ArtifactItem> items = super.getProcessedArtifactItems( removeVersion );
        for ( ArtifactItem artifactItem : items )
        {
            if ( StringUtils.isEmpty( artifactItem.getIncludes() ) )
            {
                artifactItem.setIncludes( getIncludes() );
            }
            if ( StringUtils.isEmpty( artifactItem.getExcludes() ) )
            {
                artifactItem.setExcludes( getExcludes() );
            }
        }
        return items;
    }

    /**
     * @return Returns the markersDirectory.
     */
    public File getMarkersDirectory()
    {
        return this.markersDirectory;
    }

    /**
     * @param theMarkersDirectory The markersDirectory to set.
     */
    public void setMarkersDirectory( File theMarkersDirectory )
    {
        this.markersDirectory = theMarkersDirectory;
    }

    /**
     * @return Returns a comma separated list of excluded items
     */
    public String getExcludes()
    {
        return this.excludes;
    }

    /**
     * @param excludes A comma separated list of items to exclude i.e. **\/*.xml, **\/*.properties
     */
    public void setExcludes( String excludes )
    {
        this.excludes = excludes;
    }

    /**
     * @return Returns a comma separated list of included items
     */
    public String getIncludes()
    {
        return this.includes;
    }

    /**
     * @param includes A comma separated list of items to include i.e. **\/*.xml, **\/*.properties
     */
    public void setIncludes( String includes )
    {
        this.includes = includes;
    }
}
