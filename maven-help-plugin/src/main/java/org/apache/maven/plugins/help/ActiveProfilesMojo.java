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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Profile;
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

            message.append( "\n\n" );
        }

        if ( output != null )
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Created by: ").append(getClass().getName()).append("\n");
            sb.append("Created on: ").append(new Date()).append("\n").append( "\n" );
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
        Map<String, List<String>> activeProfileIds = new LinkedHashMap<String, List<String>>();
        try 
        {
            activeProfileIds.putAll( getInjectedProfileIds( project ) );
        }
        catch ( UnsupportedOperationException uoe )
        {
            // Fall back to M2 approach
            @SuppressWarnings( "unchecked" )
            List<Profile> profiles = new ArrayList<Profile>( project.getActiveProfiles() );
            
            for ( Profile profile : profiles )
            {
                List<String> profileIds = activeProfileIds.get( profile.getSource() );
                if ( profileIds == null )
                {
                    profileIds = new ArrayList<String>();
                    activeProfileIds.put( profile.getSource(), profileIds );
                }
                profileIds.add( profile.getId() );
            }
        }
        

        message.append( "\n" );

        message.append("Active Profiles for Project \'").append(project.getId()).append("\': \n\n");

        if ( activeProfileIds.isEmpty() )
        {
            message.append( "There are no active profiles." );
        }
        else
        {
            message.append( "The following profiles are active:\n" );

            for ( Map.Entry<String, List<String>> entry : activeProfileIds.entrySet() )
            {
                for ( String profileId : entry.getValue() )
                {
                    message.append( "\n - " ).append( profileId );
                    message.append( " (source: " ).append( entry.getKey() ).append( ")" );
                }
            }
        }

        message.append( "\n" );
    }

    @SuppressWarnings( "unchecked" )
    private Map<String, List<String>> getInjectedProfileIds( MavenProject project ) throws UnsupportedOperationException
    {
        try
        {
            // This method was introduced with M3
            Method getInjectedProfileIdsMethod = MavenProject.class.getMethod( "getInjectedProfileIds" );
            return (Map<String, List<String>>) getInjectedProfileIdsMethod.invoke( project );
        }
        catch ( SecurityException e )
        {
            throw new UnsupportedOperationException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new UnsupportedOperationException( e.getMessage(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new UnsupportedOperationException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new UnsupportedOperationException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new UnsupportedOperationException( e.getMessage(), e );
        }
    }
}
