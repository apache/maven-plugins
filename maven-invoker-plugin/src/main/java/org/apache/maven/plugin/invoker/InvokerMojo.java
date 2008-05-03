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
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
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
import org.codehaus.plexus.util.interpolation.Interpolator;
import org.codehaus.plexus.util.interpolation.MapBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, and
 * outputting the results to the command line.
 *
 * @goal run
 * @requiresDependencyResolution test
 * @since 1.0
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 */
public class InvokerMojo
    extends AbstractMojo
{
    /**
     * Maven artifact install component to copy artifacts to the local repository.
     * 
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * Used to create artifacts
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Used to create artifacts
     *
     * @component
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * Flag to determine if the project artifact(s) should be installed to the
     * local repository.
     * 
     * @parameter default-value="false"
     * @since 1.2
     */
    private boolean installProjectArtifacts;
    
    /**
     * Flag used to suppress certain invocations. This is useful in tailoring the
     * build using profiles.
     *
     * @parameter default-value="false"
     * @since 1.1
     */
    private boolean skipInvocation;

    /**
     * Flag used to suppress the summary output notifying of successes and failures. If set to <code>true</code>,
     * the only indication of the build's success or failure will be the effect it has on the main
     * build (if it fails, the main build should fail as well). If <code>streamLogs</code> is enabled, the sub-build
     * summary will also provide an indication.
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
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * The local repository for caching artifacts.
     *
     * @parameter expression="${invoker.localRepositoryPath}"
     */
    private File localRepositoryPath;

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
     * If this parameter is not set, the plugin will search for all <code>pom.xml</code> files one directory below
     * {@link #projectsDirectory} (<code>*&#47;pom.xml</code>).
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
     * @deprecated As of version 1.2 the properties file specified by the parameter invokerPropertiesFile should be used
     *             instead.
     */
    private String goalsFile;

    /**
     * @component
     */
    private Invoker invoker;

    /**
     * Relative path of a pre-build hook BeanShell script to run prior to executing the build.
     *
     * @parameter expression="${invoker.preBuildHookScript}" default-value="prebuild.bsh"
     */
    private String preBuildHookScript;

    /**
     * Relative path of a cleanup/verification BeanShell script to run after executing the build.
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
     * Suppress logging to the <code>build.log</code> file.
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
     * 
     * @parameter expression="${invoker.test}"
     * @since 1.1
     */
    private String invokerTest;

    /**
     * The name of the project-specific file that contains the enumeration of profiles to use for that test. <b>If the
     * file exists and empty no profiles will be used even if the profiles is set</b>
     * 
     * @parameter expression="${invoker.profilesFile}" default-value="profiles.txt"
     * @since 1.1
     * @deprecated As of version 1.2 the properties file specified by the parameter invokerPropertiesFile should be used
     *             instead.
     */
    private String profilesFile;

    /**
     * Path to an alternate <code>settings.xml</code> to use for Maven invocation with all ITs.
     * 
     * @parameter expression="${invoker.settingsFile}"
     * @since 1.2
     */
    private File settingsFile;

    /**
     * The <code>MAVEN_OPTS</code> environment variable to use when invoking Maven. This value can be overridden for
     * individual integration tests by using {@link #invokerPropertiesFile}.
     * 
     * @parameter expression="${invoker.mavenOpts}"
     * @since 1.2
     */
    private String mavenOpts;

    /**
     * The file encoding for the BeanShell scripts and the list files for goals and profiles.
     * 
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 1.2
     */
    private String encoding;
    
    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     * @since 1.2
     */
    private Settings settings;    

    /**
     * A flag whether the test class path of the project under test should be added to the class path of the BeanShell
     * scripts. If set to <code>false</code>, the scripts can only access classes from the <a
     * href="dependencies.html">runtime class path</a> of the Maven Invoker Plugin.
     * 
     * @parameter expression="${invoker.addTestClassPath}" default-value="false"
     * @since 1.2
     */
    private boolean addTestClassPath;

    /**
     * The name of an optional test-specific file that contains properties used to configure the invocation of an
     * integration test. This properties file may be used to specify settings for an individual test invocation. Any
     * property present in the file will override the corresponding setting from the plugin configuration. The values of
     * the properties are filtered and may use expressions like <code>${project.version}</code> to reference project
     * properties or values from the parameter {@link #interpolationsProperties}. The snippet below describes the
     * supported properties:
     * 
     * <pre>
     * # A comma or space separated list of goals/phases to execute, may
     * # specify an empty list to execute the default goal of the IT project
     * invoker.goals=clean package site
     * 
     * # A comma or space separated list of profiles to activate
     * invoker.profiles=its,jdk15
     * 
     * # The value for the environment variable MAVEN_OPTS
     * invoker.mavenOpts=-Dfile.encoding=UTF-16 -Xms32m -Xmx256m
     * 
     * # Possible values are &quot;fail-fast&quot; (default), &quot;fail-at-end&quot; and &quot;fail-never&quot;
     * invoker.failureBehavior=fail-never
     * 
     * # The expected result of the build, possible values are &quot;success&quot; (default) and &quot;failure&quot;
     * invoker.buildResult=failure
     * 
     * # A boolean value controlling the -N flag, defaults to &quot;false&quot;
     * invoker.nonRecursive=false
     * </pre>
     * 
     * @parameter expression="${invoker.invokerPropertiesFile}" default-value="invoker.properties"
     * @since 1.2
     */
    private String invokerPropertiesFile;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipInvocation )
        {
            getLog().info( "Skipping invocation per configuration. If this is incorrect, ensure the skipInvocation parameter is not set to true." );
            return;
        }

        if ( installProjectArtifacts )
        {
            installProjectArtifacts();
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

        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn(
                           "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
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

    /**
     * Creates a new reader for the specified file, using the plugin's {@link #encoding} parameter.
     * 
     * @param file The file to create a reader for, must not be <code>null</code>.
     * @return The reader for the file, never <code>null</code>.
     * @throws IOException If the specified file was not found or the configured encoding is not supported.
     */
    private Reader newReader( File file )
        throws IOException
    {
        if ( StringUtils.isNotEmpty( encoding ) )
        {
            return ReaderFactory.newReader( file, encoding );
        }
        else
        {
            return ReaderFactory.newPlatformReader( file );
        }
    }

    /**
     * Install the main project artifact and any attached artifacts to the local repository.
     * 
     * @throws MojoExecutionException
     */
    private void installProjectArtifacts()
        throws MojoExecutionException
    {
        ArtifactRepository integrationTestRepository = localRepository;
        
        try
        {
            if ( localRepositoryPath != null )
            {
                if ( ! localRepositoryPath.exists() )
                {
                    localRepositoryPath.mkdirs();
                }
                integrationTestRepository =
                    artifactRepositoryFactory.createArtifactRepository( "it-repo",
                                                                        localRepositoryPath.toURL().toString(),
                                                                        localRepository.getLayout(),
                                                                        localRepository.getSnapshots(),
                                                                        localRepository.getReleases() );
            }
                        
            // Install the pom
            Artifact pomArtifact = artifactFactory.createArtifact( project.getGroupId(), project.getArtifactId(), 
                                                          project.getVersion(), null, "pom" );
            installer.install( project.getFile(), pomArtifact, integrationTestRepository );
            
            // Install the main project artifact
            installer.install( project.getArtifact().getFile(), project.getArtifact(), integrationTestRepository );
            
            // Install any attached project artifacts
            List attachedArtifacts = project.getAttachedArtifacts();
            Iterator artifactIter = attachedArtifacts.iterator();
            while ( artifactIter.hasNext() )
            {
                Artifact theArtifact = (Artifact)artifactIter.next();
                installer.install( theArtifact.getFile(), theArtifact, integrationTestRepository );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "ArtifactInstallationException: " + e.getMessage(), e );
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
        File interpolatedPomFile = buildInterpolatedFile( pomFile, basedir, "interpolated-pom.xml" );
        FileLogger logger = null;
        try
        {
            getLog().info( "Building: " + pom );

            final File outputLog = new File( basedir, "build.log" );

            final Properties invokerProperties = getInvokerProperties( basedir );
            if ( getLog().isDebugEnabled() && !invokerProperties.isEmpty() )
            {
                getLog().debug( "Using invoker properties:" );
                for ( Iterator it = new TreeSet( invokerProperties.keySet() ).iterator(); it.hasNext(); )
                {
                    String key = (String) it.next();
                    String value = invokerProperties.getProperty( key );
                    getLog().debug( "  " + key + ": " + value );
                }
            }

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

            final InvocationRequest request = new DefaultInvocationRequest();

            final List invocationGoals = getGoals( basedir );

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
                File localRepoDir = localRepositoryPath;

                getLog().debug( "Using local repository: " + localRepoDir );

                if ( ! localRepositoryPath.exists() )
                {
                    localRepositoryPath.mkdirs();
                }
                
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

            request.setProfiles( getProfiles( basedir ) );

            if ( settingsFile != null )
            {
                buildInterpolatedFile( settingsFile, settingsFile.getParentFile(), settingsFile.getName()
                    + ".interpolated" );
                request.setUserSettingsFile( new File( settingsFile.getParentFile(), settingsFile.getName()
                    + ".interpolated" ) );
            }

            request.setMavenOpts( mavenOpts );

            configureInvocation( request, invokerProperties );

            try
            {
                getLog().debug( "Using MAVEN_OPTS: " + request.getMavenOpts() );
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
            final boolean nonZeroExit =
                "failure".equalsIgnoreCase( invokerProperties.getProperty( "invoker.buildResult" ) );

            if ( executionException != null )
            {
                if ( !suppressSummaries )
                {
                    getLog().info( "...FAILED. See " + outputLog.getAbsolutePath() + " for details." );
                }
                failures.add( pom );
            }
            else if ( ( result.getExitCode() != 0 ) != nonZeroExit )
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
        if ( testProperties == null )
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

            if ( addTestClassPath )
            {
                getLog().debug( "Adding test class path to BeanShell interpreter:" );
                try
                {
                    List testClassPath = project.getTestClasspathElements();
                    for ( Iterator it = testClassPath.iterator(); it.hasNext(); )
                    {
                        String path = (String) it.next();
                        getLog().debug( "  " + path );
                        engine.getClassManager().addClassPath( new File( path ).toURI().toURL() );
                    }
                }
                catch ( Exception e )
                {
                    getLog().error( "Failed to add test class path to BeanShell interpreter", e );
                }
            }

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

                reader = newReader( script );

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
            reader = new BufferedReader( new InterpolationFilterReader( newReader( projectGoalList ), composite ) );

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

    protected File buildInterpolatedFile( File originalFile, File targetDirectory, String targetFileName )
        throws MojoExecutionException
    {
        File interpolatedFile = new File( targetDirectory, targetFileName );
        if ( interpolatedFile.exists() )
        {
            interpolatedFile.delete();
        }
        interpolatedFile.deleteOnExit();
        if ( settings.getLocalRepository() != null )
        {
            if ( this.interpolationsProperties == null )
            {
                this.interpolationsProperties = new Properties();
            }
            this.interpolationsProperties.put( "localRepository", settings.getLocalRepository() );
        }
        Map composite = new CompositeMap( this.project, this.interpolationsProperties );

        try
        {
            boolean created = interpolatedFile.createNewFile();
            if ( !created )
            {
                throw new MojoExecutionException( "fail to create file " + interpolatedFile.getPath() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "fail to create file " + interpolatedFile.getPath() );
        }
        getLog().debug( "interpolate it pom to create interpolated in " + interpolatedFile.getPath() );

        BufferedReader reader = null;
        Writer writer = null;
        try
        {
            // interpolation with token @...@
            reader = new BufferedReader( new InterpolationFilterReader( ReaderFactory.newXmlReader( originalFile ), composite, "@",
                                                                        "@" ) );
            writer = WriterFactory.newXmlWriter( interpolatedFile );
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

        if ( interpolatedFile == null )
        {
            // null check : normally impossibe but :-)
            throw new MojoExecutionException( "pom file is null after interpolation" );
        }
        return interpolatedFile;
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
            reader = new BufferedReader( newReader( projectProfilesFile ) );
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

    /**
     * Gets the (interpolated) invoker properties for an integration test.
     * 
     * @param projectDirectory The base directory of the IT project, must not be <code>null</code>.
     * @return The invoker properties, may be empty but never <code>null</code>.
     * @throws MojoExecutionException If an error occurred.
     */
    private Properties getInvokerProperties( final File projectDirectory )
        throws MojoExecutionException
    {
        Properties props = new Properties();
        if ( invokerPropertiesFile != null )
        {
            File propertiesFile = new File( projectDirectory, invokerPropertiesFile );
            if ( propertiesFile.isFile() )
            {
                InputStream in = null;
                try
                {
                    in = new FileInputStream( propertiesFile );
                    props.load( in );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to read invoker properties: " + propertiesFile, e );
                }
                finally
                {
                    IOUtil.close( in );
                }
            }

            Map filter = new CompositeMap( project, this.interpolationsProperties );
            Interpolator interpolator = new RegexBasedInterpolator();
            interpolator.addValueSource( new MapBasedValueSource( filter ) );
            for ( Iterator it = props.keySet().iterator(); it.hasNext(); )
            {
                String key = (String) it.next();
                String value = props.getProperty( key );
                value = interpolator.interpolate( value, "" );
                props.setProperty( key, value );
            }
        }
        return props;
    }

    /**
     * Configures the specified invocation request from the given invoker properties. Settings not present in the
     * invoker properties will be left unchanged in the invocation request.
     * 
     * @param request The invocation request to configure, must not be <code>null</code>.
     * @param properties The invoker properties used to configure the invocation, must not be <code>null</code>.
     * @return The configured invocation request.
     */
    private InvocationRequest configureInvocation( InvocationRequest request, Properties properties )
    {
        String goals = properties.getProperty( "invoker.goals" );
        if ( goals != null )
        {
            request.setGoals( new ArrayList( Arrays.asList( goals.split( "[,\\s]+" ) ) ) );
        }

        String profiles = properties.getProperty( "invoker.profiles" );
        if ( profiles != null )
        {
            request.setProfiles( new ArrayList( Arrays.asList( profiles.split( "[,\\s]+" ) ) ) );
        }

        String opts = properties.getProperty( "invoker.mavenOpts" );
        if ( opts != null )
        {
            request.setMavenOpts( opts );
        }

        String failureBehavior = properties.getProperty( "invoker.failureBehavior" );
        if ( failureBehavior != null )
        {
            request.setFailureBehavior( failureBehavior );
        }

        String nonRecursive = properties.getProperty( "invoker.nonRecursive" );
        if ( nonRecursive != null )
        {
            request.setRecursive( !Boolean.valueOf( nonRecursive ).booleanValue() );
        }

        return request;
    }

}
