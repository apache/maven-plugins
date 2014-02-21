package org.apache.maven.plugin.gpg;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * A signer implementation that uses the GnuPG command line executable.
 */
public class GpgSigner
    extends AbstractGpgSigner
{
    private String executable;

    public GpgSigner( String executable )
    {
        this.executable = executable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void generateSignatureForFile( File file, File signature )
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // Set up the command line
        // ----------------------------------------------------------------------------

        Commandline cmd = new Commandline();

        if ( StringUtils.isNotEmpty( executable ) )
        {
            cmd.setExecutable( executable );
        }
        else
        {
            cmd.setExecutable( "gpg" + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" ) );
        }

        if ( args != null )
        {
            for ( String arg : args )
            {
                cmd.createArg().setValue( arg );
            }
        }

        if ( homeDir != null )
        {
            cmd.createArg().setValue( "--homedir" );
            cmd.createArg().setFile( homeDir );
        }

        if ( useAgent )
        {
            cmd.createArg().setValue( "--use-agent" );
        }
        else
        {
            cmd.createArg().setValue( "--no-use-agent" );
        }

        InputStream in = null;
        if ( null != passphrase )
        {
            // make --passphrase-fd effective in gpg2
            cmd.createArg().setValue( "--batch" );

            cmd.createArg().setValue( "--passphrase-fd" );

            cmd.createArg().setValue( "0" );

            // Prepare the input stream which will be used to pass the passphrase to the executable
            in = new ByteArrayInputStream( passphrase.getBytes() );
        }

        if ( null != keyname )
        {
            cmd.createArg().setValue( "--local-user" );

            cmd.createArg().setValue( keyname );
        }

        cmd.createArg().setValue( "--armor" );

        cmd.createArg().setValue( "--detach-sign" );

        if ( !isInteractive )
        {
            cmd.createArg().setValue( "--no-tty" );
        }

        if ( !defaultKeyring )
        {
            cmd.createArg().setValue( "--no-default-keyring" );
        }

        if ( StringUtils.isNotEmpty( secretKeyring ) )
        {
            cmd.createArg().setValue( "--secret-keyring" );
            cmd.createArg().setValue( secretKeyring );
        }

        if ( StringUtils.isNotEmpty( publicKeyring ) )
        {
            cmd.createArg().setValue( "--keyring" );
            cmd.createArg().setValue( publicKeyring );
        }

        if ( "once".equalsIgnoreCase( lockMode ) )
        {
            cmd.createArg().setValue( "--lock-once" );
        }
        else if ( "multiple".equalsIgnoreCase( lockMode ) )
        {
            cmd.createArg().setValue( "--lock-multiple" );
        }
        else if ( "never".equalsIgnoreCase( lockMode ) )
        {
            cmd.createArg().setValue( "--lock-never" );
        }

        cmd.createArg().setValue( "--output" );
        cmd.createArg().setFile( signature );

        cmd.createArg().setFile( file );

        // ----------------------------------------------------------------------------
        // Execute the command line
        // ----------------------------------------------------------------------------

        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, in, new DefaultConsumer(), new DefaultConsumer() );

            if ( exitCode != 0 )
            {
                throw new MojoExecutionException( "Exit code: " + exitCode );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute gpg command", e );
        }
    }

}
