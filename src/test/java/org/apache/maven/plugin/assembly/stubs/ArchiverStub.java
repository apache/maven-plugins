package org.apache.maven.plugin.assembly.stubs;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Edwin Punzalan
 */
public class ArchiverStub
    implements Archiver
{
    private Map files = new HashMap();

    private File destFile;

    private int fileMode = 0;

    private int dirMode = 0;

    private boolean includeEmptyDirs = false;

    public void createArchive()
        throws ArchiverException, IOException
    {
        destFile.getParentFile().mkdirs();

        if ( !destFile.createNewFile() )
        {
            throw new ArchiverException( "Unable to create archive file" );
        }
    }

    public void addDirectory( File file )
        throws ArchiverException
    {
        System.out.println( "Adding dir " + file.getPath() );

        files.put( file.getPath(), file );
    }

    public void addDirectory( File file, String string )
        throws ArchiverException
    {
        addDirectory( file );
    }

    public void addDirectory( File file, String[] strings, String[] strings1 )
        throws ArchiverException
    {
        addDirectory( file );
    }

    public void addDirectory( File file, String string, String[] strings, String[] strings1 )
        throws ArchiverException
    {
        addDirectory( file );
    }

    public void addFile( File file, String string )
        throws ArchiverException
    {
        System.out.println( "Adding file " + file.getPath() );

        files.put( file.getPath(), file );
    }

    public void addFile( File file, String string, int i )
        throws ArchiverException
    {
        addFile( file, string );
    }

    public File getDestFile()
    {
        return destFile;
    }

    public void setDestFile( File file )
    {
        destFile = file;
    }

    public void setDefaultFileMode( int i )
    {
        fileMode = i;
    }

    public int getDefaultFileMode()
    {
        return fileMode;
    }

    public void setDefaultDirectoryMode( int i )
    {
        dirMode = i;
    }

    public int getDefaultDirectoryMode()
    {
        return dirMode;
    }

    public boolean getIncludeEmptyDirs()
    {
        return includeEmptyDirs;
    }

    public void setIncludeEmptyDirs( boolean b )
    {
        includeEmptyDirs = b;
    }

    public Map getFiles()
    {
        return files;
    }
}
