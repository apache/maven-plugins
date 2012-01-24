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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Signs a JAR using jarsigner.
 *
 * @author <a href="jerome@coffeebreaks.org">Jerome Lacoste</a>
 * @version $Id$
 * @goal sign
 * @phase package
 * @requiresProject
 * @todo refactor the common code with javadoc plugin
 * @requiresDependencyResolution runtime
 * @deprecated As of version 2.3, this goal is no longer supported in favor of the dedicated maven-jarsigner-plugin.
 */
public class JarSignMojo
    extends AbstractMojo
{
    /**
     * Set this to <code>true</code> to disable signing.
     * Useful to speed up build process in development environment.
     *
     * @parameter expression="${maven.jar.sign.skip}" default-value="false"
     */
    private boolean skip;

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
     * Path of the jar to sign. When specified, the finalName is ignored.
     *
     * @parameter alias="jarpath" default-value="${project.build.directory}/${project.build.finalName}.${project.packaging}"
     */
    private File jarPath;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keystore}"
     */
    private String keystore;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${storepass}"
     */
    private String storepass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keypass}"
     */
    private String keypass;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${sigfile}"
     * @todo make a File?
     */
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     * <p/>
     * Not specifying this argument will sign the jar in-place (your original jar is going to be overwritten).
     *
     * @parameter expression="${signedjar}"
     */
    private File signedjar;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     * The corresponding option in the command line is -storetype.
     *
     * @parameter expression="${type}"
     */
    private String type;

    /**
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${alias}"
     * @required
     */
    private String alias;

    /**
     * Automatically verify a jar after signing it.
     * <p/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${verify}" default-value="false"
     */
    private boolean verify;

    /**
     * Skip attaching the signed artifact. By default the signed artifact is attached.
     * This is not a Mojo parameter as we shouldn't need this when using this mojo.
     * Just needed when reusing the implementation. See MJAR-84 for discussions.
     */
    private boolean skipAttachSignedArtifact;

    /**
     * Enable verbose.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Classifier to use for the generated artifact.
     * If not specified, the generated artifact becomes the primary artifact.
     *
     * @parameter expression="${classifier}"
     */
    private String classifier;

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping JAR signing for file: " + getJarFile().getAbsolutePath() );
            return;
        }

        if ( project != null )
        {
            ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
            if ( artifactHandler != null && !"java".equals( artifactHandler.getLanguage() ) )
            {
                getLog().debug( "Not executing jar:sign as the project is not a Java module" );
                return;
            }
        }

        // we use this mojo to check if there's a need to sign.
        // If we sign and if we need to verify, we reuse it to check the signature
        JarSignVerifyMojo verifyMojo = createJarSignVerifyMojo();

        verifyMojo.setWorkingDir( workingDirectory );

        verifyMojo.setBasedir( basedir );

        File signedJarFile = signedjar != null ? signedjar : getJarFile();

        verifyMojo.setVerbose( verbose );

        verifyMojo.setJarPath( signedJarFile );

        if ( signedJarFile.exists() )
        {
            verifyMojo.setErrorWhenNotSigned( false );
            verifyMojo.execute();
        }

        if ( verifyMojo.isSigned() )
        {
            getLog().info( "JAR " + signedJarFile.getAbsoluteFile() + " is already signed. Skipping." );
            return;
        }

        signJar();

        if ( this.verify )
        {
            verifyMojo.setErrorWhenNotSigned( true );
            verifyMojo.execute();
        }
    }

    protected JarSignVerifyMojo createJarSignVerifyMojo()
    {
        return new JarSignVerifyMojo();
    }


    File getJarFile()
    {
        if ( jarPath != null )
        {
            return jarPath;
        }
        else
        {
            return AbstractJarMojo.getJarFile( basedir, finalName, null );
        }
    }

    void signJar()
        throws MojoExecutionException
    {
        List arguments = new ArrayList();

        Commandline commandLine = new Commandline();

        commandLine.setExecutable( getJarsignerPath() );

        addArgIf( arguments, verbose, "-verbose" );

        // I believe Commandline to add quotes where appropriate, although I haven't tested it enough.
        // FIXME addArgIfNotEmpty will break those parameters containing a space.
        // Look at webapp:gen-keystore for a way to fix that
        addArgIfNotEmpty( arguments, "-keystore", this.keystore );
        addArgIfNotEmpty( arguments, "-storepass", this.storepass );
        addArgIfNotEmpty( arguments, "-keypass", this.keypass );
        addArgIfNotEmpty( arguments, "-signedjar", this.signedjar );
        addArgIfNotEmpty( arguments, "-storetype", this.type );
        addArgIfNotEmpty( arguments, "-sigfile", this.sigfile );

        arguments.add( getJarFile() );

        addArgIf( arguments, alias != null, this.alias );

        for ( Iterator it = arguments.iterator(); it.hasNext(); )
        {
            commandLine.createArgument().setValue( it.next().toString() );
        }

        commandLine.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        createParentDirIfNecessary( signedjar );

        if ( signedjar == null )
        {
            getLog().debug( "Signing JAR in-place (overwritting original JAR)." );
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Executing: " + purgePassword( commandLine ) );
        }

        // jarsigner may ask for some input if the parameters are missing or incorrect.
        // This should take care of it and make it fail gracefully
        final InputStream inputStream = new InputStream()
        {
            public int read()
            {
                return -1;
            }
        };
        StreamConsumer outConsumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                getLog().info( line );
            }
        };
        final StringBuffer errBuffer = new StringBuffer();
        StreamConsumer errConsumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                errBuffer.append( line );
                getLog().warn( line );
            }
        };

        try
        {
            int result = executeCommandLine( commandLine, inputStream, outConsumer, errConsumer );

            if ( result != 0 )
            {
                throw new MojoExecutionException( "Result of " + purgePassword( commandLine ) +
                    " execution is: \'" + result + "\'." );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "command execution failed", e );
        }

        // signed in place, no need to attach
        if ( signedjar == null || skipAttachSignedArtifact )
        {
            return;
        }

        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, "jar", classifier, signedjar );
        }
        else
        {
            project.getArtifact().setFile( signedjar );
        }
    }

    private String purgePassword( Commandline commandLine )
    {
        String out = commandLine.toString();
        if ( keypass != null && out.indexOf( keypass ) != -1 )
        {
            out = StringUtils.replace( out, keypass, "******" );
        }
        return out;
    }

    private void createParentDirIfNecessary( File file )
    {
        if ( file != null )
        {
            File fileDir = file.getParentFile();

            if ( fileDir != null )
            { // not a relative path
                boolean mkdirs = fileDir.mkdirs();
                getLog().debug( "mdkirs: " + mkdirs + " " + fileDir );
            }
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
        String path = getJDKCommandExe( command ).getAbsolutePath();
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
     * @param b         the flag which controls if the argument is added or not.
     * @param value     the argument value to be added.
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
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key       the argument name.
     * @param value     the argument value to be added.
     * @see #addArgIfNotEmpty(java.util.List,String,Object,boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, Object value )
    {
        addArgIfNotEmpty( arguments, key, value, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key       the argument name.
     * @param value     the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List arguments, String key, Object value, boolean repeatKey )
    {
        if ( value != null && !StringUtils.isEmpty( value.toString() ) )
        {
            arguments.add( key );

            StringTokenizer token = new StringTokenizer( value.toString(), "," );
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

    protected int executeCommandLine( Commandline commandLine, InputStream inputStream, StreamConsumer stream1,
                                      StreamConsumer stream2 )
        throws CommandLineException
    {
        return CommandLineUtils.executeCommandLine( commandLine, inputStream, stream1, stream2 );
    }

    public void setWorkingDir( File workingDir )
    {
        this.workingDirectory = workingDir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public void setKeystore( String keystore )
    {
        this.keystore = keystore;
    }

    public void setKeypass( String keypass )
    {
        this.keypass = keypass;
    }

    public void setSignedJar( File signedjar )
    {
        this.signedjar = signedjar;
    }

    public void setAlias( String alias )
    {
        this.alias = alias;
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

    public void setStorepass( String storepass )
    {
        this.storepass = storepass;
    }

    public void setSigFile( String sigfile )
    {
        this.sigfile = sigfile;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    public void setSkipAttachSignedArtifact( boolean skipAttachSignedArtifact )
    {
        this.skipAttachSignedArtifact = skipAttachSignedArtifact;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public void setVerify( boolean verify )
    {
        this.verify = verify;
    }
}
