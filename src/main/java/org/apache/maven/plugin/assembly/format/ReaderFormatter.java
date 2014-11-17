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

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.LineEndings;
import org.apache.maven.plugin.assembly.utils.LineEndingsUtils;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenReaderFilterRequest;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 
 */
public class ReaderFormatter
{
    private static Reader createReaderFilter( @Nonnull Reader source, String escapeString, List<String> delimiters,
                                              AssemblerConfigurationSource configSource, boolean isPropertiesFile )
        throws IOException
    {
        try
        {

            MavenReaderFilterRequest filterRequest =
                new MavenReaderFilterRequest( source, true, configSource.getProject(), configSource.getFilters(),
                                              isPropertiesFile, null, configSource.getMavenSession(), null );
//            filterRequest.setInjectProjectBuildFilters(true);
            filterRequest.setEscapeString( escapeString );

            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            if ( delimiters != null && !delimiters.isEmpty() )
            {
                LinkedHashSet<String> delims = new LinkedHashSet<String>();
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
            else
            {
                filterRequest.setDelimiters( filterRequest.getDelimiters() );
            }

            filterRequest.setInjectProjectBuildFilters( configSource.isIncludeProjectBuildFilters() );
            return configSource.getMavenReaderFilter().filter( filterRequest );
        }
        catch ( MavenFilteringException e )
        {
            IOException ioe = new IOException( "Error filtering file '" + source + "': " + e.getMessage() );
            ioe.initCause( e ); // plain old Java 5...
            throw ioe;
        }
    }

    public static
    @Nullable
    InputStreamTransformer getFileSetTransformers( final AssemblerConfigurationSource configSource,
                                                   final boolean isFiltered, String fileSetLineEnding )
        throws AssemblyFormattingException
    {
        final LineEndings lineEndingToUse = LineEndingsUtils.getLineEnding( fileSetLineEnding );

        final boolean transformLineEndings = !LineEndings.keep.equals( lineEndingToUse );

        if ( transformLineEndings || isFiltered )
        {
            return new InputStreamTransformer()
            {
                public InputStream transform( PlexusIoResource plexusIoResource, InputStream inputStream )
                    throws IOException
                {
                    InputStream result = inputStream;
                    if ( isFiltered )
                    {
                        final String encoding = configSource.getEncoding();

                        Reader source = encoding != null
                            ? new InputStreamReader( inputStream, encoding )
                            : new InputStreamReader( inputStream ); // wtf platform encoding ? TODO: Fix this
                        boolean isPropertyFile = AssemblyFileUtils.isPropertyFile( plexusIoResource.getName() );
                        Reader filtered =
                            createReaderFilter( source, configSource.getEscapeString(), configSource.getDelimiters(),
                                                configSource, isPropertyFile );
                        result = encoding != null
                            ? new ReaderInputStream( filtered, encoding )
                            : new ReaderInputStream( filtered );
                    }
                    if ( transformLineEndings )
                    {
                        result = LineEndingsUtils.lineEndingConverter( result, lineEndingToUse );
                    }
                    return result;
                }
            };
        }
        return null;
    }
}
