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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Print out the calculated settings for this project, given any profile enhancement and
 *  the inheritance of the global settings into the user-level settings.
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

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
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
}
