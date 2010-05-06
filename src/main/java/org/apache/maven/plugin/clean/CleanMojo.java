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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * Goal which cleans the build.
 *
 * <P>This attempts to clean a project's working directory of the files that
 * were generated at build-time. By default, it discovers and deletes the
 * directories configured in <code>project.build.directory</code>,
 * <code>project.build.outputDirectory</code>,
 * <code>project.build.testOutputDirectory</code>, and
 * <code>project.reporting.outputDirectory</code>. </P>
 *
 * <P>Files outside the default may also be included in the deletion by
 * configuring the <code>filesets</code> tag.</P>
 *
 * @author <a href="mailto:evenisse@maven.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal clean
 * @threadSafe
 * @since 2.0
 * @see org.apache.maven.plugin.clean.Fileset
 */
public class CleanMojo
    extends AbstractMojo
{

    /**
     * This is where build results go.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /**
     * This is where compiled classes go.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * This is where compiled test classes go.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;

    /**
     * This is where the site plugin generates its pages.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     * @since 2.1.1
     */
    private File reportDirectory;

    /**
     * Sets whether the plugin runs in verbose mode. As of plugin version 2.3, the default value is derived from Maven's
     * global debug flag (compare command line switch <code>-X</code>).
     * 
     * @parameter expression="${clean.verbose}"
     * @since 2.1
     */
    private Boolean verbose;

    /**
     * The list of file sets to delete, in addition to the default directories.
     *
     * @parameter
     * @since 2.1
     */
    private Fileset[] filesets;

    /**
     * Sets whether the plugin should follow symbolic links while deleting files from the default output directories of
     * the project. Not following symlinks requires more IO operations and heap memory, regardless whether symlinks are
     * actually present. So projects with a huge output directory that knowingly does not contain symlinks can improve
     * performance by setting this parameter to <code>true</code>.
     * 
     * @parameter expression="${clean.followSymLinks}" default-value="false"
     * @since 2.1
     */
    private boolean followSymLinks;

    /**
     * Disables the plugin execution.
     *
     * @parameter expression="${clean.skip}" default-value="false"
     * @since 2.2
     */
    private boolean skip;

    /**
     * Indicates whether the build will continue even if there are clean errors.
     *
     * @parameter expression="${maven.clean.failOnError}" default-value="true"
     * @since 2.2
     */
    private boolean failOnError;

    /**
     * Disables the deletion of the default output directories configured for a project. If set to <code>true</code>,
     * only the files/directories selected via the parameter {@link #filesets} will be deleted.
     * 
     * @parameter expression="${clean.excludeDefaultDirectories}" default-value="false"
     * @since 2.3
     */
    private boolean excludeDefaultDirectories;

    /**
     * Deletes file-sets in the following project build directory order: (source) directory, output directory, test
     * directory, report directory, and then the additional file-sets.
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Clean is skipped." );
            return;
        }

        Cleaner cleaner = new Cleaner( getLog(), isVerbose() );

        try
        {
            File[] directories = getDirectories();
            for ( int i = 0; i < directories.length; i++ )
            {
                File directory = directories[i];
                if ( directory != null )
                {
                    cleaner.delete( directory, null, followSymLinks, failOnError );
                }
            }

            if ( filesets != null )
            {
                for ( int i = 0; i < filesets.length; i++ )
                {
                    Fileset fileset = filesets[i];
                    if ( fileset.getDirectory() == null )
                    {
                        throw new MojoExecutionException( "Missing base directory for " + fileset );
                    }
                    GlobSelector selector = new GlobSelector( fileset.getIncludes(), fileset.getExcludes() );
                    cleaner.delete( fileset.getDirectory(), selector, fileset.isFollowSymlinks(), failOnError );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to clean project: " + e.getMessage(), e );
        }
    }

    /**
     * Indicates whether verbose output is enabled.
     * 
     * @return <code>true</code> if verbose output is enabled, <code>false</code> otherwise.
     */
    private boolean isVerbose()
    {
        return ( verbose != null ) ? verbose.booleanValue() : getLog().isDebugEnabled();
    }

    /**
     * Gets the directories to clean (if any). The returned array may contain null entries.
     * 
     * @return The directories to clean or an empty array if none, never <code>null</code>.
     */
    private File[] getDirectories()
    {
        File[] directories;
        if ( excludeDefaultDirectories )
        {
            directories = new File[0];
        }
        else
        {
            directories = new File[] { directory, outputDirectory, testOutputDirectory, reportDirectory };
        }
        return directories;
    }

}
