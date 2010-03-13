package org.apache.maven.plugin.dependency.utils.markers;

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

import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:dbradicich@comcast.net">Damian Bradicich</a>
 * @version $Id$
 */
public class UnpackFileMarkerHandler extends DefaultFileMarkerHandler
{
    protected ArtifactItem artifactItem;

    public UnpackFileMarkerHandler( File markerFilesDirectory )
    {
        super( markerFilesDirectory );
    }

    public UnpackFileMarkerHandler( ArtifactItem artifactItem, File markerFilesDirectory )
    {
        this( markerFilesDirectory );
        setArtifactItem( artifactItem );
    }

    protected File getMarkerFile()
    {
        /**
         * Build a hash of all include/exclude strings, to determine
         * if an artifactItem has been unpacked using the include/exclude
         * parameters, this will allow an artifact to be included multiple
         * times with different include/exclude parameters
         */
        File markerFile = null;
        if ( this.artifactItem == null
            || ( StringUtils.isEmpty( this.artifactItem.getIncludes() )
            &&	StringUtils.isEmpty( this.artifactItem.getExcludes() ) ) )
        {
            markerFile = super.getMarkerFile();
        }
        else
        {
            int includeExcludeHash = 0;

            if ( StringUtils.isNotEmpty( this.artifactItem.getIncludes() ) )
            {
                includeExcludeHash += this.artifactItem.getIncludes().hashCode();
            }

            if ( StringUtils.isNotEmpty( this.artifactItem.getExcludes() ) )
            {
                includeExcludeHash += this.artifactItem.getExcludes().hashCode();
            }

            markerFile = new File( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + includeExcludeHash );
        }

        return markerFile;
    }

    public void setArtifactItem( ArtifactItem artifactItem )
    {
        this.artifactItem = artifactItem;

        if ( this.artifactItem != null )
        {
            setArtifact( this.artifactItem.getArtifact() );
        }
    }

    public ArtifactItem getArtifactItem( )
    {
        return this.artifactItem;
    }
}
