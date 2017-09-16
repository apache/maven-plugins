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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
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
 * This contains the code to handle toolchains and the execution of the command which is similar to code in
 * maven-jlink-plugin (maven-jdeps-plugin?). Later we need to think about a way to combine that code to reduce
 * duplication.
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
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

    /**
     * <p>
     * Specify the requirements for this jdk toolchain. This overrules the toolchain selected by the
     * maven-toolchain-plugin.
     * </p>
     * <strong>note:</strong> requires at least Maven 3.3.1
     */
    @Parameter
    private Map<String, String> jdkToolchain;
    
    // TODO: Check how to prevent code duplication in maven-jlink, maven-jmod and maven-jdeps plugin?
    protected String getJModExecutable()
        throws IOException
    {
        Toolchain tc = getToolchain();

        String jModExecutable = null;
        if ( tc != null )
        {
            jModExecutable = tc.findTool( "jmod" );
        }

        String jModCommand = "jmod" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File jModExe;

        if ( StringUtils.isNotEmpty( jModExecutable ) )
        {
            jModExe = new File( jModExecutable );

            if ( jModExe.isDirectory() )
            {
                jModExe = new File( jModExe, jModCommand );
            }

            if ( SystemUtils.IS_OS_WINDOWS && jModExe.getName().indexOf( '.' ) < 0 )
            {
                jModExe = new File( jModExe.getPath() + ".exe" );
            }

            if ( !jModExe.isFile() )
            {
                throw new IOException( "The jmod executable '" + jModExe + "' doesn't exist or is not a file." );
            }
            return jModExe.getAbsolutePath();
        }

        // ----------------------------------------------------------------------
        // Try to find jmod from System.getProperty( "java.home" )
        // By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
        // should be in the JDK_HOME
        // ----------------------------------------------------------------------
        jModExe = new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin", jModCommand );

        // ----------------------------------------------------------------------
        // Try to find jmod from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !jModExe.exists() || !jModExe.isFile() )
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

            jModExe = new File( javaHome + File.separator + "bin", jModCommand );
        }

        if ( !jModExe.getCanonicalFile().exists() || !jModExe.getCanonicalFile().isFile() )
        {
            throw new IOException( "The jmod executable '" + jModExe
                + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return jModExe.getAbsolutePath();
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

    protected void executeCommand( Commandline cmd, File outputDirectory )
        throws MojoExecutionException
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
                    // Reconsider to use WARN / ERROR ?
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
                String[] splitLines = StringUtils.split( output, "\n" );
                for ( String outputLine : splitLines )
                {
                    getLog().info( outputLine );
                }
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute jmod command: " + e.getMessage(), e );
        }

    }

    /**
     * Convert a list into a
     * 
     * @param modules The list of modules.
     * @return The string with the module list which is separated by {@code ,}.
     */
    protected String getCommaSeparatedList( List<String> modules )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : modules )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( module );
        }
        return sb.toString();
    }

    protected Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( jdkToolchain != null )
        {
            // Maven 3.3.1 has plugin execution scoped Toolchain Support
            try
            {
                Method getToolchainsMethod = toolchainManager.getClass().getMethod( "getToolchains", MavenSession.class,
                                                                                    String.class, Map.class );

                @SuppressWarnings( "unchecked" )
                List<Toolchain> tcs =
                    (List<Toolchain>) getToolchainsMethod.invoke( toolchainManager, session, "jdk", jdkToolchain );

                if ( tcs != null && tcs.size() > 0 )
                {
                    tc = tcs.get( 0 );
                }
            }
            catch ( NoSuchMethodException e )
            {
                // ignore
            }
            catch ( SecurityException e )
            {
                // ignore
            }
            catch ( IllegalAccessException e )
            {
                // ignore
            }
            catch ( IllegalArgumentException e )
            {
                // ignore
            }
            catch ( InvocationTargetException e )
            {
                // ignore
            }
        }

        if ( tc == null )
        {
            // TODO: Check if we should make the type configurable?
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
