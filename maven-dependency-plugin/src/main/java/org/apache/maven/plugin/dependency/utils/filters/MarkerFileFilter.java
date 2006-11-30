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

/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.dependency.utils.markers.MarkerHandler;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class MarkerFileFilter
    extends AbstractArtifactsFilter
    implements ArtifactItemFilter
{

    boolean overWriteReleases;

    boolean overWriteSnapshots;

    boolean overWriteIfNewer;

    MarkerHandler handler;

    public MarkerFileFilter( boolean overWriteReleases, boolean overWriteSnapshots, boolean overWriteIfNewer,
                            MarkerHandler handler )
    {
        this.overWriteReleases = overWriteReleases;
        this.overWriteSnapshots = overWriteSnapshots;
        this.overWriteIfNewer = overWriteIfNewer;
        this.handler = handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mojo.dependency.utils.filters.ArtifactsFilter#filter(java.util.Set,
     *      org.apache.maven.plugin.logging.Log)
     */
    public Set filter( Set artifacts, Log log )
        throws MojoExecutionException
    {
        Set result = new HashSet();

        Iterator iter = artifacts.iterator();
        // log.debug("Artifacts:"+ artifacts.size());
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( okToProcess( new ArtifactItem( artifact ) ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    public boolean okToProcess( ArtifactItem item )
        throws MojoExecutionException
    {
        Artifact artifact = item.getArtifact();
        boolean overWrite = false;
        boolean result = false;
        if ( ( artifact.isSnapshot() && this.overWriteSnapshots )
            || ( !artifact.isSnapshot() && this.overWriteReleases ) )
        {
            overWrite = true;
        }

        handler.setArtifact( artifact );

        if ( overWrite || ( !handler.isMarkerSet() || ( overWriteIfNewer && handler.isMarkerOlder( artifact ) ) ) )
        {
            result = true;
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
     * @param overWriteReleases
     *            The overWriteReleases to set.
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
     * @param overWriteSnapshots
     *            The overWriteSnapshots to set.
     */
    public void setOverWriteSnapshots( boolean overWriteSnapshots )
    {
        this.overWriteSnapshots = overWriteSnapshots;
    }

    /**
     * @return Returns the overWriteIfNewer.
     */
    public boolean isOverWriteIfNewer()
    {
        return this.overWriteIfNewer;
    }

    /**
     * @param overWriteIfNewer
     *            The overWriteIfNewer to set.
     */
    public void setOverWriteIfNewer( boolean overWriteIfNewer )
    {
        this.overWriteIfNewer = overWriteIfNewer;
    }
}
