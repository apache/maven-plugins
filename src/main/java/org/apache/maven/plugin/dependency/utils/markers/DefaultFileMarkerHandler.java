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
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DefaultFileMarkerHandler
    implements MarkerHandler
{
    protected Artifact artifact;

    protected File markerFilesDirectory;

    public DefaultFileMarkerHandler( File theMarkerFilesDirectory )
    {
        this.markerFilesDirectory = theMarkerFilesDirectory;
    }

    public DefaultFileMarkerHandler( Artifact theArtifact, File theMarkerFilesDirectory )
    {
        this.artifact = theArtifact;
        this.markerFilesDirectory = theMarkerFilesDirectory;
    }

    /**
     * Returns properly formatted File
     * 
     * @return File object for marker. The file is not guaranteed to exist.
     */
    protected File getMarkerFile()
    {
        return new File( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + ".marker" );
    }

    /**
     * Tests whether the file or directory denoted by this abstract pathname
     * exists.
     * 
     * @return <code>true</code> if and only if the file or directory denoted
     *         by this abstract pathname exists; <code>false</code> otherwise
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkRead(java.lang.String)}</code>
     *             method denies read access to the file or directory
     */
    public boolean isMarkerSet()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        return marker.exists();
    }

    public boolean isMarkerOlder( Artifact artifact1 )
        throws MojoExecutionException
    {
        // temporary debug to find out why this intermittent failure on Sonatype grid Windows
        System.out.println( "> isMarkerOlder:" );
        File marker = getMarkerFile();
        System.out.println( "  artifact1 = " + artifact1.getFile().getPath() );
        System.out.println( "  marker    = " + marker.getPath() );
        if ( marker.exists() )
        {
            System.out.println( "    artifact1 lastModified: " + artifact1.getFile().lastModified() );
            System.out.println( "    marker lastModified: " + marker.lastModified() );
            System.out.println( "< " + ( artifact1.getFile().lastModified() > marker.lastModified() ) + " = marker older than artifact?"  );
            new Exception().printStackTrace( System.out );
            return artifact1.getFile().lastModified() > marker.lastModified();
        }
        else
        {
            System.out.println( "< true : marker does not exist" );
            // if the marker doesn't exist, we want to copy so assume it is
            // infinitely older
            return true;
        }
    }

    public void setMarker()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        // create marker file
        try
        {
            marker.getParentFile().mkdirs();
        }
        catch ( NullPointerException e )
        {
            // parent is null, ignore it.
        }
        try
        {
            marker.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create Marker: " + marker.getAbsolutePath(), e );
        }
        
        // update marker file timestamp
        try
        {
            long ts;
            if ( this.artifact != null && this.artifact.getFile() != null )
            {
                ts = this.artifact.getFile().lastModified();
            }
            else
            {
                ts = System.currentTimeMillis();
            }
            marker.setLastModified( ts );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to update Marker timestamp: " + marker.getAbsolutePath(), e );
        }
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname. If this
     * pathname denotes a directory, then the directory must be empty in order
     * to be deleted.
     * 
     * @return <code>true</code> if and only if the file or directory is
     *         successfully deleted; <code>false</code> otherwise
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkDelete}</code>
     *             method denies delete access to the file
     */
    public boolean clearMarker()
        throws MojoExecutionException
    {
        File marker = getMarkerFile();
        return marker.delete();
    }

    /**
     * @return Returns the artifact.
     */
    public Artifact getArtifact()
    {
        return this.artifact;
    }

    /**
     * @param artifact
     *            The artifact to set.
     */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /**
     * @return Returns the markerFilesDirectory.
     */
    public File getMarkerFilesDirectory()
    {
        return this.markerFilesDirectory;
    }

    /**
     * @param markerFilesDirectory
     *            The markerFilesDirectory to set.
     */
    public void setMarkerFilesDirectory( File markerFilesDirectory )
    {
        this.markerFilesDirectory = markerFilesDirectory;
    }
}
