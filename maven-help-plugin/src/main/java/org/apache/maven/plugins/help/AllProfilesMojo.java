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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.SettingsUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays a list of available profiles under the current project.
 * <br>
 * <b>Note</b>: it will list <b>all</b> profiles for a project. If a
 * profile comes up with a status <b>inactive</b> then there might be a need to
 * set profile activation switches/property.
 *
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @version $Id$
 * @since 2.1
 */
@Mojo( name = "all-profiles", requiresProject = false )
public class AllProfilesMojo
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

    /**
     * The list of profiles defined in the current Maven settings.
     */
    @Parameter( defaultValue = "${settings.profiles}", readonly = true, required = true )
    private List<org.apache.maven.settings.Profile> settingsProfiles;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        StringBuilder descriptionBuffer = new StringBuilder();

        for ( MavenProject project : projects )
        {
            descriptionBuffer.append( "Listing Profiles for Project: " ).append( project.getId() ).append( LS );
            
            Map<String, Profile> allProfilesByIds = new HashMap<String, Profile>();
            addSettingsProfiles( allProfilesByIds );
            addProjectPomProfiles( project, allProfilesByIds );

            // now display
            if ( allProfilesByIds.isEmpty() )
            {
                getLog().warn( "No profiles detected!" );
            }
            else
            {
                // active Profiles will be a subset of *all* profiles
                List<Profile> activeProfiles = project.getActiveProfiles();
                for ( Profile activeProfile : activeProfiles )
                {
                    // we already have the active profiles for the project, so remove them from the list of all
                    // profiles.
                    allProfilesByIds.remove( activeProfile.getId() );
                }

                // display active profiles
                for ( Profile p : activeProfiles )
                {
                    descriptionBuffer.append( "  Profile Id: " ).append( p.getId() );
                    descriptionBuffer.append( " (Active: true , Source: " ).append( p.getSource() ).append( ")" );
                    descriptionBuffer.append( LS );
                }

                // display inactive profiles
                for ( Profile p : allProfilesByIds.values() )
                {
                    descriptionBuffer.append( "  Profile Id: " ).append( p.getId() );
                    descriptionBuffer.append( " (Active: false , Source: " ).append( p.getSource() ).append( ")" );
                    descriptionBuffer.append( LS );
                }
            }
        }

        if ( output != null )
        {
            try
            {
                writeFile( output, descriptionBuffer );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write profiles description to output: " + output, e );
            }

            getLog().info( "Wrote descriptions to: " + output );
        }
        else
        {
            getLog().info( descriptionBuffer.toString() );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Adds the profiles from <code>pom.xml</code> and all of its parents.
     *
     * @param project could be null
     * @param allProfiles Map to add the profiles to.
     */
    private void addProjectPomProfiles( MavenProject project, Map<String, Profile> allProfiles )
    {
        if ( project == null )
        {
            // shouldn't happen as this mojo requires a project
            getLog().debug( "No pom.xml found to read Profiles from." );
            return;
        }

        getLog().debug( "Attempting to read profiles from pom.xml..." );

        for ( Profile profile : project.getModel().getProfiles() )
        {
            allProfiles.put( profile.getId(), profile );
        }

        MavenProject parent = project.getParent();
        while ( parent != null )
        {
            for ( Profile profile : parent.getModel().getProfiles() )
            {
                allProfiles.put( profile.getId(), profile );
            }
            parent = parent.getParent();
        }
    }

    /**
     * Adds the profiles from <code>settings.xml</code>.
     *
     * @param allProfiles Map to add the profiles to.
     */
    private void addSettingsProfiles( Map<String, Profile> allProfiles )
    {
        getLog().debug( "Attempting to read profiles from settings.xml..." );
        for ( org.apache.maven.settings.Profile settingsProfile : settingsProfiles )
        {
            Profile profile = SettingsUtils.convertFromSettingsProfile( settingsProfile );
            allProfiles.put( profile.getId(), profile );
        }
    }
}
