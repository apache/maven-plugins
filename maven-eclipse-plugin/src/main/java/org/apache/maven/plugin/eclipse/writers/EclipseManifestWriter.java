/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Create or adapt the manifest files for the RAD6 runtime dependencys. attention these will not be used for the real
 * ear these are just to get the runtime enviorment using the maven dependencies. WARNING: The manifest resources added
 * here will not have the benefit of the dependencies of the project, since that's not provided in the setup() apis, one
 * of the locations from which this writer is used in the RadPlugin.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class EclipseManifestWriter
    extends AbstractEclipseManifestWriter
{

    private static final String GENERATED_RESOURCE_DIRNAME =
        "target" + File.separatorChar + "generated-resources" + File.separatorChar + "eclipse";

    private static final String WEBAPP_RESOURCE_DIR =
        "src" + File.separatorChar + "main" + File.separatorChar + "webapp";

    /**
     * Returns absolute path to the web content directory based on configuration of the war plugin or default one
     * otherwise.
     * 
     * @param project
     * @return absolute directory path as String
     * @throws MojoExecutionException
     */
    private static String getWebContentBaseDirectory( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        // getting true location of web source dir from config
        File warSourceDirectory =
            new File( IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                 "warSourceDirectory", WEBAPP_RESOURCE_DIR ) );
        // getting real and correct path to the web source dir
        String webContentDir =
            IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), warSourceDirectory, false );

        // getting the path to meta-inf base dir
        String result = config.getProject().getBasedir().getAbsolutePath() + File.separatorChar + webContentDir;

        return result;
    }

    /**
     * Search the project for the existing META-INF directory where the manifest should be located.
     * 
     * @return the absolute path to the META-INF directory
     * @throws MojoExecutionException
     */
    protected String getMetaInfBaseDirectory( MavenProject project )
        throws MojoExecutionException
    {
        String metaInfBaseDirectory = null;

        if ( this.config.getProject().getPackaging().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {

            // getting the path to meta-inf base dir
            metaInfBaseDirectory = getWebContentBaseDirectory( this.config );

            this.log.debug( "Attempting to use: " + metaInfBaseDirectory + " for location of META-INF in war project." );

            File metaInfDirectoryFile =
                new File( metaInfBaseDirectory + File.separatorChar + AbstractEclipseManifestWriter.META_INF_DIRECTORY );

            if ( !metaInfDirectoryFile.exists()
                || ( metaInfDirectoryFile.exists() && !metaInfDirectoryFile.isDirectory() ) )
            {
                metaInfBaseDirectory = null;
            }
        }

        for ( int index = this.config.getSourceDirs().length - 1; metaInfBaseDirectory == null && index >= 0; index-- )
        {

            File manifestFile =
                new File( this.config.getEclipseProjectDirectory(), this.config.getSourceDirs()[index].getPath()
                    + File.separatorChar + AbstractEclipseManifestWriter.META_INF_DIRECTORY + File.separatorChar
                    + AbstractEclipseManifestWriter.MANIFEST_MF_FILENAME );

            this.log.debug( "Checking for existence of META-INF/MANIFEST.MF file: " + manifestFile );

            if ( manifestFile.exists() )
            {
                metaInfBaseDirectory = manifestFile.getParentFile().getParent();
            }
        }

        return metaInfBaseDirectory;
    }

    /**
     * make room for a Manifest file. use a generated resource for JARS and for WARS use the manifest in the
     * webapp/META-INF directory.
     * 
     * @throws MojoExecutionException
     */
    public static void addManifestResource( Log log, EclipseWriterConfig config )
        throws MojoExecutionException
    {

        AbstractEclipseManifestWriter manifestWriter = new EclipseManifestWriter();
        manifestWriter.init( log, config );

        String packaging = config.getProject().getPackaging();

        String manifestDirectory = manifestWriter.getMetaInfBaseDirectory( config.getProject() );

        if ( !Constants.PROJECT_PACKAGING_EAR.equals( packaging )
            && !Constants.PROJECT_PACKAGING_WAR.equals( packaging ) && manifestDirectory == null )
        {

            String generatedResourceDir =
                config.getProject().getBasedir().getAbsolutePath() + File.separatorChar
                    + EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME;

            manifestDirectory = generatedResourceDir + File.separatorChar + "META-INF";

            try
            {
                new File( manifestDirectory ).mkdirs();
                File manifestFile = new File( manifestDirectory + File.separatorChar + "MANIFEST.MF" );
                if ( manifestFile.exists() )
                {
                    manifestFile.delete();
                }
                manifestFile.createNewFile();
            }
            catch ( IOException e )
            {
                log.error( Messages.getString( "EclipsePlugin.cantwritetofile", new Object[] { manifestDirectory
                    + File.separatorChar + "META-INF" + File.separatorChar + "MANIFEST.MF" } ) );
            }

            log.debug( "Adding " + EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME + " to eclipse sources " );

            EclipseSourceDir[] sourceDirs = config.getSourceDirs();
            EclipseSourceDir[] newSourceDirs = new EclipseSourceDir[sourceDirs.length + 1];
            System.arraycopy( sourceDirs, 0, newSourceDirs, 0, sourceDirs.length );
            newSourceDirs[sourceDirs.length] =
                new EclipseSourceDir( EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME, null, true, false, null, null,
                                      false );
            config.setSourceDirs( newSourceDirs );
        }

        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            new File( getWebContentBaseDirectory( config ) + File.separatorChar + "META-INF" ).mkdirs();
        }

        // special case must be done first because it can add stuff to the
        // classpath that will be
        // written by the superclass
        manifestWriter.write();
    }
}
