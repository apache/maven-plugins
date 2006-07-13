package org.apache.maven.plugin.assembly.stubs;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class CountingArchiver implements Archiver
{
    
    private int dirsAdded = 0;
    private int filesAdded = 0;
    private File destFile;

    public void addDirectory( File arg0 )
        throws ArchiverException
    {
        dirsAdded++;
    }

    public int getFileCount()
    {
        return filesAdded;
    }
    
    public int getDirCount()
    {
        return dirsAdded;
    }

    public void addDirectory( File arg0, String arg1 )
        throws ArchiverException
    {
        System.out.println( "Adding dir: " + arg0 );
        dirsAdded++;
    }

    public void addDirectory( File arg0, String[] arg1, String[] arg2 )
        throws ArchiverException
    {
        System.out.println( "Adding dir: " + arg0 );
        dirsAdded++;
    }

    public void addDirectory( File arg0, String arg1, String[] arg2, String[] arg3 )
        throws ArchiverException
    {
        System.out.println( "Adding dir: " + arg0 );
        dirsAdded++;
    }

    public void addFile( File arg0, String arg1 )
        throws ArchiverException
    {
        System.out.println( "Adding file: " + arg0 );
        filesAdded++;
    }

    public void addFile( File arg0, String arg1, int arg2 )
        throws ArchiverException
    {
        System.out.println( "Adding file: " + arg0 );
        filesAdded++;
    }

    public void createArchive()
        throws ArchiverException, IOException
    {
    }

    public int getDefaultDirectoryMode()
    {
        return 0;
    }

    public int getDefaultFileMode()
    {
        return 0;
    }

    public File getDestFile()
    {
        return destFile;
    }

    public Map getFiles()
    {
        return null;
    }

    public boolean getIncludeEmptyDirs()
    {
        return false;
    }

    public void setDefaultDirectoryMode( int arg0 )
    {
    }

    public void setDefaultFileMode( int arg0 )
    {
    }

    public void setDestFile( File arg0 )
    {
        this.destFile = arg0;
    }

    public void setIncludeEmptyDirs( boolean arg0 )
    {
    }
    
}
