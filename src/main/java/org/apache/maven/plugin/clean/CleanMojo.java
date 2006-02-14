package org.apache.maven.plugin.clean;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * Goal which cleans the build.

 * @goal clean
 * @author <a href="mailto:evenisse@maven.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class CleanMojo
    extends AbstractMojo
{
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;

    /** 
     * This is where build results go.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /** 
     * This is where compiled classes go.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /** 
     * This is where compiled test classes go.
     * 
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;
    
    /**
     * Be verbose in the debug log-level?
     * 
     * @parameter default=value="false" expression="${clean.verbose}"
     */
    private boolean verbose;

    /**
     * Should we follow symbolically linked files?
     * 
     * @parameter default=value="false" expression="${clean.followSymLinks}"
     */
    private boolean followSymLinks;

    public void execute()
        throws MojoExecutionException
    {
        removeDirectory( directory );
        removeDirectory( outputDirectory );
        removeDirectory( testOutputDirectory );
    }

    private void removeDirectory( File dir )
        throws MojoExecutionException
    {
        if ( dir != null )
        {
            if ( dir.exists() && dir.isDirectory() )
            {
                getLog().info( "Deleting directory " + dir.getAbsolutePath() );
                removeDir( dir );
            }
        }
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     */
    private boolean delete( File f )
    {
        if ( !f.delete() )
        {
            if ( verbose )
            {
                getLog().debug( "Failed to delete: " + f + " on first pass. Taking evasive actions." );
            }
            
            if ( System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) > -1 )
            {
                System.gc();
            }
            try
            {
                if ( verbose )
                {
                    getLog().debug( "Waiting: " + DELETE_RETRY_SLEEP_MILLIS + "ms to retry delete." );
                }
                
                Thread.sleep( DELETE_RETRY_SLEEP_MILLIS );
                return f.delete();
            }
            catch ( InterruptedException ex )
            {
                return f.delete();
            }
        }
        return true;
    }

    /**
     * Delete a directory
     *
     * @param d the directory to delete
     */
    protected void removeDir( File d )
        throws MojoExecutionException
    {
        if ( verbose )
        {
            getLog().debug( "Deleting directory: " + d + ". Traversing children first." );
        }
        
        String[] list = d.list();
        if ( list == null )
        {
            list = new String[0];
        }
        for ( int i = 0; i < list.length; i++ )
        {
            String s = list[i];
            File f = new File( d, s );

            if ( verbose )
            {
                getLog().debug( "Deleting: " + f + "." );
            }
            
            if ( f.isDirectory() && ( followSymLinks || !isSymLink( f ) ) )
            {
                removeDir( f );
            }
            else
            {
                if ( !delete( f ) )
                {
                    String message = "Unable to delete file " + f.getAbsolutePath();
// TODO:...
//                    if ( failOnError )
//                    {
                        throw new MojoExecutionException( message );
//                    }
//                    else
//                    {
//                        getLog().info( message );
//                    }
                }
            }
        }

        if ( !delete( d ) )
        {
            String message = "Unable to delete directory " + d.getAbsolutePath();
// TODO:...
//            if ( failOnError )
//            {
                throw new MojoExecutionException( message );
//            }
//            else
//            {
//                getLog().info( message );
//            }
        }
    }

    private boolean isSymLink( File file ) throws MojoExecutionException
    {
        File dir = file.getParentFile();
        try
        {
            return !file.getCanonicalPath().startsWith( dir.getCanonicalPath() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error checking whether file: " + file + " is a symbolic link. Error: " + e.getMessage(), e );
        }
    }
}
