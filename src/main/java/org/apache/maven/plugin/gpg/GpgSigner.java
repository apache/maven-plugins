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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

public class GpgSigner
{

    public static final String SIGNATURE_EXTENSION = ".asc";

    private String executable;
 
    private boolean useAgent;

    private boolean isInteractive = true;

    private String keyname;

    private String passphrase;

    private File outputDir;

    private File buildDir;

    private File baseDir;

    private File homeDir;

    public void setExecutable( String executable )
    {
        this.executable = executable;
    }

    public void setInteractive( boolean b )
    {
        isInteractive = b;
    }

    public void setUseAgent( boolean b )
    {
        useAgent = b;
    }

    public void setKeyName( String s )
    {
        keyname = s;
    }

    public void setPassPhrase( String s )
    {
        passphrase = s;
    }

    public void setOutputDirectory( File out )
    {
        outputDir = out;
    }

    public void setBuildDirectory( File out )
    {
        buildDir = out;
    }

    public void setBaseDirectory( File out )
    {
        baseDir = out;
    }

    public void setHomeDirectory( File homeDirectory )
    {
        homeDir = homeDirectory;
    }

    public File generateSignatureForArtifact( File file )
        throws MojoExecutionException
    {
        File signature = new File( file + SIGNATURE_EXTENSION );

        boolean isInBuildDir = false;
        if ( buildDir != null )
        {
            File parent = signature.getParentFile();
            if ( buildDir.equals( parent ) )
            {
                isInBuildDir = true;
            }
        }
        if ( !isInBuildDir && outputDir != null )
        {
            String fileDirectory = "";
            File signatureDirectory = signature;

            while ( ( signatureDirectory = signatureDirectory.getParentFile() ) != null )
            {
                if ( !signatureDirectory.equals( baseDir ) )
                {
                    fileDirectory = signatureDirectory.getName() + File.separatorChar + fileDirectory;
                }
                else
                {
                    break;
                }
            }
            signatureDirectory = new File( outputDir, fileDirectory );
            if ( !signatureDirectory.exists() )
            {
                signatureDirectory.mkdirs();
            }
            signature = new File( signatureDirectory, file.getName() + SIGNATURE_EXTENSION );
        }

        if ( signature.exists() )
        {
            signature.delete();
        }

        Commandline cmd = new Commandline();

        if ( StringUtils.isNotEmpty( executable ) )
        {
            cmd.setExecutable( executable );
        }
        else
        {
            cmd.setExecutable( "gpg" + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" ) );
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

        cmd.createArg().setValue( "--output" );
        cmd.createArg().setFile( signature );

        cmd.createArg().setFile( file );

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

        return signature;
    }

    private MavenProject findReactorProject( MavenProject prj )
    {
        if ( prj.getParent() != null && prj.getParent().getBasedir() != null && prj.getParent().getBasedir().exists() )
        {
            return findReactorProject( prj.getParent() );
        }
        return prj;
    }

    public String getPassphrase( MavenProject project )
        throws IOException
    {
        String pass = null;

        if ( project != null )
        {
            pass = project.getProperties().getProperty( "gpg.passphrase" );
            if ( pass == null )
            {
                MavenProject prj2 = findReactorProject( project );
                pass = prj2.getProperties().getProperty( "gpg.passphrase" );
            }
        }
        if ( pass == null )
        {
            // TODO: with JDK 1.6, we could call System.console().readPassword("GPG Passphrase: ", null);

            BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
            while ( System.in.available() != 0 )
            {
                // there's some junk already on the input stream, consume it
                // so we can get the real passphrase
                System.in.read();
            }

            System.out.print( "GPG Passphrase:  " );
            MaskingThread thread = new MaskingThread();
            thread.start();

            pass = in.readLine();

            // stop masking
            thread.stopMasking();
        }
        if ( project != null )
        {
            findReactorProject( project ).getProperties().setProperty( "gpg.passphrase", pass );
        }
        return pass;
    }

    // based on ideas from http://java.sun.com/developer/technicalArticles/Security/pwordmask/
    class MaskingThread
        extends Thread
    {
        private volatile boolean stop;

        /**
         * Begin masking until asked to stop.
         */
        public void run()
        {
            // this needs to be high priority to make sure the characters don't
            // really get to the screen.

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority( Thread.MAX_PRIORITY );

            try
            {
                stop = false;
                while ( !stop )
                {
                    // print a backspace + * to overwrite anything they type
                    System.out.print( "\010*" );
                    try
                    {
                        // attempt masking at this rate
                        Thread.sleep( 1 );
                    }
                    catch ( InterruptedException iex )
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally
            {
                // restore the original priority
                Thread.currentThread().setPriority( priority );
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking()
        {
            this.stop = true;
        }
    }

}
