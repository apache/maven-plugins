package org.apache.maven.plugins.jlink;

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
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
public abstract class AbstractJLinkMojo
    extends AbstractMojo
{
    /**
     * <p>
     * Specify the requirements for this jdk toolchain. This overrules the toolchain selected by the
     * maven-toolchain-plugin.
     * </p>
     * <strong>note:</strong> requires at least Maven 3.3.1
     */
    @Parameter
    private Map<String, String> jdkToolchain;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Component
    private ToolchainManager toolchainManager;

    protected String getJLinkExecutable()
        throws IOException
    {
        Toolchain tc = getToolchain();

        String jLinkExecutable = null;
        if ( tc != null )
        {
            jLinkExecutable = tc.findTool( "jlink" );
        }

        String jLinkCommand = "jlink" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

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
                throw new IOException( "The jlink executable '" + jLinkExe + "' doesn't exist or is not a file." );
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
        // Try to find javadocExe from JAVA_HOME environment variable
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
                    getLog().error( output );
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
            throw new MojoExecutionException( "Unable to execute jlink command: " + e.getMessage(), e );
        }

    }

    private Toolchain getToolchain()
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

    protected MavenProject getProject()
    {
        return project;
    }

    protected MavenSession getSession()
    {
        return session;
    }

    /**
     * Returns the archive file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param finalName the name of the ear file
     * @param classifier an optional classifier
     * @return the file to generate
     */
    protected File getArchiveFile( File basedir, String finalName, String classifier, String archiveExt )
    {
        if ( basedir == null )
        {
            throw new IllegalArgumentException( "basedir is not allowed to be null" );
        }
        if ( finalName == null )
        {
            throw new IllegalArgumentException( "finalName is not allowed to be null" );
        }
        if ( archiveExt == null )
        {
            throw new IllegalArgumentException( "archiveExt is not allowed to be null" );
        }

        if ( finalName.isEmpty() )
        {
            throw new IllegalArgumentException( "finalName is not allowed to be empty." );
        }
        if ( archiveExt.isEmpty() )
        {
            throw new IllegalArgumentException( "archiveExt is not allowed to be empty." );
        }

        StringBuilder fileName = new StringBuilder( finalName );

        if ( hasClassifier( classifier ) )
        {
            fileName.append( "-" ).append( classifier );
        }

        fileName.append( '.' );
        fileName.append( archiveExt );

        return new File( basedir, fileName.toString() );
    }

    protected boolean hasClassifier( String classifier )
    {
        boolean result = false;
        if ( classifier != null && classifier.trim().length() > 0 )
        {
            result = true;
        }

        return result;
    }

}
