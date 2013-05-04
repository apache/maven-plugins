package org.apache.maven.plugin.assembly.filter;

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

import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractLineAggregatingHandler
    implements ContainerDescriptorHandler
{

    private Map<String, List<String>> catalog = new HashMap<String, List<String>>();

    private boolean excludeOverride = false;

    protected abstract String getOutputPathPrefix( final FileInfo fileInfo );

    protected abstract boolean fileMatches( final FileInfo fileInfo );

    protected String getEncoding()
    {
        return "UTF-8";
    }

    public void finalizeArchiveCreation( final Archiver archiver )
        throws ArchiverException
    {
        // this will prompt the isSelected() call, below, for all resources added to the archive.
        // FIXME: This needs to be corrected in the AbstractArchiver, where
        // runArchiveFinalizers() is called before regular resources are added...
        // which is done because the manifest needs to be added first, and the
        // manifest-creation component is a finalizer in the assembly plugin...
        for ( final ResourceIterator it = archiver.getResources(); it.hasNext(); )
        {
            it.next();
        }

        addToArchive( archiver );
    }

    protected void addToArchive( final Archiver archiver )
        throws ArchiverException
    {
        for ( final Map.Entry<String, List<String>> entry : catalog.entrySet() )
        {
            final String name = entry.getKey();
            final String fname = new File( name ).getName();

            PrintWriter writer = null;
            File f;
            try
            {
                f = File.createTempFile( "assembly-" + fname, ".tmp" );
                f.deleteOnExit();

                writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( f ), getEncoding() ) );
                for ( final String line : entry.getValue() )
                {
                    writer.println( line );
                }
            }
            catch ( final IOException e )
            {
                throw new ArchiverException( "Error adding aggregated content for: " + fname
                                + " to finalize archive creation. Reason: " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( writer );
            }

            excludeOverride = true;
            archiver.addFile( f, name );
            excludeOverride = false;
        }
    }

    public void finalizeArchiveExtraction( final UnArchiver unArchiver )
        throws ArchiverException
    {
    }

    public List<String> getVirtualFiles()
    {
        return new ArrayList<String>( catalog.keySet() );
    }

    public boolean isSelected( final FileInfo fileInfo )
        throws IOException
    {
        if ( excludeOverride )
        {
            return true;
        }

        String name = fileInfo.getName();
        name = AssemblyFileUtils.normalizePath( name );
        name = name.replace( File.separatorChar, '/' );

        if ( fileInfo.isFile() && fileMatches( fileInfo ) )
        {
            name = getOutputPathPrefix( fileInfo ) + new File( name ).getName();

            List<String> lines = catalog.get( name );
            if ( lines == null )
            {
                lines = new ArrayList<String>();
                catalog.put( name, lines );
            }

            readLines( fileInfo, lines );

            return false;
        }

        return true;
    }

    protected void readLines( final FileInfo fileInfo, final List<String> lines )
        throws IOException
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new InputStreamReader( fileInfo.getContents(), getEncoding() ) );

            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                if ( !lines.contains( line ) )
                {
                    lines.add( line );
                }
            }
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

}
