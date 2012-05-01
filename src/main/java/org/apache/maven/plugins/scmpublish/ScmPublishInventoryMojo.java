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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
