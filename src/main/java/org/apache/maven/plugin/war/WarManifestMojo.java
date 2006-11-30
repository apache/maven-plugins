package org.apache.maven.plugin.war;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generate a manifest for this WAR.
 *
 * @author Mike Perham
 * @version $Id: WarMojo.java 307363 2005-10-09 04:50:58Z brett $
 * @goal manifest
 * @phase process-resources
 * @requiresDependencyResolution runtime
 */
public class WarManifestMojo
    extends AbstractWarMojo
{
    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#war}"
     * @required
     */
    private WarArchiver warArchiver;


    /**
     * Executes the WarMojo on the current project.
     *
     * @throws MojoExecutionException if an error occured while building the webapp
     */
    public void execute()
        throws MojoExecutionException
    {
        File manifestDir = new File( getWarSourceDirectory(), "META-INF" );
        if ( !manifestDir.exists() )
        {
            manifestDir.mkdirs();
        }
        File manifestFile = new File( manifestDir, "MANIFEST.MF" );
        MavenArchiver ma = new MavenArchiver();
        ma.setArchiver( warArchiver );
        ma.setOutputFile( manifestFile );

        PrintWriter printWriter = null;
        try
        {
            Manifest mf = ma.getManifest( getProject(), archive.getManifest() );
            printWriter = new PrintWriter( new FileWriter( manifestFile ) );
            mf.write( printWriter );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest: " + e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( printWriter );
        }
    }
}
