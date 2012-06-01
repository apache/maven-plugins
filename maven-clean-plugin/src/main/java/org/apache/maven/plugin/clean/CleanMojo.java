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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

/**
 * Goal which cleans the build.
 * <p/>
 * <P>This attempts to clean a project's working directory of the files that
 * were generated at build-time. By default, it discovers and deletes the
 * directories configured in <code>project.build.directory</code>,
 * <code>project.build.outputDirectory</code>,
 * <code>project.build.testOutputDirectory</code>, and
 * <code>project.reporting.outputDirectory</code>. </P>
 * <p/>
 * <P>Files outside the default may also be included in the deletion by
 * configuring the <code>filesets</code> tag.</P>
 *
 * @author <a href="mailto:evenisse@maven.org">Emmanuel Venisse</a>
 * @version $Id$
 * @see org.apache.maven.plugin.clean.Fileset
 * @since 2.0
 */
@Mojo( name = "clean", threadSafe = true )
public class CleanMojo
    extends AbstractMojo
{

    /**
     * This is where build results go.
     */
    @Parameter( defaultValue = "${project.build.directory}", readonly = true, required = true )
    private File directory;

    /**
     * This is where compiled classes go.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", readonly = true, required = true )
    private File outputDirectory;

    /**
     * This is where compiled test classes go.
     */
    @Parameter( defaultValue = "${project.build.testOutputDirectory}", readonly = true, required = true )
    private File testOutputDirectory;

    /**
     * This is where the site plugin generates its pages.
     *
     * @since 2.1.1
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", readonly = true, required = true )
    private File reportDirectory;

    /**
     * Sets whether the plugin runs in verbose mode. As of plugin version 2.3, the default value is derived from Maven's
     * global debug flag (compare command line switch <code>-X</code>).
     *
     * @since 2.1
     */
    @Parameter( property = "clean.verbose" )
    private Boolean verbose;

    /**
     * The list of file sets to delete, in addition to the default directories. For example:
     * <pre>
     * &lt;filesets&gt;
     *   &lt;fileset&gt;
     *     &lt;directory&gt;src/main/generated&lt;/directory&gt;
     *     &lt;followSymlinks&gt;false&lt;/followSymlinks&gt;
     *     &lt;useDefaultExcludes&gt;true&lt;/useDefaultExcludes&gt;
     *     &lt;includes&gt;
     *       &lt;include&gt;*.java&lt;/include&gt;
     *     &lt;/includes&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;Template*&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;/fileset&gt;
     * &lt;/filesets&gt;
     * </pre>
     *
     * @since 2.1
     */
    @Parameter
    private Fileset[] filesets;

    /**
     * Sets whether the plugin should follow symbolic links while deleting files from the default output directories of
     * the project. Not following symlinks requires more IO operations and heap memory, regardless whether symlinks are
     * actually present. So projects with a huge output directory that knowingly does not contain symlinks can improve
     * performance by setting this parameter to <code>true</code>.
     *
     * @since 2.1
     */
    @Parameter( property = "clean.followSymLinks", defaultValue = "false" )
    private boolean followSymLinks;

    /**
     * Disables the plugin execution.
     *
     * @since 2.2
     */
    @Parameter( property = "clean.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Indicates whether the build will continue even if there are clean errors.
     *
     * @since 2.2
     */
    @Parameter( property = "maven.clean.failOnError", defaultValue = "true" )
    private boolean failOnError;

    /**
     * Indicates whether the plugin should undertake additional attempts (after a short delay) to delete a file if the
     * first attempt failed. This is meant to help deleting files that are temporarily locked by third-party tools like
     * virus scanners or search indexing.
     *
     * @since 2.4.2
     */
    @Parameter( property = "maven.clean.retryOnError", defaultValue = "true" )
    private boolean retryOnError;

    /**
     * Disables the deletion of the default output directories configured for a project. If set to <code>true</code>,
     * only the files/directories selected via the parameter {@link #filesets} will be deleted.
     *
     * @since 2.3
     */
    @Parameter( property = "clean.excludeDefaultDirectories", defaultValue = "false" )
    private boolean excludeDefaultDirectories;

    /**
     * Deletes file-sets in the following project build directory order: (source) directory, output directory, test
     * directory, report directory, and then the additional file-sets.
     *
     * @throws MojoExecutionException When a directory failed to get deleted.
     * @see org.apache.maven.plugin.Mojo#execute()
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
                    cleaner.delete( directory, null, followSymLinks, failOnError, retryOnError );
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
                    GlobSelector selector = new GlobSelector( fileset.getIncludes(), fileset.getExcludes(),
                                                              fileset.isUseDefaultExcludes() );
                    cleaner.delete( fileset.getDirectory(), selector, fileset.isFollowSymlinks(), failOnError,
                                    retryOnError );
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
            directories = new File[]{ directory, outputDirectory, testOutputDirectory, reportDirectory };
        }
        return directories;
    }

}
