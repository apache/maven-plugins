package org.apache.maven.plugin.clean;

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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Os;

/**
 * Cleans directories.
 * 
 * @author Benjamin Bentmann
 */
class Cleaner
{

    private static final boolean ON_WINDOWS = Os.isFamily( Os.FAMILY_WINDOWS );

    private final Logger logDebug;

    private final Logger logInfo;

    private final Logger logVerbose;

    private final Logger logWarn;

    /**
     * Creates a new cleaner.
     * 
     * @param log The logger to use, may be <code>null</code> to disable logging.
     * @param verbose Whether to perform verbose logging.
     */
    public Cleaner( final Log log, boolean verbose )
    {
        logDebug = ( log == null || !log.isDebugEnabled() ) ? null : new Logger()
        {
            public void log( CharSequence message )
            {
                log.debug( message );
            }
        };

        logInfo = ( log == null || !log.isInfoEnabled() ) ? null : new Logger()
        {
            public void log( CharSequence message )
            {
                log.info( message );
            }
        };

        logWarn = ( log == null || !log.isWarnEnabled() ) ? null : new Logger()
        {
            public void log( CharSequence message )
            {
                log.warn( message );
            }
        };

        logVerbose = verbose ? logInfo : logDebug;
    }

    /**
     * Deletes the specified directories and its contents.
     * 
     * @param basedir The directory to delete, must not be <code>null</code>. Non-existing directories will be silently
     *            ignored.
     * @param selector The selector used to determine what contents to delete, may be <code>null</code> to delete
     *            everything.
     * @param followSymlinks Whether to follow symlinks.
     * @param failOnError Whether to abort with an exception in case a selected file/directory could not be deleted.
     * @param retryOnError Whether to undertake additional delete attempts in case the first attempt failed.
     * @throws IOException If a file/directory could not be deleted and <code>failOnError</code> is <code>true</code>.
     */
    public void delete( File basedir, Selector selector, boolean followSymlinks, boolean failOnError,
                        boolean retryOnError )
        throws IOException
    {
        if ( !basedir.isDirectory() )
        {
            if ( !basedir.exists() )
            {
                if ( logDebug != null )
                {
                    logDebug.log( "Skipping non-existing directory " + basedir );
                }
                return;
            }
            throw new IOException( "Invalid base directory " + basedir );
        }

        if ( logInfo != null )
        {
            logInfo.log( "Deleting " + basedir + ( selector != null ? " (" + selector + ")" : "" ) );
        }

        File file = followSymlinks ? basedir : basedir.getCanonicalFile();

        delete( file, "", selector, followSymlinks, failOnError, retryOnError );
    }

    /**
     * Deletes the specified file or directory.
     * 
     * @param file The file/directory to delete, must not be <code>null</code>. If <code>followSymlinks</code> is
     *            <code>false</code>, it is assumed that the parent file is canonical.
     * @param pathname The relative pathname of the file, using {@link File#separatorChar}, must not be
     *            <code>null</code>.
     * @param selector The selector used to determine what contents to delete, may be <code>null</code> to delete
     *            everything.
     * @param followSymlinks Whether to follow symlinks.
     * @param failOnError Whether to abort with an exception in case a selected file/directory could not be deleted.
     * @param retryOnError Whether to undertake additional delete attempts in case the first attempt failed.
     * @return The result of the cleaning, never <code>null</code>.
     * @throws IOException If a file/directory could not be deleted and <code>failOnError</code> is <code>true</code>.
     */
    private Result delete( File file, String pathname, Selector selector, boolean followSymlinks, boolean failOnError,
                           boolean retryOnError )
        throws IOException
    {
        Result result = new Result();

        boolean isDirectory = file.isDirectory();

        if ( isDirectory )
        {
            if ( selector == null || selector.couldHoldSelected( pathname ) )
            {
                File canonical = followSymlinks ? file : file.getCanonicalFile();
                if ( followSymlinks || file.equals( canonical ) )
                {
                    String[] filenames = canonical.list();
                    if ( filenames != null )
                    {
                        String prefix = ( pathname.length() > 0 ) ? pathname + File.separatorChar : "";
                        for ( int i = filenames.length - 1; i >= 0; i-- )
                        {
                            String filename = filenames[i];
                            File child = new File( canonical, filename );
                            result.update( delete( child, prefix + filename, selector, followSymlinks, failOnError,
                                                   retryOnError ) );
                        }
                    }
                }
                else if ( logDebug != null )
                {
                    logDebug.log( "Not recursing into symlink " + file );
                }
            }
            else if ( logDebug != null )
            {
                logDebug.log( "Not recursing into directory without included files " + file );
            }
        }

        if ( !result.excluded && ( selector == null || selector.isSelected( pathname ) ) )
        {
            if ( logVerbose != null )
            {
                if ( isDirectory )
                {
                    logVerbose.log( "Deleting directory " + file );
                }
                else if ( file.exists() )
                {
                    logVerbose.log( "Deleting file " + file );
                }
                else
                {
                    logVerbose.log( "Deleting dangling symlink " + file );
                }
            }
            result.failures += delete( file, failOnError, retryOnError );
        }
        else
        {
            result.excluded = true;
        }

        return result;
    }

    /**
     * Deletes the specified file, directory. If the path denotes a symlink, only the link is removed, its target is
     * left untouched.
     * 
     * @param file The file/directory to delete, must not be <code>null</code>.
     * @param failOnError Whether to abort with an exception in case the file/directory could not be deleted.
     * @param retryOnError Whether to undertake additional delete attempts in case the first attempt failed.
     * @return <code>0</code> if the file was deleted, <code>1</code> otherwise.
     * @throws IOException If a file/directory could not be deleted and <code>failOnError</code> is <code>true</code>.
     */
    private int delete( File file, boolean failOnError, boolean retryOnError )
        throws IOException
    {
        if ( !file.delete() )
        {
            boolean deleted = false;

            if ( retryOnError )
            {
                if ( ON_WINDOWS )
                {
                    // try to release any locks held by non-closed files
                    System.gc();
                }

                int[] delays = { 50, 250, 750 };
                for ( int i = 0; !deleted && i < delays.length; i++ )
                {
                    try
                    {
                        Thread.sleep( delays[i] );
                    }
                    catch ( InterruptedException e )
                    {
                        // ignore
                    }
                    deleted = file.delete() || !file.exists();
                }
            }
            else
            {
                deleted = !file.exists();
            }

            if ( !deleted )
            {
                if ( failOnError )
                {
                    throw new IOException( "Failed to delete " + file );
                }
                else
                {
                    if ( logWarn != null )
                    {
                        logWarn.log( "Failed to delete " + file );
                    }
                    return 1;
                }
            }
        }

        return 0;
    }

    private static class Result
    {

        public int failures;

        public boolean excluded;

        public void update( Result result )
        {
            failures += result.failures;
            excluded |= result.excluded;
        }

    }

    private static interface Logger
    {

        public void log( CharSequence message );

    }

}
