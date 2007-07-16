package org.apache.maven.plugin.assembly.archive.archiver;

import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.FinalizerEnabled;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PrefixingProxyArchiver
    implements Archiver
{

    private Archiver delegate;

    private String rootPrefix;

    private FileSelector[] selectors;

    private ThreadLocal inPublicApi = new ThreadLocal();

    public PrefixingProxyArchiver( String rootPrefix, Archiver delegate, List containerDescriptorHandlers,
                                   List extraSelectors, List extraFinalizers )
    {
        this.rootPrefix = rootPrefix;
        this.delegate = delegate;

        if ( !"".equals( rootPrefix ) && !rootPrefix.endsWith( "/" ) )
        {
            this.rootPrefix += "/";
        }

        List selectors = new ArrayList();

        boolean isFinalizerEnabled = ( delegate instanceof FinalizerEnabled );

        if ( containerDescriptorHandlers != null )
        {
            for ( Iterator it = containerDescriptorHandlers.iterator(); it.hasNext(); )
            {
                ContainerDescriptorHandler handler = (ContainerDescriptorHandler) it.next();

                selectors.add( handler );

                if ( isFinalizerEnabled )
                {
                    ( (FinalizerEnabled) delegate ).addArchiveFinalizer( handler );
                }
            }
        }

        if ( extraSelectors != null )
        {
            for ( Iterator it = extraSelectors.iterator(); it.hasNext(); )
            {
                FileSelector selector = (FileSelector) it.next();
                selectors.add( selector );
            }
        }

        if ( ( extraFinalizers != null ) && isFinalizerEnabled )
        {
            for ( Iterator it = extraFinalizers.iterator(); it.hasNext(); )
            {
                ( (FinalizerEnabled) delegate ).addArchiveFinalizer( (ArchiveFinalizer) it.next() );
            }
        }

        if ( !selectors.isEmpty() )
        {
            this.selectors = (FileSelector[]) selectors.toArray( new FileSelector[0] );
        }
    }

    public void addArchivedFileSet( File archiveFile, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            delegate.addArchivedFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( File archiveFile, String prefix )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            delegate.addArchivedFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( File archiveFile, String[] includes, String[] excludes )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            delegate.addArchivedFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( File archiveFile )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            delegate.addArchivedFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addDirectory( File directory, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultFileSet fs = new DefaultFileSet();

            fs.setDirectory( directory );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            delegate.addFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addDirectory( File directory, String prefix )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultFileSet fs = new DefaultFileSet();

            fs.setDirectory( directory );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            delegate.addFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addDirectory( File directory, String[] includes, String[] excludes )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultFileSet fs = new DefaultFileSet();

            fs.setDirectory( directory );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            delegate.addFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addDirectory( File directory )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultFileSet fs = new DefaultFileSet();

            fs.setDirectory( directory );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            delegate.addFileSet( fs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addFile( File inputFile, String destFileName, int permissions )
        throws ArchiverException
    {
        if ( acceptFile( inputFile ) )
        {
            inPublicApi.set( Boolean.TRUE );
            try
            {
                delegate.addFile( inputFile, rootPrefix + destFileName, permissions );
            }
            finally
            {
                inPublicApi.set( null );
            }
        }
    }

    public void addFile( File inputFile, String destFileName )
        throws ArchiverException
    {
        if ( acceptFile( inputFile ) )
        {
            inPublicApi.set( Boolean.TRUE );
            try
            {
                delegate.addFile( inputFile, rootPrefix + destFileName );
            }
            finally
            {
                inPublicApi.set( null );
            }
        }
    }

    public void createArchive()
        throws ArchiverException, IOException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.createArchive();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public int getDefaultDirectoryMode()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.getDefaultDirectoryMode();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public int getDefaultFileMode()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.getDefaultFileMode();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public File getDestFile()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.getDestFile();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public Map getFiles()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.getFiles();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public boolean getIncludeEmptyDirs()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.getIncludeEmptyDirs();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public boolean isForced()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.isForced();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public boolean isSupportingForced()
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            return delegate.isSupportingForced();
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setDefaultDirectoryMode( int mode )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.setDefaultDirectoryMode( mode );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setDefaultFileMode( int mode )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.setDefaultFileMode( mode );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setDestFile( File destFile )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.setDestFile( destFile );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setForced( boolean forced )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.setForced( forced );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setIncludeEmptyDirs( boolean includeEmptyDirs )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.setIncludeEmptyDirs( includeEmptyDirs );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void setDotFileDirectory( File dotFileDirectory )
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            throw new UnsupportedOperationException(
                                                     "Undocumented feature of plexus-archiver; this is not yet supported." );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( ArchivedFileSet fileSet )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.addArchivedFileSet( new PrefixedArchivedFileSet( fileSet, rootPrefix, selectors ) );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addFileSet( FileSet fileSet )
        throws ArchiverException
    {
        inPublicApi.set( Boolean.TRUE );
        try
        {
            delegate.addFileSet( new PrefixedFileSet( fileSet, rootPrefix, selectors ) );
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    private boolean acceptFile( File inputFile )
        throws ArchiverException
    {
        if ( Boolean.TRUE != inPublicApi.get() )
        {
            if ( selectors != null )
            {
                FileInfo fileInfo = new DefaultFileInfo( inputFile );

                for ( int i = 0; i < selectors.length; i++ )
                {
                    try
                    {
                        if ( !selectors[i].isSelected( fileInfo ) )
                        {
                            return false;
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new ArchiverException( "Error processing file: " + inputFile + " using selector: "
                                                     + selectors[i], e );
                    }
                }
            }
        }

        return true;
    }

    private static final class DefaultFileInfo
        implements FileInfo
    {

        private final File inputFile;

        DefaultFileInfo( File inputFile )
        {
            this.inputFile = inputFile;
        }

        public InputStream getContents()
            throws IOException
        {
            return new FileInputStream( inputFile );
        }

        public String getName()
        {
            return inputFile.getName();
        }

        public boolean isDirectory()
        {
            return inputFile.isDirectory();
        }

        public boolean isFile()
        {
            return inputFile.isFile();
        }

    }

}
