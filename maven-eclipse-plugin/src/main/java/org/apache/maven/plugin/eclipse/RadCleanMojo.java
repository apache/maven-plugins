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
package org.apache.maven.plugin.eclipse;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Deletes the config files used by Rad-6. the files .j2ee and the file .websettings
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 * @goal rad-clean
 */
public class RadCleanMojo
    extends EclipseCleanMojo
{
    /**
     * The project whose project files to clean.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    protected void cleanExtras()
        throws MojoExecutionException
    {
        delete( new File( getBasedir(), ".j2ee" ) );
        delete( new File( getBasedir(), ".websettings" ) );
        delete( new File( getBasedir(), ".website-config" ) );

        handleLibs();
    }

    /**
     * getter for the instancevarriable project.
     * 
     * @return the maven project decriptor
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * getter for the instancevarriable project.
     * 
     * @param project the maven project decriptor
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Delete all jars in the EAR project root directory.
     * 
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    private void handleEarLibs()
        throws MojoExecutionException
    {
        File targetDir = this.project.getBasedir();
        deleteJarArtifactsInDirectory( targetDir );
        deleteWarArtifactsInDirectory( targetDir );
    }

    /**
     * Delete all jars in the project that were required by rad6.
     * 
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    private void handleLibs()
        throws MojoExecutionException
    {

        if ( Constants.PROJECT_PACKAGING_EAR.equals( getPackaging() ) )
        {
            handleEarLibs();
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( getPackaging() ) )
        {
            handleWarLibs();
        }
    }

    /**
     * Delete all jars in the WAR project WEB-INF/lib directory.
     * 
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    private void handleWarLibs()
        throws MojoExecutionException
    {
        File basedir = this.project.getBasedir();

        File warSourceDirectory =
            new File( IdeUtils.getPluginSetting( this.project, JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                 "warSourceDirectory", //$NON-NLS-1$
                                                 "src/main/webapp" ) ); //$NON-NLS-1$

        String webContentDir = IdeUtils.toRelativeAndFixSeparator( basedir, warSourceDirectory, false );

        String srcMainWebappWebInfLibDirname =
            basedir.getAbsolutePath() + File.separatorChar + webContentDir + File.separatorChar + "WEB-INF"
                + File.separatorChar + "lib";

        File srcMainWebappWebInfLibDir = new File( srcMainWebappWebInfLibDirname );
        srcMainWebappWebInfLibDir.mkdirs();

        deleteJarArtifactsInDirectory( srcMainWebappWebInfLibDir );
    }

    /**
     * delete all Jar artifacts in the specified directory.
     * 
     * @param directory to delete the jars from
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    protected void deleteJarArtifactsInDirectory( File directory )
        throws MojoExecutionException
    {
        deleteArtifactsInDirectory( directory, Constants.PROJECT_PACKAGING_JAR );
    }

    /**
     * delete all War artifacts in the specified directory (cleaning up EAR's for example).
     * 
     * @param directory to delete the wars from
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    protected void deleteWarArtifactsInDirectory( File directory )
        throws MojoExecutionException
    {
        deleteArtifactsInDirectory( directory, Constants.PROJECT_PACKAGING_WAR );
    }

    /**
     * Deletes all artifacts of specified packaging type in the specified directory
     * 
     * @param directory - to delete the jars from
     * @param packagingType - packaging type (file extensions in fact - can be dangerous)
     * @see Constants#PROJECT_PACKAGING_JAR
     * @see Constants#PROJECT_PACKAGING_WAR
     * @throws MojoExecutionException if a file exists and can't be deleted
     */
    private void deleteArtifactsInDirectory( File directory, String packagingType )
        throws MojoExecutionException
    {

        // sanity check, only support cleanup of 2 types - jar and war
        if ( Constants.PROJECT_PACKAGING_JAR.equalsIgnoreCase( packagingType )
            || Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packagingType ) )
        {
            String[] oldFiles =
                FileUtils.getFilesFromExtension( directory.getAbsolutePath(), new String[] { packagingType } );
            for ( int index = 0; index < oldFiles.length; index++ )
            {
                File f = new File( oldFiles[index] );

                delete( f );
            }
        }
    }
}
