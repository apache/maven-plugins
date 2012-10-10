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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.MatchPatterns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Publish a content to scm. By default, content is taken from default site staging directory
 * <code>${project.build.directory}/staging</code>.
 * Can be used without project, so usable to update any SCM with any content.
 */
@Mojo ( name = "publish-scm", aggregator = true, requiresProject = false )
public class ScmPublishPublishScmMojo
    extends AbstractScmPublishMojo
{
    /**
     * The content to be published.
     */
    @Parameter ( property = "scmpublish.content", defaultValue = "${project.build.directory}/staging" )
    private File content;

    /**
     */
    @Component
    protected MavenProject project;

    private List<File> deleted = new ArrayList<File>();

    private List<File> added = new ArrayList<File>();

    private List<File> updated = new ArrayList<File>();

    /**
     * Update scm checkout directory with content.
     *
     * @param checkout        the scm checkout directory
     * @param dir             the content to put in scm (can be <code>null</code>)
     * @param doNotDeleteDirs directory names that should not be deleted from scm even if not in new content:
     *                        used for modules, which content is available only when staging
     * @throws IOException
     */
    private void update( File checkout, File dir, List<String> doNotDeleteDirs )
        throws IOException
    {
        String[] files =
            checkout.list( new NotFileFilter( new NameFileFilter( scmProvider.getScmSpecificFilename() ) ) );

        Set<String> checkoutContent = new HashSet<String>( Arrays.asList( files ) );
        List<String> dirContent = ( dir != null ) ? Arrays.asList( dir.list() ) : Collections.<String>emptyList();

        Set<String> deleted = new HashSet<String>( checkoutContent );
        deleted.removeAll( dirContent );

        MatchPatterns ignoreDeleteMatchPatterns = null;
        List<String> pathsAsList = new ArrayList<String>( 0 );
        if ( ignorePathsToDelete != null && ignorePathsToDelete.length > 0 )
        {
            ignoreDeleteMatchPatterns = MatchPatterns.from( ignorePathsToDelete );
            pathsAsList = Arrays.asList( ignorePathsToDelete );
        }

        for ( String name : deleted )
        {
            if ( ignoreDeleteMatchPatterns != null && ignoreDeleteMatchPatterns.matches( name, true ) )
            {
                getLog().debug(
                    name + " match one of the patterns '" + pathsAsList + "': do not add to deleted files" );
                continue;
            }
            getLog().debug( "file marked for deletion:" + name );
            File file = new File( checkout, name );

            if ( ( doNotDeleteDirs != null ) && file.isDirectory() && ( doNotDeleteDirs.contains( name ) ) )
            {
                // ignore directory not available
                continue;
            }

            if ( file.isDirectory() )
            {
                update( file, null, null );
            }
            this.deleted.add( file );
        }

        for ( String name : dirContent )
        {
            File file = new File( checkout, name );
            File source = new File( dir, name );

            if ( source.isDirectory() )
            {
                if ( !checkoutContent.contains( name ) )
                {
                    this.added.add( file );
                    file.mkdir();
                }

                update( file, source, null );
            }
            else
            {
                if ( checkoutContent.contains( name ) )
                {
                    this.updated.add( file );
                }
                else
                {
                    this.added.add( file );
                }

                copyFile( source, file );
            }
        }
    }

    /**
     * Copy a file content, normalizing newlines when necessary.
     *
     * @param srcFile  the source file
     * @param destFile the destination file
     * @throws IOException
     * @see #requireNormalizeNewlines(File)
     */
    private void copyFile( File srcFile, File destFile )
        throws IOException
    {
        if ( requireNormalizeNewlines( srcFile ) )
        {
            copyAndNormalizeNewlines( srcFile, destFile );
        }
        else
        {
            FileUtils.copyFile( srcFile, destFile );
        }
    }

    /**
     * Copy and normalize newlines.
     *
     * @param srcFile  the source file
     * @param destFile the destination file
     * @throws IOException
     */
    private void copyAndNormalizeNewlines( File srcFile, File destFile )
        throws IOException
    {
        BufferedReader in = null;
        PrintWriter out = null;
        try
        {
            in = new BufferedReader( new InputStreamReader( new FileInputStream( srcFile ), siteOutputEncoding ) );
            out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( destFile ), siteOutputEncoding ) );
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
        }
    }

    public void scmPublishExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( siteOutputEncoding == null )
        {
            getLog().warn( "No output encoding, defaulting to UTF-8." );
            siteOutputEncoding = "utf-8";
        }

        if ( !content.exists() )
        {
            throw new MojoExecutionException( "Configured content directory does not exist: " + content );
        }

        if ( !content.canRead() )
        {
            throw new MojoExecutionException( "Can't read content directory: " + content );
        }

        checkoutExisting();

        try
        {
            logInfo( "Updating content..." );
            update( checkoutDirectory, content, ( project == null ) ? null : project.getModel().getModules() );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Could not copy content to scm checkout", ioe );
        }

        logInfo( "Publish files: %d addition(s), %d update(s), %d delete(s)", added.size(), updated.size(),
                 deleted.size() );

        if ( isDryRun() )
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
            addFiles( added );
        }

        if ( !deleted.isEmpty() )
        {
            deleteFiles( deleted );
        }

        logInfo( "Checking in SCM..." );
        checkinFiles();
    }
}
