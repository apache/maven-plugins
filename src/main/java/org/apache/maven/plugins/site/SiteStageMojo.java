package org.apache.maven.plugins.site;

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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Generates a site in a local staging or mock directory based on the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM.
 * <p>
 * It can be used to test that links between module sites in a multi module
 * build works.
 * </p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage
 * @requiresDependencyResolution test
 */
public class SiteStageMojo
    extends AbstractDeployMojo
{
    protected static final String DEFAULT_STAGING_DIRECTORY = "staging";

    /**
     * Staging directory location. This needs to be an absolute path, like
     * <code>C:\stagingArea\myProject\</code> on Windows or
     * <code>/stagingArea/myProject/</code> on Unix.
     *
     * @parameter expression="${stagingDirectory}"
     */
    private File stagingDirectory;

    @Override
    protected String getDeployRepositoryID()
        throws MojoExecutionException
    {
        return "stagingLocal";
    }

    @Override
    protected String getDeployRepositoryURL()
        throws MojoExecutionException
    {
        final String stageDir = ( stagingDirectory == null ) ? null : stagingDirectory.getAbsolutePath();
        final String outputDir = getStagingDirectory( stageDir );

        getLog().info( "Using this directory for staging: " + outputDir );

        final File outputDirectory = new File( outputDir );
        // Safety
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        return "file://" + outputDirectory.getAbsolutePath();
    }

    /**
     * Find the directory where staging will take place.
     *
     * @param usersStagingDirectory The staging directory as suggested by the user's configuration
     *
     * @return the directory for staging
     */
    private String getStagingDirectory( String usersStagingDirectory )
    {
        String topLevelURL = null;

        if ( usersStagingDirectory != null )
        {
            // the user has specified a stagingDirectory - use it
            getLog().debug( "stagingDirectory specified by the user: " + usersStagingDirectory );
            topLevelURL = usersStagingDirectory;
        }
        else
        {
            // The user didn't specify a URL, use the top level target dir
            topLevelURL =
                getTopLevelBuildDirectory().getAbsolutePath() + "/" + DEFAULT_STAGING_DIRECTORY;
            getLog().debug( "stagingDirectory NOT specified, using the top level project: " + topLevelURL );
        }

        // Return either
        //   usersURL
        // or
        //   topLevelProjectURL + "staging"
        return topLevelURL;
    }
}
