package org.apache.maven.plugin.assembly.archive.archiver;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PrefixingProxyArchiver
    implements Archiver
{

    private Archiver delegate;
    private String rootPrefix;

    public PrefixingProxyArchiver( String rootPrefix, Archiver delegate )
    {
        this.rootPrefix = rootPrefix;
        this.delegate = delegate;

        if ( !rootPrefix.endsWith( "/" ) )
        {
            this.rootPrefix += "/";
        }
    }

    public void addArchivedFileSet( File archiveFile, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        delegate.addArchivedFileSet( archiveFile, rootPrefix + prefix, includes, excludes );
    }

    public void addArchivedFileSet( File archiveFile, String prefix )
        throws ArchiverException
    {
        delegate.addArchivedFileSet( archiveFile, rootPrefix + prefix );
    }

    public void addArchivedFileSet( File archiveFile, String[] includes, String[] excludes )
        throws ArchiverException
    {
        delegate.addArchivedFileSet( archiveFile, rootPrefix, includes, excludes );
    }

    public void addArchivedFileSet( File archiveFile )
        throws ArchiverException
    {
        delegate.addArchivedFileSet( archiveFile, rootPrefix );
    }

    public void addDirectory( File directory, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        delegate.addDirectory( directory, rootPrefix + prefix, includes, excludes );
    }

    public void addDirectory( File directory, String prefix )
        throws ArchiverException
    {
        delegate.addDirectory( directory, rootPrefix + prefix );
    }

    public void addDirectory( File directory, String[] includes, String[] excludes )
        throws ArchiverException
    {
        delegate.addDirectory( directory, rootPrefix, includes, excludes );
    }

    public void addDirectory( File directory )
        throws ArchiverException
    {
        delegate.addDirectory( directory, rootPrefix );
    }

    public void addFile( File inputFile, String destFileName, int permissions )
        throws ArchiverException
    {
        delegate.addFile( inputFile, rootPrefix + destFileName, permissions );
    }

    public void addFile( File inputFile, String destFileName )
        throws ArchiverException
    {
        delegate.addFile( inputFile, rootPrefix + destFileName );
    }

    public void createArchive()
        throws ArchiverException, IOException
    {
        delegate.createArchive();
    }

    public int getDefaultDirectoryMode()
    {
        return delegate.getDefaultDirectoryMode();
    }

    public int getDefaultFileMode()
    {
        return delegate.getDefaultFileMode();
    }

    public File getDestFile()
    {
        return delegate.getDestFile();
    }

    public Map getFiles()
    {
        return delegate.getFiles();
    }

    public boolean getIncludeEmptyDirs()
    {
        return delegate.getIncludeEmptyDirs();
    }

    public boolean isForced()
    {
        return delegate.isForced();
    }

    public boolean isSupportingForced()
    {
        return delegate.isSupportingForced();
    }

    public void setDefaultDirectoryMode( int mode )
    {
        delegate.setDefaultDirectoryMode( mode );
    }

    public void setDefaultFileMode( int mode )
    {
        delegate.setDefaultFileMode( mode );
    }

    public void setDestFile( File destFile )
    {
        delegate.setDestFile( destFile );
    }

    public void setForced( boolean forced )
    {
        delegate.setForced( forced );
    }

    public void setIncludeEmptyDirs( boolean includeEmptyDirs )
    {
        delegate.setIncludeEmptyDirs( includeEmptyDirs );
    }

    public void setDotFileDirectory( File dotFileDirectory )
    {
        throw new UnsupportedOperationException( "Undocumented feature of plexus-archiver; this is not yet supported." );
    }

    public void addArchivedFileSet( ArchivedFileSet fileSet )
        throws ArchiverException
    {
        delegate.addArchivedFileSet( new PrefixedArchivedFileSet( fileSet, rootPrefix ) );
    }

    public void addFileSet( FileSet fileSet )
        throws ArchiverException
    {
        delegate.addFileSet( fileSet );
    }

}
