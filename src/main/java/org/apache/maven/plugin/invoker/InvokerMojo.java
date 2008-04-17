package org.apache.maven.plugin.invoker;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.cli.CommandLineException;

import bsh.EvalError;
import bsh.Interpreter;

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
     * Flag used to suppress certain invocations. This is useful in tailoring the
     * build using profiles.
     *
     * @parameter default-value="false"
     * @since 1.1
     */
    private boolean skipInvocation;

    /**
     * Flag used to suppress the summary output notifying of successes and failures. If set to true,
     * the only indication of the build's success or failure will be the effect it has on the main
     * build (if it fails, the main build should fail as well). If streamLogs is enabled, the sub-build
     * summary will also provide an indication. By default, this parameter is set to false.
     *
     * @parameter default-value="false"
     */
    private boolean suppressSummaries;

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
     * Directory to which projects should be cloned prior to execution.
     *
     * @parameter
     * @since 1.1
     */
    private File cloneProjectsTo;

    /**
     * Some files are normally excluded when copying from the projectsDirectory
     * to the "cloneProjectsTo" directory (.svn, CVS, *~, etc).  Setting this parameter to true
     * will cause all files to be copied to the cloneProjectsTo directory.
     *
     * @parameter default-value="false"
     * @since 1.2
     */
    private boolean cloneAllFiles;

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
     * Common set of test properties to pass in on each IT's command line, via -D parameters.
     *
     * @parameter
     * @deprecated Use properties parameter instead.
     */
    private Properties testProperties;

    /**
     * Common set of properties to pass in on each project's command line, via -D parameters.
     *
     * @parameter
     * @since 1.1
     */
    private Map properties;

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

    /**
     * List of profileId's to explicitly trigger in the build.
     * 
     * @parameter
     * @since 1.1
     */
    private List profiles;

    /**
     * List properties which will be used to interpolate goal files.
     *
     * @parameter
     * @since 1.1
     */
    private Properties interpolationsProperties;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 1.1
     */
    private MavenProject project;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>pomIncludes</code>
     * and <code>pomExcludes</code> parameters.  Each pattern you specify here will be used to create an 
     * include pattern formatted like <code>${projectsDirectory}/${invoker.test}</code>, 
     * so you can just type "-Dinvoker.test=MyTest" to run a single it in ${projectsDirectory}/${invoker.test}".  
     * @parameter expression="${invoker.test}"
     * @since 1.1
     */
    private String invokerTest;

    /**
     * The name of the project-specific file that contains the enumeration of profiles to use for that test.
     * <b>If the file exists and empty no profiles will be used even if the profiles is set</b>
     * @parameter expression="${invoker.profilesFile}" default-value="profiles.txt"
     * @since 1.1
     */
    private String profilesFile;

    /**
     * Path to an alternate settings.xml to use for maven invocation with all ITs
     * 
     * @parameter expression="${invoker.settingsFile}"
     * @since 1.2
     */
    private File settingsFile;
    

    /**
     * The MAVEN_OPTS env var to use when invoking maven
     * 
     * @parameter expression="${invoker.mavenOpts}"
     * @since 1.2
     */    
    private String mavenOpts;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipInvocation )
        {
            getLog().info( "Skipping invocation per configuration. If this is incorrect, ensure the skipInvocation parameter is not set to true." );
            return;
        }

        String[] includedPoms;
        if ( pom != null )
        {
            try
            {
                projectsDirectory = pom.getCanonicalFile().getParentFile();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to discover projectsDirectory from pom File parameter. Reason: "
                    + e.getMessage(), e );
            }

            includedPoms = new String[]{ pom.getName() };
        }
        else
        {
            try
            {
                includedPoms = getPoms();
            }
            catch ( final IOException e )
            {
                throw new MojoExecutionException( "Error retrieving POM list from includes, excludes, "
                                + "and projects directory. Reason: " + e.getMessage(), e );
            }
        }


        if ( ( includedPoms == null ) || ( includedPoms.length < 1 ) )
        {
            getLog().info( "No test-projects were selected for execution." );
            return;
        }

        File projectsDir = projectsDirectory;

        if ( cloneProjectsTo != null )
        {
            cloneProjectsTo.mkdirs();

            try
            {
                cloneProjects( includedPoms );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to clone projects from: " + projectsDirectory + " to: "
                    + cloneProjectsTo + ". Reason: " + e.getMessage(), e );
            }

            projectsDir = cloneProjectsTo;
        }

        final List failures = new ArrayList();

        for ( int i = 0; i < includedPoms.length; i++ )
        {
            final String pom = includedPoms[i];

            runBuild( projectsDir, pom, failures );
        }


        if ( !suppressSummaries )
        {
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
        }

        if ( !failures.isEmpty() )
        {
            String message = failures.size() + " builds failed.";

            throw new MojoFailureException( this, message, message );
        }
    }

    private void cloneProjects( String[] includedPoms )
        throws IOException
    {
        List clonedSubpaths = new ArrayList();

        for ( int i = 0; i < includedPoms.length; i++ )
        {
            String subpath = includedPoms[i];
            int lastSep = subpath.lastIndexOf( File.separator );

            if ( lastSep > -1 )
            {
                subpath = subpath.substring( 0, lastSep );
            }
            else
            {
                subpath = ".";
            }

            // avoid copying subdirs that are already cloned.
            if ( !alreadyCloned( subpath, clonedSubpaths ) )
            {
                // avoid creating new files that point to dir/.
                if ( ".".equals( subpath ) )
                {
                    String cloneSubdir = normalizePath( cloneProjectsTo, projectsDirectory.getCanonicalPath() );

                    // avoid infinite recursion if the cloneTo path is a subdirectory.
                    if ( cloneSubdir != null )
                    {
                        File temp = File.createTempFile( "pre-invocation-clone.", "" );
                        temp.delete();
                        temp.mkdirs();

                        copyDirectoryStructure( projectsDirectory, temp );

                        FileUtils.deleteDirectory( new File( temp, cloneSubdir ) );

                        copyDirectoryStructure( temp, cloneProjectsTo );
                    }
                    else
                    {
                        copyDirectoryStructure( projectsDirectory, cloneProjectsTo );
                    }
                }
                else
                {
                    copyDirectoryStructure( new File( projectsDirectory, subpath ), new File( cloneProjectsTo, subpath ) );
                }

                clonedSubpaths.add( subpath );
            }
        }
    }
    
    /**
     * Copied a directory structure with deafault exclusions (.svn, CVS, etc)
     * 
     * @param sourceDir
     * @param destDir
     * @throws IOException
     */
    private void copyDirectoryStructure( File sourceDir, File destDir ) throws IOException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        if ( ! cloneAllFiles )
        {
            scanner.addDefaultExcludes();
        }
        scanner.scan();
        
        String [] includedFiles = scanner.getIncludedFiles();
        for ( int i = 0; i < includedFiles.length; ++i )
        {
            File sourceFile = new File( sourceDir, includedFiles[ i ] );
            File destFile = new File( destDir, includedFiles[ i ] );
            FileUtils.copyFile( sourceFile, destFile );
        }
    }

    private boolean alreadyCloned( String subpath, List clonedSubpaths )
    {
        for ( Iterator iter = clonedSubpaths.iterator(); iter.hasNext(); )
        {
            String path = (String) iter.next();

            if ( ".".equals( path ) || subpath.startsWith( path ) )
            {
                return true;
            }
        }

        return false;
    }

    private void runBuild( final File projectsDir, final String pom, final List failures )
        throws MojoExecutionException
    {

        File pomFile = new File( projectsDir, pom );
        final File basedir = pomFile.getParentFile();
        File interpolatedPomFile = buildInterpolatedPomFile( pomFile, basedir );
        FileLogger logger = null;
        try
        {
            getLog().info( "Building: " + pom );

            final File outputLog = new File( basedir, "build.log" );

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
                    getLog().info( "...FAILED[could not initialize logfile in: " + outputLog + "]" );

                    failures.add( pom );

                    return;
                }
            }

            if ( !prebuild( basedir, interpolatedPomFile, failures, logger ) )
            {
                getLog().info( "...FAILED[pre-build script returned false]" );

                failures.add( pom );

                return;
            }

            final List invocationGoals = getGoals( basedir );

            final InvocationRequest request = new DefaultInvocationRequest();

            if ( ( invocationGoals.size() == 1 ) && "_default".equals( invocationGoals.get( 0 ) ) )
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
                Properties collectedTestProperties = new Properties();

                if ( testProperties != null )
                {
                    collectedTestProperties.putAll( testProperties );
                }

                if ( properties != null )
                {
                    collectedTestProperties.putAll( properties );
                }

                final Properties loadedProperties = loadTestProperties( basedir );

                if ( loadedProperties != null )
                {
                    collectedTestProperties.putAll( loadedProperties );
                }

                request.setProperties( collectedTestProperties );
            }
            catch ( final IOException e )
            {
                getLog().debug( "Error reading test-properties file in: " + testPropertiesFile, e );
                getLog().info( "...FAILED[error reading test properties in: " + testPropertiesFile + "]" );

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

            request.setPomFile( interpolatedPomFile );

            request.setProfiles( getProfiles(basedir) );

            if ( settingsFile != null )
            {
                request.setUserSettingsFile( settingsFile );
            }
            
            if ( mavenOpts != null )
            {
                request.setMavenOpts( mavenOpts );
            }

            try
            {
                getLog().debug( "Executing: " + new MavenCommandLineBuilder().build( request ) );
            }
            catch ( CommandLineConfigurationException e )
            {
                getLog().debug( "Failed to display command line: " + e.getMessage() );
            }

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
                if ( !suppressSummaries )
                {
                    getLog().info( "...FAILED. See " + outputLog.getAbsolutePath() + " for details." );
                }
                failures.add( pom );
            }
            else if ( result.getExitCode() != 0 )
            {
                if ( !suppressSummaries )
                {
                    getLog().info(
                                   "...FAILED[code=" + result.getExitCode() + "]. See " + outputLog.getAbsolutePath()
                                       + " for details." );
                }

                failures.add( pom );
            }
            else if ( !verify( basedir, interpolatedPomFile, failures, logger ) )
            {
                if ( !suppressSummaries )
                {
                    getLog().info( "...FAILED[verify script returned false]." );
                }

                failures.add( pom );
            }
            else if ( !suppressSummaries )
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
        if (testProperties == null)
        {
            return new Properties();
        }
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

    private boolean verify( final File basedir, final File pom, final List failures, final FileLogger logger )
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
            }
            catch ( final EvalError e )
            {
                String errorMessage = "error evaluating script " + basedir.getPath() + File.separatorChar
                    + postBuildHookScript + ", " + e.getMessage();
                getLog().error( errorMessage, e );
                result = false;
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

            Reader reader = null;
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

                reader = ReaderFactory.newPlatformReader( script );

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

    private boolean prebuild( final File basedir, final File pom, final List failures, final FileLogger logger )
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
            }
            catch ( final EvalError e )
            {
                String errorMessage = "error evaluating script " + basedir.getPath() + File.separatorChar
                    + postBuildHookScript + ", " + e.getMessage();
                getLog().error( errorMessage, e );
                result = false;
            }
        }

        return result;
    }

    protected List getGoals( final File basedir )
    {
        List invocationGoals = goals;

        if ( goalsFile != null )
        {
            final File projectGoalList = new File( basedir, goalsFile );

            if ( projectGoalList.exists() )
            {
                final List goals = readFromFile( projectGoalList );

                if ( ( goals != null ) && !goals.isEmpty() )
                {
                    getLog().debug( "Using goals specified in file: " + projectGoalList );
                    invocationGoals = goals;
                }
            }
        }

        return invocationGoals;
    }

    protected String[] getPoms()
        throws IOException
    {
        String[] poms;

        if ( ( pom != null ) && pom.exists() )
        {
            poms = new String[]{ pom.getAbsolutePath() };
        }
        else if ( invokerTest != null )
        {
            String[] testRegexes = StringUtils.split( invokerTest, "," );
            List /* String */includes = new ArrayList( testRegexes.length );

            for ( int i = 0, size = testRegexes.length; i < size; i++ )
            {
                // user just use -Dinvoker.test=MWAR191,MNG111 to use a directory thats the end is not pom.xml
                includes.add( testRegexes[i].endsWith( "pom.xml" ) ? testRegexes[i] : testRegexes[i]
                    + File.separatorChar + "pom.xml" );
            }

            final FileSet fs = new FileSet();

            fs.setIncludes( includes );
            //fs.setExcludes( pomExcludes );
            fs.setDirectory( projectsDirectory.getCanonicalPath() );
            fs.setFollowSymlinks( false );
            fs.setUseDefaultExcludes( false );

            final FileSetManager fsm = new FileSetManager( getLog() );

            poms = fsm.getIncludedFiles( fs );
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

            poms = fsm.getIncludedFiles( fs );
        }

        poms = normalizePomPaths( poms );

        return poms;
    }

    private String[] normalizePomPaths( String[] poms )
        throws IOException
    {
        String projectsDirPath = projectsDirectory.getCanonicalPath();

        String[] results = new String[poms.length];
        for ( int i = 0; i < poms.length; i++ )
        {
            String pomPath = poms[i];

            File pom = new File( pomPath );

            if ( !pom.isAbsolute() )
            {
                pom = new File( projectsDirectory, pomPath );
            }

            String normalizedPath = normalizePath( pom, projectsDirPath );

            if ( normalizedPath == null )
            {
                normalizedPath = pomPath;
            }

            results[i] = normalizedPath;
        }

        return results;
    }

    private String normalizePath( File path, String withinDirPath )
        throws IOException
    {
        String normalizedPath = path.getCanonicalPath();

        if ( normalizedPath.startsWith( withinDirPath ) )
        {
            normalizedPath = normalizedPath.substring( withinDirPath.length() );
            if ( normalizedPath.startsWith( File.separator ) )
            {
                normalizedPath = normalizedPath.substring( File.separator.length() );
            }

            return normalizedPath;
        }
        else
        {
            return null;
        }
    }

    private List readFromFile( final File projectGoalList )
    {
        BufferedReader reader = null;

        List result = null;

        try
        {
            Map composite = new CompositeMap( this.project, this.interpolationsProperties );
            reader = new BufferedReader( new InterpolationFilterReader( ReaderFactory.newPlatformReader(  projectGoalList ), composite ) );

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
            IOUtil.close( reader );
        }

        return result;
    }

    private List collectListFromCSV( final String csv )
    {
        final List result = new ArrayList();

        if ( ( csv != null ) && ( csv.trim().length() > 0 ) )
        {
            final StringTokenizer st = new StringTokenizer( csv, "," );

            while ( st.hasMoreTokens() )
            {
                result.add( st.nextToken().trim() );
            }
        }

        return result;
    }

    protected File buildInterpolatedPomFile( File pomFile, File targetDirectory )
        throws MojoExecutionException
    {
        File interpolatedPomFile = new File( targetDirectory, "interpolated-pom.xml" );
        if (interpolatedPomFile.exists())
        {
            interpolatedPomFile.delete();
        }
        interpolatedPomFile.deleteOnExit();
        Map composite = new CompositeMap( this.project, this.interpolationsProperties );

        try
        {
            boolean created = interpolatedPomFile.createNewFile();
            if ( !created )
            {
                throw new MojoExecutionException( "fail to create file " + interpolatedPomFile.getPath() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "fail to create file " + interpolatedPomFile.getPath() );
        }
        getLog().debug( "interpolate it pom to create interpolated in " + interpolatedPomFile.getPath() );

        BufferedReader reader = null;
        Writer writer = null;
        try
        {
            // pom interpolation with token @...@
            reader = new BufferedReader( new InterpolationFilterReader( ReaderFactory.newXmlReader( pomFile ), composite, "@",
                                                                        "@" ) );
            writer = WriterFactory.newXmlWriter( interpolatedPomFile );
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                writer.write( line );
            }
            writer.flush();
        }
        catch ( IOException e )
        {
            String message = "error when interpolating it pom";
            throw new MojoExecutionException( message, e );
        }
        finally
        {
            // IOUtil in p-u is null check and silently NPE
            IOUtil.close( reader );
            IOUtil.close( writer );
        }

        if ( interpolatedPomFile == null )
        {
            // null check : normally impossibe but :-)
            throw new MojoExecutionException( "pom file is null after interpolation" );
        }
        return interpolatedPomFile;
    }

    protected List getProfiles( File projectDirectory )
        throws MojoExecutionException
    {
        if ( profilesFile == null )
        {
            return profiles == null ? Collections.EMPTY_LIST : profiles;
        }
        File projectProfilesFile = new File( projectDirectory, profilesFile );
        if ( !projectProfilesFile.exists() )
        {
            return profiles == null ? Collections.EMPTY_LIST : profiles;
        }
        BufferedReader reader = null;
        try
        {
            List profilesInFiles = new ArrayList();
            reader = new BufferedReader( ReaderFactory.newPlatformReader( projectProfilesFile ) );
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                profilesInFiles.addAll( collectListFromCSV( line ) );
            }
            return profilesInFiles;
        }
        catch ( FileNotFoundException e )
        {
            // as we check first if the file it should not happened
            throw new MojoExecutionException( projectProfilesFile + " not found ", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "error reading profile in file " + projectProfilesFile + " not found ", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }
}
