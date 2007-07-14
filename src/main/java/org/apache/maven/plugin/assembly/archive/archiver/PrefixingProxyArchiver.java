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
        DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

        fs.setArchive( archiveFile );
        fs.setIncludes( includes );
        fs.setExcludes( excludes );
        fs.setPrefix( rootPrefix + prefix );
        fs.setFileSelectors( selectors );

        delegate.addArchivedFileSet( fs );
    }

    public void addArchivedFileSet( File archiveFile, String prefix )
        throws ArchiverException
    {
        DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

        fs.setArchive( archiveFile );
        fs.setPrefix( rootPrefix + prefix );
        fs.setFileSelectors( selectors );

        delegate.addArchivedFileSet( fs );
    }

    public void addArchivedFileSet( File archiveFile, String[] includes, String[] excludes )
        throws ArchiverException
    {
        DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

        fs.setArchive( archiveFile );
        fs.setIncludes( includes );
        fs.setExcludes( excludes );
        fs.setPrefix( rootPrefix );
        fs.setFileSelectors( selectors );

        delegate.addArchivedFileSet( fs );
    }

    public void addArchivedFileSet( File archiveFile )
        throws ArchiverException
    {
        DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

        fs.setArchive( archiveFile );
        fs.setPrefix( rootPrefix );
        fs.setFileSelectors( selectors );

        delegate.addArchivedFileSet( fs );
    }

    public void addDirectory( File directory, String prefix, String[] includes, String[] excludes )
        throws ArchiverException
    {
        DefaultFileSet fs = new DefaultFileSet();

        fs.setDirectory( directory );
        fs.setIncludes( includes );
        fs.setExcludes( excludes );
        fs.setPrefix( rootPrefix + prefix );
        fs.setFileSelectors( selectors );

        delegate.addFileSet( fs );
    }

    public void addDirectory( File directory, String prefix )
        throws ArchiverException
    {
        DefaultFileSet fs = new DefaultFileSet();

        fs.setDirectory( directory );
        fs.setPrefix( rootPrefix + prefix );
        fs.setFileSelectors( selectors );

        delegate.addFileSet( fs );
    }

    public void addDirectory( File directory, String[] includes, String[] excludes )
        throws ArchiverException
    {
        DefaultFileSet fs = new DefaultFileSet();

        fs.setDirectory( directory );
        fs.setIncludes( includes );
        fs.setExcludes( excludes );
        fs.setPrefix( rootPrefix );
        fs.setFileSelectors( selectors );

        delegate.addFileSet( fs );
    }

    public void addDirectory( File directory )
        throws ArchiverException
    {
        DefaultFileSet fs = new DefaultFileSet();

        fs.setDirectory( directory );
        fs.setPrefix( rootPrefix );
        fs.setFileSelectors( selectors );

        delegate.addFileSet( fs );
    }

    public void addFile( File inputFile, String destFileName, int permissions )
        throws ArchiverException
    {
        if ( acceptFile( inputFile ) )
        {
            delegate.addFile( inputFile, rootPrefix + destFileName, permissions );
        }
    }

    public void addFile( File inputFile, String destFileName )
        throws ArchiverException
    {
        if ( acceptFile( inputFile ) )
        {
            delegate.addFile( inputFile, rootPrefix + destFileName );
        }
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
        delegate.addArchivedFileSet( new PrefixedArchivedFileSet( fileSet, rootPrefix, selectors ) );
    }

    public void addFileSet( FileSet fileSet )
        throws ArchiverException
    {
        delegate.addFileSet( new PrefixedFileSet( fileSet, rootPrefix, selectors ) );
    }

    private boolean acceptFile( File inputFile )
        throws ArchiverException
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
