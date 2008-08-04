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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

/**
 * Displays a list of available profiles under the current project. <p>
 *
 * <b>Note</b>: this Mojo lists <b>all</b> profiles for a project. If a
 * profile comes up with a status <b>inactive</b> then there might be a need to
 * set profile activation switches/property. <p>
 *
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @version $Id$
 * @since 2.1
 * @goal all-profiles
 */
public class AllProfilesMojo
    extends AbstractMojo
{
    /**
     * This is the list of projects currently slated to be built by Maven.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List projects;

    /**
     * The current build session instance. This is used for plugin manager API
     * calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * Lists details for all profiles discovered.
     *
     * @throws MojoExecutionException if there was a <em>unexpected</em> build error.
     * @throws MojoFailureException if there was an <em>expected</em> error leading to build failure.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        for ( Iterator iter = projects.iterator(); iter.hasNext(); )
        {
            MavenProject project = (MavenProject) iter.next();
            if ( getLog().isInfoEnabled() )
                getLog().info( "Listing Profiles for Project: " + project.getId() );
            DefaultProfileManager pm = new DefaultProfileManager( session.getContainer() );

            // Obtain Profiles from external profiles.xml
            try
            {
                loadProjectExternalProfiles( pm, project.getBasedir() );
            }
            catch ( ProfileActivationException e )
            {
                String error = "Error obtaining external Profiles.";
                if ( getLog().isErrorEnabled() )
                    getLog().error( error, e );
                throw new MojoExecutionException( error, e );
            }

            // Attempt to obtain settings profiles
            loadSettingsProfiles( pm, session.getSettings() );

            // Attempt to obtain profiles from pom.xml
            loadProjectPomProfiles( pm, project );

            // now display
            if ( null == pm.getExplicitlyActivatedIds() || pm.getExplicitlyActivatedIds().size() == 0 )
            {
                if ( getLog().isWarnEnabled() )
                    getLog().warn( "No profiles detected!" );
            }
            else
            {
                // This feels more like a hack to filter out inactive profiles, there is no 'direct'
                // way to query activation status on a Profile instance.
                Map allProfilesByIds = pm.getProfilesById();
                // active Profiles will be a subset of *all* profiles
                List activeProfiles = project.getActiveProfiles();
                for ( Iterator itr = activeProfiles.iterator(); itr.hasNext(); )
                {
                    Profile activeProfile = (Profile) itr.next();
                    // we already have the active profiles for the project, so remove them from the list of all profiles.
                    allProfilesByIds.remove( activeProfile.getId() );
                }

                // display active profiles
                for ( Iterator it = activeProfiles.iterator(); it.hasNext(); )
                {
                    Profile p = (Profile) it.next();
                    getLog().info( "\tProfile Id: " + p.getId() + " (Active: true , Source: " + p.getSource() + ")" );
                }
                // display inactive profiles
                Iterator it = allProfilesByIds.keySet().iterator();
                while ( it.hasNext() )
                {
                    Profile p = (Profile) allProfilesByIds.get( (String) it.next() );
                    getLog().info( "\tProfile Id: " + p.getId() + " (Active: false , Source: " + p.getSource() + ")" );
                }
            }
        }
    }

    /**
     * Loads up external Profiles using <code>profiles.xml</code> (if any)
     * located in the current project's <code>${basedir}</code>.
     *
     * @param profileManager ProfileManager instance to use to load profiles from external Profiles.
     * @param projectDir location of the current project.
     * @throws ProfileActivationException, if there was an error loading profiles.
     */
    private void loadProjectExternalProfiles( ProfileManager profileManager, File projectDir )
        throws ProfileActivationException
    {
        if ( projectDir != null )
        {
            if ( getLog().isDebugEnabled() )
                getLog().debug( "Attempting to read profiles from external profiles.xml..." );
            try
            {
                ProfilesRoot root = null;
                DefaultMavenProfilesBuilder profilesBuilder = new DefaultMavenProfilesBuilder();
                root = profilesBuilder.buildProfiles( projectDir );
                if ( root != null )
                {
                    for ( Iterator it = root.getProfiles().iterator(); it.hasNext(); )
                    {
                        org.apache.maven.profiles.Profile rawProfile = (org.apache.maven.profiles.Profile) it.next();
                        Profile converted = ProfilesConversionUtils.convertFromProfileXmlProfile( rawProfile );
                        profileManager.addProfile( converted );
                        profileManager.explicitlyActivate( converted.getId() );
                    }
                }
                else if ( getLog().isDebugEnabled() )
                    getLog().debug( "ProfilesRoot was found to be NULL" );
            }
            catch ( IOException e )
            {
                throw new ProfileActivationException(
                                                      "Cannot read profiles.xml resource from directory: " + projectDir,
                                                      e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ProfileActivationException( "Cannot parse profiles.xml resource from directory: "
                    + projectDir, e );
            }
        }
    }

    /**
     * Load profiles from pom.xml.
     *
     * @param profilesManager
     * @param projectDir
     */
    private void loadProjectPomProfiles( ProfileManager profilesManager, MavenProject project )
    {
        if ( null != project )
        {
            if ( getLog().isDebugEnabled() )
                getLog().debug( "Attempting to read profiles from pom.xml..." );
            // Attempt to obtain the list of profiles from  pom.xml
            Iterator it = project.getModel().getProfiles().iterator();
            while ( it.hasNext() )
            {
                Profile profile = (Profile) it.next();
                profilesManager.addProfile( profile );
                profilesManager.explicitlyActivate( profile.getId() );
            }
        }
        else
        {
            // shouldn't happen as this mojo requires a project
            if ( getLog().isDebugEnabled() )
                getLog().debug( "No pom.xml found to read Profiles from." );
        }
    }

    /**
     * Load profiles from settings.xml.
     *
     * @param profileManager
     * @param settings
     */
    private void loadSettingsProfiles( ProfileManager profileManager, Settings settings )
    {
        if ( null != settings )
        {
            if ( getLog().isDebugEnabled() )
                getLog().debug( "Attempting to read profiles from settings.xml..." );
            Iterator it = settings.getProfiles().iterator();
            while ( it.hasNext() )
            {
                Profile profile = SettingsUtils.convertFromSettingsProfile( (org.apache.maven.settings.Profile) it
                    .next() );
                profileManager.addProfile( profile );
                profileManager.explicitlyActivate( profile.getId() );
            }
        }
        else
        {
            if ( getLog().isDebugEnabled() )
                getLog().debug( "No settings.xml detected." );
        }
    }
}
