package org.apache.maven.plugin.assembly.archive.task;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

/**
 * @version $Id$
 */
public class AddDirectoryTask
    implements ArchiverTask
{

    private final File directory;

    private List includes;

    private List excludes;

    private String outputDirectory;

    private boolean useDefaultExcludes = true;

    private int directoryMode = -1;

    private int fileMode = -1;

    public AddDirectoryTask( File directory )
    {
        this.directory = directory;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        if ( ".".equals( outputDirectory ) )
        {
            outputDirectory = "";
        }
        else if ( "..".equals( outputDirectory ) )
        {
            throw new ArchiveCreationException( "Cannot add source directory: " + directory + " to archive-path: "
                + outputDirectory + ". All paths must be within the archive root directory." );
        }
        
        int oldDirMode = archiver.getOverrideDirectoryMode();
        int oldFileMode = archiver.getOverrideFileMode();

        boolean fileModeSet = false;
        boolean dirModeSet = false;
        
        try
        {
            if ( directoryMode != -1 )
            {
                archiver.setDirectoryMode( directoryMode );
                dirModeSet = true;
            }

            if ( fileMode != -1 )
            {
                archiver.setFileMode( fileMode );
                fileModeSet = true;
            }

            if ( directory.exists() )
            {
                List directoryExcludes;
                if ( excludes != null && !excludes.isEmpty() )
                {
                    directoryExcludes = new ArrayList( excludes );
                }
                else
                {
                    directoryExcludes = new ArrayList();
                }

                try
                {
                    String[] includesArray = null;
                    if ( includes != null && !includes.isEmpty() )
                    {
                        includesArray = new String[includes.size()];
                        
                        int i = 0;
                        for ( Iterator it = includes.iterator(); it.hasNext(); )
                        {
                            String value = (String) it.next();
                            if ( value.startsWith( "./" ) || value.startsWith( ".\\" ) )
                            {
                                value = value.substring( 2 );
                            }
                            
                            if ( value.startsWith( "/" ) || value.startsWith( "\\" ) )
                            {
                                value = value.substring( 1 );
                            }
                            
                            includesArray[i] = value;
                            
                            i++;
                        }
                    }

                    // this one is guaranteed to be non-null by code above.
                    String[] excludesArray = new String[directoryExcludes.size()];
                    
                    int i = 0;
                    for ( Iterator it = directoryExcludes.iterator(); it.hasNext(); )
                    {
                        String value = (String) it.next();
                        if ( value.startsWith( "./" ) || value.startsWith( ".\\" ) )
                        {
                            value = value.substring( 2 );
                        }
                        
                        if ( value.startsWith( "/" ) || value.startsWith( "\\" ) )
                        {
                            value = value.substring( 1 );
                        }
                        
                        excludesArray[i] = value;
                        
                        i++;
                    }

                    DefaultFileSet fs = new DefaultFileSet();
                    fs.setUsingDefaultExcludes( useDefaultExcludes );
                    fs.setPrefix( outputDirectory );
                    fs.setDirectory( directory );
                    fs.setIncludes( includesArray );
                    fs.setExcludes( excludesArray );

                    archiver.addFileSet( fs );
                }
                catch ( ArchiverException e )
                {
                    throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
                }
            }
        }
        finally
        {
            if ( dirModeSet )
            {
                archiver.setDirectoryMode( oldDirMode );
            }
            
            if ( fileModeSet )
            {
                archiver.setFileMode( oldFileMode );
            }
        }
    }

    public void setExcludes( List excludes )
    {
        this.excludes = excludes;
    }

    public void setIncludes( List includes )
    {
        this.includes = includes;
    }

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setDirectoryMode( int directoryMode )
    {
        this.directoryMode = directoryMode;
    }

    public void setFileMode( int fileMode )
    {
        this.fileMode = fileMode;
    }

    public void setUseDefaultExcludes( boolean useDefaultExcludes )
    {
        this.useDefaultExcludes = useDefaultExcludes;
    }

}
