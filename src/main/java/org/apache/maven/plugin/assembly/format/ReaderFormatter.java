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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.List;

public class ReaderFormatter
{
    private static Reader createReaderFilter( @Nonnull Reader source, String sourceName, String encoding,
                                              String escapeString, List<String> delimiters,
                                              AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        try
        {
            // @todo this test can be improved
            boolean isPropertiesFile = AssemblyFileUtils.isPropertyFile( sourceName );

            MavenReaderFilterRequest filterRequest =
                new MavenReaderFilterRequest( source, true, configSource.getProject(), configSource.getFilters(),
                                            isPropertiesFile, encoding, configSource.getMavenSession(), null );
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
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }
    }

    public static InputStreamTransformer getFileSetTransformers( final AssemblerConfigurationSource configSource, final boolean isFiltered, String fileSetLineEnding )
        throws AssemblyFormattingException
    {
        final String lineEndingHint = fileSetLineEnding;

        String lineEnding = LineEndingsUtils.getLineEndingCharacters( lineEndingHint );

        if ( ( lineEnding != null ) || isFiltered )
        {
            return new InputStreamTransformer()
            {
                public InputStream transform( PlexusIoResource plexusIoResource, InputStream inputStream )
                    throws IOException
                {
                    if ( isFiltered )
                    {
                        final String encoding = configSource.getEncoding();

                        Reader source = encoding != null ? new InputStreamReader( inputStream, encoding )
                            : new InputStreamReader( inputStream ); // wtf platform encoding ?
                        try
                        {
                            Reader filtered = createReaderFilter( source, plexusIoResource.getName(),
                                                                                  configSource.getEncoding(),
                                                                                  configSource.getEscapeString(),
                                                                                  configSource.getDelimiters(),
                                                                                  configSource );
                            final ReaderInputStream readerInputStream = encoding != null ? new ReaderInputStream( filtered, encoding)
                                : new ReaderInputStream( filtered );

                            LineEndings lineEnding = LineEndingsUtils.getLineEnding( lineEndingHint );
                            if ( !LineEndings.keep.equals( lineEnding ) )
                            {
                                return LineEndingsUtils.lineEndingConverter( readerInputStream, lineEnding );

                            }
                            return readerInputStream;

                        }
                        catch ( AssemblyFormattingException e )
                        {
                            throw new IOException( e.getMessage());
                        }

                    }
                    else
                    {
                        return inputStream;
                    }
                }
            };
        }
        return null;
    }
}
