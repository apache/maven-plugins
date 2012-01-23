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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generate a manifest for this webapp. The manifest file is created in the
 * <code>warSourceDirectory</code>.
 *
 * @author Mike Perham
 * @version $Id$
 * @goal manifest
 * @phase process-resources
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class WarManifestMojo
    extends AbstractWarMojo
{
    /**
     * The WAR archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="war"
     */
    private WarArchiver warArchiver;


    /**
     * Executes this mojo on the current project.
     *
     * @throws MojoExecutionException if an error occurred while building the webapp
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
            Manifest mf = ma.getManifest( getSession(), getProject(), getArchive() );
            printWriter = new PrintWriter( WriterFactory.newWriter( manifestFile, WriterFactory.UTF_8 ) );
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
