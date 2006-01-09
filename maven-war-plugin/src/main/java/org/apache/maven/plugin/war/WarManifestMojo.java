package org.apache.maven.plugin.war;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;

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
     * The maven archive configuration to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

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

        try
        {
            Manifest mf = ma.getManifest( getProject(), archive.getManifest() );
            FileWriter fileWriter = new FileWriter( manifestFile );
            PrintWriter printWriter = new PrintWriter( fileWriter );
            try
            {
                mf.write( printWriter );
            }
            finally
            {
                printWriter.close();
                fileWriter.close();
            }
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest", e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error preparing the manifest", e );
        }
    }
}
