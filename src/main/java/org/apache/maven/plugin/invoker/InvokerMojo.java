package org.apache.maven.plugin.invoker;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import bsh.EvalError;
import bsh.Interpreter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, and
 * outputting the results to the screen.
 * 
 * @goal run
 * 
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 */
public class InvokerMojo
    extends AbstractMojo
{
    /**
     * Flag used to determine whether the build logs should be output to the normal mojo log.
     * 
     * @parameter expression="${invoker.streamLogs}" default-value="false"
     */
    private boolean streamLogs;
    
    /**
     * The local repository for caching artifacts.
     * 
     * @parameter expression="${invoker.localRepositoryPath}"
     */
    private String localRepositoryPath;

    /**
     * Directory to search for integration tests.
     * 
     * @parameter expression="${invoker.projectsDirectory}" default-value="${basedir}/src/projects/"
     */
    private File projectsDirectory;
    
    /**
     * A single POM to build, skipping any scanning parameters and behavior.
     * 
     * @parameter expression="${invoker.pom}"
     */
    private File pom;

    /**
     * Includes for searching the integration test directory. This parameter is meant to be set from the POM.
     * 
     * @parameter
     */
    private List pomIncludes = Collections.singletonList( "*/pom.xml" );

    /**
     * Excludes for searching the integration test directory. This parameter is meant to be set from the POM.
     * 
     * @parameter
     */
    private List pomExcludes = Collections.EMPTY_LIST;

    /**
     * The comma-separated list of goals to execute on each project. Default is 'package'.
     * 
     * @parameter
     */
    private List goals = Collections.singletonList( "package" );

    /**
     * The name of the project-specific file that contains the enumeration of goals to execute for that test.
     * 
     * @parameter expression="${invoker.goalsFile}" default-value="goals.txt"
     */
    private String goalsFile;

    /**
     * @component
     */
    private Invoker invoker;

    /**
     * relative path of a pre-build hook beanshell script to run prior to executing the build.
     * 
     * @parameter expression="${invoker.preBuildHookScript}" default-value="prebuild.bsh"
     */
    private String preBuildHookScript;

    /**
     * relative path of a cleanup/verification beanshell script to run after executing the build.
     * 
     * @parameter expression="${invoker.postBuildHookScript}" default-value="postbuild.bsh"
     */
    private String postBuildHookScript;

    /**
     * Location of a properties file that defines CLI properties for the test.
     * 
     * @parameter expression="${invoker.testPropertiesFile}" default-value="test.properties"
     */
    private String testPropertiesFile;

    /**
     * Whether to show errors in the build output.
     * 
     * @parameter expression="${invoker.showErrors}" default-value="false"
     */
    private boolean showErrors;

    /**
     * Whether to show debug statements in the build output.
     * 
     * @parameter expression="${invoker.debug}" default-value="false"
     */
    private boolean debug;

    /**
     * Suppress logging to the build.log file.
     * 
     * @parameter expression="${invoker.noLog}" default-value="false"
     */
    private boolean noLog;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String[] includedPoms;
        try
        {
            includedPoms = getPoms();
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Error retrieving POM list from includes, excludes, "
                            + "and IT directory. Reason: " + e.getMessage(), e );
        }

        if ( includedPoms == null || includedPoms.length < 1 )
        {
            getLog().info( "No test-projects were selected for execution." );
            return;
        }

        final List failures = new ArrayList();

        for ( int i = 0; i < includedPoms.length; i++ )
        {
            final String pom = includedPoms[i];

            runBuild( pom, failures );
        }

        final StringBuffer summary = new StringBuffer();
        summary.append( "\n\n" );
        summary.append( "---------------------------------------\n" );
        summary.append( "Execution Summary:\n" );
        summary.append( "Builds Passing: " ).append( includedPoms.length - failures.size() ).append( "\n" );
        summary.append( "Builds Failing: " ).append( failures.size() ).append( "\n" );
        summary.append( "---------------------------------------\n" );

        if ( !failures.isEmpty() )
        {
            summary.append( "\nThe following builds failed:\n" );

            for ( final Iterator it = failures.iterator(); it.hasNext(); )
            {
                final String pom = ( String ) it.next();
                summary.append( "\n*  " ).append( pom );
            }

            summary.append( "\n" );
        }

        getLog().info( summary.toString() );

        if ( !failures.isEmpty() )
        {
            throw new MojoFailureException( this, "One or more builds failed.", failures.size() + " builds failed." );
        }
    }

    private void runBuild( final String pom, final List failures )
        throws MojoExecutionException
    {
        File pomFile = new File( pom );
        
        if ( !pomFile.isAbsolute() )
        {
            pomFile = new File( projectsDirectory, pom );
        }
        
        final File basedir = pomFile.getParentFile();

        getLog().info( "Building: " + pom );

        final File outputLog = new File( basedir, "build.log" );

        FileLogger logger = null;

        try
        {
            if ( !noLog )
            {
                outputLog.getParentFile().mkdirs();

                try
                {
                    if ( streamLogs )
                    {
                        logger = new FileLogger( outputLog, getLog() );
                    }
                    else
                    {
                        logger = new FileLogger( outputLog );
                    }
                    
                    getLog().debug( "build log initialized in: " + outputLog );
                }
                catch ( final IOException e )
                {
                    getLog().debug( "Error initializing build logfile in: " + outputLog, e );
                    getLog().info( "...FAILED[could not initialize logfile in: " + outputLog );

                    failures.add( pom );

                    return;
                }
            }

            if ( !prebuild( basedir, pom, failures, logger ) )
            {
                getLog().info( "...FAILED[pre-build script returned false]" );

                failures.add( pom );

                return;
            }

            final List invocationGoals = getGoals( basedir );

            final InvocationRequest request = new DefaultInvocationRequest();

            if ( invocationGoals.size() == 1 && "_default".equals( invocationGoals.get( 0 ) ) )
            {
                getLog().debug( "Executing default goal for project in: " + pom );
            }
            else
            {
                getLog().debug( "Executing goals: " + invocationGoals + " for project in: " + pom );

                request.setGoals( invocationGoals );
            }

            try
            {
                final Properties testProperties = loadTestProperties( basedir );

                request.setProperties( testProperties );
            }
            catch ( final IOException e )
            {
                getLog().debug( "Error reading test-properties file in: " + testPropertiesFile, e );
                getLog().info( "...FAILED[error reading test properties in: " + testPropertiesFile );

                failures.add( pom );

                return;
            }

            if ( localRepositoryPath != null )
            {
                File localRepoDir = new File( localRepositoryPath );

                if ( !localRepoDir.isAbsolute() )
                {
                    localRepoDir = new File( basedir, localRepositoryPath );
                }

                getLog().debug( "Using local repository: " + localRepoDir );

                request.setLocalRepositoryDirectory( localRepoDir );
            }

            request.setInteractive( false );

            request.setShowErrors( showErrors );

            request.setDebug( debug );
            
            request.setBaseDirectory( basedir );

            if ( !noLog )
            {
                request.setErrorHandler( logger );

                request.setOutputHandler( logger );
            }

            request.setPomFile( pomFile );

            InvocationResult result = null;

            try
            {
                result = invoker.execute( request );
            }
            catch ( final MavenInvocationException e )
            {
                getLog().debug( "Error invoking Maven: " + e.getMessage(), e );
                getLog().info( "...FAILED[error invoking Maven]" );

                failures.add( pom );
            }

            final CommandLineException executionException = result.getExecutionException();

            if ( executionException != null )
            {
                getLog().info( "...FAILED. See " + outputLog.getAbsolutePath() + " for details." );
                failures.add( pom );
            }
            else if ( result.getExitCode() != 0 )
            {
                getLog().info(
                                "...FAILED[code=" + result.getExitCode() + "]. See " + outputLog.getAbsolutePath()
                                                + " for details." );

                failures.add( pom );
            }
            else if ( !verify( basedir, pom, failures, logger ) )
            {
                getLog().info( "...FAILED[verify script returned false]." );

                failures.add( pom );
            }
            else
            {
                getLog().info( "...SUCCESS." );
            }
        }
        finally
        {
            if ( logger != null )
            {
                logger.close();
            }
        }
    }

    private Properties loadTestProperties( final File basedir )
        throws IOException
    {
        final File testProperties = new File( basedir, testPropertiesFile );

        final Properties testProps = new Properties();

        if ( testProperties.exists() )
        {
            FileInputStream fin = null;
            try
            {
                fin = new FileInputStream( testProperties );

                testProps.load( fin );
            }
            finally
            {
                IOUtil.close( fin );
            }
        }

        return testProps;
    }

    private boolean verify( final File basedir, final String pom, final List failures, final FileLogger logger )
    {
        boolean result = true;

        if ( postBuildHookScript != null )
        {
            try
            {
                result = runScript( "verification script", basedir, postBuildHookScript, logger );
            }
            catch ( final IOException e )
            {
                result = false;
                failures.add( pom );
            }
            catch ( final EvalError e )
            {
                result = false;
                failures.add( pom );
            }
        }

        return result;
    }

    private boolean runScript( final String scriptDescription, final File basedir, final String relativeScriptPath,
                               final FileLogger logger )
        throws IOException, EvalError
    {
        final File script = new File( basedir, relativeScriptPath );

        boolean scriptResult = false;

        if ( script.exists() )
        {
            final Interpreter engine = new Interpreter();

            PrintStream origOut = System.out;
            PrintStream origErr = System.err;
            
            FileReader reader = null;
            try
            {
                if ( !noLog )
                {
                    logger.consumeLine( "Running " + scriptDescription + " in: " + script );

                    System.setErr( logger.getPrintStream() );
                    System.setOut( logger.getPrintStream() );
                    
                    engine.setErr( logger.getPrintStream() );
                    engine.setOut( logger.getPrintStream() );
                }

                engine.set( "basedir", basedir );

                reader = new FileReader( script );

                final Object result = engine.eval( reader );

                scriptResult = Boolean.TRUE.equals( result ) || "true".equals( result );
            }
            finally
            {
                IOUtil.close( reader );
                System.setErr( origErr );
                System.setOut( origOut );
            }

            if ( !noLog )
            {
                logger.consumeLine( "Finished " + scriptDescription + " in: " + script );
            }
        }
        else
        {
            scriptResult = true;
        }

        return scriptResult;
    }

    private boolean prebuild( final File basedir, final String pom, final List failures, final FileLogger logger )
    {
        boolean result = true;

        if ( preBuildHookScript != null )
        {
            try
            {
                result = runScript( "pre-build script", basedir, preBuildHookScript, logger );
            }
            catch ( final IOException e )
            {
                result = false;
                failures.add( pom );
            }
            catch ( final EvalError e )
            {
                result = false;
                failures.add( pom );
            }
        }

        return result;
    }

    private List getGoals( final File basedir )
    {
        List invocationGoals = goals;

        if ( goalsFile != null )
        {
            final File projectGoalList = new File( basedir, goalsFile );

            if ( projectGoalList.exists() )
            {
                final List goals = readFromFile( projectGoalList );

                if ( goals != null && !goals.isEmpty() )
                {
                    getLog().debug( "Using goals specified in file: " + projectGoalList );
                    invocationGoals = goals;
                }
            }
        }

        return invocationGoals;
    }

    private String[] getPoms()
        throws IOException
    {
        if ( pom != null && pom.exists() )
        {
            return new String[]{ pom.getAbsolutePath() };
        }
        else
        {
            final FileSet fs = new FileSet();

            fs.setIncludes( pomIncludes );
            fs.setExcludes( pomExcludes );
            fs.setDirectory( projectsDirectory.getCanonicalPath() );
            fs.setFollowSymlinks( false );
            fs.setUseDefaultExcludes( false );

            final FileSetManager fsm = new FileSetManager( getLog() );

            return fsm.getIncludedFiles( fs );
        }
    }

    private List readFromFile( final File projectGoalList )
    {
        BufferedReader reader = null;

        List result = null;

        try
        {
            reader = new BufferedReader( new FileReader( projectGoalList ) );

            result = new ArrayList();

            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                result.addAll( collectListFromCSV( line ) );
            }
        }
        catch ( final IOException e )
        {
            getLog().warn(
                           "Failed to load goal list from file: " + projectGoalList
                                           + ". Using 'goal' parameter configured on this plugin instead." );
            getLog().debug( "Error reading goals file: " + projectGoalList, e );
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( final IOException e )
                {
                }
            }
        }

        return result;
    }

    private List collectListFromCSV( final String csv )
    {
        final List result = new ArrayList();

        if ( csv != null && csv.trim().length() > 0 )
        {
            final StringTokenizer st = new StringTokenizer( csv, "," );

            while ( st.hasMoreTokens() )
            {
                result.add( st.nextToken().trim() );
            }
        }

        return result;
    }

}
