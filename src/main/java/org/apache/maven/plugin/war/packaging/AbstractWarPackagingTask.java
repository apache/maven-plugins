package org.apache.maven.plugin.war.packaging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractWarPackagingTask
    implements WarPackagingTask
{


    /**
     * Copies the files if possible.
     * <p/>
     * Copy uses a first-win strategy: files that have already been copied by previous
     * tasks are ignored. This method makes sure to update the list of protected files
     * which gives the list of files that have already been copied.
     *
     * @param context        the context to use
     * @param sourceBaseDir  the base directory from which the <tt>sourceFilesSet</tt> will be copied
     * @param sourceFilesSet the files to be copied
     * @throws IOException if an error occured while copying the files
     */
    protected void copyFiles( WarPackagingContext context, File sourceBaseDir, PathSet sourceFilesSet )
        throws IOException
    {
        for ( Iterator iter = sourceFilesSet.iterator(); iter.hasNext(); )
        {
            String fileToCopyName = (String) iter.next();
            if ( !context.getProtectedFiles().contains( fileToCopyName ) )
            {
                File sourceFile = new File( sourceBaseDir, fileToCopyName );
                File targetFile = new File( context.getWebAppDirectory(), fileToCopyName );
                copyFile( sourceFile, targetFile );

                // Add the file to the protected list
                context.getProtectedFiles().add( fileToCopyName );
                context.getLogger().debug( " + " + fileToCopyName + " has been copied." );
            }
            else
            {
                context.getLogger().debug(
                    " - " + fileToCopyName + " wasn't copied because it has already been packaged." );
            }
        }
    }


    /**
     * Unpacks the specified file to the specified directory.
     *
     * @param context         the packaging context
     * @param file            the file to unpack
     * @param unpackDirectory the directory to use for th unpacked file
     * @throws MojoExecutionException if an error occured while unpacking the file
     */
    protected void doUnpack( WarPackagingContext context, File file, File unpackDirectory )
        throws MojoExecutionException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver = context.getArchiverManager().getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( unpackDirectory );
            unArchiver.setOverwrite( true );
            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file[" + file.getAbsolutePath() + "]" + "to[" +
                unpackDirectory.getAbsolutePath() + "]", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file[" + file.getAbsolutePath() + "]" + "to[" +
                unpackDirectory.getAbsolutePath() + "]", e );
        }
        catch ( NoSuchArchiverException e )
        {
            context.getLogger().warn( "Skip unpacking dependency file[" + file.getAbsolutePath() +
                " with unknown extension[" + archiveExt + "]" );
        }
    }

    /**
     * Copy file from source to destination. The directories up to <code>destination</code>
     * will be created if they don't already exist. <code>destination</code> will be
     * overwritten if it already exists.
     * <p/>
     * TODO: Remove this method when Maven moves to plexus-utils version 1.4
     *
     * @param source      an existing non-directory <code>File</code> to copy bytes from
     * @param destination a non-directory <code>File</code> to write bytes to (possibly overwriting).
     * @throws IOException                   if <code>source</code> does not exist, <code>destination</code> cannot
     *                                       be written to, or an IO error occurs during copying
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     */
    protected static void copyFile( File source, File destination )
        throws IOException
    {
        FileUtils.copyFile( source.getCanonicalFile(), destination );
        // preserve timestamp
        destination.setLastModified( source.lastModified() );
    }

}
