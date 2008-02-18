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
import org.apache.maven.plugin.assembly.utils.PropertyUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @version $Id$
 */
public class FileFormatter
{

    private final Logger logger;

    private final AssemblerConfigurationSource configSource;

    private Properties filterProperties;

    public FileFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    // used for unit testing currently.
    protected FileFormatter( Properties filterProperties, AssemblerConfigurationSource configSource, Logger logger )
    {
        this.filterProperties = filterProperties;
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
        File result = source;

        AssemblyFileUtils.verifyTempDirectoryAvailability( tempRoot, logger );

        String sourceName = source.getName();

        try
        {
            boolean contentIsChanged = false;

            String rawContents = readFile( source );

            String contents = rawContents;

            if ( filter )
            {
                contents = filter( contents, source );
            }

            contentIsChanged = !contents.equals( rawContents );

            BufferedReader contentReader = new BufferedReader( new StringReader( contents ) );

            File tempFilterFile = FileUtils.createTempFile( sourceName + ".", ".filtered", tempRoot );

            boolean fileWritten = formatLineEndings( contentReader, tempFilterFile, lineEnding, contentIsChanged );

            if ( fileWritten )
            {
                result = tempFilterFile;
            }
        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyFormattingException( "File to filter not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }

        return result;
    }

    /**
     * Read file content, using platform encoding.
     * @param source the file to read
     * @return the file content, read with platform encoding
     */
    private String readFile( File source )
        throws IOException
    {
        FileReader fileReader = null;

        StringWriter contentWriter = new StringWriter();

        try
        {
            fileReader = new FileReader( source ); // platform encoding

            IOUtil.copy( fileReader, contentWriter );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        return contentWriter.toString();
    }

    private boolean formatLineEndings( BufferedReader contentReader, File tempFilterFile, String lineEnding,
                                    boolean contentIsChanged )
        throws IOException, AssemblyFormattingException
    {
        boolean fileWritten = false;

        String lineEndingChars = AssemblyFileUtils.getLineEndingCharacters( lineEnding );

        if ( lineEndingChars != null )
        {
            AssemblyFileUtils.convertLineEndings( contentReader, tempFilterFile, lineEndingChars );

            fileWritten = true;
        }
        else if ( contentIsChanged )
        {
            FileWriter fileWriter = null;

            try
            {
                fileWriter = new FileWriter( tempFilterFile ); // platform encoding

                IOUtil.copy( contentReader, fileWriter );

                fileWritten = true;
            }
            finally
            {
                IOUtil.close( fileWriter );
            }
        }

        return fileWritten;
    }

    private String filter( String rawContents, File source )
        throws IOException, AssemblyFormattingException
    {
        initializeFiltering();

        Reader reader  = new BufferedReader(new StringReader(rawContents));

        // support ${token}
        reader = new InterpolationFilterReader( reader, filterProperties, "${", "}" );

        // support @token@
        reader = new InterpolationFilterReader( reader, filterProperties, "@", "@" );

        boolean isPropertiesFile = false;

        if ( source.isFile() && source.getName().endsWith( ".properties" ) )
        {
            isPropertiesFile = true;
        }

        reader = new InterpolationFilterReader( reader, new ReflectionProperties( configSource.getProject(), isPropertiesFile ), "${", "}" );

        StringWriter sw = new StringWriter();
        IOUtil.copy( reader, new BufferedWriter(sw));

        return sw.getBuffer().toString();


    }

    private void initializeFiltering()
        throws AssemblyFormattingException
    {
        logger.debug( "Initializing assembly filters..." );

        if ( filterProperties == null )
        {
            // System properties
            filterProperties = new Properties( System.getProperties() );

            // Project properties
            MavenProject project = configSource.getProject();
            filterProperties.putAll( project.getProperties() );

            List filters = configSource.getFilters();

            if ( ( filters != null ) && !filters.isEmpty() )
            {
                for ( Iterator i = filters.iterator(); i.hasNext(); )
                {
                    String filtersfile = (String) i.next();

                    try
                    {
                        Properties properties = PropertyUtils
                            .getInterpolatedPropertiesFromFile( new File( filtersfile ), true, true );

                        filterProperties.putAll( properties );
                    }
                    catch ( IOException e )
                    {
                        throw new AssemblyFormattingException( "Error loading property file '" + filtersfile + "'", e );
                    }
                }
            }
        }
    }

}
