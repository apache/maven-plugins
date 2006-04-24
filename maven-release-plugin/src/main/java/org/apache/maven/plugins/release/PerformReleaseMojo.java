package org.apache.maven.plugins.release;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.Maven;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Perform a release from SCM
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @aggregator
 * @goal perform
 */
public class PerformReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * Comma or space separated goals
     *
     * @parameter expression="${goals}"
     */
    private String goals = "deploy";

    /**
     * Comma or space separated arguments such as
     *
     * @parameter expression="${arguments}"
     */
    private String arguments;

    /**
     * @parameter expression="${project.build.directory}/checkout"
     * @required
     */
    protected String workingDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${settings.interactiveMode}"
     * @required
     * @readonly
     */
    private boolean interactive;

    /**
     * @parameter expression="${releasePom}"
     */
    private String releasePom;

    private ReleaseProgressTracker releaseProgress;

    private final static String MAVEN_TEST_SKIP = "maven.test.skip";

    public void execute()
        throws MojoExecutionException
    {
        checkout();

        runGoals();

        cleanup();
    }

    private void checkout()
        throws MojoExecutionException
    {
        getLog().info( "Checking out the project to perform the release ..." );

        try
        {
            ScmHelper scm = getScm( workingDirectory );

            scm.checkout();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkout process.", e );
        }
    }

    private void runGoals()
        throws MojoExecutionException
    {
        // TODO: we need to get a reference to the maven component and use that so this
        // will work purely in an embedded mode. Not sure how to pass the release setting to the plugin in that
        // instance though - still via -D, or is there a better way?

        Commandline cl = new Commandline();

        try
        {
            addSystemEnvironment( cl );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Can't add system environment variables to mvn command line.", e );
        }

        cl.addEnvironment( "MAVEN_TERMINATE_CMD", "on" );

        cl.setExecutable( "mvn" );

        cl.setWorkingDirectory( workingDirectory );

        cl.createArgument().setLine( "-DperformRelease=true" );

        if ( this.goals != null )
        {
            // accept both space and comma, so the old way still work
            String [] tokens = StringUtils.split( this.goals, ", " );

            for ( int i = 0; i < tokens.length; ++i )
            {
                cl.createArgument().setValue( tokens[i] );
            }
        }

        if ( this.arguments != null )
        {
            String [] tokens = StringUtils.split( this.arguments, ", " );

            for ( int i = 0; i < tokens.length; ++i )
            {
                cl.createArgument().setValue( tokens[i] );
            }
        }

        cl.createArgument().setLine( "--no-plugin-updates" );

        if ( !interactive )
        {
            cl.createArgument().setLine( "--batch-mode" );
        }

        if ( StringUtils.isNotEmpty( System.getProperty( MAVEN_TEST_SKIP ) ) )
        {
            cl.createArgument().setLine( "-D" + MAVEN_TEST_SKIP + "=" + System.getProperty( MAVEN_TEST_SKIP ) );
        }

        if ( StringUtils.isEmpty( releasePom ) )
        {
            File pomFile = project.getFile();

            releasePom = pomFile.getName();
        }

        if ( releasePom.equals( Maven.RELEASE_POMv4 ) && interactive )
        {
            StringBuffer warning = new StringBuffer();
            warning.append( "\n*******************************************************************************\n" );
            warning.append( "\nYou have chosen to use the fully resolved release-POM to deploy this project." );
            warning.append( "\n" );
            warning.append( "\nNOTE: Deploying artifacts using the fully resolved release-POM " );
            warning.append( "\nwill result in loss of any version ranges specified for your" );
            warning.append( "\nproject's dependencies." );
            warning.append( "\n" );
            warning.append( "\nAre you sure you want to do this?" );
            warning.append( "\n" );
            warning.append( "\n*******************************************************************************\n" );

            getLog().warn( warning );

            getLog().info( "Enter the POM filename to use for deployment: [" + releasePom + "] " );

            try
            {
                String input = getInputHandler().readLine();

                if ( !StringUtils.isEmpty( input ) )
                {
                    releasePom = input;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "An error has occurred while reading the pom file location.", e );
            }
        }

        getLog().info( "Releasing project based on POM: " + releasePom + " in working directory: " + workingDirectory );

        cl.createArgument().setLine( "-f " + releasePom );

        List profiles = project.getActiveProfiles();

        if ( profiles != null && !profiles.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer();

            buffer.append( "-P " );

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                buffer.append( profile.getId() ).append( "," );
            }

            buffer.setLength( buffer.length() - 1 );

            cl.createArgument().setLine( buffer.toString() );
        }

        StreamConsumer consumer = new DefaultConsumer();

        try
        {
            this.getLog().info( cl.toString() );

            int result = CommandLineUtils.executeCommandLine( cl, consumer, consumer );

            if ( result != 0 )
            {
                throw new MojoExecutionException( "Result of mvn execution is: \'" + result + "\'. Release failed." );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Can't run goal " + goals, e );
        }
    }

    private void cleanup()
    {
        removeReleaseProperties();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------


    protected ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException
    {
        if ( releaseProgress == null )
        {
            try
            {
                releaseProgress = ReleaseProgressTracker.load( basedir );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to load release information from file: " +
                    ReleaseProgressTracker.getReleaseProgressFilename(), e );
            }
        }

        return releaseProgress;
    }

    /**
     * Add system environment variables
     * Moved to plexus-utils 1.0.5
     */
    private void addSystemEnvironment( Commandline cl )
        throws Exception
    {
        Properties envVars = getSystemEnvVars();

        for ( Iterator i = envVars.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            cl.addEnvironment( key, envVars.getProperty( key ) );
        }
    }

    private Properties getSystemEnvVars()
        throws Exception
    {
        Process p = null;

        Properties envVars = new Properties();

        Runtime r = Runtime.getRuntime();

        String os = System.getProperty( "os.name" ).toLowerCase();

        //If this is windows set the shell to command.com or cmd.exe with correct arguments.
        if ( os.indexOf( "windows" ) != -1 )
        {
            if ( os.indexOf( "95" ) != -1 || os.indexOf( "98" ) != -1 || os.indexOf( "Me" ) != -1 )
            {
                p = r.exec( "command.com /c set" );
            }
            else
            {
                p = r.exec( "cmd.exe /c set" );
            }
        }
        else
        {
            p = r.exec( "env" );
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );

        String line;

        while ( ( line = br.readLine() ) != null )
        {
            int idx = line.indexOf( '=' );

            String key = line.substring( 0, idx );

            String value = line.substring( idx + 1 );

            envVars.setProperty( key, value );
            // System.out.println( key + " = " + value );
        }

        return envVars;
    }
}
