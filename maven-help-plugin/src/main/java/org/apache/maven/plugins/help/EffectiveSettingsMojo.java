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
import java.io.StringWriter;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Displays the calculated settings for this project, given any profile enhancement and the inheritance
 * of the global settings into the user-level settings.
 *
 * @version $Id$
 * @since 2.0
 * @goal effective-settings
 * @requiresProject false
 */
public class EffectiveSettingsMojo
    extends AbstractHelpMojo
{
    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global- and user-level settings files.
     *
     * @parameter expression="${settings}"
     * @readonly
     * @required
     */
    private Settings settings;

    /**
     * For security reasons, all passwords are hidden by default. Set this to 'true' to show all passwords.
     *
     * @since 2.1
     * @parameter expression="${showPasswords}" default-value="false"
     */
    private boolean showPasswords;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        Settings copySettings;
        if ( showPasswords )
        {
            copySettings = settings;
        }
        else
        {
            copySettings = copySettings( settings );
            hidePasswords( copySettings );
        }

        StringWriter sWriter = new StringWriter();

        SettingsXpp3Writer settingsWriter = new SettingsXpp3Writer();

        try
        {
            settingsWriter.write( sWriter, copySettings );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot serialize Settings to XML.", e );
        }

        if ( output != null )
        {
            try
            {
                writeFile( output, sWriter.toString() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write effective-settings to output: " + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Effective-settings written to: " + output );
            }
        }
        else
        {
            StringBuffer message = new StringBuffer();

            message.append( "\nEffective settings:\n\n" );
            message.append( sWriter.toString() );
            message.append( "\n\n" );

            getLog().info( message );
        }
    }

    /**
     * Hide proxy and server passwords.
     *
     * @param aSettings not null
     */
    private void hidePasswords( Settings aSettings )
    {
        for ( Iterator it = aSettings.getProxies().iterator(); it.hasNext(); )
        {
            Proxy proxy = (Proxy) it.next();

            if ( StringUtils.isNotEmpty( proxy.getPassword() ) )
            {
                proxy.setPassword( StringUtils.repeat( "*", proxy.getPassword().length() ) );
            }
        }

        for ( Iterator it = aSettings.getServers().iterator(); it.hasNext(); )
        {
            Server server = (Server) it.next();

            if ( StringUtils.isNotEmpty( server.getPassword() ) )
            {
                server.setPassword( StringUtils.repeat( "*", server.getPassword().length() ) );
            }
        }
    }

    /**
     * TODO: should be replaced by SettingsUtils#copySettings()
     *
     * @param settings could be null
     * @return a new instance of settings or null if settings was null.
     */
    public static Settings copySettings( Settings settings )
    {
        if ( settings == null )
        {
            return null;
        }

        Settings clone = new Settings();
        clone.setActiveProfiles( settings.getActiveProfiles() );
        clone.setInteractiveMode( settings.isInteractiveMode() );
        clone.setLocalRepository( settings.getLocalRepository() );
        clone.setMirrors( settings.getMirrors() );
        clone.setOffline( settings.isOffline() );
        clone.setPluginGroups( settings.getPluginGroups() );
        clone.setProfiles( settings.getProfiles() );
        clone.setProxies( settings.getProxies() );
        clone.setRuntimeInfo( settings.getRuntimeInfo() );
        clone.setServers( settings.getServers() );
        clone.setSourceLevel( settings.getSourceLevel() );
        clone.setUsePluginRegistry( settings.isUsePluginRegistry() );

        return clone;
    }
}
