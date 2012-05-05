package org.apache.maven.plugins.scmpublish;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

/**
 * Publish a content to scm in one step. By default, site staging content is published.
 * Could be extended to work without project, so usable to update any SCM with any content.
 * 
 * @goal publish-scm
 * @aggregate
 * @requiresProject false
 */
public class ScmPublishPublishScmMojo
    extends ScmPublishPublishMojo
{
    /**
     * The content to be published.
     * 
     * @parameter expression="${scmpublish.content}" default-value="${project.build.directory}/staging"
     */
    private File content;

    private List<File> deleted = new ArrayList<File>();
    private List<File> added = new ArrayList<File>();
    private List<File> updated = new ArrayList<File>();

    private void update( File checkout, File dir )
        throws IOException
    {
        Set<String> checkoutContent =
            new HashSet<String>( ScmPublishInventory.listInventory( checkout, scmProvider.getScmSpecificFilename() ) );
        List<String> dirContent = ( dir != null ) ? Arrays.asList( dir.list() ) : Collections.<String>emptyList();

        Set<String> deleted = new HashSet<String>( checkoutContent );
        deleted.removeAll( dirContent );

        for ( String name : deleted )
        {
            File file = new File( checkout, name );
            if ( file.isDirectory() )
            {
                update( file, null );
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

                update( file, source );
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

    private void copyFile( File srcFile, File destFile )
        throws IOException
    {
        if ( requireNormalizeNewlines( srcFile ) )
        {
            normalizeNewlines( srcFile, destFile );
        }
        else
        {
            FileUtils.copyFile( srcFile, destFile );
        }
    }

    private void normalizeNewlines( File srcFile, File destFile )
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

        checkoutExisting();

        try
        {
            logInfo( "Updating content..." );
            update( checkoutDirectory, content );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "could not copy content to scm checkout", ioe );
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
