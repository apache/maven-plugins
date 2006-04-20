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
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;

/**
 * @author brianf
 *
 */
public class MarkerFileFilter
    implements ArtifactsFilter
{

    boolean overWriteReleases;

    boolean overWriteSnapshots;

    File markerFileDirectory;

    public MarkerFileFilter( boolean overWriteReleases, boolean overWriteSnapshots, File markerFileDirectory )
    {
        this.overWriteReleases = overWriteReleases;
        this.overWriteSnapshots = overWriteSnapshots;
        this.markerFileDirectory = markerFileDirectory;
    }

    /* (non-Javadoc)
     * @see org.apache.mojo.dependency.utils.filters.ArtifactsFilter#filter(java.util.Set, org.apache.maven.plugin.logging.Log)
     */
    public Set filter( Set artifacts, Log log )
        throws MojoExecutionException
    {
        Set result = new HashSet();

        boolean overWrite = false;

        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( ( artifact.isSnapshot() && this.overWriteSnapshots )
                || ( !artifact.isSnapshot() && this.overWriteReleases ) )
            {
                overWrite = true;
            }

            MarkerHandler marker = new DefaultFileMarkerHandler( artifact, markerFileDirectory );
            if ( overWrite || !marker.isMarkerSet() )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    /**
     * @return Returns the overWriteReleases.
     */
    public boolean isOverWriteReleases()
    {
        return this.overWriteReleases;
    }

    /**
     * @param overWriteReleases The overWriteReleases to set.
     */
    public void setOverWriteReleases( boolean overWriteReleases )
    {
        this.overWriteReleases = overWriteReleases;
    }

    /**
     * @return Returns the overWriteSnapshots.
     */
    public boolean isOverWriteSnapshots()
    {
        return this.overWriteSnapshots;
    }

    /**
     * @param overWriteSnapshots The overWriteSnapshots to set.
     */
    public void setOverWriteSnapshots( boolean overWriteSnapshots )
    {
        this.overWriteSnapshots = overWriteSnapshots;
    }
}
