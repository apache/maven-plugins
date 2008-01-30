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
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Lists the platform details like system properties and environment variables.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 * @goal system
 */
public class SystemMojo
    extends AbstractMojo
{
    /**
     * Optional parameter for a file destination for the output of this mojo.
     *
     * @parameter expression="${output}"
     */
    private File output;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        StringBuffer message = new StringBuffer();

        message.append( "===== Platform Details =====" ).append( '\n' );
        message.append( '\n' );
        message.append( "===== System Properties =====" ).append( '\n' );

        Properties systemProperties = System.getProperties();
        for ( Iterator it = systemProperties.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            message.append( "\n" );
            message.append( key ).append( "=" ).append( systemProperties.get( key ) );
        }

        message.append( '\n' ).append( '\n' );
        message.append( "===== Environment Variables =====" ).append( '\n' );
        try
        {
            Properties envVars = CommandLineUtils.getSystemEnvVars();
            for ( Iterator it2 = envVars.keySet().iterator(); it2.hasNext(); )
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
            writeFile( message );
        }
        else
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( message );
            }
        }
    }

    /**
     * Method for writing the output file of the active profiles information.
     *
     * @param message the output to be written to the file
     * @throws MojoExecutionException if any
     */
    private void writeFile( StringBuffer message )
        throws MojoExecutionException
    {
        Writer writer = null;
        try
        {
            File dir = output.getParentFile();
            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            writer = WriterFactory.newPlatformWriter( output );

            writer.write( "Created by: " + getClass().getName() + "\n" );
            writer.write( "Created on: " + new Date() + "\n\n" );
            writer.write( message.toString() );
            writer.flush();

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "System report written to: " + output );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write output to file: " + output, e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "Failed to close output file writer.", e );
                    }
                }
            }
        }
    }
}
