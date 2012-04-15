package org.apache.maven.plugins.scmpublish;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;

/**
 * Compare the list of files now on disk to the original inventory, then fire off scm adds and deletes as needed.
 * 
 * @goal publish
 * @phase post-site
 * @aggregate
 */
public class ScmPublishPublishMojo
    extends AbstractScmPublishMojo
{

    /**
     * Display list of added, deleted, and changed files, but do not do any actual SCM operations.
     * 
     * @parameter expression="${scmpublish.dryRun}"
     */
    private boolean dryRun;

    /**
     * Run add and delete commands, but leave the actually checkin for the user to run manually.
     * 
     * @parameter expression="${scmpublish.skipCheckin}"
     */
    private boolean skipCheckin;

    /**
     * SCM log/checkin comment for this publication.
     * @parameter expression="${scmpublish.checkinComment}"
     */
    private String checkinComment;

    /**
     * Filename extensions of files which need new line normalization.
     */
    private final static String[] NORMALIZE_EXTENSIONS = { "html", "css", "js" };

    private File relativize( File base, File file )
    {
        return new File( base.toURI().relativize( file.toURI() ).getPath() );
    }

    private boolean requireNormalizeNewlines( File f )
        throws IOException
    {
        return FilenameUtils.isExtension( f.getName(), NORMALIZE_EXTENSIONS );
    }

    private void normalizeNewlines( File f )
        throws IOException
    {
        File tmpFile = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try
        {
            tmpFile = File.createTempFile( "maven-site-scm-plugin-", ".tmp" );
            FileUtils.copyFile( f, tmpFile );
            in = new BufferedReader( new InputStreamReader( new FileInputStream( tmpFile ), siteOutputEncoding ) );
            out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( f ), siteOutputEncoding ) );
            String line;
            while ( ( line = in.readLine() ) != null ) 
            {
                if ( in.ready() )
                {
                    out.println( line );
                }
                else
                {
                    out.print( line );
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly( out );
            IOUtils.closeQuietly( in );
            FileUtils.deleteQuietly( tmpFile );
        }
    }

    private void normalizeNewLines( Set<File> files )
        throws MojoFailureException
    {
        for ( File f : files )
        {
            try
            {
                if ( requireNormalizeNewlines( f ) )
                {
                    normalizeNewlines( f );
                }
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to normalize newlines in " + f.getAbsolutePath(), e );
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( siteOutputEncoding == null )
        {
            getLog().warn( "No output encoding, defaulting to UTF-8." );
            siteOutputEncoding = "utf-8";
        }

        // read in the list left behind by prepare; fail if it's not there.
        List<File> inventory = readInventory();

        // setup the scm plugin with help from release plugin utilities
        try
        {
            setupScm();
        }
        catch ( ReleaseScmRepositoryException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        // what files are in stock now?
        Set<File> added = new HashSet<File>();
        Collection<File> newInventory = FileUtils.listFiles( checkoutDirectory, new DotFilter(), new DotFilter() );
        added.addAll( newInventory );

        Set<File> deleted = new HashSet<File>();
        deleted.addAll( inventory );
        deleted.removeAll( added ); // old - new = deleted. (Added is the complete new inventory at this point.)
        added.removeAll( inventory ); // new - old = added.

        Set<File> updated = new HashSet<File>();
        updated.addAll( newInventory );
        updated.retainAll( inventory ); // set intersection

        logInfo( "Publish files: %d addition(s), %d update(s), %d delete(s)", added.size(), updated.size(),
                 deleted.size() );

        if ( dryRun )
        {
            for ( File addedFile : added )
            {
                logInfo( "Added %s", addedFile.getAbsolutePath() );
            }
            for ( File deletedFile : deleted )
            {
                logInfo( "Deleted %s", deletedFile.getAbsolutePath() );
            }
            for ( File updatedFile : updated )
            {
                logInfo( "Updated %s", updatedFile.getAbsolutePath() );
            }
            return;
        }

        if ( !added.isEmpty() )
        {
            normalizeNewLines( added );

            addFiles( added );
        }

        if ( !deleted.isEmpty() )
        {
            deleteFiles( deleted );
        }

        normalizeNewLines( updated );

        if ( !skipCheckin )
        {
            checkinFiles();
        }
    }

    private void checkinFiles()
        throws MojoExecutionException
    {
        if ( checkinComment == null )
        {
            checkinComment = "Site checkin for project " + project.getName();
        }
        ScmFileSet updatedFileSet = new ScmFileSet( checkoutDirectory );
        try
        {
            CheckInScmResult checkinResult = scmProvider.checkIn( scmRepository, updatedFileSet, checkinComment );
            if ( !checkinResult.isSuccess() )
            {
                logError( "checkin operation failed: %s",
                          checkinResult.getProviderMessage() + " " + checkinResult.getCommandOutput() );
                throw new MojoExecutionException( "Failed to checkin files: " + checkinResult.getProviderMessage()
                    + " " + checkinResult.getCommandOutput() );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to perform checkin SCM", e );
        }
    }

    private void deleteFiles( Set<File> deleted )
        throws MojoExecutionException
    {
        List<File> deletedList = new ArrayList<File>();
        for ( File f : deleted )
        {
            deletedList.add( relativize( checkoutDirectory, f ) );
        }
        ScmFileSet deletedFileSet = new ScmFileSet( checkoutDirectory, deletedList );
        try
        {
            RemoveScmResult deleteResult =
                scmProvider.remove( scmRepository, deletedFileSet, "Deleting obsolete site files." );
            if ( !deleteResult.isSuccess() )
            {
                logError( "delete operation failed: %s",
                          deleteResult.getProviderMessage() + " " + deleteResult.getCommandOutput() );
                throw new MojoExecutionException( "Failed to delete files: " + deleteResult.getProviderMessage()
                    + " " + deleteResult.getCommandOutput() );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to delete removed files to SCM", e );
        }
    }

    private void addFiles( Set<File> added )
        throws MojoFailureException, MojoExecutionException
    {
        List<File> addedList = new ArrayList<File>();
        Set<File> createdDirs = new HashSet<File>();
        Set<File> dirsToAdd = new TreeSet<File>();

        createdDirs.add( relativize( checkoutDirectory, checkoutDirectory ) );

        for ( File f : added )
        {
            for ( File dir = f.getParentFile(); !dir.equals( checkoutDirectory ); dir = dir.getParentFile() )
            {
                File relativized = relativize( checkoutDirectory, dir );
                //  we do the best we can with the directories
                if ( createdDirs.add( relativized ) )
                {
                    dirsToAdd.add( relativized );
                }
                else
                {
                    break;
                }
            }
            addedList.add( relativize( checkoutDirectory, f ) );
        }

        for ( File relativized : dirsToAdd )
        {
            try 
            {
                ScmFileSet fileSet = new ScmFileSet( checkoutDirectory , relativized );
                AddScmResult addDirResult = scmProvider.add( scmRepository, fileSet, "Adding directory" );
                if ( !addDirResult.isSuccess() )
                {
                    getLog().debug( " Error adding directory " + relativized + " " + addDirResult.getCommandOutput() );
                }
            }
            catch ( ScmException e )
            {
                //
            }
        }

        ScmFileSet addedFileSet = new ScmFileSet( checkoutDirectory, addedList );
        try
        {
            AddScmResult addResult = scmProvider.add( scmRepository, addedFileSet, "Adding new site files." );
            if ( !addResult.isSuccess() )
            {
                logError( "add operation failed: %s",
                          addResult.getProviderMessage() + " " + addResult.getCommandOutput() );
                throw new MojoExecutionException( "Failed to add new files: " + addResult.getProviderMessage()
                    + " " + addResult.getCommandOutput() );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to add new files to SCM", e );
        }
    }
}
