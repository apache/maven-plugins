package org.apache.maven.plugin.assembly.format;

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

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @version $Id$
 */
public class FileSetFormatter
{

    private final AssemblerConfigurationSource configSource;

    private final Logger logger;

    public FileSetFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    public File formatFileSetForAssembly( File fileSetDir, org.apache.maven.plugin.assembly.model.FileSet set )
        throws AssemblyFormattingException, IOException
    {
        String lineEndingHint = set.getLineEnding();

        String lineEnding = AssemblyFileUtils.getLineEndingCharacters( lineEndingHint );

        if ( ( lineEnding != null ) || set.isFiltered() )
        {

            FileSet fileSet = new FileSet();
            fileSet.setLineEnding( lineEnding );
            
            fileSet.setDirectory( fileSetDir.getAbsolutePath() );
                        
            fileSet.setIncludes( set.getIncludes() );

            fileSet.setExcludes( set.getExcludes() );
            fileSet.setUseDefaultExcludes( true );

            FileSetManager fsm = new FileSetManager( logger );
            String[] files = fsm.getIncludedFiles( fileSet );

            // if we don't have anything to process, let's just skip all of this mess.
            if ( ( files == null ) || ( files.length == 0 ) )
            {
                logger.info( "No files selected for line-ending conversion or filtering. Skipping: " + fileSet.getDirectory() );
            }
            else
            {
                File formattedDir =
                    FileUtils.createTempFile( "fileSetFormatter.", ".tmp", configSource.getTemporaryRootDirectory() );
                
                logger.debug( "Filtering files from: " + fileSetDir + " into temp dir: " + formattedDir );

                formattedDir.delete();
                formattedDir.mkdirs();

                FileFormatter fileFormatter = new FileFormatter( configSource, logger );
                for ( String file : files )
                {
                    logger.debug( "Filtering: " + file );

                    File targetFile = new File( formattedDir, file );

                    targetFile.getParentFile().mkdirs();

                    File sourceFile = new File( fileSetDir, file );
                    try
                    {
                        sourceFile = fileFormatter.format( sourceFile, set.isFiltered(), lineEndingHint, formattedDir,
                                                           configSource.getEncoding() );
                        AssemblyFileUtils.copyFile( sourceFile, targetFile );
                    }
                    catch ( AssemblyFormattingException e )
                    {
                        deleteDirectory( formattedDir );
                        throw e;
                    }
                    catch ( IOException e )
                    {
                        deleteDirectory( formattedDir );
                        throw e;
                    }
                }
                return formattedDir;
            }
        }
        else
        {
            logger.debug( "NOT reformatting any files in " + fileSetDir );
        }

        return fileSetDir;
    }

    private static void deleteDirectory( File formattedDir )
    {
        try
        {
            FileUtils.deleteDirectory( formattedDir );
        }
        catch ( IOException e1 )
        {
            // ignore
        }
    }

}
