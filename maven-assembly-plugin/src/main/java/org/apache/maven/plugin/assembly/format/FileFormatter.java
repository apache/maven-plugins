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
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Locale;

/**
 * @version $Id$
 */
public class FileFormatter
{

    private final Logger logger;

    private final AssemblerConfigurationSource configSource;

    public FileFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    public File format( File source, boolean filter, String lineEnding )
        throws AssemblyFormattingException
    {
        return format ( source, filter, lineEnding, configSource.getTemporaryRootDirectory() );
    }

    public File format( File source, boolean filter, String lineEnding, File tempRoot )
        throws AssemblyFormattingException
    {
        AssemblyFileUtils.verifyTempDirectoryAvailability( tempRoot, logger );

        File result = source;

        if ( filter )
            result = doFileFilter( source, tempRoot );

        String lineEndingChars = AssemblyFileUtils.getLineEndingCharacters( lineEnding );
        if ( lineEndingChars != null )
        {
            result = formatLineEndings( lineEndingChars, result, tempRoot );
        }

        return result;
    }

    private File doFileFilter( File source, File tempRoot )
        throws AssemblyFormattingException
    {
        try
        {
            File target = FileUtils.createTempFile( source.getName() + ".", ".filtered", tempRoot );

            //@todo this test can be improved
            boolean isPropertiesFile = source.getName().toLowerCase( Locale.ENGLISH ).endsWith( ".properties" );

            configSource.getMavenFileFilter().copyFile( source, target, true, configSource.getProject(),
                    Collections.EMPTY_LIST, isPropertiesFile, null, configSource.getMavenSession() );

            return target;
        }
        catch (MavenFilteringException e)
        {
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }
    }

    private File formatLineEndings( String lineEndingChars, File source, File tempRoot )
        throws AssemblyFormattingException
    {
        try
        {
            Reader contentReader = new FileReader( source );

            File target = FileUtils.createTempFile( source.getName() + ".", ".formatted", tempRoot );

            AssemblyFileUtils.convertLineEndings( contentReader, target, lineEndingChars );

            return target;
        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyFormattingException( "File to filter not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Error line formatting file '" + source + "': " + e.getMessage(), e );
        }
    }
}
