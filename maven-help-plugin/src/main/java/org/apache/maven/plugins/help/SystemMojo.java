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
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
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

        message.append( '\n' );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );
        message.append( StringUtils.repeat( "=", REPEAT ) );
        message.append( " Platform Properties Details " );
        message.append( StringUtils.repeat( "=", REPEAT ) ).append( '\n' );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );
        message.append( '\n' );

        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );
        message.append( "System Properties" ).append( '\n' );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );

        Properties systemProperties = System.getProperties();
        for ( Iterator<?> it = systemProperties.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            message.append( "\n" );
            message.append( key ).append( "=" ).append( systemProperties.get( key ) );
        }

        message.append( '\n' ).append( '\n' );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );
        message.append( "Environment Variables" ).append( '\n' );
        message.append( StringUtils.repeat( "=", LINE_LENGTH ) ).append( '\n' );
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            for ( Iterator<?> it2 = envVars.keySet().iterator(); it2.hasNext(); )
            {
                String key = it2.next().toString();
                message.append( "\n" );
                message.append( key ).append( "=" ).append( envVars.get( key ) );
            }
        }
        catch ( IOException e )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "IOException: " + e.getMessage() );
            }
        }

        message.append( "\n" );

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
                throw new MojoExecutionException( "Cannot write system report to output: " + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "System report written to: " + output );
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
}
