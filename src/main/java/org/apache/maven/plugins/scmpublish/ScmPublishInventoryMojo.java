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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Prepare a directory for version-managed site generation. This checks out the specified directory from the SCM,
 * then takes inventory of all the resulting files then deletes every files.
 * This inventory then allows the 'publish' target to tee up deletions
 * as well as modifications and additions.
 * 
 * There's an assumption here that an entire directory in SCM is dedicated to
 * the publication process for this project. In the aggregate case, this is going to take some doing. 
 * 
 * If we allow this to be non-aggregate, then each module has to configure pathnames, which would be a pain. So
 * we assume that in an aggregate project this runs once, at the top -- then all of the projects site-deploy
 * into the file: url this creates. 
 * 
 * 
 * TODO: we want
 * multiple includes/excludes, but the scm API doesn't go there.
 * 
 * @goal prepare
 * @phase pre-site
 * @aggregate
 */
public class ScmPublishInventoryMojo
    extends AbstractScmPublishMojo
{
    private void checkoutExisting()
        throws MojoExecutionException
    {
        logInfo( "Checking out the pub tree ..." );

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                logError( e.getMessage() );

                throw new MojoExecutionException( "Unable to remove old checkout directory: " + e.getMessage(), e );
            }
        }

        checkoutDirectory.mkdirs();

        CheckOutScmResult scmResult;

        try
        {
            ScmFileSet fileSet = new ScmFileSet( checkoutDirectory, includes, excludes );
            scmResult = scmProvider.checkOut( scmRepository, fileSet );
        }
        catch ( ScmException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }

        String scmRelativePathProjectDirectory = scmResult.getRelativePathProjectDirectory();
        if ( StringUtils.isEmpty( scmRelativePathProjectDirectory ) )
        {
            String basedir;
            try
            {
                basedir = ReleaseUtil.getCommonBasedir( reactorProjects );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Exception occurred while calculating common basedir: "
                    + e.getMessage(), e );
            }

            String rootProjectBasedir = rootProject.getBasedir().getAbsolutePath();
            try
            {
                if ( ReleaseUtil.isSymlink( rootProject.getBasedir() ) )
                {
                    rootProjectBasedir = rootProject.getBasedir().getCanonicalPath();
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            if ( rootProjectBasedir.length() > basedir.length() )
            {
                scmRelativePathProjectDirectory = rootProjectBasedir.substring( basedir.length() + 1 );
            }
        }

        if ( !scmResult.isSuccess() )
        {
            logError( scmResult.getProviderMessage() );

            throw new MojoExecutionException( "Unable to checkout from SCM" + "\nProvider message:\n"
                + scmResult.getProviderMessage() + "\nCommand output:\n" + scmResult.getCommandOutput() );
        }
    }

    /**
     * Clear out the data, so we can tell what's left after the run of the site plugin.
     * For now, don't bother with deleting empty directories. They are fairly harmless,
     * and leaving them around allows this to work with pre-1.7 svn.
     */
    private void deleteInventory( List<File> inventory ) 
    {
        for ( File f : inventory )
        {
            if ( f.isFile() )
            {
                FileUtils.deleteQuietly( f );
            }
        }
    }

    public void scmPublishExecute()
        throws MojoExecutionException, MojoFailureException
    {
        checkoutExisting();

        List<File> inventory = ScmPublishInventory.listInventoryFiles( checkoutDirectory );

        ScmPublishInventory.writeInventory( inventory, inventoryFile );

        deleteInventory( inventory );
    }
}
