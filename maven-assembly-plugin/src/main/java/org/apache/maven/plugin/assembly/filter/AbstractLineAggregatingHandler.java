/*
 *  Copyright (C) 2010 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.plugin.assembly.filter;

import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractLineAggregatingHandler
    implements ContainerDescriptorHandler
{

    private Map<String, StringWriter> catalog = new HashMap<String, StringWriter>();

    private Map<String, List<String>> files = new HashMap<String, List<String>>();

    protected abstract String getOutputPathPrefix( final FileInfo fileInfo );

    protected abstract boolean fileMatches( final FileInfo fileInfo );

    public void finalizeArchiveCreation( final Archiver archiver ) throws ArchiverException
    {
        for ( final Map.Entry<String, StringWriter> entry : catalog.entrySet() )
        {
            final String name = entry.getKey();
            final String fname = new File( name ).getName();

            Writer writer = null;
            File f;
            try
            {
                f = File.createTempFile( "assembly-" + fname, ".tmp" );
                f.deleteOnExit();

                writer = new FileWriter( f );
                writer.write( entry.getValue().toString() );
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
        }
    }

    public void finalizeArchiveExtraction( final UnArchiver unArchiver ) throws ArchiverException
    {
    }

    public List<String> getVirtualFiles()
    {
        return new ArrayList<String>( catalog.keySet() );
    }

    public boolean isSelected( final FileInfo fileInfo ) throws IOException
    {
        String name = fileInfo.getName();
        name = AssemblyFileUtils.normalizePath( name );
        name = name.replace( File.separatorChar, '/' );

        name = getOutputPathPrefix( fileInfo ) + new File( name ).getName();

        if ( fileInfo.isFile() && fileMatches( fileInfo ) )
        {
            StringWriter writer = catalog.get( name );
            if ( writer == null )
            {
                writer = new StringWriter();
                catalog.put( name, writer );
            }

            readLines( fileInfo, new PrintWriter( writer ) );

            List<String> aggregated = files.get( name );
            if ( aggregated == null )
            {
                aggregated = new ArrayList<String>();
                files.put( name, aggregated );
            }

            aggregated.add( fileInfo.getName() );

            return false;
        }

        return true;
    }

    protected void readLines( final FileInfo fileInfo, final PrintWriter writer ) throws IOException
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new InputStreamReader( fileInfo.getContents() ) ); // platform encoding
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                writer.println( line );
            }
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    protected final Map<String, StringWriter> getCatalog()
    {
        return catalog;
    }

    protected final Map<String, List<String>> getFiles()
    {
        return files;
    }

    protected final void setCatalog( final Map<String, StringWriter> catalog )
    {
        this.catalog = catalog;
    }

    protected final void setFiles( final Map<String, List<String>> files )
    {
        this.files = files;
    }

}
