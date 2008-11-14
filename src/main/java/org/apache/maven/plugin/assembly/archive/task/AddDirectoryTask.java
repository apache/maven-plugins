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
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.util.FileUtils;

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

        int oldDirMode = archiver.getDefaultDirectoryMode();
        int oldFileMode = archiver.getDefaultFileMode();

        try
        {
            if ( directoryMode != -1 )
            {
                archiver.setDefaultDirectoryMode( directoryMode );
            }

            if ( fileMode != -1 )
            {
                archiver.setDefaultFileMode( fileMode );
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

                if ( useDefaultExcludes )
                {
                    directoryExcludes.addAll( FileUtils.getDefaultExcludesAsList() );
                }

                try
                {
                    String[] includesArray = null;
                    if ( includes != null && !includes.isEmpty() )
                    {
                        includesArray = (String[]) includes.toArray( new String[0] );
                    }

                    // this one is guaranteed to be non-null by code above.
                    String[] excludesArray = (String[]) directoryExcludes.toArray( new String[0] );

                    archiver.addDirectory( directory, outputDirectory, includesArray, excludesArray );
                }
                catch ( ArchiverException e )
                {
                    throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
                }
            }
        }
        finally
        {
            archiver.setDefaultDirectoryMode( oldDirMode );
            archiver.setDefaultFileMode( oldFileMode );
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
