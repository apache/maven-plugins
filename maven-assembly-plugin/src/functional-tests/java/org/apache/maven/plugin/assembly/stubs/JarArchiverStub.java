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

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Edwin Punzalan
 */
public class JarArchiverStub
    extends JarArchiver
{
    private Map files = new HashMap();

    private File destFile;

    private int fileMode = 0;

    private int dirMode = 0;

    private boolean includeEmptyDirs = false;

    private Manifest manifest;

    public void createArchive()
        throws ArchiverException, IOException
    {
        destFile.getParentFile().mkdirs();
        destFile.delete();

        if ( !destFile.createNewFile() )
        {
            throw new ArchiverException( "Unable to create archive file: " + destFile.getAbsolutePath() );
        }
    }

    public void addDirectory( File file )
        throws ArchiverException
    {
        addDirectory( file, "" );
    }

    public void addDirectory( File file, String string )
        throws ArchiverException
    {
        addDirectory( file, string, null, null );
    }

    public void addDirectory( File file, String[] strings, String[] strings1 )
        throws ArchiverException
    {
        addDirectory( file, file.getName(), strings, strings1 );
    }

    public void addDirectory( File file, String string, String[] includes, String[] excludes )
        throws ArchiverException
    {
        System.out.println( "Adding dir " + file.getPath() );

        files.put( file, new ArchiverFile( file, string, includes, excludes ) );
    }

    public void addFile( File file, String string )
        throws ArchiverException
    {
        addFile( file, string, 0 );
    }

    public void addFile( File file, String string, int i )
        throws ArchiverException
    {
        System.out.println( "Adding file " + file.getPath() );

        ArchiverFile archiverFile = new ArchiverFile( file, string, null, null );

        archiverFile.setFileMode( i );

        files.put( file, archiverFile );
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

    public void addConfiguredManifest( Manifest newManifest )
        throws ManifestException
    {
        manifest = newManifest;
    }

    public Manifest getManifest()
    {
        return manifest;
    }

    public class ArchiverFile
    {
        private File file;

        private String outputName;

        private String[] includes, excludes;

        private int fileMode;

        private ArchiverFile( File file, String outputName, String[] includes, String[] excludes )
        {
            this.file = file;
            this.outputName = outputName;
            this.includes = includes;
            this.excludes = excludes;
        }

        public File getFile()
        {
            return file;
        }

        public void setFile( File file )
        {
            this.file = file;
        }

        public String getOutputName()
        {
            return outputName;
        }

        public void setOutputName( String outputName )
        {
            this.outputName = outputName;
        }

        public String[] getIncludes()
        {
            return includes;
        }

        public void setIncludes( String[] includes )
        {
            this.includes = includes;
        }

        public String[] getExcludes()
        {
            return excludes;
        }

        public void setExcludes( String[] excludes )
        {
            this.excludes = excludes;
        }

        public int getFileMode()
        {
            return fileMode;
        }

        public void setFileMode( int fileMode )
        {
            this.fileMode = fileMode;
        }
    }
}
