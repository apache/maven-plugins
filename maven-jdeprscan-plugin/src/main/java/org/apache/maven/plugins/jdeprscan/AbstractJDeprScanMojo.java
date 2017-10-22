package org.apache.maven.plugins.jdeprscan;

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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Abstract class for all mojos
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public abstract class AbstractJDeprScanMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;
    
    @Component
    private ToolchainManager toolchainManager;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String jExecutable;
        try
        {
            jExecutable = getJDeprScanExecutable();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Unable to find jdeprscan command: " + e.getMessage(), e );
        }

        // Synopsis
        // jdeprscan [options] {dir|jar|class} ...
        Commandline cmd = new Commandline();
        cmd.setExecutable( jExecutable );

        addJDeprScanOptions( cmd );

        executeJDeprScanCommandLine( cmd, getConsumer() );
        
        verify();
    }
    
    protected CommandLineUtils.StringStreamConsumer getConsumer()
    {
      return null;    
    }
    
    protected void verify() throws MojoExecutionException
    {
    }
    
    protected abstract boolean isForRemoval();

    protected void addJDeprScanOptions( Commandline cmd ) throws MojoFailureException
    {
        if ( isForRemoval() )
        {
            cmd.createArg().setValue( "--for-removal" );
        }
    }

    private String getJDeprScanExecutable()
        throws IOException
    {
        Toolchain tc = getToolchain();

        String jdeprscanExecutable = null;
        if ( tc != null )
        {
            jdeprscanExecutable = tc.findTool( "jdeprscan" );
        }

        String jdepsCommand = "jdeprscan" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File jdeprscanExe;

        if ( StringUtils.isNotEmpty( jdeprscanExecutable ) )
        {
            jdeprscanExe = new File( jdeprscanExecutable );

            if ( jdeprscanExe.isDirectory() )
            {
                jdeprscanExe = new File( jdeprscanExe, jdepsCommand );
            }

            if ( SystemUtils.IS_OS_WINDOWS && jdeprscanExe.getName().indexOf( '.' ) < 0 )
            {
                jdeprscanExe = new File( jdeprscanExe.getPath() + ".exe" );
            }

            if ( !jdeprscanExe.isFile() )
            {
                throw new IOException( "The jdeprscan executable '" + jdeprscanExe
                    + "' doesn't exist or is not a file." );
            }
            return jdeprscanExe.getAbsolutePath();
        }

        jdeprscanExe =
            new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "sh", jdepsCommand );

        // ----------------------------------------------------------------------
        // Try to find jdepsExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !jdeprscanExe.exists() || !jdeprscanExe.isFile() )
        {
            Properties env = CommandLineUtils.getSystemEnvVars();
            String javaHome = env.getProperty( "JAVA_HOME" );
            if ( StringUtils.isEmpty( javaHome ) )
            {
                throw new IOException( "The environment variable JAVA_HOME is not correctly set." );
            }
            if ( ( !new File( javaHome ).getCanonicalFile().exists() )
                || ( new File( javaHome ).getCanonicalFile().isFile() ) )
            {
                throw new IOException( "The environment variable JAVA_HOME=" + javaHome
                    + " doesn't exist or is not a valid directory." );
            }

            jdeprscanExe = new File( javaHome + File.separator + "bin", jdepsCommand );
        }

        if ( !jdeprscanExe.getCanonicalFile().exists() || !jdeprscanExe.getCanonicalFile().isFile() )
        {
            throw new IOException( "The jdeps executable '" + jdeprscanExe
                + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return jdeprscanExe.getAbsolutePath();
    }

    private void executeJDeprScanCommandLine( Commandline cmd,
                                          CommandLineUtils.StringStreamConsumer consumer )
        throws MojoExecutionException
    {
        if ( getLog().isDebugEnabled() )
        {
            // no quoted arguments
            getLog().debug( "Executing: " + CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" ) );
        }

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out;
        if ( consumer != null )
        {
            out = consumer;
        }
        else
        {
            out = new CommandLineUtils.StringStreamConsumer();
        }

        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

            String output = ( StringUtils.isEmpty( out.getOutput() ) ? null : '\n' + out.getOutput().trim() );

            if ( StringUtils.isNotEmpty( output ) )
            {
                getLog().info( output );
            }

            if ( exitCode != 0 )
            {
                StringBuilder msg = new StringBuilder( "\nExit code: " );
                msg.append( exitCode );
                if ( StringUtils.isNotEmpty( err.getOutput() ) )
                {
                    msg.append( " - " ).append( err.getOutput() );
                }
                msg.append( '\n' );
                msg.append( "Command line was: " ).append( cmd ).append( '\n' ).append( '\n' );

                throw new MojoExecutionException( msg.toString() );
            }

        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute jdeprscan command: " + e.getMessage(), e );
        }

        // ----------------------------------------------------------------------
        // Handle JDeprScan warnings
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( err.getOutput() ) && getLog().isWarnEnabled() )
        {
            getLog().warn( "JDeprScan Warnings" );

            StringTokenizer token = new StringTokenizer( err.getOutput(), "\n" );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();

                getLog().warn( current );
            }
        }
    }

    private Toolchain getToolchain()
    {
        Toolchain tc = null;
        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );

            if ( tc == null )
            {
                // Maven 3.2.6 has plugin execution scoped Toolchain Support
                try
                {
                    Method getToolchainsMethod =
                        toolchainManager.getClass().getMethod( "getToolchains", MavenSession.class, String.class,
                                                               Map.class );

                    @SuppressWarnings( "unchecked" )
                    List<Toolchain> tcs =
                        (List<Toolchain>) getToolchainsMethod.invoke( toolchainManager, session, "jdk",
                                                                      Collections.singletonMap( "version", "[9,)" ) );

                    if ( tcs != null && tcs.size() > 0 )
                    {
                        // pick up latest, jdeps of JDK9 has more options compared to JDK8
                        tc = tcs.get( tcs.size() - 1 );
                    }
                }
                catch ( ReflectiveOperationException e )
                {
                    // ignore
                }
                catch ( SecurityException e )
                {
                    // ignore
                }
                catch ( IllegalArgumentException e )
                {
                    // ignore
                }
            }
        }

        return tc;
    }
}
