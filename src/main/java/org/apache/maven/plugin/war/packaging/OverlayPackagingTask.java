package org.apache.maven.plugin.war.packaging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Handles an overlay.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id$
 */
public class OverlayPackagingTask
    extends AbstractWarPackagingTask
{
    private final Overlay overlay;


    public OverlayPackagingTask( Overlay overlay, Overlay currentProjectOverlay )
    {
        if ( overlay == null )
        {
            throw new NullPointerException( "overlay could not be null." );
        }
        if ( overlay.equals( currentProjectOverlay ) )
        {
            throw new IllegalStateException( "Could not handle the current project with this task." );
        }
        this.overlay = overlay;
    }


    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException
    {
        context.getLog().debug(
            "OverlayPackagingTask performPackaging overlay.getTargetPath() " + overlay.getTargetPath() );
        if ( overlay.shouldSkip() )
        {
            context.getLog().info( "Skipping overlay[" + overlay + "]" );
        }
        else
        {
            try
            {
                context.getLog().info( "Processing overlay[" + overlay + "]" );

                // Step1: Extract if necessary
                final File tmpDir = unpackOverlay( context, overlay );

                // Step2: setup
                final PathSet includes = getFilesToIncludes( tmpDir, overlay.getIncludes(), overlay.getExcludes() );

                // Copy
                if ( null == overlay.getTargetPath() )
                {
                    copyFiles( overlay.getId(), context, tmpDir, includes, overlay.isFiltered() );
                }
                else
                {
                    // overlay.getTargetPath() must ended with /
                    // if not we add it
                    String targetPath = overlay.getTargetPath();
                    if ( !targetPath.endsWith( "/" ) )
                    {
                        targetPath = targetPath + "/";
                    }
                    copyFiles( overlay.getId(), context, tmpDir, includes, targetPath, overlay.isFiltered() );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy file for overlay[" + overlay + "]", e );
            }
        }
    }

    /**
     * Unpacks the specified overlay.
     * <p/>
     * Makes sure to skip the unpack process if the overlay has
     * already been unpacked.
     *
     * @param context the packaging context
     * @param overlay the overlay
     * @return the directory containing the unpacked overlay
     * @throws MojoExecutionException if an error occurred while unpacking the overlay
     */
    protected File unpackOverlay( WarPackagingContext context, Overlay overlay )
        throws MojoExecutionException
    {
        final File tmpDir = getOverlayTempDirectory( context, overlay );

        // TODO: not sure it's good, we should reuse the markers of the dependency plugin
        if ( FileUtils.sizeOfDirectory( tmpDir ) == 0
            || overlay.getArtifact().getFile().lastModified() > tmpDir.lastModified() )
        {
            doUnpack( context, overlay.getArtifact().getFile(), tmpDir );
        }
        else
        {
            context.getLog().debug( "Overlay[" + overlay + "] was already unpacked" );
        }
        return tmpDir;
    }

    /**
     * Returns the directory to use to unpack the specified overlay.
     *
     * @param context the packaging context
     * @param overlay the overlay
     * @return the temp directory for the overlay
     */
    protected File getOverlayTempDirectory( WarPackagingContext context, Overlay overlay )
    {
        final File groupIdDir = new File( context.getOverlaysWorkDirectory(), overlay.getGroupId() );
        if ( !groupIdDir.exists() )
        {
            groupIdDir.mkdir();
        }
        String directoryName = overlay.getArtifactId();
        if ( overlay.getClassifier() != null )
        {
            directoryName = directoryName + "-" + overlay.getClassifier();
        }
        final File result = new File( groupIdDir, directoryName );
        if ( !result.exists() )
        {
            result.mkdirs();
        }
        return result;
    }
}
