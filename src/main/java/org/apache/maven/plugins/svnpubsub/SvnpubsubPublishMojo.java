package org.apache.maven.plugins.svnpubsub;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;

/**
 * Compare the list of files now on disk to the original inventory. Fire off scm adds and deletes as needed.
 * 
 * @goal publish
 * @phase post-site
 * @aggregate
 */
public class SvnpubsubPublishMojo
    extends CommonSvnpubsubMojo
{

    /**
     * Display list of added, deleted, and changed files, but do not do any actual SCM operations.
     * 
     * @parameter expression="${svnpubsub.dryRun}"
     */
    private boolean dryRun;

    /**
     * Run add and delete commands, but leave the actually checkin for the user to run manually.
     * 
     * @parameter expression="${svnpubsub.skipCheckin}"
     */
    private boolean skipCheckin;

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // read in the list left behind by prepare; fail if it's not there.
        readInventory();
        // setup the scm plugin with help from release plugin utilities
        try
        {
            setupScm();
        }
        catch ( ReleaseScmRepositoryException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }

        // what files are in stock now?
        Set<File> added = new HashSet<File>();
        Collection<File> newInventory = FileUtils.listFiles( checkoutDirectory, new DotFilter(), new DotFilter() );
        added.addAll( newInventory );

        /*
         * I originally thought that this was a 'Diff' problem, but I don't think so now. I think this is most easily
         * managed with set membership.
         */
        Set<File> deleted = new HashSet<File>();
        deleted.addAll( inventory );
        deleted.removeAll( added ); // old - new = deleted. (Added is the complete new inventory at this point.)
        added.removeAll( inventory ); // new - old = added.

        Set<File> updated = new HashSet<File>();
        updated.addAll( newInventory );
        updated.retainAll( inventory ); // set intersection

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
            List<File> addedList = new ArrayList<File>();
            addedList.addAll( added );
            ScmFileSet addedFileSet = new ScmFileSet( checkoutDirectory, addedList );
            try
            {
                scmProvider.add( scmRepository, addedFileSet, "Adding new site files." );
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Failed to add new files to SCM", e );
            }
        }

        if ( !deleted.isEmpty() )
        {
            List<File> deletedList = new ArrayList<File>();
            deletedList.addAll( deleted );
            ScmFileSet deletedFileSet = new ScmFileSet( checkoutDirectory, deletedList );
            try
            {
                scmProvider.remove( scmRepository, deletedFileSet, "Deleting obsolete site files." );
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Failed to delete removed files to SCM", e );
            }
        }

        if ( !skipCheckin )
        {
            ScmFileSet updatedFileSet = new ScmFileSet( checkoutDirectory );
            try
            {
                scmProvider.checkIn( scmRepository, updatedFileSet, "Checking in site." );
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Failed to perform checkin SCM", e );
            }
        }
    }

}
