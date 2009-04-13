package org.apache.maven.plugin.ant;

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
import java.io.IOException;
import java.util.Locale;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.InputHandler;

/**
 * Clean all Ant build files.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal clean
 */
public class AntCleanMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     * @since 2.1.1
     */
    private Settings settings;

    /**
     * Input handler, needed for command line handling.
     *
     * @component
     * @since 2.1.1
     */
    private InputHandler inputHandler;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The working project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Forcing the deletion of the custom <code>build.xml</code>.
     *
     * @parameter expression="${force}"
     * @since 2.1.1
     */
    private boolean force;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        deleteCustomBuild();

        File mavenBuildXml = new File( project.getBasedir(), AntBuildWriter.DEFAULT_MAVEN_BUILD_FILENAME );
        if ( mavenBuildXml.exists() && !mavenBuildXml.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + mavenBuildXml.getAbsolutePath() );
        }

        File mavenBuildProperties =
            new File( project.getBasedir(), AntBuildWriter.DEFAULT_MAVEN_PROPERTIES_FILENAME );
        if ( mavenBuildProperties.exists() && !mavenBuildProperties.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + mavenBuildProperties.getAbsolutePath() );
        }

        getLog().info(
                       "Deleted Ant project for " + project.getArtifactId() + " in "
                           + project.getBasedir().getAbsolutePath() );
    }

    /**
     * Deleting the <code>build.xml</code> depending the user interaction.
     *
     * @throws MojoExecutionException if any
     */
    private void deleteCustomBuild()
        throws MojoExecutionException
    {
        // add warranty msg
        if ( !preCheck() )
        {
            return;
        }

        File buildXml = new File( project.getBasedir(), AntBuildWriter.DEFAULT_BUILD_FILENAME );
        if ( buildXml.exists() && !buildXml.delete() )
        {
            throw new MojoExecutionException( "Cannot delete " + buildXml.getAbsolutePath() );
        }
    }

    /**
     * @return <code>true</code> if the user wants to proceed, <code>false</code> otherwise.
     * @throws MojoExecutionException if any
     */
    private boolean preCheck()
        throws MojoExecutionException
    {
        if ( force )
        {
            return true;
        }

        if ( !settings.isInteractiveMode() )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error(
                                "Maven is not attempt to interact with the user for input. "
                                    + "Verify the <interactiveMode/> configuration in your settings." );
            }
            return false;
        }

        if ( getLog().isWarnEnabled() )
        {
            getLog().warn( "" );
            getLog().warn( "    WARRANTY DISCLAIMER" );
            getLog().warn( "" );
            getLog().warn( "This Maven goal will delete your build.xml." );
            getLog().warn( "" );
        }

        while ( true )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Are you sure to proceed? [Y]es [N]o" );
            }

            try
            {
                String userExpression = inputHandler.readLine();
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Y" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Yes" ) )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "OK, let's proceed..." );
                    }
                    break;
                }
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "N" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "No" ) )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "No changes on the build.xml occur." );
                    }
                    return false;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to read from standard input.", e );
            }
        }

        return true;
    }
}
