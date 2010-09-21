package org.apache.maven.plugin.assembly.testutils;

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

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingArchiverStub
    implements Archiver
{

    private static final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

    public boolean forced;

    public File destFile;

    public final List<Addition> added = new ArrayList<Addition>();

    public boolean created;

    public void createArchive()
        throws ArchiverException, IOException
    {
        created = true;
    }

    public void addDirectory( final File directory )
        throws ArchiverException
    {
        added.add( new Addition( directory, null, null, null, -1 ) );
    }

    public void addDirectory( final File directory, final String prefix )
        throws ArchiverException
    {
        added.add( new Addition( directory, prefix, null, null, -1 ) );
    }

    public void addDirectory( final File directory, final String[] includes, final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( directory, null, includes, excludes, -1 ) );
    }

    public void addDirectory( final File directory, final String prefix, final String[] includes,
                              final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( directory, prefix, includes, excludes, -1 ) );
    }

    public void addFileSet( final FileSet fileSet )
        throws ArchiverException
    {
        added.add( new Addition( fileSet, null, null, null, -1 ) );
    }

    public void addFile( final File inputFile, final String destFileName )
        throws ArchiverException
    {
        added.add( new Addition( inputFile, destFileName, null, null, -1 ) );
    }

    public void addFile( final File inputFile, final String destFileName, final int permissions )
        throws ArchiverException
    {
        added.add( new Addition( inputFile, destFileName, null, null, permissions ) );
    }

    public void addArchivedFileSet( final File archiveFile )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, null, null, null, -1 ) );
    }

    public void addArchivedFileSet( final File archiveFile, final String prefix )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, prefix, null, null, -1 ) );
    }

    public void addArchivedFileSet( final File archiveFile, final String[] includes, final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, null, includes, excludes, -1 ) );
    }

    public void addArchivedFileSet( final File archiveFile, final String prefix, final String[] includes,
                                    final String[] excludes )
        throws ArchiverException
    {
        added.add( new Addition( archiveFile, prefix, includes, excludes, -1 ) );
    }

    public void addArchivedFileSet( final ArchivedFileSet fileSet )
        throws ArchiverException
    {
        added.add( new Addition( fileSet, null, null, null, -1 ) );
    }

    public void addResource( final PlexusIoResource resource, final String destFileName, final int permissions )
        throws ArchiverException
    {
        added.add( new Addition( resource, destFileName, null, null, permissions ) );
    }

    public void addResources( final PlexusIoResourceCollection resources )
        throws ArchiverException
    {
        added.add( new Addition( resources, null, null, null, -1 ) );
    }

    public File getDestFile()
    {
        return destFile;
    }

    public void setDestFile( final File destFile )
    {
        this.destFile = destFile;
    }

    public void setFileMode( final int mode )
    {
    }

    public int getFileMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0644", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0644", e );
        }
    }

    public int getOverrideFileMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0644", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0644", e );
        }
    }

    public void setDefaultFileMode( final int mode )
    {
    }

    public int getDefaultFileMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0644", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0644", e );
        }
    }

    public void setDirectoryMode( final int mode )
    {
    }

    public int getDirectoryMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0755", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0755", e );
        }
    }

    public int getOverrideDirectoryMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0755", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0755", e );
        }
    }

    public void setDefaultDirectoryMode( final int mode )
    {
    }

    public int getDefaultDirectoryMode()
    {
        try
        {
            return TypeConversionUtils.modeToInt( "0755", logger );
        }
        catch ( final AssemblyFormattingException e )
        {
            throw new IllegalStateException( "Failed to parse mode 0755", e );
        }
    }

    public boolean getIncludeEmptyDirs()
    {
        return false;
    }

    public void setIncludeEmptyDirs( final boolean includeEmptyDirs )
    {
    }

    public void setDotFileDirectory( final File dotFileDirectory )
    {
    }

    public ResourceIterator getResources()
        throws ArchiverException
    {
        return null;
    }

    @SuppressWarnings( "rawtypes" )
    public Map getFiles()
    {
        return new HashMap();
    }

    public boolean isForced()
    {
        return false;
    }

    public void setForced( final boolean forced )
    {
    }

    public boolean isSupportingForced()
    {
        return true;
    }

    public String getDuplicateBehavior()
    {
        return null;
    }

    public void setDuplicateBehavior( final String duplicate )
    {
    }

    public class Addition
    {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            final StringBuilder builder = new StringBuilder();
            builder.append( "Addition (\n    resource= " );
            builder.append( resource );
            builder.append( "\n    directory= " );
            builder.append( directory );
            builder.append( "\n    destination= " );
            builder.append( destination );
            builder.append( "\n    permissions= " );
            builder.append( permissions );
            builder.append( "\n    includes= " );
            builder.append( includes == null ? "-none-" : StringUtils.join( includes, ", " ) );
            builder.append( "\n    excludes= " );
            builder.append( excludes == null ? "-none-" : StringUtils.join( excludes, ", " ) );
            builder.append( "\n)" );
            return builder.toString();
        }

        public final Object resource;

        public final File directory;

        public final String destination;

        public final int permissions;

        public final String[] includes;

        public final String[] excludes;

        public Addition( final Object resource, final String destination, final String[] includes,
                         final String[] excludes, final int permissions )
        {
            this.resource = resource;
            if ( resource instanceof FileSet )
            {
                final FileSet fs = (FileSet) resource;
                directory = fs.getDirectory();
                this.destination = fs.getPrefix();
                this.includes = fs.getIncludes();
                this.excludes = fs.getExcludes();
                this.permissions = permissions;
            }
            else
            {
                if ( resource instanceof File && ( (File) resource ).isDirectory() )
                {
                    directory = (File) resource;
                }
                else
                {
                    directory = null;
                }

                this.destination = destination;
                this.includes = includes;
                this.excludes = excludes;
                this.permissions = permissions;
            }
        }
    }
}
