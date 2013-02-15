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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.profiles.DefaultMavenProfilesBuilder;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.ProfilesConversionUtils;
import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Displays a list of available profiles under the current project.
 * <br/>
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
     * The current build session instance. This is used for plugin manager API calls.
     */
    @Component
    private MavenSession session;

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
            descriptionBuffer.append( "Listing Profiles for Project: " ).append( project.getId() ).append( "\n" );

            DefaultProfileManager pm =
                new DefaultProfileManager( session.getContainer(), session.getExecutionProperties() );

            // Obtain Profiles from external profiles.xml
            try
            {
                loadProjectExternalProfiles( pm, project.getBasedir() );
            }
            catch ( ProfileActivationException e )
            {
                throw new MojoExecutionException( "Error obtaining external Profiles:" + e.getMessage(), e );
            }

            // Attempt to obtain settings profiles
            loadSettingsProfiles( pm, session.getSettings() );

            // Attempt to obtain profiles from pom.xml
            loadProjectPomProfiles( pm, project );

            // now display
            if ( null == pm.getExplicitlyActivatedIds() || pm.getExplicitlyActivatedIds().size() == 0 )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "No profiles detected!" );
                }
            }
            else
            {
                // This feels more like a hack to filter out inactive profiles, there is no 'direct'
                // way to query activation status on a Profile instance.
                @SuppressWarnings( "unchecked" )
                Map<String, Profile> allProfilesByIds = pm.getProfilesById();

                // active Profiles will be a subset of *all* profiles
                @SuppressWarnings( "unchecked" )
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
                    descriptionBuffer.append( " (Active: true , Source: " ).append( p.getSource() ).append( ")\n" );
                }

                // display inactive profiles
                for ( Profile p : allProfilesByIds.values() )
                {
                    descriptionBuffer.append( "  Profile Id: " ).append( p.getId() );
                    descriptionBuffer.append( " (Active: false , Source: " ).append( p.getSource() ).append( ")\n" );
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

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Wrote descriptions to: " + output );
            }
        }
        else
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( descriptionBuffer.toString() );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Loads up external Profiles using <code>profiles.xml</code> (if any) located in the current
     * project's <code>${basedir}</code>.
     *
     * @param profileManager ProfileManager instance to use to load profiles from external Profiles.
     * @param projectDir location of the current project, could be null.
     * @throws ProfileActivationException, if there was an error loading profiles.
     */
    private void loadProjectExternalProfiles( ProfileManager profileManager, File projectDir )
        throws ProfileActivationException
    {
        if ( projectDir == null )
        {
            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Attempting to read profiles from external profiles.xml..." );
        }

        try
        {
            DefaultMavenProfilesBuilder profilesBuilder = new DefaultMavenProfilesBuilder();
            ProfilesRoot root = profilesBuilder.buildProfiles( projectDir );
            if ( root != null )
            {
                List<org.apache.maven.profiles.Profile> profiles = root.getProfiles(); 
                for ( org.apache.maven.profiles.Profile rawProfile : profiles )
                {
                    Profile converted = ProfilesConversionUtils.convertFromProfileXmlProfile( rawProfile );
                    profileManager.addProfile( converted );
                    profileManager.explicitlyActivate( converted.getId() );
                }
            }
            else if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "ProfilesRoot was found to be NULL" );
            }
        }
        catch ( IOException e )
        {
            throw new ProfileActivationException( "Cannot read profiles.xml resource from directory: "
                + projectDir, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ProfileActivationException( "Cannot parse profiles.xml resource from directory: "
                + projectDir, e );
        }
    }

    /**
     * Load profiles from <code>pom.xml</code>.
     *
     * @param profilesManager not null
     * @param project could be null
     */
    private void loadProjectPomProfiles( ProfileManager profilesManager, MavenProject project )
    {
        if ( project == null )
        {
            // shouldn't happen as this mojo requires a project
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "No pom.xml found to read Profiles from." );
            }

            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Attempting to read profiles from pom.xml..." );
        }

        // Attempt to obtain the list of profiles from pom.xml
        List<Profile> profiles = project.getModel().getProfiles();
        for ( Profile profile : profiles )
        {
            profilesManager.addProfile( profile );
            profilesManager.explicitlyActivate( profile.getId() );
        }

        MavenProject parent = project.getParent();
        while ( parent != null )
        {
            List<Profile> profiles2 = parent.getModel().getProfiles();
            for ( Profile profile : profiles2 )
            {
                profilesManager.addProfile( profile );
                profilesManager.explicitlyActivate( profile.getId() );
            }

            parent = parent.getParent();
        }
    }

    /**
     * Load profiles from <code>settings.xml</code>.
     *
     * @param profileManager not null
     * @param settings could be null
     */
    private void loadSettingsProfiles( ProfileManager profileManager, Settings settings )
    {
        if ( settings == null )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "No settings.xml detected." );
            }

            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Attempting to read profiles from settings.xml..." );
        }

        List<org.apache.maven.settings.Profile> profiles = settings.getProfiles();
        for ( org.apache.maven.settings.Profile rawProfile : profiles )
        {
            Profile profile = SettingsUtils.convertFromSettingsProfile( rawProfile );
            profileManager.addProfile( profile );
            profileManager.explicitlyActivate( profile.getId() );
        }
    }
}
