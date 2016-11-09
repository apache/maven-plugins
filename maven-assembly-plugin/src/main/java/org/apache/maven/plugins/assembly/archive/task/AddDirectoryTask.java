package org.apache.maven.plugins.assembly.archive.task;

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

import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id$
 */
public class AddDirectoryTask
{

    private final File directory;

    private final InputStreamTransformer transformer;

    private List<String> includes;

    private List<String> excludes;

    private String outputDirectory;

    private boolean useDefaultExcludes = true;

    private int directoryMode = -1;

    private int fileMode = -1;

    public AddDirectoryTask( final File directory, InputStreamTransformer transformers )
    {
        this.directory = directory;

        this.transformer = transformers;
    }

    public AddDirectoryTask( final File directory )
    {
        this( directory, null );
    }

    public void execute( final Archiver archiver )
        throws ArchiveCreationException
    {
        if ( ".".equals( outputDirectory ) )
        {
            outputDirectory = "";
        }
        else if ( "..".equals( outputDirectory ) )
        {
            throw new ArchiveCreationException(
                "Cannot add source directory: " + directory + " to archive-path: " + outputDirectory
                    + ". All paths must be within the archive root directory." );
        }

        final int oldDirMode = archiver.getOverrideDirectoryMode();
        final int oldFileMode = archiver.getOverrideFileMode();

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
                List<String> directoryExcludes;
                if ( excludes != null && !excludes.isEmpty() )
                {
                    directoryExcludes = new ArrayList<String>( excludes );
                }
                else
                {
                    directoryExcludes = new ArrayList<String>();
                }

                try
                {
                    String[] includesArray = null;
                    if ( includes != null && !includes.isEmpty() )
                    {
                        includesArray = new String[includes.size()];

                        int i = 0;
                        for ( String include : includes )
                        {
                            includesArray[i++] = normalize( include );
                        }
                    }

                    // this one is guaranteed to be non-null by code above.
                    final String[] excludesArray = new String[directoryExcludes.size()];

                    int i = 0;
                    for ( String directoryExclude : directoryExcludes )
                    {
                        excludesArray[i++] = normalize( directoryExclude );
                    }

                    final DefaultFileSet fs = new DefaultFileSet();
                    fs.setUsingDefaultExcludes( useDefaultExcludes );
                    fs.setPrefix( outputDirectory );
                    fs.setDirectory( directory );
                    fs.setIncludes( includesArray );
                    fs.setExcludes( excludesArray );
                    if ( transformer != null )
                    {
                        fs.setStreamTransformer( transformer );
                    }

                    archiver.addFileSet( fs );
                }
                catch ( final ArchiverException e )
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

    private String normalize( String include )
    {
        String value = AssemblyFormatUtils.fixRelativeRefs( include );

        if ( value.startsWith( "/" ) || value.startsWith( "\\" ) )
        {
            value = value.substring( 1 );
        }
        return value;
    }

    public void setExcludes( final List<String> excludes )
    {
        this.excludes = excludes;
    }

    public void setIncludes( final List<String> includes )
    {
        this.includes = includes;
    }

    public void setOutputDirectory( final String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setDirectoryMode( final int directoryMode )
    {
        this.directoryMode = directoryMode;
    }

    public void setFileMode( final int fileMode )
    {
        this.fileMode = fileMode;
    }

    public void setUseDefaultExcludes( final boolean useDefaultExcludes )
    {
        this.useDefaultExcludes = useDefaultExcludes;
    }

}
