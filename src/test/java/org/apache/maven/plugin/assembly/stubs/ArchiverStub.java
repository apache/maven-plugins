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
import java.util.Map;

/**
 * @author Edwin Punzalan
 */
public class ArchiverStub
    implements Archiver
{
    public void createArchive()
        throws ArchiverException, IOException
    {
    }

    public void addDirectory( File file )
        throws ArchiverException
    {
    }

    public void addDirectory( File file, String string )
        throws ArchiverException
    {
    }

    public void addDirectory( File file, String[] strings, String[] strings1 )
        throws ArchiverException
    {
    }

    public void addDirectory( File file, String string, String[] strings, String[] strings1 )
        throws ArchiverException
    {
    }

    public void addFile( File file, String string )
        throws ArchiverException
    {
    }

    public void addFile( File file, String string, int i )
        throws ArchiverException
    {
    }

    public File getDestFile()
    {
        return null;
    }

    public void setDestFile( File file )
    {
    }

    public void setDefaultFileMode( int i )
    {
    }

    public int getDefaultFileMode()
    {
        return 0;
    }

    public void setDefaultDirectoryMode( int i )
    {
    }

    public int getDefaultDirectoryMode()
    {
        return 0;
    }

    public boolean getIncludeEmptyDirs()
    {
        return false;
    }

    public void setIncludeEmptyDirs( boolean b )
    {
    }

    public Map getFiles()
    {
        return null;
    }
}
