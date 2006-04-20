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
package org.apache.maven.plugin.dependency.utils.markers;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author brianf
 *
 */
public class DefaultFileMarkerHandler
    implements MarkerHandler
{
    protected Artifact artifact;

    protected File markerFilesDirectory;

    public DefaultFileMarkerHandler( Artifact artifact, File markerFilesDirectory)
    {
        this.artifact = artifact;
        this.markerFilesDirectory = markerFilesDirectory;
    }

    /**
     * Returns properly formatted File
     *        
     * @return
     *        File object for marker. The file is not guaranteed to exist.
     */
    protected File getMarkerFile()
    {
        return new File( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + ".marker" );
    }

    /**
     * Tests whether the file or directory denoted by this abstract pathname
     * exists.
     *
     * @return  <code>true</code> if and only if the file or directory denoted
     *          by this abstract pathname exists; <code>false</code> otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkRead(java.lang.String)}</code>
     *          method denies read access to the file or directory
     */
    public boolean isMarkerSet()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        return marker.exists();
    }

    public void setMarker()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        //create marker file
        marker.getParentFile().mkdirs();
        try
        {
            marker.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create Marker: " + marker.getAbsolutePath(), e );
        }

    }

    /**
     * Deletes the file or directory denoted by this abstract pathname.  If
     * this pathname denotes a directory, then the directory must be empty in
     * order to be deleted.
     *
     * @return  <code>true</code> if and only if the file or directory is
     *          successfully deleted; <code>false</code> otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkDelete}</code> method denies
     *          delete access to the file
     */
    public boolean clearMarker()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        return marker.delete();
    }
}
