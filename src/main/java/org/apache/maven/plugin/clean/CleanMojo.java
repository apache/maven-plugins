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
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
 * @since 2.0
 * @see org.apache.maven.plugin.clean.Fileset
 */
public class CleanMojo
    extends AbstractMojo
{
    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     * @since 2.2
     */
    private MavenProject project;

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
    private List filesets;

    /**
     * Sets whether the plugin should follow symbolic links to delete files.
     *
     * @parameter expression="${clean.followSymLinks}" default-value="false"
     * @since 2.1
     */
    private boolean followSymLinks;

    /**
     * Finds and retrieves included and excluded files, and handles their
     * deletion
     *
     * @since 2.1
     */
    private FileSetManager fileSetManager;

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
     * Deletes file-sets in the following project build directory order:
     * (source) directory, output directory, test directory, report directory,
     * and then the additional file-sets.
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

        try
        {
            fileSetManager = new FileSetManager( getLog(), isVerbose() );

            if ( !excludeDefaultDirectories )
            {
                removeDirectory( directory );
                removeDirectory( outputDirectory );
                removeDirectory( testOutputDirectory );
                removeDirectory( reportDirectory );
            }

            removeAdditionalFilesets();
        }
        catch ( MojoExecutionException e )
        {
            if ( failOnError )
            {
                throw e;
            }

            getLog().warn( e.getMessage() );
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
     * Deletes additional file-sets specified by the <code>filesets</code> tag.
     *
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    private void removeAdditionalFilesets()
        throws MojoExecutionException
    {
        if ( filesets != null && !filesets.isEmpty() )
        {
            for ( Iterator it = filesets.iterator(); it.hasNext(); )
            {
                FileSet fileset = (FileSet) it.next();

                removeFileSet( fileset );
            }
        }
    }

    /**
     * Deletes a directory and its contents.
     *
     * @param dir The base directory of the included and excluded files.
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    private void removeDirectory( File dir )
        throws MojoExecutionException
    {
        if ( dir != null )
        {
            FileSet fs = new Fileset();
            fs.setDirectory( dir.getPath() );
            fs.addInclude( "**" );
            fs.setFollowSymlinks( followSymLinks );

            removeFileSet( fs );
        }
    }

    /**
     * Deletes the specified file set. If the base directory of the file set is relative, it will be resolved against
     * the base directory of the current project.
     *
     * @param fileset The file set to delete, must not be <code>null</code>.
     * @throws MojoExecutionException When the file set failed to get deleted.
     */
    private void removeFileSet( FileSet fileset )
        throws MojoExecutionException
    {
        try
        {
            File dir = new File( fileset.getDirectory() );

            if ( !dir.isAbsolute() )
            {
                dir = new File( project.getBasedir(), fileset.getDirectory() );
                fileset.setDirectory( dir.getPath() );
            }

            if ( !dir.exists() )
            {
                getLog().debug( "Skipping non-existing directory: " + dir );
                return;
            }

            if ( !dir.isDirectory() )
            {
                throw new MojoExecutionException( dir + " is not a directory." );
            }

            getLog().info( "Deleting " + fileset );
            fileSetManager.delete( fileset, failOnError );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to delete directory: " + fileset.getDirectory() + ". Reason: "
                + e.getMessage(), e );
        }
        catch ( IllegalStateException e )
        {
            // TODO: IOException from plexus-utils should be acceptable here
            throw new MojoExecutionException( "Failed to delete directory: " + fileset.getDirectory() + ". Reason: "
                + e.getMessage(), e );
        }
    }

    /**
     * Sets the project build directory.
     *
     * @param newDirectory The project build directory to set.
     */
    protected void setDirectory( File newDirectory )
    {
        this.directory = newDirectory;
    }

    /**
     * Sets the project build output directory.
     *
     * @param newOutputDirectory The project build output directory to set.
     */
    protected void setOutputDirectory( File newOutputDirectory )
    {
        this.outputDirectory = newOutputDirectory;
    }

    /**
     * Sets the project build test output directory.
     *
     * @param newTestOutputDirectory The project build test output directory to set.
     */
    protected void setTestOutputDirectory( File newTestOutputDirectory )
    {
        this.testOutputDirectory = newTestOutputDirectory;
    }

    /**
     * Sets the project build report directory.
     *
     * @param newReportDirectory The project build report directory to set.
     */
    protected void setReportDirectory( File newReportDirectory )
    {
        this.reportDirectory = newReportDirectory;
    }

    /**
     * Adds a file-set to the list of file-sets to clean.
     *
     * @param fileset the fileset
     */
    public void addFileset( Fileset fileset )
    {
        if ( filesets == null )
        {
            filesets = new LinkedList();
        }
        filesets.add( fileset );
    }
}
