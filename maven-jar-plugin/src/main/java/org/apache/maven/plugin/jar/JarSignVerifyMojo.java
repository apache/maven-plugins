package org.apache.maven.plugin.jar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Checks the signature of a signed jar using jarsigner.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * @goal sign-verify
 * @phase package
 * @requiresProject
 * @todo refactor the common code with javadoc plugin
 * @requiresDependencyResolution runtime
 * @deprecated As of version 2.3, this goal is no longer supported in favor of the dedicated maven-jarsigner-plugin.
 */
public class JarSignVerifyMojo
    extends AbstractMojo
{
    /**
     * The working directory in which the jarsigner executable will be run.
     *
     * @parameter expression="${workingdir}" default-value="${basedir}"
     * @required
     */
    private File workingDirectory;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Name of the generated JAR (without classifier and extension).
     *
     * @parameter alias="jarname" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Path of the signed jar. When specified, the finalName is ignored.
     *
     * @parameter expression="${jarpath}"
     */
    private File jarPath;

    /**
     * Check certificates. Requires {@link #setVerbose(boolean)}.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${checkcerts}" default-value="false"
     */
    private boolean checkCerts;

    /**
     * Enable verbose
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    /** When <code>true</code> this will make the execute() operation fail,
     * throwing an exception, when verifying a non signed jar.
     * Primarily to keep backwards compatibility with existing code, and allow reusing the
     * bean in unattended operations when set to <code>false</code>.
     *
     * @parameter expression="${errorWhenNotSigned}" default-value="true"
     **/
    private boolean errorWhenNotSigned = true;

    /**
     * Is the jar signed ? output property set by the execute call. The value will be accessible
     * when execute() ends and if errorWhenNotSigned has been set to false.
     **/
    private boolean signed;

    File getJarFile()
    {
        if ( jarPath != null )
        {
            return jarPath;
        }
        else
        {
            return AbstractJarMojo.getJarFile( basedir, finalName, null);
        }
    }

    public void execute()
        throws MojoExecutionException
    {
        List arguments = new ArrayList();

        Commandline commandLine = new Commandline();

        commandLine.setExecutable( getJarsignerPath() );

        arguments.add( "-verify" );

        addArgIf( arguments, this.verbose, "-verbose" );
        addArgIf( arguments, this.checkCerts, "-certs" );

        arguments.add( getJarFile() );

        for ( Iterator it = arguments.iterator() ; it.hasNext() ; )
        {
            commandLine.createArgument().setValue( it.next().toString() );
        }

        commandLine.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        getLog().debug("Executing: " + commandLine );

        LineMatcherStreamConsumer outConsumer = new LineMatcherStreamConsumer( "jar verified." );

        final StringBuffer errBuffer = new StringBuffer();
        StreamConsumer errConsumer = new StreamConsumer()
        {
            public void consumeLine(String line)
            {
                 errBuffer.append( line );
                 getLog().warn( line );
            }
        };


        try
        {
            int result = executeCommandLine( commandLine, null, outConsumer, errConsumer );

            if ( result != 0 )
            {
                throw new MojoExecutionException("Result of " + commandLine
                    + " execution is: \'" + result + "\'." );
            }

            signed = outConsumer.matched;

            if ( !signed && errorWhenNotSigned )
            {
                throw new MojoExecutionException( "Verify failed: " + outConsumer.firstOutLine );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "command execution failed", e );
        }
    }

    // checks if a consumed line matches
    // also keeps track of the first consumed line.
    class LineMatcherStreamConsumer
        implements StreamConsumer
    {
        private String toMatch;
        private boolean matched;
        private String firstOutLine;

        LineMatcherStreamConsumer( String toMatch )
        {
             this.toMatch = toMatch;
        }

        public void consumeLine(String line)
        {
            if ( firstOutLine == null )
            {
                 firstOutLine = line;
            }
            matched = matched || toMatch.equals( line );

            getLog().info( line );
        }
    }

  // taken from JavadocReport then slightly refactored
    // should probably share with other plugins that use $JAVA_HOME/bin tools

    /**
     * Get the path of jarsigner tool depending the OS.
     *
     * @return the path of the jarsigner tool
     */
    private String getJarsignerPath()
    {
        return getJDKCommandPath( "jarsigner", getLog() );
    }

    private static String getJDKCommandPath( String command, Log logger )
    {
        String path = getJDKCommandExe(command).getAbsolutePath();
        logger.debug( command + " executable=[" + path + "]" );
        return path;
    }

    private static File getJDKCommandExe( String command )
    {
        String fullCommand = command + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File exe;

        // For IBM's JDK 1.2
        if ( SystemUtils.IS_OS_AIX )
        {
            exe = new File( SystemUtils.getJavaHome() + "/../sh", fullCommand );
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            exe = new File( SystemUtils.getJavaHome() + "/bin", fullCommand );
        }
        else
        {
            exe = new File( SystemUtils.getJavaHome() + "/../bin", fullCommand );
        }

        return exe;
    }


    // Helper methods. Could/should be shared e.g. with JavadocReport

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * conditionally based on the given flag.
     *
     * @param arguments
     * @param b the flag which controls if the argument is added or not.
     * @param value the argument value to be added.
     */
    private void addArgIf( List arguments, boolean b, String value )
    {
        if ( b )
        {
            arguments.add( value );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key the argument name.
     * @param value the argument value to be added.
     * @see #addArgIfNotEmpty(java.util.List,String,String,boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value )
    {
        addArgIfNotEmpty( arguments, key, value, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            arguments.add( key );

            StringTokenizer token = new StringTokenizer( value, "," );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();

                if ( !StringUtils.isEmpty( current ) )
                {
                    arguments.add( current );

                    if ( token.hasMoreTokens() && repeatKey )
                    {
                        arguments.add( key );
                    }
                }
            }
        }
    }

    //
    // methods used for tests purposes - allow mocking and simulate automatic setters
    //

    protected int executeCommandLine( Commandline commandLine, InputStream inputStream,
                                      StreamConsumer systemOut, StreamConsumer systemErr )
        throws CommandLineException
    {
        return CommandLineUtils.executeCommandLine( commandLine, inputStream, systemOut, systemErr );
    }

    public void setWorkingDir( File workingDir )
    {
        this.workingDirectory = workingDir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    // hiding for now - I don't think this is required to be seen
    /*
    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }
    */

    public void setJarPath( File jarPath )
    {
        this.jarPath = jarPath;
    }

    public void setCheckCerts( boolean checkCerts )
    {
        this.checkCerts = checkCerts;
    }

    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    /**
     * Is the JAR file signed ? Output property set by the {@link #execute()} call.
     *
     * @return <code>true</code> if the jar was signed, <code>false</code> otherwise.
     */
    public boolean isSigned()
    {
        return signed;
    }

    /**
     * Sets a boolean that is to determine if an exception should be thrown when
     * the JAR file being verified is unsigned. If you just what to check if a
     * JAR is unsigned and then act on the result, then you probably want to
     * set this to <code>true</code>.
     */
    public void setErrorWhenNotSigned( boolean errorWhenNotSigned )
    {
        this.errorWhenNotSigned = errorWhenNotSigned;
    }
}
