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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Displays the calculated settings as XML for this project, given any profile enhancement and the inheritance
 * of the global settings into the user-level settings.
 *
 * @version $Id$
 * @since 2.0
 * @goal effective-settings
 * @requiresProject false
 */
public class EffectiveSettingsMojo
    extends AbstractEffectiveMojo
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     *
     * @parameter expression="${settings}"
     * @readonly
     * @required
     */
    private Settings settings;

    /**
     * For security reasons, all passwords are hidden by default. Set this to <code>true</code> to show all passwords.
     *
     * @since 2.1
     * @parameter expression="${showPasswords}" default-value="false"
     */
    private boolean showPasswords;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

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

        StringWriter w = new StringWriter();
        XMLWriter writer =
            new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE ),
                                      copySettings.getModelEncoding(), null );

        writeHeader( writer );

        writeEffectiveSettings( copySettings, writer );

        String effectiveSettings = w.toString();

        if ( output != null )
        {
            try
            {
                writeXmlFile( output, effectiveSettings, copySettings.getModelEncoding() );
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

            message.append( "\nEffective user-specific configuration settings:\n\n" );
            message.append( effectiveSettings );
            message.append( "\n" );

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( message.toString() );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Hide proxy and server passwords.
     *
     * @param aSettings not null
     */
    private static void hidePasswords( Settings aSettings )
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
            // Password
            if ( StringUtils.isNotEmpty( server.getPassword() ) )
            {
                server.setPassword( StringUtils.repeat( "*", server.getPassword().length() ) );
            }
            // Passphrase
            if ( StringUtils.isNotEmpty( server.getPassphrase() ) )
            {
                server.setPassphrase( StringUtils.repeat( "*", server.getPassphrase().length() ) );
            }
        }
    }

    /**
     * TODO: should be replaced by SettingsUtils#copySettings() in 2.0.10+.
     *
     * @param settings could be null
     * @return a new instance of settings or null if settings was null.
     */
    private static Settings copySettings( Settings settings )
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

    /**
     * Method for writing the effective settings informations.
     *
     * @param settings the settings, not null.
     * @param writer the XML writer used, not null.
     * @throws MojoExecutionException if any
     */
    private static void writeEffectiveSettings( Settings settings, XMLWriter writer )
        throws MojoExecutionException
    {
        cleanSettings( settings );

        String effectiveSettings;

        StringWriter sWriter = new StringWriter();
        SettingsXpp3Writer settingsWriter = new SettingsXpp3Writer();
        try
        {
            settingsWriter.write( sWriter, settings );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot serialize Settings to XML.", e );
        }

        effectiveSettings = addMavenNamespace( sWriter.toString(), false );

        writeComment( writer, "Effective Settings for '" + getUserName() + "' on '" + getHostName() + "'" );

        writer.writeMarkup( effectiveSettings );
    }

    /**
     * Apply some logic to clean the model before writing it.
     *
     * @param settings not null
     */
    private static void cleanSettings( Settings settings )
    {
        for ( Iterator it = settings.getProfiles().iterator(); it.hasNext(); )
        {
            Profile profile = (Profile) it.next();

            Properties properties = new SortedProperties();
            properties.putAll( profile.getProperties() );
            profile.setProperties( properties );
        }
    }

    /**
     * @return the current host name or <code>unknown</code> if error
     * @see InetAddress#getLocalHost()
     */
    private static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch ( UnknownHostException e )
        {
            return "unknown";
        }
    }

    /**
     * @return the user name or <code>unknown</code> if <code>user.name</code> is not a system property.
     */
    private static String getUserName()
    {
        String userName = System.getProperty( "user.name" );
        if ( StringUtils.isEmpty( userName ) )
        {
            return "unknown";
        }

        return userName;
    }
}
