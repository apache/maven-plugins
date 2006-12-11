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
package org.apache.maven.plugin.eclipse.writers.rad;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Copy all dependent jar in the directorys where RAD6 needs then to use the
 * runtime enviorment in RAD6. so all dependent jars in the EAR rootdirectory
 * and all dependend jars in the WAR WEB-INF/lib directory
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class RadLibCopier
    extends AbstractEclipseWriter
{

    /**
     * copy the jars in the apropreate directorys.
     * 
     * @throws MojoExecutionException
     *             when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        MavenProject project = config.getProject();
        IdeDependency[] deps = config.getDeps();

        String packaging = project.getPackaging();
        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            handleEarLibs( deps );
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            handleWarLibs( deps );
        }
    }

    /**
     * Copies the Artifact after building the destination file name if
     * overridden. This method also checks if the classifier is set and adds it
     * to the destination file name if needed.
     * 
     * @param deps
     *            representing the dependencies to be copied.
     * @param destDir
     *            where should the atifact go.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see DependencyUtil#copyFile(File, File, Log)
     * @see DependencyUtil#getFormattedFileName(Artifact, boolean)
     */
    private void copyArtifact( IdeDependency[] deps, File destDir )
        throws MojoExecutionException
    {
        String[] oldFiles = FileUtils.getFilesFromExtension( destDir.getAbsolutePath(),
                                                             new String[] { Constants.PROJECT_PACKAGING_JAR } );
        for ( int index = 0; index < oldFiles.length; index++ )
        {
            if ( !new File( oldFiles[index] ).delete() )
            {
                log.error( Messages.getString( "Rad6LibCopier.cantdeletefile", new Object[] { oldFiles[index] } ) );
            }
        }
        for ( int index = 0; index < deps.length; index++ )
        {
            if ( !deps[index].isTestDependency() && !deps[index].isProvided() && !deps[index].isReferencedProject()
                && !deps[index].isSystemScoped() )
            {
                copyFile( deps[index].getFile(), new File( destDir, deps[index].getFile().getName() ), log );
            }
        }
    }

    /**
     * Does the actual copy of the file and logging.
     * 
     * @param artifact
     *            represents the file to copy.
     * @param destFile
     *            file name of destination file.
     * @param log
     *            to use for output.
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     */
    private void copyFile( File artifact, File destFile, Log log )
        throws MojoExecutionException
    {
        try
        {
            log.info( "Copying " + artifact.getAbsolutePath() + " to " + destFile );
            FileUtils.copyFile( artifact, destFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying artifact from " + artifact + " to " + destFile, e );
        }
    }

    /**
     * EARs need the jars in the root directory.
     * 
     * @param deps
     *            dependencys to include
     * @throws MojoExecutionException
     *             if the copying fails
     */
    private void handleEarLibs( IdeDependency[] deps )
        throws MojoExecutionException
    {
        File targetDir = config.getProject().getBasedir();
        copyArtifact( deps, targetDir );
    }

    /**
     * WARs need the jars in the WEB-INF/lib directory.
     * 
     * @param deps
     *            dependencys to include
     * @throws MojoExecutionException
     *             if the copying fails
     */
    private void handleWarLibs( IdeDependency[] deps )
        throws MojoExecutionException
    {
        File basedir = config.getProject().getBasedir();

        String srcMainWebappWebInfLibDirName = basedir.getAbsolutePath() + File.separatorChar + "src"
            + File.separatorChar + "main" + File.separatorChar + "webapp" + File.separatorChar + "WEB-INF"
            + File.separatorChar + "lib";

        File srcMainWebappWebInfLibDir = new File( srcMainWebappWebInfLibDirName );
        srcMainWebappWebInfLibDir.mkdirs();

        copyArtifact( deps, srcMainWebappWebInfLibDir );
    }

}
