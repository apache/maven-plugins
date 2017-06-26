package org.apache.maven.plugins.jmod;

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
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 *
 */
public abstract class AbstractJModMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Component
    private ToolchainManager toolchainManager;
    
    protected String getJModExecutable() throws IOException
    {
        Toolchain tc = getToolchain();

        String jLinkExecutable = null;
        if ( tc != null )
        {
            jLinkExecutable = tc.findTool( "jmod" );
        }

        String jLinkCommand = "jmod" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File jLinkExe;

        if ( StringUtils.isNotEmpty( jLinkExecutable ) )
        {
            jLinkExe = new File( jLinkExecutable );

            if ( jLinkExe.isDirectory() )
            {
                jLinkExe = new File( jLinkExe, jLinkCommand );
            }

            if ( SystemUtils.IS_OS_WINDOWS && jLinkExe.getName().indexOf( '.' ) < 0 )
            {
                jLinkExe = new File( jLinkExe.getPath() + ".exe" );
            }

            if ( !jLinkExe.isFile() )
            {
                throw new IOException( "The jlink executable '" + jLinkExe
                    + "' doesn't exist or is not a file." );
            }
            return jLinkExe.getAbsolutePath();
        }

        // ----------------------------------------------------------------------
        // Try to find jlink from System.getProperty( "java.home" )
        // By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
        // should be in the JDK_HOME
        // ----------------------------------------------------------------------
        // For IBM's JDK 1.2 
        // Really ?
        if ( SystemUtils.IS_OS_AIX )
        {
            jLinkExe =
                new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "sh", jLinkCommand );
        }
        // For Apple's JDK 1.6.x (and older?) on Mac OSX
        // CHECKSTYLE_OFF: MagicNumber
        else if ( SystemUtils.IS_OS_MAC_OSX && SystemUtils.JAVA_VERSION_FLOAT < 1.7f )
        // CHECKSTYLE_ON: MagicNumber
        {
            jLinkExe = new File( SystemUtils.getJavaHome() + File.separator + "bin", jLinkCommand );
        }
        else
        {
            jLinkExe =
                new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin", jLinkCommand );
        }

        // ----------------------------------------------------------------------
        // Try to find jlink from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !jLinkExe.exists() || !jLinkExe.isFile() )
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

            jLinkExe = new File( javaHome + File.separator + "bin", jLinkCommand );
        }

        if ( !jLinkExe.getCanonicalFile().exists() || !jLinkExe.getCanonicalFile().isFile() )
        {
            throw new IOException( "The jlink executable '" + jLinkExe
                + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return jLinkExe.getAbsolutePath();
    }
    
    protected boolean projectHasAlreadySetAnArtifact()
    {
        if ( getProject().getArtifact().getFile() != null )
        {
            return getProject().getArtifact().getFile().isFile();
        }
        else
        {
            return false;
        }
    }
    
    protected void executeCommand ( Commandline cmd, File outputDirectory ) throws MojoExecutionException
    {
        if ( getLog().isDebugEnabled() )
        {
            // no quoted arguments ???
            getLog().debug( CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" ) );
        }

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

            String output = ( StringUtils.isEmpty( out.getOutput() ) ? null : '\n' + out.getOutput().trim() );

            if ( exitCode != 0 )
            {
                if ( StringUtils.isNotEmpty( output ) )
                {
                    //Reconsider to use WARN / ERROR ?
                    getLog().info( output );
                }

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

            if ( StringUtils.isNotEmpty( output ) )
            {
                getLog().info( output );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute jmod command: " + e.getMessage(), e );
        }

    }
    
    private Toolchain getToolchain()
    {
        Toolchain tc = null;
        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }

        return tc;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public MavenSession getSession()
    {
        return session;
    }


}
