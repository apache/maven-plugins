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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

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
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> projects;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        StringBuilder message = new StringBuilder();

        for ( MavenProject project : projects )
        {
            getActiveProfileStatement( project, message );

            message.append( LS ).append( LS );
        }

        if ( output != null )
        {
            String formattedDateTime = DateFormatUtils.ISO_DATETIME_FORMAT.format( System.currentTimeMillis() );
            StringBuilder sb = new StringBuilder();
            sb.append( "Created by: " ).append( getClass().getName() ).append( LS );
            sb.append( "Created on: " ).append( formattedDateTime ).append( LS ).append( LS );
            sb.append( message.toString() );

            try
            {
                writeFile( output, sb );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write active profiles to output: " + output, e );
            }

            getLog().info( "Active profile report written to: " + output );
        }
        else
        {
            getLog().info( message );
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
        Map<String, List<String>> activeProfileIds = project.getInjectedProfileIds();
        
        message.append( LS );
        message.append( "Active Profiles for Project \'" ).append( project.getId() ).append( "\':" );
        message.append( LS ).append( LS );

        if ( activeProfileIds.isEmpty() )
        {
            message.append( "There are no active profiles." );
        }
        else
        {
            message.append( "The following profiles are active:" ).append( LS );

            for ( Map.Entry<String, List<String>> entry : activeProfileIds.entrySet() )
            {
                for ( String profileId : entry.getValue() )
                {
                    message.append( LS ).append( " - " ).append( profileId );
                    message.append( " (source: " ).append( entry.getKey() ).append( ")" );
                }
            }
        }

        message.append( LS );
    }

}
