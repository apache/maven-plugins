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

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Displays a list of the platform details like system properties and environment variables.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
@Mojo( name = "system", requiresProject = false )
public class SystemMojo
    extends AbstractHelpMojo
{
    /** Magic number to beautify the output */
    private static final int REPEAT = 25;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        StringBuilder message = new StringBuilder();

        message.append( LS );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );
        message.append( StringUtils.repeat( "=", REPEAT ) );
        message.append( " Platform Properties Details " );
        message.append( StringUtils.repeat( "=", REPEAT ) ).append( LS );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );
        message.append( LS );

        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );
        message.append( "System Properties" ).append( LS );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );

        Properties systemProperties = System.getProperties();
        for ( String key : systemProperties.stringPropertyNames() )
        {
            message.append( LS );
            message.append( key ).append( "=" ).append( systemProperties.getProperty( key ) );
        }

        message.append( LS ).append( LS );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );
        message.append( "Environment Variables" ).append( LS );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( LS );
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            for ( String key : envVars.stringPropertyNames() )
            {
                message.append( LS );
                message.append( key ).append( "=" ).append( envVars.getProperty( key ) );
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "Unable to get the environment variables: " + e.getMessage() );
        }

        message.append( LS );

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
                throw new MojoExecutionException( "Cannot write system report to output: " + output, e );
            }

            getLog().info( "System report written to: " + output );
        }
        else
        {
            getLog().info( message );
        }
    }
}
