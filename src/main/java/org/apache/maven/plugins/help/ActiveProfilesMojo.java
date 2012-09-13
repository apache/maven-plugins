package org.apache.maven.plugins.help;

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

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Displays a list of the profiles which are currently active for this build.
 *
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "active-profiles", aggregator = true )
public class ActiveProfilesMojo
    extends AbstractHelpMojo
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * This is the list of projects currently slated to be built by Maven.
     */
    @Parameter( property = "reactorProjects", required = true, readonly = true )
    private List projects;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        StringBuilder message = new StringBuilder();

        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            getActiveProfileStatement( project, message );

            message.append( "\n\n" );
        }

        if ( output != null )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Created by: " + getClass().getName() ).append( "\n" );
            sb.append( "Created on: " + new Date() ).append( "\n" ).append( "\n" );
            sb.append( message.toString() );

            try
            {
                writeFile( output, sb );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write active profiles to output: " + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Active profile report written to: " + output );
            }
        }
        else
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( message );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Method to get the active profiles for the project
     *
     * @param project   the current project
     * @param message   the object where the information will be appended to
     */
    private void getActiveProfileStatement( MavenProject project, StringBuilder message )
    {
        // Get active profiles into our own list,
        // since we'll be modifying it, further below
        List profiles = new ArrayList( project.getActiveProfiles() );

        message.append( "\n" );

        message.append( "Active Profiles for Project \'" + project.getId() + "\': \n\n" );

        if ( profiles == null || profiles.isEmpty() )
        {
            message.append( "There are no active profiles." );
        }
        else
        {
            message.append( "The following profiles are active:\n" );

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                message.append( "\n - " ).append( profile.getId() );
                message.append( " (source: " ).append( profile.getSource() ).append( ")" );
            }

        }

        message.append( "\n" );
    }
}
