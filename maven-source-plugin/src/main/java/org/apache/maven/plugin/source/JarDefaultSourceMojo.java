package org.apache.maven.plugin.source;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.io.IOException;

/**
 * This plugin bundles all the generated sources into a jar archive.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: AbstractJarSourceMojo.java 389062 2006-03-27 08:19:25Z aramirez $
 * @goal jar
 * @phase package
 * @execute phase="generate-sources"
 */
public class JarDefaultSourceMojo
    extends AbstractJarSourceMojo
{
    public void execute()
        throws MojoExecutionException
    {
        if ( "pom".equals( packaging ) )
        {
            getLog().info( "NOT adding sources to attached artifacts for packaging: \'" + packaging + "\'." );
        }
        else
        {
            File outputFile = new File( outputDirectory, finalName + "-sources.jar" );
            File[] sourceDirectories = getDefaultSources();

            try
            {
                createJar( outputFile, sourceDirectories, new JarArchiver() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }

            attachArtifact( outputFile, "sources" );
        }
    }

}
