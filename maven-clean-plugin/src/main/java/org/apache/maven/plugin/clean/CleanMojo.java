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
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.shared.monitor.MojoLogMonitorAdaptor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Goal which cleans the build.

 * @goal clean
 * @author <a href="mailto:evenisse@maven.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class CleanMojo
    extends AbstractMojo
{
    /** 
     * This is where build results go.
     * 
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /** 
     * This is where compiled classes go.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /** 
     * This is where compiled test classes go.
     * 
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;
    
    /**
     * Be verbose in the debug log-level?
     * 
     * @parameter expression="${clean.verbose}" default=value="false"
     */
    private boolean verbose;
    
    /**
     * The list of filesets to delete, in addition to the default directories.
     * 
     * @parameter
     */
    private List filesets;

    /**
     * Should we follow symbolically linked files?
     * 
     * @parameter expression="${clean.followSymLinks}" default=value="false"
     */
    private boolean followSymLinks;

    private FileSetManager fileSetManager;

    public void execute()
        throws MojoExecutionException
    {
        MojoLogMonitorAdaptor monitor = new MojoLogMonitorAdaptor( getLog() );
        fileSetManager = new FileSetManager( monitor, verbose );
        
        removeDirectory( directory );
        removeDirectory( outputDirectory );
        removeDirectory( testOutputDirectory );
        
        removeAdditionalFilesets();
    }

    private void removeAdditionalFilesets() throws MojoExecutionException
    {
        if ( filesets != null && !filesets.isEmpty() )
        {
            for ( Iterator it = filesets.iterator(); it.hasNext(); )
            {
                Fileset fileset = (Fileset) it.next();
                
                try
                {
                    getLog().info( "Deleting " + fileset );
                    
                    fileSetManager.delete( fileset );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to delete directory: " + fileset.getDirectory() + ". Reason: " + e.getMessage(), e );
                }
            }
        }
    }

    private void removeDirectory( File dir )
        throws MojoExecutionException
    {
        FileSet fs = new FileSet();
        fs.setDirectory( dir.getPath() );
        fs.addInclude( "**/**" );
        fs.setFollowSymlinks( followSymLinks );
        
        try
        {
            getLog().info( "Deleting directory " + dir.getAbsolutePath() );
            fileSetManager.delete( fs );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to delete directory: " + dir + ". Reason: " + e.getMessage(), e );
        }
    }

}
