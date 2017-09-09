package org.apache.maven.plugins.dependency.utils.filters;

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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugins.dependency.utils.markers.MarkerHandler;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class MarkerFileFilter
    extends AbstractArtifactsFilter
    implements ArtifactItemFilter
{

    private boolean overWriteReleases;

    private boolean overWriteSnapshots;

    private boolean overWriteIfNewer;

    protected final MarkerHandler handler;

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
     * @see org.apache.mojo.dependency.utils.filters.ArtifactsFilter#filter(java.util.Set,
     * org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Set<Artifact> filter( Set<Artifact> artifacts )
        throws ArtifactFilterException
    {
        Set<Artifact> result = new LinkedHashSet<Artifact>();

        for ( Artifact artifact : artifacts )
        {
            if ( isArtifactIncluded( new ArtifactItem( artifact ) ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    @Override
    public boolean isArtifactIncluded( ArtifactItem item )
        throws ArtifactFilterException
    {
        Artifact artifact = item.getArtifact();

        boolean overWrite = ( artifact.isSnapshot() && this.overWriteSnapshots )
            || ( !artifact.isSnapshot() && this.overWriteReleases );

        handler.setArtifact( artifact );

        try
        {
            return overWrite || !handler.isMarkerSet() || ( overWriteIfNewer && handler.isMarkerOlder( artifact ) );
        }
        catch ( MojoExecutionException e )
        {
            throw new ArtifactFilterException( e.getMessage(), e );
        }
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

    /**
     * @return Returns the overWriteIfNewer.
     */
    public boolean isOverWriteIfNewer()
    {
        return this.overWriteIfNewer;
    }

    /**
     * @param overWriteIfNewer The overWriteIfNewer to set.
     */
    public void setOverWriteIfNewer( boolean overWriteIfNewer )
    {
        this.overWriteIfNewer = overWriteIfNewer;
    }
}
