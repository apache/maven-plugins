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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

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
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, and
 * outputting the results to the command line.
 *
 * @goal run
 * @phase integration-test
 * @requiresDependencyResolution test
 * @since 1.0
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @version $Id$
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
     * The local repository for caching artifacts. It is strongly recommended to specify a path to an isolated
     * repository like <code>${project.build.directory}/it-repo</code>. Otherwise, your ordinary local repository will
     * be used, potentially soiling it with broken artifacts.
     * 
     * @parameter expression="${invoker.localRepositoryPath}" default-value="${settings.localRepository}"
     */
    private File localRepositoryPath;

    /**
     * Directory to search for integration tests.
     *
     * @parameter expression="${invoker.projectsDirectory}" default-value="${basedir}/src/projects/"
     */
    private File projectsDirectory;

    /**
     * Directory to which projects should be cloned prior to execution. If not specified, each integration test will be
     * run in the directory in which the corresponding IT POM was found. In this case, you most likely want to configure
     * your SCM to ignore <code>target</code> and <code>build.log</code> in the test's base directory.
     * 
     * @parameter
     * @since 1.1
     */
    private File cloneProjectsTo;

    /**
     * Some files are normally excluded when copying the IT projects from the directory specified by the parameter
     * projectsDirectory to the directory given by cloneProjectsTo (e.g. <code>.svn</code>, <code>CVS</code>,
     * <code>*~</code>, etc). Setting this parameter to <code>true</code> will cause all files to be copied to the
     * cloneProjectsTo directory.
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
     * Include patterns for searching the integration test directory for projects. This parameter is meant to be set
     * from the POM. If this parameter is not set, the plugin will search for all <code>pom.xml</code> files one
     * directory below {@link #projectsDirectory} (i.e. <code>*&#47;pom.xml</code>).<br>
     * <br>
     * Starting with version 1.3, mere directories can also be matched by these patterns. For example, the include
     * pattern <code>*</code> will run Maven builds on all immediate sub directories of {@link #projectsDirectory},
     * regardless if they contain a <code>pom.xml</code>. This allows to perform builds that need/should not depend on
     * the existence of a POM.
     * 
     * @parameter
     */
    private List pomIncludes = Collections.singletonList( "*/pom.xml" );

    /**
     * Exclude patterns for searching the integration test directory. This parameter is meant to be set from the POM.
     * By default, no POM files are excluded.
     * 
     * @parameter
     */
    private List pomExcludes = Collections.EMPTY_LIST;

    /**
     * The list of goals to execute on each project. Default value is: <code>package</code>.
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
     * Relative path of a pre-build hook BeanShell or Groovy script to run prior to executing the build. If the file
     * extension is omitted (e.g. "prebuild"), the plugin searches for the file by trying out the known extensions
     * ".bsh" and ".groovy".<br>
     * <br>
     * <em>Note:</em> Support for Groovy was added in version 1.3 of the plugin.
     * 
     * @parameter expression="${invoker.preBuildHookScript}" default-value="prebuild.bsh"
     */
    private String preBuildHookScript;

    /**
     * Relative path of a cleanup/verification BeanShell or Groovy script to run after executing the build. If the file
     * extension is omitted (e.g. "verify"), the plugin searches for the file by trying out the known extensions
     * ".bsh" and ".groovy".<br>
     * <br>
     * <em>Note:</em> Support for Groovy was added in version 1.3 of the plugin.
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
     * List of profile identifiers to explicitly trigger in the build.
     * 
     * @parameter
     * @since 1.1
     */
    private List profiles;

    /**
     * List of properties which will be used to interpolate goal files.
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
     * A comma separated list of project names to run. Specify this parameter to run individual tests by file name,
     * overriding the <code>pomIncludes</code> and <code>pomExcludes</code> parameters. Each pattern you specify here
     * will be used to create an include pattern formatted like <code>${projectsDirectory}/<i>pattern</i></code>, so
     * you can just type "-Dinvoker.test=FirstTest,SecondTest" to run builds in "${projectsDirectory}/FirstTest" and
     * "${projectsDirectory}/SecondTest".
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
     * Path to an alternate <code>settings.xml</code> to use for Maven invocation with all ITs. Note that the
     * <code>&lt;localRepository&gt;</code> element of this settings file is always ignored, i.e. the path given by the
     * parameter <code>localRepositoryPath</code> is dominant.
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
     * The file encoding for the pre-/post-build scripts and the list files for goals and profiles.
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
     * A flag whether the test class path of the project under test should be included in the class path of the
     * pre-/post-build scripts. If set to <code>false</code>, the class path of script interpreter consists only of
     * the <a href="dependencies.html">runtime dependencies</a> of the Maven Invoker Plugin. If set the
     * <code>true</code>, the project's test class path will be prepended to the interpreter class path. Among
     * others, this feature allows the scripts to access utility classes from the test sources of your project.
     * 
     * @parameter expression="${invoker.addTestClassPath}" default-value="false"
     * @since 1.2
     */
    private boolean addTestClassPath;

    /**
     * The test class path of the project under test.
     * 
     * @parameter default-value="${project.testClasspathElements}"
     * @readonly
     */
    private List testClassPath;

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
     * invoker.goals=clean install
     * 
     * # Optionally, a list of goals to run during further invocations of Maven
     * invoker.goals.2=${project.groupId}:${project.artifactId}:${project.version}:run
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

    /**
     * The supported script interpreters, indexed by the file extension of their associated script files.
     */
    private Map scriptInterpreters;


    /**
     * Invokes Maven on the configured test projects.
     * 
     * @throws MojoExecutionException If the goal encountered severe errors.
     * @throws MojoFailureException If any of the Maven builds failed.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipInvocation )
        {
            getLog().info( "Skipping invocation per configuration."
                + " If this is incorrect, ensure the skipInvocation parameter is not set to true." );
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
                throw new MojoExecutionException( "Failed to discover projectsDirectory from pom File parameter."
                    + " Reason: " + e.getMessage(), e );
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
            getLog().info( "No test projects were selected for execution." );
            return;
        }

        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn(
                           "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
        }

        scriptInterpreters = new LinkedHashMap();
        scriptInterpreters.put( "bsh", new BeanShellScriptInterpreter() );
        scriptInterpreters.put( "groovy", new GroovyScriptInterpreter() );

        File projectsDir = projectsDirectory;

        if ( cloneProjectsTo != null )
        {
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

        if ( !localRepositoryPath.exists() )
        {
            localRepositoryPath.mkdirs();
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
     * Copies the specified IT projects to the directory given by {@link #cloneProjectsTo}. A project may either be
     * denoted by a path to a POM file or merely by a path to a base directory.
     * 
     * @param includedProjects The paths to the IT projects, relative to the projects directory, must not be
     *            <code>null</code>.
     * @throws IOException The the projects could not be copied.
     */
    private void cloneProjects( String[] includedProjects )
        throws IOException
    {
        cloneProjectsTo.mkdirs();

        List clonedSubpaths = new ArrayList();

        for ( int i = 0; i < includedProjects.length; i++ )
        {
            String subpath = includedProjects[i];
            if ( !new File( projectsDirectory, subpath ).isDirectory() )
            {
                int lastSep = subpath.lastIndexOf( File.separator );
                if ( lastSep > -1 )
                {
                    subpath = subpath.substring( 0, lastSep );
                }
                else
                {
                    subpath = ".";
                }
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
                    copyDirectoryStructure( new File( projectsDirectory, subpath ), new File( cloneProjectsTo,
                                                                                              subpath ) );
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
    private void copyDirectoryStructure( File sourceDir, File destDir )
        throws IOException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        if ( !cloneAllFiles )
        {
            scanner.addDefaultExcludes();
        }
        scanner.scan();

        /*
         * NOTE: Make sure the destination directory is always there (even if empty) to support POM-less ITs.
         */
        destDir.mkdirs();
        String[] includedDirs = scanner.getIncludedDirectories();
        for ( int i = 0; i < includedDirs.length; ++i )
        {
            File clonedDir = new File( destDir, includedDirs[i] );
            clonedDir.mkdirs();
        }

        String[] includedFiles = scanner.getIncludedFiles();
        for ( int i = 0; i < includedFiles.length; ++i )
        {
            File sourceFile = new File( sourceDir, includedFiles[i] );
            File destFile = new File( destDir, includedFiles[i] );
            FileUtils.copyFile( sourceFile, destFile );
        }
    }

    static boolean alreadyCloned( String subpath, List clonedSubpaths )
    {
        for ( Iterator iter = clonedSubpaths.iterator(); iter.hasNext(); )
        {
            String path = (String) iter.next();

            if ( ".".equals( path ) || subpath.equals( path ) || subpath.startsWith( path + File.separator ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Runs the specified project.
     * 
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param project The relative path to the project, either to a POM file or merely to a directory, must not be
     *            <code>null</code>.
     * @param failures The list to record build failures in, must not be <code>null</code>.
     * @throws MojoExecutionException If the project could not be launched.
     */
    private void runBuild( File projectsDir, String project, List failures )
        throws MojoExecutionException
    {
        File pomFile = new File( projectsDir, project );
        File basedir;
        if ( pomFile.isDirectory() )
        {
            basedir = pomFile;
            pomFile = new File( basedir, "pom.xml" );
            if ( !pomFile.exists() )
            {
                pomFile = null;
            }
            else
            {
                project += File.separator + "pom.xml";
            }
        }
        else
        {
            basedir = pomFile.getParentFile();
        }

        getLog().info( "Building: " + project );

        File interpolatedPomFile = null;
        if ( pomFile != null )
        {
            interpolatedPomFile = new File( basedir, "interpolated-" + pomFile.getName() );
            buildInterpolatedFile( pomFile, interpolatedPomFile );
        }

        try
        {
            runBuild( project, basedir, interpolatedPomFile, failures );
        }
        finally
        {
            if ( interpolatedPomFile != null )
            {
                interpolatedPomFile.delete();
            }
        }
    }

    /**
     * Runs the specified project.
     * 
     * @param project The relative path to the project, either to a POM file or merely to a directory, must not be
     *            <code>null</code>.
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param pomFile The (already interpolated) POM file, may be <code>null</code> for a POM-less Maven invocation.
     * @param failures The list to record build failures in, must not be <code>null</code>.
     * @throws MojoExecutionException If the project could not be launched.
     */
    private void runBuild( String project, File basedir, File pomFile, List failures )
        throws MojoExecutionException
    {
        InvokerProperties invokerProperties = getInvokerProperties( basedir );
        if ( getLog().isDebugEnabled() && !invokerProperties.getProperties().isEmpty() )
        {
            Properties props = invokerProperties.getProperties();
            getLog().debug( "Using invoker properties:" );
            for ( Iterator it = new TreeSet( props.keySet() ).iterator(); it.hasNext(); )
            {
                String key = (String) it.next();
                String value = props.getProperty( key );
                getLog().debug( "  " + key + " = " + value );
            }
        }

        List goals = getGoals( basedir );

        List profiles = getProfiles( basedir );

        Properties systemProperties = getTestProperties( basedir );

        FileLogger logger = setupLogger( basedir );
        try
        {
            if ( !prebuild( basedir, logger ) )
            {
                getLog().info( "...FAILED[pre-build script did not succeed]" );

                failures.add( project );

                return;
            }

            final InvocationRequest request = new DefaultInvocationRequest();

            request.setLocalRepositoryDirectory( localRepositoryPath );

            if ( settingsFile != null )
            {
                File interpolatedSettingsFile =
                    new File( settingsFile.getParentFile(), "interpolated-" + settingsFile.getName() );
                buildInterpolatedFile( settingsFile, interpolatedSettingsFile );
                request.setUserSettingsFile( interpolatedSettingsFile );
            }

            request.setProperties( systemProperties );

            request.setInteractive( false );

            request.setShowErrors( showErrors );

            request.setDebug( debug );

            if ( logger != null )
            {
                request.setErrorHandler( logger );

                request.setOutputHandler( logger );
            }

            request.setBaseDirectory( basedir );

            if ( pomFile != null )
            {
                request.setPomFile( pomFile );
            }

            for ( int invocationIndex = 1;; invocationIndex++ )
            {
                if ( invocationIndex > 1 && !invokerProperties.isInvocationDefined( invocationIndex ) )
                {
                    break;
                }

                request.setGoals( goals );

                request.setProfiles( profiles );

                request.setMavenOpts( mavenOpts );

                invokerProperties.configureInvocation( request, invocationIndex );

                try
                {
                    getLog().debug( "Using MAVEN_OPTS: " + request.getMavenOpts() );
                    getLog().debug( "Executing: " + new MavenCommandLineBuilder().build( request ) );
                }
                catch ( CommandLineConfigurationException e )
                {
                    getLog().debug( "Failed to display command line: " + e.getMessage() );
                }

                InvocationResult result;

                try
                {
                    result = invoker.execute( request );
                }
                catch ( final MavenInvocationException e )
                {
                    getLog().debug( "Error invoking Maven: " + e.getMessage(), e );
                    getLog().info( "...FAILED[error invoking Maven]" );

                    failures.add( project );

                    return;
                }

                if ( result.getExecutionException() != null )
                {
                    if ( !suppressSummaries )
                    {
                        StringBuffer buffer = new StringBuffer( 256 );
                        buffer.append( "...FAILED. " );
                        if ( logger != null )
                        {
                            buffer.append( "See " );
                            buffer.append( logger.getOutputFile().getAbsolutePath() );
                            buffer.append( " for details." );
                        }
                        else
                        {
                            buffer.append( "See console output for details." );
                        }
                        getLog().info( buffer.toString() );
                    }

                    failures.add( project );

                    return;
                }
                else if ( !invokerProperties.isExpectedResult( result.getExitCode(), invocationIndex ) )
                {
                    if ( !suppressSummaries )
                    {
                        StringBuffer buffer = new StringBuffer( 256 );
                        buffer.append( "...FAILED[code=" ).append( result.getExitCode() ).append( "]. " );
                        if ( logger != null )
                        {
                            buffer.append( "See " );
                            buffer.append( logger.getOutputFile().getAbsolutePath() );
                            buffer.append( " for details." );
                        }
                        else
                        {
                            buffer.append( "See console output for details." );
                        }
                        getLog().info( buffer.toString() );
                    }

                    failures.add( project );

                    return;
                }
            }

            if ( !verify( basedir, logger ) )
            {
                if ( !suppressSummaries )
                {
                    getLog().info( "...FAILED[verify script did not succeed]." );
                }

                failures.add( project );
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

    /**
     * Initializes the build logger for the specified project.
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The build logger or <code>null</code> if logging has been disabled.
     * @throws MojoExecutionException If the log file could not be created.
     */
    private FileLogger setupLogger( File basedir )
        throws MojoExecutionException
    {
        FileLogger logger = null;

        if ( !noLog )
        {
            File outputLog = new File( basedir, "build.log" );
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
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error initializing build logfile in: " + outputLog, e );
            }
        }

        return logger;
    }

    /**
     * Gets the system properties to use for the specified project.
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The system properties to use, may be empty but never <code>null</code>.
     * @throws MojoExecutionException If the properties file exists but could not be read.
     */
    private Properties getTestProperties( final File basedir )
        throws MojoExecutionException
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

        if ( testPropertiesFile != null )
        {
            final File testProperties = new File( basedir, testPropertiesFile );

            if ( testProperties.exists() )
            {
                InputStream fin = null;
                try
                {
                    fin = new FileInputStream( testProperties );

                    Properties loadedProperties = new Properties();
                    loadedProperties.load( fin );
                    collectedTestProperties.putAll( loadedProperties );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error reading system properties for test: "
                        + testPropertiesFile );
                }
                finally
                {
                    IOUtil.close( fin );
                }
            }
        }

        return collectedTestProperties;
    }

    /**
     * Runs the pre-build-hook script of the specified project (if any).
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param logger The logger to redirect the script output to, may be <code>null</code> to use stdout/stderr.
     * @return <code>true</code> if the script does not exist or completed successfully, <code>false</code> otherwise.
     * @throws MojoExecutionException If an I/O error occurred while reading the script file.
     */
    private boolean prebuild( final File basedir, final FileLogger logger )
        throws MojoExecutionException
    {
        boolean result = true;

        if ( preBuildHookScript != null )
        {
            result = runScript( "pre-build script", basedir, preBuildHookScript, logger );
        }

        return result;
    }

    /**
     * Runs the post-build-hook script of the specified project (if any).
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param logger The logger to redirect the script output to, may be <code>null</code> to use stdout/stderr.
     * @return <code>true</code> if the script does not exist or completed successfully, <code>false</code> otherwise.
     * @throws MojoExecutionException If an I/O error occurred while reading the script file.
     */
    private boolean verify( final File basedir, final FileLogger logger )
        throws MojoExecutionException
    {
        boolean result = true;

        if ( postBuildHookScript != null )
        {
            result = runScript( "verification script", basedir, postBuildHookScript, logger );
        }

        return result;
    }

    /**
     * Runs the specified hook script of the specified project (if any).
     * 
     * @param scriptDescription The description of the script to use for logging, must not be <code>null</code>.
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param relativeScriptPath The path to the script relative to the project base directory, must not be
     *            <code>null</code>.
     * @param logger The logger to redirect the script output to, may be <code>null</code> to use stdout/stderr.
     * @return <code>true</code> if the script does not exist or completed successfully, <code>false</code> otherwise.
     * @throws MojoExecutionException If an I/O error occurred while reading the script file.
     */
    private boolean runScript( final String scriptDescription, final File basedir, final String relativeScriptPath,
                               final FileLogger logger )
        throws MojoExecutionException
    {
        final File scriptFile = resolveScript( new File( basedir, relativeScriptPath ) );

        if ( scriptFile.exists() )
        {
            List classPath = addTestClassPath ? testClassPath : Collections.EMPTY_LIST;

            Map globalVariables = new HashMap();
            globalVariables.put( "basedir", basedir );
            globalVariables.put( "localRepositoryPath", localRepositoryPath );

            PrintStream out = ( logger != null ) ? logger.getPrintStream() : null;

            ScriptInterpreter interpreter = getInterpreter( scriptFile );
            if ( getLog().isDebugEnabled() )
            {
                String name = interpreter.getClass().getName();
                name = name.substring( name.lastIndexOf( '.' ) + 1 );
                getLog().debug( "Running script with " + name );
            }

            String script;
            try
            {
                script = FileUtils.fileRead( scriptFile, encoding );
            }
            catch ( IOException e )
            {
                String errorMessage =
                    "error reading " + scriptDescription + " " + basedir.getPath() + File.separatorChar
                        + postBuildHookScript + ", " + e.getMessage();
                throw new MojoExecutionException( errorMessage, e );
            }

            try
            {
                if ( logger != null )
                {
                    logger.consumeLine( "Running " + scriptDescription + " in: " + scriptFile );
                }
                Object result = interpreter.evaluateScript( script, classPath, globalVariables, out );
                if ( logger != null )
                {
                    logger.consumeLine( "Finished " + scriptDescription + " in: " + scriptFile );
                }
                return Boolean.TRUE.equals( result ) || "true".equals( result );
            }
            catch ( Exception e )
            {
                String errorMessage =
                    "error evaluating " + scriptDescription + " " + basedir.getPath() + File.separatorChar
                        + postBuildHookScript + ", " + e.getMessage();
                getLog().error( errorMessage, e );
                if ( logger != null )
                {
                    e.printStackTrace( logger.getPrintStream() );
                }
                return false;
            }

        }

        return true;
    }

    /**
     * Gets the effective path to the specified script. For convenience, we allow to specify a script path as "verify"
     * and have the plugin auto-append the file extension to search for "verify.bsh" and "verify.groovy".
     * 
     * @param scriptFile The script file to resolve, may be <code>null</code>.
     * @return The effective path to the script file or <code>null</code> if the input was <code>null</code>.
     */
    private File resolveScript( File scriptFile )
    {
        if ( scriptFile != null && !scriptFile.exists() )
        {
            for ( Iterator it = this.scriptInterpreters.keySet().iterator(); it.hasNext(); )
            {
                String ext = (String) it.next();
                File candidateFile = new File( scriptFile.getPath() + '.' + ext );
                if ( candidateFile.exists() )
                {
                    scriptFile = candidateFile;
                    break;
                }
            }
        }
        return scriptFile;
    }

    /**
     * Determines the script interpreter for the specified script file by looking at its file extension. In this
     * context, file extensions are considered case-insensitive. For backward compatibility with plugin versions 1.2-,
     * the BeanShell interpreter will be used for any unrecognized extension.
     * 
     * @param scriptFile The script file for which to determine an interpreter, must not be <code>null</code>.
     * @return The script interpreter for the file, never <code>null</code>.
     */
    private ScriptInterpreter getInterpreter( File scriptFile )
    {
        String ext = FileUtils.extension( scriptFile.getName() ).toLowerCase( Locale.ENGLISH );
        ScriptInterpreter interpreter = (ScriptInterpreter) scriptInterpreters.get( ext );
        if ( interpreter == null )
        {
            interpreter = (ScriptInterpreter) scriptInterpreters.get( "bsh" );
        }
        return interpreter;
    }

    /**
     * Gets the goal list for the specified project.
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The list of goals to run when building the project, may be empty but never <code>null</code>.
     * @throws MojoExecutionException If the profile file could not be read.
     */
    List getGoals( final File basedir )
        throws MojoExecutionException
    {
        try
        {
            return getTokens( basedir, goalsFile, goals );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "error reading goals", e );
        }
    }

    /**
     * Gets the profile list for the specified project.
     * 
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The list of profiles to activate when building the project, may be empty but never <code>null</code>.
     * @throws MojoExecutionException If the profile file could not be read.
     */
    List getProfiles( File basedir )
        throws MojoExecutionException
    {
        try
        {
            return getTokens( basedir, profilesFile, profiles );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "error reading profiles", e );
        }
    }

    String[] getPoms()
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
                includes.add( testRegexes[i] );
            }

            final FileSet fs = new FileSet();

            fs.setIncludes( includes );
            //fs.setExcludes( pomExcludes );
            fs.setDirectory( projectsDirectory.getCanonicalPath() );
            fs.setFollowSymlinks( false );
            fs.setUseDefaultExcludes( true );

            final FileSetManager fsm = new FileSetManager( getLog() );

            List included = new ArrayList();
            included.addAll( Arrays.asList( fsm.getIncludedFiles( fs ) ) );
            included.addAll( Arrays.asList( fsm.getIncludedDirectories( fs ) ) );
            poms = (String[]) included.toArray( new String[included.size()] );
        }
        else
        {
            final FileSet fs = new FileSet();

            fs.setIncludes( pomIncludes );
            fs.setExcludes( pomExcludes );
            fs.setDirectory( projectsDirectory.getCanonicalPath() );
            fs.setFollowSymlinks( false );
            fs.setUseDefaultExcludes( true );

            final FileSetManager fsm = new FileSetManager( getLog() );

            List included = new ArrayList();
            included.addAll( Arrays.asList( fsm.getIncludedFiles( fs ) ) );
            included.addAll( Arrays.asList( fsm.getIncludedDirectories( fs ) ) );
            poms = (String[]) included.toArray( new String[included.size()] );
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

    /**
     * Gets goal/profile names for the specified project, either directly from the plugin configuration or from an
     * external token file.
     * 
     * @param basedir The base directory of the test project, must not be <code>null</code>.
     * @param filename The (simple) name of an optional file in the project base directory from which to read
     *            goals/profiles, may be <code>null</code>.
     * @param defaultTokens The list of tokens to return in case the specified token file does not exist, may be
     *            <code>null</code>.
     * @return The list of goal/profile names, may be empty but never <code>null</code>.
     * @throws IOException If the token file exists but could not be parsed.
     */
    private List getTokens( File basedir, String filename, List defaultTokens )
        throws IOException
    {
        List tokens = ( defaultTokens != null ) ? defaultTokens : new ArrayList();

        if ( StringUtils.isNotEmpty( filename ) )
        {
            File tokenFile = new File( basedir, filename );

            if ( tokenFile.exists() )
            {
                tokens = readTokens( tokenFile );
            }
        }

        return tokens;
    }

    /**
     * Reads the tokens from the specified file. Tokens are separated either by line terminators or commas. During
     * parsing, the file contents will be interpolated.
     * 
     * @param tokenFile The file to read the tokens from, must not be <code>null</code>.
     * @return The list of tokens, may be empty but never <code>null</code>.
     * @throws IOException If the token file could not be read.
     */
    private List readTokens( final File tokenFile )
        throws IOException
    {
        List result = new ArrayList();

        BufferedReader reader = null;
        try
        {
            Map composite = new CompositeMap( this.project, this.interpolationsProperties );
            reader = new BufferedReader( new InterpolationFilterReader( newReader( tokenFile ), composite ) );

            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                result.addAll( collectListFromCSV( line ) );
            }
        }
        finally
        {
            IOUtil.close( reader );
        }

        return result;
    }

    /**
     * Gets a list of comma separated tokens from the specified line.
     * 
     * @param csv The line with comma separated tokens, may be <code>null</code>.
     * @return The list of tokens from the line, may be empty but never <code>null</code>.
     */
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

    /**
     * Interpolates the specified POM/settings file to a temporary file.
     * 
     * @param originalFile The XML file to interpolate, must not be <code>null</code>.
     * @param interpolatedFile The target file to write the interpolated contents of the original file to, must not be
     *            <code>null</code>.
     * @throws MojoExecutionException If the target file could not be created.
     */
    void buildInterpolatedFile( File originalFile, File interpolatedFile )
        throws MojoExecutionException
    {
        if ( interpolatedFile.exists() )
        {
            interpolatedFile.delete();
        }
        interpolatedFile.deleteOnExit();
        try
        {
            if ( !interpolatedFile.createNewFile() )
            {
                throw new MojoExecutionException( "failed to create file " + interpolatedFile.getPath() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "failed to create file " + interpolatedFile.getPath(), e );
        }
        getLog().debug( "interpolate file to create interpolated in " + interpolatedFile.getPath() );

        if ( settings.getLocalRepository() != null )
        {
            if ( this.interpolationsProperties == null )
            {
                this.interpolationsProperties = new Properties();
            }
            this.interpolationsProperties.put( "localRepository", settings.getLocalRepository() );
        }
        Map composite = new CompositeMap( this.project, this.interpolationsProperties );

        BufferedReader reader = null;
        Writer writer = null;
        try
        {
            // interpolation with token @...@
            reader =
                new BufferedReader( new InterpolationFilterReader( ReaderFactory.newXmlReader( originalFile ),
                                                                   composite, "@", "@" ) );
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
            throw new MojoExecutionException( "failed to interpolate file " + originalFile.getPath(), e );
        }
        finally
        {
            // IOUtil in p-u is null check and silently NPE
            IOUtil.close( reader );
            IOUtil.close( writer );
        }
    }

    /**
     * Gets the (interpolated) invoker properties for an integration test.
     * 
     * @param projectDirectory The base directory of the IT project, must not be <code>null</code>.
     * @return The invoker properties, may be empty but never <code>null</code>.
     * @throws MojoExecutionException If an I/O error occurred during reading the properties.
     */
    private InvokerProperties getInvokerProperties( final File projectDirectory )
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
                try
                {
                    value = interpolator.interpolate( value, "" );
                }
                catch ( InterpolationException e )
                {
                    throw new MojoExecutionException( "Failed to interpolate invoker properties: " + propertiesFile,
                                                      e );
                }
                props.setProperty( key, value );
            }
        }
        return new InvokerProperties( props );
    }

}
