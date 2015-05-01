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

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactItemFilter;
import org.apache.maven.plugin.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;
import org.apache.maven.plugin.dependency.utils.markers.UnpackFileMarkerHandler;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Goal that retrieves a list of artifacts from the repository and unpacks them in a defined location.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo( name = "unpack", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false, threadSafe = true )
public class UnpackMojo
    extends AbstractFromConfigurationMojo
{

    /**
     * Directory to store flag files after unpack
     */
    @Parameter( defaultValue = "${project.build.directory}/dependency-maven-plugin-markers" )
    private File markersDirectory;

    /**
     * A comma separated list of file patterns to include when unpacking the artifact. i.e. **\/*.xml,**\/*.properties
     * NOTE: Excludes patterns override the includes. (component code = return isIncluded( name ) AND !isExcluded( name
     * );)
     *
     * @since 2.0-alpha-5
     */
    @Parameter( property = "mdep.unpack.includes" )
    private String includes;

    /**
     * A comma separated list of file patterns to exclude when unpacking the artifact. i.e. **\/*.xml,**\/*.properties
     * NOTE: Excludes patterns override the includes. (component code = return isIncluded( name ) AND !isExcluded( name
     * );)
     *
     * @since 2.0-alpha-5
     */
    @Parameter( property = "mdep.unpack.excludes" )
    private String excludes;

    /**
     * The artifact to unpack from commandLine.
     * Use {@link #artifactItems} within the pom-configuration.
     */
    @SuppressWarnings( "unused" ) //marker-field, setArtifact(String) does the magic
    @Parameter( property = "artifact" )
    private String artifact;

    /**
     * Main entry into mojo. This method gets the ArtifactItems and iterates through each one passing it to
     * unpackArtifact.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     * @see ArtifactItem
     * @see #getArtifactItems
     * @see #unpackArtifact(ArtifactItem)
     */
    protected void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( isSkip() )
        {
            return;
        }
        
        verifyRequirements();

        List<ArtifactItem> processedItems = getProcessedArtifactItems( false );
        Set<String> purgedOutputDirectories = purgeOutputDirectories(processedItems);

        for ( ArtifactItem artifactItem : processedItems )
        {
            if ( artifactItem.isNeedsProcessing()
                    || purgedOutputDirectories.contains(artifactItem.getOutputDirectory().getAbsolutePath()) )
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
     * Processes a list of artifacts and gets a map indicating for each output folder if it should be purged or not.
     *
     * @param artifactItems all the artifact items to be processed
     * @return a {@link Map} where the key is the output folder and the value is true of false depending on whether
     * the output folder should be purged or not
     * @throws MojoExecutionException if conflicting settings for purging the output folders are found in the
     * artifacts
     */
    private Map<String, Boolean> getPurgeFlagsByOutputDirectory(List<ArtifactItem> artifactItems)
            throws MojoExecutionException {
        HashMap<String, Boolean> purgeFlagsByOutputDirectory = new HashMap<String, Boolean>();

        for (ArtifactItem artifactItem : artifactItems) {
            String outputDirectoryPath = artifactItem.getOutputDirectory().getAbsolutePath();

            Boolean purgeFlag = purgeFlagsByOutputDirectory.get(outputDirectoryPath);

            if (purgeFlag == null) {
                purgeFlagsByOutputDirectory.put(outputDirectoryPath, artifactItem.shouldPurgeOutputDirectory());
            } else if (purgeFlag && !artifactItem.shouldPurgeOutputDirectory()) {
                throw new MojoExecutionException(
                        "Conflicting settings for purgeOutputDirectory set for the artifacts. "
                                + "purgeOutputDirectory for the same outputFolder should have the same "
                                + "value in all artifacts.");
            }
        }

        return purgeFlagsByOutputDirectory;
    }

    /**
     * Purges the output directories of the input artifact items which require purging.
     *
     * @param artifactItems the artifact items whose output directories should be purged
     * @return a {@link Set} with the paths of all purged output directories
     * @throws MojoExecutionException in case of an error while purging the required output directories
     */
    private Set<String> purgeOutputDirectories(
            List<ArtifactItem> artifactItems)
            throws MojoExecutionException {
        Map<String, Boolean> purgeFlagsByOutputDirectory = getPurgeFlagsByOutputDirectory(artifactItems);
        HashSet<String> purgedOutputDirectories = new HashSet<String>();

        for (ArtifactItem artifactItem : artifactItems) {
            if (!artifactItem.isNeedsProcessing()) {
                continue;
            }

            String outputDirectoryPath = artifactItem.getOutputDirectory().getAbsolutePath();

            if (!purgedOutputDirectories.contains(outputDirectoryPath)
                    && purgeFlagsByOutputDirectory.get(outputDirectoryPath)) {
                purge(artifactItem.getOutputDirectory());
                purgedOutputDirectories.add(outputDirectoryPath);
            }
        }

        return purgedOutputDirectories;
    }

    /**
     * This method gets the Artifact object and calls DependencyUtil.unpackFile.
     *
     * @param artifactItem containing the information about the Artifact to unpack.
     * @throws MojoExecutionException with a message if an error occurs.
     * @see #getArtifact
     */
    private void unpackArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        MarkerHandler handler = new UnpackFileMarkerHandler( artifactItem, this.markersDirectory );

        unpack( artifactItem.getArtifact(), artifactItem.getOutputDirectory(), artifactItem.getIncludes(),
                artifactItem.getExcludes() );
        handler.setMarker();
    }

    ArtifactItemFilter getMarkedArtifactFilter( ArtifactItem item )
    {
        MarkerHandler handler = new UnpackFileMarkerHandler( item, this.markersDirectory );

        return new MarkerFileFilter( this.isOverWriteReleases(), this.isOverWriteSnapshots(), this.isOverWriteIfNewer(),
                                     handler );
    }

    protected List<ArtifactItem> getProcessedArtifactItems( boolean removeVersion )
        throws MojoExecutionException
    {
        List<ArtifactItem> items =
            super.getProcessedArtifactItems( new ProcessArtifactItemsRequest( removeVersion, false, false, false ) );
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
