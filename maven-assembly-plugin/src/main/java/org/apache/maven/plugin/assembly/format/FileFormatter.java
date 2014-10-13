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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.LineEndings;
import org.apache.maven.plugin.assembly.utils.LineEndingsUtils;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

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

    public File format( File source, boolean filter, String lineEnding, String encoding )
        throws AssemblyFormattingException
    {
        return format( source, filter, lineEnding, configSource.getTemporaryRootDirectory(), encoding );
    }

    public File format( @Nonnull File source, boolean filter, String lineEndingCharacters, @Nullable File tempRoot,
                        String encoding )
        throws AssemblyFormattingException
    {
        AssemblyFileUtils.verifyTempDirectoryAvailability( tempRoot );

        File result = source;

        if ( StringUtils.isEmpty( encoding ) && filter )
        {
            logger.warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                + ", i.e. build is platform dependent!" );
        }

        if ( filter )
        {
            result = doFileFilter( source, tempRoot, encoding, configSource.getEscapeString(), configSource.getDelimiters(), configSource.isUseDefaultDelimiters());
        }

        LineEndings lineEnding = LineEndingsUtils.getLineEnding( lineEndingCharacters );
        if ( !LineEndings.keep.equals( lineEnding ) )
        {
            result = formatLineEndings( lineEnding, result, tempRoot, encoding );
        }

        return result;
    }

    private File doFileFilter( @Nonnull File source, @Nullable File tempRoot, String encoding, String escapeString, List<String> delimiters, boolean useDefaultDelimiters)
        throws AssemblyFormattingException
    {
        try
        {
            File target = FileUtils.createTempFile( source.getName() + ".", ".filtered", tempRoot );

            // @todo this test can be improved
            boolean isPropertiesFile = source.getName().toLowerCase( Locale.ENGLISH ).endsWith( ".properties" );

            MavenFileFilterRequest filterRequest =
                new MavenFileFilterRequest( source, target, true, configSource.getProject(), configSource.getFilters(),
                                            isPropertiesFile, encoding, configSource.getMavenSession(), null );
            filterRequest.setEscapeString( escapeString );
            
            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            if ( delimiters != null && !delimiters.isEmpty() )
            {
                LinkedHashSet<String> delims = new LinkedHashSet<String>();
                if ( useDefaultDelimiters )
                {
                    delims.addAll( filterRequest.getDelimiters() );
                }

                for ( String delim : delimiters )
                {
                    if ( delim == null )
                    {
                        // FIXME: ${filter:*} could also trigger this condition. Need a better long-term solution.
                        delims.add( "${*}" );
                    }
                    else
                    {
                        delims.add( delim );
                    }
                }

                filterRequest.setDelimiters( delims );
            }
            
            filterRequest.setInjectProjectBuildFilters( configSource.isIncludeProjectBuildFilters() );
            configSource.getMavenFileFilter().copyFile( filterRequest );

            return target;
        }
        catch ( MavenFilteringException e )
        {
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }
    }

    private File formatLineEndings( LineEndings lineEnding, File source, File tempRoot, String encoding )
        throws AssemblyFormattingException
    {
        Reader contentReader = null;
        try
        {
            File target = FileUtils.createTempFile( source.getName() + ".", ".formatted", tempRoot );

            LineEndingsUtils.convertLineEndings( source, target, lineEnding, null, encoding );

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
        finally
        {
            IOUtil.close( contentReader );
        }
    }
}
