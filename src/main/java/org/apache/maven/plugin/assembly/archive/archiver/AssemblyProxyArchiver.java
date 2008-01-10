package org.apache.maven.plugin.assembly.archive.archiver;

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
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Delegating archiver implementation that supports:
 * <ul>
 * <li>dry-running (where the delegate archiver is never actually called)</li>
 * <li>prefixing (where all paths have a set global prefix prepended before addition)</li>
 * <li>duplication checks on archive additions (for archive-file path + prefix)</li>
 * </ul>
 *
 * @author jdcasey
 * @version $Id$
 */
public class AssemblyProxyArchiver
    implements Archiver
{

    private Archiver delegate;

    private String rootPrefix;

    private FileSelector[] selectors;

    private ThreadLocal inPublicApi = new ThreadLocal();

    private final Logger logger;

    private final boolean dryRun;

    private Set seenPaths = new HashSet();

    public AssemblyProxyArchiver( String rootPrefix, Archiver delegate, List containerDescriptorHandlers,
                                   List extraSelectors, List extraFinalizers, Logger logger )
    {
        this( rootPrefix, delegate, containerDescriptorHandlers, extraSelectors, extraFinalizers, logger, false );
    }

    public AssemblyProxyArchiver( String rootPrefix, Archiver delegate, List containerDescriptorHandlers,
                                   List extraSelectors, List extraFinalizers, Logger logger, boolean dryRun )
    {
        this.rootPrefix = rootPrefix;
        this.delegate = delegate;
        this.logger = logger;
        this.dryRun = dryRun;

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
        String archiveKey = getArchiveKey( archiveFile, prefix );
        if ( seenPaths.contains( archiveKey ) )
        {
            warn( "Archive: " + archiveFile + " has already been added. Skipping." );
            return;
        }

        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            debug( "Adding archived file-set in: " + archiveFile + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addArchivedFileSet( fs );
                seenPaths.add( archiveKey );
            }
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    private String getArchiveKey( File archiveFile, String prefix )
    {
        return archiveFile.getAbsolutePath() + ":" + prefix;
    }

    private void debug( String message )
    {
        if ( ( logger != null ) && logger.isDebugEnabled() )
        {
            logger.debug( message );
        }
    }

    private void warn( String message )
    {
        if ( ( logger != null ) && logger.isWarnEnabled() )
        {
            logger.warn( message );
        }
    }

    public void addArchivedFileSet( File archiveFile, String prefix )
        throws ArchiverException
    {
        String archiveKey = getArchiveKey( archiveFile, prefix );
        if ( seenPaths.contains( archiveKey ) )
        {
            warn( "Archive: " + archiveFile + " has already been added. Skipping." );
            return;
        }

        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setPrefix( rootPrefix + prefix );
            fs.setFileSelectors( selectors );

            debug( "Adding archived file-set in: " + archiveFile + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addArchivedFileSet( fs );
                seenPaths.add( archiveKey );
            }
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( File archiveFile, String[] includes, String[] excludes )
        throws ArchiverException
    {
        String archiveKey = getArchiveKey( archiveFile, "" );
        if ( seenPaths.contains( archiveKey ) )
        {
            warn( "Archive: " + archiveFile + " has already been added. Skipping." );
            return;
        }

        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setIncludes( includes );
            fs.setExcludes( excludes );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            debug( "Adding archived file-set in: " + archiveFile + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addArchivedFileSet( fs );
                seenPaths.add( archiveKey );
            }
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    public void addArchivedFileSet( File archiveFile )
        throws ArchiverException
    {
        String archiveKey = getArchiveKey( archiveFile, "" );
        if ( seenPaths.contains( archiveKey ) )
        {
            warn( "Archive: " + archiveFile + " has already been added. Skipping." );
            return;
        }

        inPublicApi.set( Boolean.TRUE );
        try
        {
            DefaultArchivedFileSet fs = new DefaultArchivedFileSet();

            fs.setArchive( archiveFile );
            fs.setPrefix( rootPrefix );
            fs.setFileSelectors( selectors );

            debug( "Adding archived file-set in: " + archiveFile + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addArchivedFileSet( fs );
                seenPaths.add( archiveKey );
            }
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

            debug( "Adding directory file-set in: " + directory + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addFileSet( fs );
            }
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

            debug( "Adding directory file-set in: " + directory + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addFileSet( fs );
            }
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

            debug( "Adding directory file-set in: " + directory + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addFileSet( fs );
            }
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

            debug( "Adding directory file-set in: " + directory + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addFileSet( fs );
            }
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
                debug( "Adding file: " + inputFile + " to archive location: " + rootPrefix + destFileName );

                if ( dryRun )
                {
                    debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
                }
                else
                {
                    delegate.addFile( inputFile, rootPrefix + destFileName, permissions );
                }
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
                debug( "Adding file: " + inputFile + " to archive location: " + rootPrefix + destFileName );

                if ( dryRun )
                {
                    debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
                }
                else
                {
                    delegate.addFile( inputFile, rootPrefix + destFileName );
                }
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
            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.createArchive();
            }
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
        throw new UnsupportedOperationException( "Undocumented feature of plexus-archiver; this is not yet supported." );
    }

    public void addArchivedFileSet( ArchivedFileSet fileSet )
        throws ArchiverException
    {
        String archiveKey = getArchiveKey( fileSet.getArchive(), "" );
        if ( seenPaths.contains( archiveKey ) )
        {
            warn( "Archive: " + fileSet.getArchive() + " has already been added. Skipping." );
            return;
        }

        inPublicApi.set( Boolean.TRUE );
        try
        {
            PrefixedArchivedFileSet fs = new PrefixedArchivedFileSet( fileSet, rootPrefix, selectors );

            debug( "Adding archived file-set in: " + fileSet.getArchive() + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addArchivedFileSet( fs );
                seenPaths.add( archiveKey );
            }
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
            PrefixedFileSet fs = new PrefixedFileSet( fileSet, rootPrefix, selectors );

            debug( "Adding file-set in: " + fileSet.getDirectory() + " to archive location: " + fs.getPrefix() );

            if ( dryRun )
            {
                debug( "DRY RUN: Skipping delegated call to: " + getMethodName() );
            }
            else
            {
                delegate.addFileSet( fs );
            }
        }
        finally
        {
            inPublicApi.set( null );
        }
    }

    private String getMethodName()
    {
        NullPointerException npe = new NullPointerException();
        StackTraceElement[] trace = npe.getStackTrace();

        StackTraceElement methodElement = trace[1];

        return methodElement.getMethodName() + " (archiver line: " + methodElement.getLineNumber() + ")";
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
                    FileSelector selector = selectors[i];

                    try
                    {
                        if ( !selector.isSelected( fileInfo ) )
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
