package org.apache.maven.plugin.eclipse;

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
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Maven2 plugin to delete the .project, .classpath, .wtpmodules files and
 * .settings folder needed for Eclipse.
 * 
 * @goal clean
 */
public class EclipseCleanMojo
    extends AbstractMojo
{

    /**
     * Definition file for Eclipse Web Tools project.
     */
    private static final String FILE_DOT_WTPMODULES = ".wtpmodules";

    /**
     * Classpath definition file for an Eclipse Java project.
     */
    private static final String FILE_DOT_CLASSPATH = ".classpath";

    /**
     * Project definition file for an Eclipse Project.
     */
    private static final String FILE_DOT_PROJECT = ".project";

    /**
     * Web Project definition file for Eclipse Web Tools Project (Release 1.0RC5
     * compatible).
     */
    private static final String DIR_DOT_SETTINGS = ".settings";

    /**
     * @parameter expression="${basedir}"
     */
    private File basedir;

    public void execute()
        throws MojoExecutionException
    {
        delete( new File( basedir, FILE_DOT_PROJECT ) );
        delete( new File( basedir, FILE_DOT_CLASSPATH ) );
        delete( new File( basedir, FILE_DOT_WTPMODULES ) );
        delete( new File( basedir, DIR_DOT_SETTINGS ) );
    }

    /**
     * Delete a file, handling log messages and exceptions
     * 
     * @param f
     *            File to be deleted
     * @throws MojoExecutionException
     *             only if a file exists and can't be deleted
     */
    private void delete( File f )
        throws MojoExecutionException
    {
        getLog().info( MessageFormat.format( "Deleting {0} file...", new Object[] { f.getName() } ) );

        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                try
                {
                    if ( getLog().isDebugEnabled() )
                        getLog().debug(
                                        MessageFormat.format( "Forcibly deleting {0} file...", new Object[] { f
                                            .getName() } ) );
                    FileUtils.forceDelete( f );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( MessageFormat.format( "Failed to delete {0} file: {0}",
                                                                            new Object[] {
                                                                                f.getName(),
                                                                                f.getAbsolutePath() } ) )
                    {
                    };
                }
            }
        }
        else
        {
            getLog().info( MessageFormat.format( "No {0} file found", new Object[] { f.getName() } ) );
        }
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

}
