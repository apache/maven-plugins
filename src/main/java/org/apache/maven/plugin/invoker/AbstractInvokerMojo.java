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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Writer;
import org.apache.maven.plugin.registry.TrackableBase;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.scriptinterpreter.RunErrorException;
import org.apache.maven.shared.scriptinterpreter.RunFailureException;
import org.apache.maven.shared.scriptinterpreter.ScriptRunner;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides common code for mojos invoking sub builds.
 *
 * @author Stephen Connolly
 * @since 15-Aug-2009 09:09:29
 */
public abstract class AbstractInvokerMojo
    extends AbstractMojo
{

    private static final int SELECTOR_MAVENVERSION = 1;

    private static final int SELECTOR_JREVERSION = 2;

    private static final int SELECTOR_OSFAMILY = 4;

    /**
     * Flag used to suppress certain invocations. This is useful in tailoring the build using profiles.
     *
     * @since 1.1
     */
    @Parameter( property = "invoker.skip", defaultValue = "false" )
    private boolean skipInvocation;

    /**
     * Flag used to suppress the summary output notifying of successes and failures. If set to <code>true</code>, the
     * only indication of the build's success or failure will be the effect it has on the main build (if it fails, the
     * main build should fail as well). If {@link #streamLogs} is enabled, the sub-build summary will also provide an
     * indication.
     */
    @Parameter( defaultValue = "false" )
    protected boolean suppressSummaries;

    /**
     * Flag used to determine whether the build logs should be output to the normal mojo log.
     */
    @Parameter( property = "invoker.streamLogs", defaultValue = "false" )
    private boolean streamLogs;

    /**
     * The local repository for caching artifacts. It is strongly recommended to specify a path to an isolated
     * repository like <code>${project.build.directory}/it-repo</code>. Otherwise, your ordinary local repository will
     * be used, potentially soiling it with broken artifacts.
     */
    @Parameter( property = "invoker.localRepositoryPath", defaultValue = "${settings.localRepository}" )
    private File localRepositoryPath;

    /**
     * Directory to search for integration tests.
     */
    @Parameter( property = "invoker.projectsDirectory", defaultValue = "${basedir}/src/it/" )
    private File projectsDirectory;

    /**
     * Base directory where all build reports are written to. Every execution of an integration test will produce an XML
     * file which contains the information about success or failure of that particular build job. The format of the
     * resulting XML file is documented in the given <a href="./build-job.html">build-job</a> reference.
     *
     * @since 1.4
     */
    @Parameter( property = "invoker.reportsDirectory", defaultValue = "${project.build.directory}/invoker-reports" )
    private File reportsDirectory;

    /**
     * A flag to disable the generation of build reports.
     *
     * @since 1.4
     */
    @Parameter( property = "invoker.disableReports", defaultValue = "false" )
    private boolean disableReports;

    /**
     * Directory to which projects should be cloned prior to execution. If not specified, each integration test will be
     * run in the directory in which the corresponding IT POM was found. In this case, you most likely want to configure
     * your SCM to ignore <code>target</code> and <code>build.log</code> in the test's base directory.
     *
     * @since 1.1
     */
    @Parameter
    private File cloneProjectsTo;

    /**
     * Some files are normally excluded when copying the IT projects from the directory specified by the parameter
     * projectsDirectory to the directory given by cloneProjectsTo (e.g. <code>.svn</code>, <code>CVS</code>,
     * <code>*~</code>, etc). Setting this parameter to <code>true</code> will cause all files to be copied to the
     * cloneProjectsTo directory.
     *
     * @since 1.2
     */
    @Parameter( defaultValue = "false" )
    private boolean cloneAllFiles;

    /**
     * Ensure the {@link #cloneProjectsTo} directory is not polluted with files from earlier invoker runs.
     *
     * @since 1.6
     */
    @Parameter( defaultValue = "false" )
    private boolean cloneClean;

    /**
     * A single POM to build, skipping any scanning parameters and behavior.
     */
    @Parameter( property = "invoker.pom" )
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
     */
    @Parameter
    private List<String> pomIncludes = Collections.singletonList( "*/pom.xml" );

    /**
     * Exclude patterns for searching the integration test directory. This parameter is meant to be set from the POM. By
     * default, no POM files are excluded. For the convenience of using an include pattern like <code>*</code>, the
     * custom settings file specified by the parameter {@link #settingsFile} will always be excluded automatically.
     */
    @Parameter
    private List<String> pomExcludes = Collections.emptyList();

    /**
     * Include patterns for searching the projects directory for projects that need to be run before the other projects.
     * This parameter allows to declare projects that perform setup tasks like installing utility artifacts into the
     * local repository. Projects matched by these patterns are implicitly excluded from the scan for ordinary projects.
     * Also, the exclusions defined by the parameter {@link #pomExcludes} apply to the setup projects, too. Default
     * value is: <code>setup*&#47;pom.xml</code>.
     *
     * @since 1.3
     */
    @Parameter
    private List<String> setupIncludes = Collections.singletonList( "setup*/pom.xml" );

    /**
     * The list of goals to execute on each project. Default value is: <code>package</code>.
     */
    @Parameter
    private List<String> goals = Collections.singletonList( "package" );

    /**
     * The name of the project-specific file that contains the enumeration of goals to execute for that test.
     *
     * @deprecated As of version 1.2, the key <code>invoker.goals</code> from the properties file specified by the
     *             parameter {@link #invokerPropertiesFile} should be used instead.
     */
    @Parameter( property = "invoker.goalsFile", defaultValue = "goals.txt" )
    private String goalsFile;

    /**
     */
    @Component
    private Invoker invoker;

    @Component
    private MavenSettingsBuilder settingsBuilder;

    /**
     * Relative path of a selector script to run prior in order to decide if the build should be executed. This script
     * may be written with either BeanShell or Groovy. If the file extension is omitted (e.g. <code>selector</code>),
     * the plugin searches for the file by trying out the well-known extensions <code>.bsh</code> and
     * <code>.groovy</code>. If this script exists for a particular project but returns any non-null value different
     * from <code>true</code>, the corresponding build is flagged as skipped. In this case, none of the pre-build hook
     * script, Maven nor the post-build hook script will be invoked. If this script throws an exception, the
     * corresponding build is flagged as in error, and none of the pre-build hook script, Maven not the post-build hook
     * script will be invoked.
     *
     * @since 1.5
     */
    @Parameter( property = "invoker.selectorScript", defaultValue = "selector" )
    private String selectorScript;

    /**
     * Relative path of a pre-build hook script to run prior to executing the build. This script may be written with
     * either BeanShell or Groovy (since 1.3). If the file extension is omitted (e.g. <code>prebuild</code>), the plugin
     * searches for the file by trying out the well-known extensions <code>.bsh</code> and <code>.groovy</code>. If this
     * script exists for a particular project but returns any non-null value different from <code>true</code> or throws
     * an exception, the corresponding build is flagged as a failure. In this case, neither Maven nor the post-build
     * hook script will be invoked.
     */
    @Parameter( property = "invoker.preBuildHookScript", defaultValue = "prebuild" )
    private String preBuildHookScript;

    /**
     * Relative path of a cleanup/verification hook script to run after executing the build. This script may be written
     * with either BeanShell or Groovy (since 1.3). If the file extension is omitted (e.g. <code>verify</code>), the
     * plugin searches for the file by trying out the well-known extensions <code>.bsh</code> and <code>.groovy</code>.
     * If this script exists for a particular project but returns any non-null value different from <code>true</code> or
     * throws an exception, the corresponding build is flagged as a failure.
     */
    @Parameter( property = "invoker.postBuildHookScript", defaultValue = "postbuild" )
    private String postBuildHookScript;

    /**
     * Location of a properties file that defines CLI properties for the test.
     */
    @Parameter( property = "invoker.testPropertiesFile", defaultValue = "test.properties" )
    private String testPropertiesFile;

    /**
     * Common set of test properties to pass in on each IT's command line, via -D parameters.
     *
     * @deprecated As of version 1.1, use the {@link #properties} parameter instead.
     */
    @Parameter
    private Properties testProperties;

    /**
     * Common set of properties to pass in on each project's command line, via -D parameters.
     *
     * @since 1.1
     */
    @Parameter
    private Map<String, String> properties;

    /**
     * Whether to show errors in the build output.
     */
    @Parameter( property = "invoker.showErrors", defaultValue = "false" )
    private boolean showErrors;

    /**
     * Whether to show debug statements in the build output.
     */
    @Parameter( property = "invoker.debug", defaultValue = "false" )
    private boolean debug;

    /**
     * Suppress logging to the <code>build.log</code> file.
     */
    @Parameter( property = "invoker.noLog", defaultValue = "false" )
    private boolean noLog;

    /**
     * List of profile identifiers to explicitly trigger in the build.
     *
     * @since 1.1
     */
    @Parameter
    private List<String> profiles;

    /**
     * List of properties which will be used to interpolate goal files.
     *
     * @since 1.1
     * @deprecated As of version 1.3, the parameter {@link #filterProperties} should be used instead.
     */
    @Parameter
    private Properties interpolationsProperties;

    /**
     * A list of additional properties which will be used to filter tokens in POMs and goal files.
     *
     * @since 1.3
     */
    @Parameter
    private Map<String, String> filterProperties;

    /**
     * The Maven Project Object
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${mojoExecution}", readonly = true, required = true )
    private MojoExecution mojoExecution;

    /**
     * A comma separated list of projectname patterns to run. Specify this parameter to run individual tests by file
     * name, overriding the {@link #setupIncludes}, {@link #pomIncludes} and {@link #pomExcludes} parameters. Each
     * pattern you specify here will be used to create an include/exclude pattern formatted like
     * <code>${projectsDirectory}/<i>pattern</i></code>. To exclude a test, prefix the pattern with a '<code>!</code>'.
     * So you can just type <nobr><code>-Dinvoker.test=SimpleTest,Comp*Test,!Compare*</code></nobr> to run builds in
     * <code>${projectsDirectory}/SimpleTest</code> and <code>${projectsDirectory}/ComplexTest</code>, but not
     * <code>${projectsDirectory}/CompareTest</code>
     *
     * @since 1.1 (exclusion since 1.8)
     */
    @Parameter( property = "invoker.test" )
    private String invokerTest;

    /**
     * The name of the project-specific file that contains the enumeration of profiles to use for that test. <b>If the
     * file exists and is empty no profiles will be used even if the parameter {@link #profiles} is set.</b>
     *
     * @since 1.1
     * @deprecated As of version 1.2, the key <code>invoker.profiles</code> from the properties file specified by the
     *             parameter {@link #invokerPropertiesFile} should be used instead.
     */
    @Parameter( property = "invoker.profilesFile", defaultValue = "profiles.txt" )
    private String profilesFile;

    /**
     * Path to an alternate <code>settings.xml</code> to use for Maven invocation with all ITs. Note that the
     * <code>&lt;localRepository&gt;</code> element of this settings file is always ignored, i.e. the path given by the
     * parameter {@link #localRepositoryPath} is dominant.
     *
     * @since 1.2
     */
    @Parameter( property = "invoker.settingsFile" )
    private File settingsFile;

    /**
     * The <code>MAVEN_OPTS</code> environment variable to use when invoking Maven. This value can be overridden for
     * individual integration tests by using {@link #invokerPropertiesFile}.
     *
     * @since 1.2
     */
    @Parameter( property = "invoker.mavenOpts" )
    private String mavenOpts;

    /**
     * The home directory of the Maven installation to use for the forked builds. Defaults to the current Maven
     * installation.
     *
     * @since 1.3
     */
    @Parameter( property = "invoker.mavenHome" )
    private File mavenHome;

    /**
     * mavenExecutable can either be a file relative to <code>${maven.home}/bin/</code> or an absolute file.
     *
     * @since 1.8
     * @see Invoker#setMavenExecutable(File)
     */
    @Parameter( property = "invoker.mavenExecutable" )
    private String mavenExecutable;

    /**
     * The <code>JAVA_HOME</code> environment variable to use for forked Maven invocations. Defaults to the current Java
     * home directory.
     *
     * @since 1.3
     */
    @Parameter( property = "invoker.javaHome" )
    private File javaHome;

    /**
     * The file encoding for the pre-/post-build scripts and the list files for goals and profiles.
     *
     * @since 1.2
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * The current user system settings for use in Maven.
     *
     * @since 1.2
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    /**
     * A flag whether the test class path of the project under test should be included in the class path of the
     * pre-/post-build scripts. If set to <code>false</code>, the class path of script interpreter consists only of the
     * <a href="dependencies.html">runtime dependencies</a> of the Maven Invoker Plugin. If set the <code>true</code>,
     * the project's test class path will be prepended to the interpreter class path. Among others, this feature allows
     * the scripts to access utility classes from the test sources of your project.
     *
     * @since 1.2
     */
    @Parameter( property = "invoker.addTestClassPath", defaultValue = "false" )
    private boolean addTestClassPath;

    /**
     * The test class path of the project under test.
     */
    @Parameter( defaultValue = "${project.testClasspathElements}", readonly = true )
    private List<String> testClassPath;

    /**
     * The name of an optional project-specific file that contains properties used to specify settings for an individual
     * Maven invocation. Any property present in the file will override the corresponding setting from the plugin
     * configuration. The values of the properties are filtered and may use expressions like
     * <code>${project.version}</code> to reference project properties or values from the parameter
     * {@link #filterProperties}. The snippet below describes the supported properties:
     * <p/>
     *
     * <pre>
     * # A comma or space separated list of goals/phases to execute, may
     * # specify an empty list to execute the default goal of the IT project
     * invoker.goals = clean install
     *
     * # Or you can give things like this if you need.
     * invoker.goals = -T2 clean verify
     *
     * # Optionally, a list of goals to run during further invocations of Maven
     * invoker.goals.2 = ${project.groupId}:${project.artifactId}:${project.version}:run
     *
     * # A comma or space separated list of profiles to activate
     * invoker.profiles = its,jdk15
     *
     * # The path to an alternative POM or base directory to invoke Maven on, defaults to the
     * # project that was originally specified in the plugin configuration
     * # Since plugin version 1.4
     * invoker.project = sub-module
     *
     * # The value for the environment variable MAVEN_OPTS
     * invoker.mavenOpts = -Dfile.encoding=UTF-16 -Xms32m -Xmx256m
     *
     * # Possible values are &quot;fail-fast&quot; (default), &quot;fail-at-end&quot; and &quot;fail-never&quot;
     * invoker.failureBehavior = fail-never
     *
     * # The expected result of the build, possible values are &quot;success&quot; (default) and &quot;failure&quot;
     * invoker.buildResult = failure
     *
     * # A boolean value controlling the aggregator mode of Maven, defaults to &quot;false&quot;
     * invoker.nonRecursive = true
     *
     * # A boolean value controlling the network behavior of Maven, defaults to &quot;false&quot;
     * # Since plugin version 1.4
     * invoker.offline = true
     *
     * # The path to the properties file from which to load system properties, defaults to the
     * # filename given by the plugin parameter testPropertiesFile
     * # Since plugin version 1.4
     * invoker.systemPropertiesFile = test.properties
     *
     * # An optional human friendly name for this build job to be included in the build reports.
     * # Since plugin version 1.4
     * invoker.name = Test Build 01
     *
     * # An optional description for this build job to be included in the build reports.
     * # Since plugin version 1.4
     * invoker.description = Checks the support for build reports.
     *
     * # A comma separated list of JRE versions on which this build job should be run.
     * # Since plugin version 1.4
     * invoker.java.version = 1.4+, !1.4.1, 1.7-
     *
     * # A comma separated list of OS families on which this build job should be run.
     * # Since plugin version 1.4
     * invoker.os.family = !windows, unix, mac
     *
     * # A comma separated list of Maven versions on which this build should be run.
     * # Since plugin version 1.5
     * invoker.maven.version = 2.0.10+, !2.1.0, !2.2.0
     *
     * # A boolean value controlling the debug logging level of Maven, , defaults to &quot;false&quot;
     * # Since plugin version 1.8
     * invoker.debug = true
     * </pre>
     *
     * @since 1.2
     */
    @Parameter( property = "invoker.invokerPropertiesFile", defaultValue = "invoker.properties" )
    private String invokerPropertiesFile;

    /**
     * flag to enable show mvn version used for running its (cli option : -V,--show-version )
     *
     * @since 1.4
     */
    @Parameter( property = "invoker.showVersion", defaultValue = "false" )
    private boolean showVersion;

    /**
     * number of threads for running tests in parallel. This will be the number of maven forked process in parallel.
     *
     * @since 1.6
     */
    @Parameter( property = "invoker.parallelThreads", defaultValue = "1" )
    private int parallelThreads;

    /**
     * @since 1.6
     */
    @Parameter( property = "plugin.artifacts", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    /**
     * If enable and if you have a settings file configured for the execution, it will be merged with your user
     * settings.
     *
     * @since 1.6
     */
    @Parameter( property = "invoker.mergeUserSettings", defaultValue = "false" )
    private boolean mergeUserSettings;

    /**
     * Additional environment variables to set on the command line.
     *
     * @since 1.8
     */
    @Parameter
    private Map<String, String> environmentVariables;

    /**
     * Additional variables for use in the hook scripts.
     *
     * @since 1.9
     */
    @Parameter
    private Map<String, String> scriptVariables;

    /**
     * The scripter runner that is responsible to execute hook scripts.
     */
    private ScriptRunner scriptRunner;

    /**
     * A string used to prefix the file name of the filtered POMs in case the POMs couldn't be filtered in-place (i.e.
     * the projects were not cloned to a temporary directory), can be <code>null</code>. This will be set to
     * <code>null</code> if the POMs have already been filtered during cloning.
     */
    private String filteredPomPrefix = "interpolated-";

    /**
     * The format for elapsed build time.
     */
    private final DecimalFormat secFormat = new DecimalFormat( "(0.0 s)", new DecimalFormatSymbols( Locale.ENGLISH ) );

    /**
     * The version of Maven which is used to run the builds
     */
    private String actualMavenVersion;

    /**
     * Invokes Maven on the configured test projects.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If the goal encountered severe errors.
     * @throws org.apache.maven.plugin.MojoFailureException If any of the Maven builds failed.
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

        // done it here to prevent issues with concurrent access in case of parallel run
        if ( !disableReports && !reportsDirectory.exists() )
        {
            reportsDirectory.mkdirs();
        }

        // CHECKSTYLE_OFF: LineLength

        BuildJob[] buildJobs;
        if ( pom != null )
        {
            try
            {
                projectsDirectory = pom.getCanonicalFile().getParentFile();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                                                  "Failed to discover projectsDirectory from pom File parameter. Reason: "
                                                      + e.getMessage(), e );
            }

            buildJobs = new BuildJob[] { new BuildJob( pom.getName(), BuildJob.Type.NORMAL ) };
        }
        else
        {
            try
            {
                buildJobs = getBuildJobs();
            }
            catch ( final IOException e )
            {
                throw new MojoExecutionException(
                                                  "Error retrieving POM list from includes, excludes, and projects directory. Reason: "
                                                      + e.getMessage(), e );
            }
        }
        // CHECKSTYLE_ON: LineLength

        if ( ( buildJobs == null ) || ( buildJobs.length < 1 ) )
        {
            doFailIfNoProjects();

            getLog().info( "No projects were selected for execution." );
            return;
        }

        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
        }

        final List<String> scriptClassPath;
        if ( addTestClassPath )
        {
            scriptClassPath = new ArrayList<String>( testClassPath );
            for ( Artifact pluginArtifact : pluginArtifacts )
            {
                scriptClassPath.remove( pluginArtifact.getFile().getAbsolutePath() );
            }
        }
        else
        {
            scriptClassPath = null;
        }
        scriptRunner = new ScriptRunner( getLog() );
        scriptRunner.setScriptEncoding( encoding );
        scriptRunner.setGlobalVariable( "localRepositoryPath", localRepositoryPath );
        if ( scriptVariables != null )
        {
            for ( Entry<String, String> entry : scriptVariables.entrySet() )
            {
                scriptRunner.setGlobalVariable( entry.getKey(), entry.getValue() );
            }
        }
        scriptRunner.setClassPath( scriptClassPath );

        Collection<String> collectedProjects = new LinkedHashSet<String>();
        for ( BuildJob buildJob : buildJobs )
        {
            collectProjects( projectsDirectory, buildJob.getProject(), collectedProjects, true );
        }

        File projectsDir = projectsDirectory;

        if ( cloneProjectsTo != null )
        {
            cloneProjects( collectedProjects );
            projectsDir = cloneProjectsTo;
        }
        else
        {
            getLog().warn( "Filtering of parent/child POMs is not supported without cloning the projects" );
        }

        runBuilds( projectsDir, buildJobs );

        writeSummaryFile( buildJobs );

        processResults( new InvokerSession( buildJobs ) );

    }

    private void writeSummaryFile( BuildJob[] buildJobs )
        throws MojoExecutionException
    {

        File summaryReportFile = new File( reportsDirectory, "invoker-summary.txt" );

        try
        {
            Writer writer = new BufferedWriter( new FileWriter( summaryReportFile ) );

            for ( int i = 0; i < buildJobs.length; i++ )
            {
                BuildJob buildJob = buildJobs[i];
                if ( !buildJob.getResult().equals( BuildJob.Result.SUCCESS ) )
                {
                    writer.append( buildJob.getResult() );
                    writer.append( " [" );
                    writer.append( buildJob.getProject() );
                    writer.append( "] " );
                    if ( buildJob.getFailureMessage() != null )
                    {
                        writer.append( " " );
                        writer.append( buildJob.getFailureMessage() );
                    }
                    writer.append( "\n" );
                }
            }

            writer.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write summary report " + summaryReportFile, e );
        }
    }

    protected void doFailIfNoProjects()
        throws MojoFailureException
    {
        // should only be used during run and verify
    }

    /**
     * Processes the results of invoking the build jobs.
     *
     * @param invokerSession The session with the build jobs, must not be <code>null</code>.
     * @throws MojoFailureException If the mojo had failed as a result of invoking the build jobs.
     * @since 1.4
     */
    abstract void processResults( InvokerSession invokerSession )
        throws MojoFailureException;

    /**
     * Creates a new reader for the specified file, using the plugin's {@link #encoding} parameter.
     *
     * @param file The file to create a reader for, must not be <code>null</code>.
     * @return The reader for the file, never <code>null</code>.
     * @throws java.io.IOException If the specified file was not found or the configured encoding is not supported.
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
     * Collects all projects locally reachable from the specified project. The method will as such try to read the POM
     * and recursively follow its parent/module elements.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param projectPath The relative path of the current project, can denote either the POM or its base directory,
     *            must not be <code>null</code>.
     * @param projectPaths The set of already collected projects to add new projects to, must not be <code>null</code>.
     *            This set will hold the relative paths to either a POM file or a project base directory.
     * @param included A flag indicating whether the specified project has been explicitly included via the parameter
     *            {@link #pomIncludes}. Such projects will always be added to the result set even if there is no
     *            corresponding POM.
     * @throws org.apache.maven.plugin.MojoExecutionException If the project tree could not be traversed.
     */
    private void collectProjects( File projectsDir, String projectPath, Collection<String> projectPaths,
                                  boolean included )
        throws MojoExecutionException
    {
        projectPath = projectPath.replace( '\\', '/' );
        File pomFile = new File( projectsDir, projectPath );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
            if ( !pomFile.exists() )
            {
                if ( included )
                {
                    projectPaths.add( projectPath );
                }
                return;
            }
            if ( !projectPath.endsWith( "/" ) )
            {
                projectPath += '/';
            }
            projectPath += "pom.xml";
        }
        else if ( !pomFile.isFile() )
        {
            return;
        }
        if ( !projectPaths.add( projectPath ) )
        {
            return;
        }
        getLog().debug( "Collecting parent/child projects of " + projectPath );

        Model model = PomUtils.loadPom( pomFile );

        try
        {
            String projectsRoot = projectsDir.getCanonicalPath();
            String projectDir = pomFile.getParent();

            String parentPath = "../pom.xml";
            if ( model.getParent() != null && StringUtils.isNotEmpty( model.getParent().getRelativePath() ) )
            {
                parentPath = model.getParent().getRelativePath();
            }
            String parent = relativizePath( new File( projectDir, parentPath ), projectsRoot );
            if ( parent != null )
            {
                collectProjects( projectsDir, parent, projectPaths, false );
            }

            Collection<String> modulePaths = new LinkedHashSet<String>();

            modulePaths.addAll( model.getModules() );

            for ( Profile profile : model.getProfiles() )
            {
                modulePaths.addAll( profile.getModules() );
            }

            for ( String modulePath : modulePaths )
            {
                String module = relativizePath( new File( projectDir, modulePath ), projectsRoot );
                if ( module != null )
                {
                    collectProjects( projectsDir, module, projectPaths, false );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to analyze POM: " + pomFile, e );
        }
    }

    /**
     * Copies the specified projects to the directory given by {@link #cloneProjectsTo}. A project may either be denoted
     * by a path to a POM file or merely by a path to a base directory. During cloning, the POM files will be filtered.
     *
     * @param projectPaths The paths to the projects to clone, relative to the projects directory, must not be
     *            <code>null</code> nor contain <code>null</code> elements.
     * @throws org.apache.maven.plugin.MojoExecutionException If the the projects could not be copied/filtered.
     */
    private void cloneProjects( Collection<String> projectPaths )
        throws MojoExecutionException
    {
        if ( !cloneProjectsTo.mkdirs() && cloneClean )
        {
            try
            {
                FileUtils.cleanDirectory( cloneProjectsTo );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not clean the cloneProjectsTo directory. Reason: "
                    + e.getMessage(), e );
            }
        }

        // determine project directories to clone
        Collection<String> dirs = new LinkedHashSet<String>();
        for ( String projectPath : projectPaths )
        {
            if ( !new File( projectsDirectory, projectPath ).isDirectory() )
            {
                projectPath = getParentPath( projectPath );
            }
            dirs.add( projectPath );
        }

        boolean filter;

        // clone project directories
        try
        {
            filter = !cloneProjectsTo.getCanonicalFile().equals( projectsDirectory.getCanonicalFile() );

            List<String> clonedSubpaths = new ArrayList<String>();

            for ( String subpath : dirs )
            {
                // skip this project if its parent directory is also scheduled for cloning
                if ( !".".equals( subpath ) && dirs.contains( getParentPath( subpath ) ) )
                {
                    continue;
                }

                // avoid copying subdirs that are already cloned.
                if ( !alreadyCloned( subpath, clonedSubpaths ) )
                {
                    // avoid creating new files that point to dir/.
                    if ( ".".equals( subpath ) )
                    {
                        String cloneSubdir = relativizePath( cloneProjectsTo, projectsDirectory.getCanonicalPath() );

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
                        File srcDir = new File( projectsDirectory, subpath );
                        File dstDir = new File( cloneProjectsTo, subpath );
                        copyDirectoryStructure( srcDir, dstDir );
                    }

                    clonedSubpaths.add( subpath );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to clone projects from: " + projectsDirectory + " to: "
                + cloneProjectsTo + ". Reason: " + e.getMessage(), e );
        }

        // filter cloned POMs
        if ( filter )
        {
            for ( String projectPath : projectPaths )
            {
                File pomFile = new File( cloneProjectsTo, projectPath );
                if ( pomFile.isFile() )
                {
                    buildInterpolatedFile( pomFile, pomFile );
                }

                //MINVOKER-186
                //The following is a temporary solution to support Maven 3.3.1 (.mvn/extensions.xml) filtering
                //Will be replaced by MINVOKER-117 with general filtering mechanism
                File baseDir = pomFile.getParentFile();
                File mvnDir = new File( baseDir, ".mvn" );
                if ( mvnDir.isDirectory() )
                {
                    File extensionsFile = new File( mvnDir, "extensions.xml" );
                    if ( extensionsFile.isFile() )
                    {
                        buildInterpolatedFile( extensionsFile, extensionsFile );
                    }
                }
                //END MINVOKER-186
            }
            filteredPomPrefix = null;
        }
    }

    /**
     * Gets the parent path of the specified relative path.
     *
     * @param path The relative path whose parent should be retrieved, must not be <code>null</code>.
     * @return The parent path or "." if the specified path has no parent, never <code>null</code>.
     */
    private String getParentPath( String path )
    {
        int lastSep = Math.max( path.lastIndexOf( '/' ), path.lastIndexOf( '\\' ) );
        return ( lastSep < 0 ) ? "." : path.substring( 0, lastSep );
    }

    /**
     * Copied a directory structure with default exclusions (.svn, CVS, etc)
     *
     * @param sourceDir The source directory to copy, must not be <code>null</code>.
     * @param destDir The target directory to copy to, must not be <code>null</code>.
     * @throws java.io.IOException If the directory structure could not be copied.
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
        // Create all the directories, including any symlinks present in source
        FileUtils.mkDirs( sourceDir, scanner.getIncludedDirectories(), destDir );

        for ( String includedFile : scanner.getIncludedFiles() )
        {
            File sourceFile = new File( sourceDir, includedFile );
            File destFile = new File( destDir, includedFile );
            FileUtils.copyFile( sourceFile, destFile );

            //ensure clone project must be writable for additional changes
            destFile.setWritable( true );
        }
    }

    /**
     * Determines whether the specified sub path has already been cloned, i.e. whether one of its ancestor directories
     * was already cloned.
     *
     * @param subpath The sub path to check, must not be <code>null</code>.
     * @param clonedSubpaths The list of already cloned paths, must not be <code>null</code> nor contain
     *            <code>null</code> elements.
     * @return <code>true</code> if the specified path has already been cloned, <code>false</code> otherwise.
     */
    static boolean alreadyCloned( String subpath, List<String> clonedSubpaths )
    {
        for ( String path : clonedSubpaths )
        {
            if ( ".".equals( path ) || subpath.equals( path ) || subpath.startsWith( path + File.separator ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Runs the specified build jobs.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param buildJobs The build jobs to run must not be <code>null</code> nor contain <code>null</code> elements.
     * @throws org.apache.maven.plugin.MojoExecutionException If any build could not be launched.
     */
    private void runBuilds( final File projectsDir, BuildJob[] buildJobs )
        throws MojoExecutionException
    {
        if ( !localRepositoryPath.exists() )
        {
            localRepositoryPath.mkdirs();
        }

        // -----------------------------------------------
        // interpolate settings file
        // -----------------------------------------------

        File interpolatedSettingsFile = null;
        if ( settingsFile != null )
        {
            if ( cloneProjectsTo != null )
            {
                interpolatedSettingsFile = new File( cloneProjectsTo, "interpolated-" + settingsFile.getName() );
            }
            else
            {
                interpolatedSettingsFile =
                    new File( settingsFile.getParentFile(), "interpolated-" + settingsFile.getName() );
            }
            buildInterpolatedFile( settingsFile, interpolatedSettingsFile );
        }

        // -----------------------------------------------
        // merge settings file
        // -----------------------------------------------

        SettingsXpp3Writer settingsWriter = new SettingsXpp3Writer();

        File mergedSettingsFile;
        Settings mergedSettings = this.settings;
        if ( mergeUserSettings )
        {
            if ( interpolatedSettingsFile != null )
            {
                // Have to merge the specified settings file (dominant) and the one of the invoking Maven process
                try
                {
                    Settings dominantSettings = settingsBuilder.buildSettings( interpolatedSettingsFile, false );
                    Settings recessiveSettings = cloneSettings();
                    SettingsUtils.merge( dominantSettings, recessiveSettings, TrackableBase.USER_LEVEL );

                    mergedSettings = dominantSettings;
                    getLog().debug( "Merged specified settings file with settings of invoking process" );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MojoExecutionException( "Could not read specified settings file", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not read specified settings file", e );
                }
            }
        }
        if ( this.settingsFile != null && !mergeUserSettings )
        {
            mergedSettingsFile = interpolatedSettingsFile;
        }
        else
        {
            try
            {
                mergedSettingsFile = File.createTempFile( "invoker-settings", ".xml" );

                FileWriter fileWriter = null;
                try
                {
                    fileWriter = new FileWriter( mergedSettingsFile );
                    settingsWriter.write( fileWriter, mergedSettings );
                }
                finally
                {
                    IOUtil.close( fileWriter );
                }

                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Created temporary file for invoker settings.xml: "
                                        + mergedSettingsFile.getAbsolutePath() );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not create temporary file for invoker settings.xml", e );
            }
        }
        final File finalSettingsFile = mergedSettingsFile;

        if ( mavenHome != null )
        {
            actualMavenVersion = SelectorUtils.getMavenVersion( mavenHome );
        }
        else
        {
            actualMavenVersion = SelectorUtils.getMavenVersion();
        }
        scriptRunner.setGlobalVariable( "mavenVersion", actualMavenVersion );

        final CharSequence actualJreVersion;
        // @todo if ( javaVersions ) ... to be picked up from toolchains
        if ( javaHome != null )
        {
            actualJreVersion = resolveExternalJreVersion();
        }
        else
        {
            actualJreVersion = SelectorUtils.getJreVersion();
        }

        try
        {
            if ( isParallelRun() )
            {
                getLog().info( "use parallelThreads " + parallelThreads );

                ExecutorService executorService = Executors.newFixedThreadPool( parallelThreads );
                for ( final BuildJob job : buildJobs )
                {
                    executorService.execute( new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                runBuild( projectsDir, job, finalSettingsFile, javaHome, actualJreVersion );
                            }
                            catch ( MojoExecutionException e )
                            {
                                throw new RuntimeException( e.getMessage(), e );
                            }
                        }
                    } );
                }

                try
                {
                    executorService.shutdown();
                    // TODO add a configurable time out
                    executorService.awaitTermination( Long.MAX_VALUE, TimeUnit.MILLISECONDS );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            }
            else
            {
                for ( BuildJob job : buildJobs )
                {
                    runBuild( projectsDir, job, finalSettingsFile, javaHome, actualJreVersion );
                }
            }
        }
        finally
        {
            if ( interpolatedSettingsFile != null && cloneProjectsTo == null )
            {
                interpolatedSettingsFile.delete();
            }
            if ( mergedSettingsFile != null && mergedSettingsFile.exists() )
            {
                mergedSettingsFile.delete();
            }
        }
    }

    private Settings cloneSettings()
    {
        Settings recessiveSettings = SettingsUtils.copySettings( this.settings );

        // MINVOKER-133: reset sourceLevelSet
        resetSourceLevelSet( recessiveSettings );
        for ( org.apache.maven.settings.Mirror mirror : recessiveSettings.getMirrors() )
        {
            resetSourceLevelSet( mirror );
        }
        for ( org.apache.maven.settings.Server server : recessiveSettings.getServers() )
        {
            resetSourceLevelSet( server );
        }
        for ( org.apache.maven.settings.Proxy proxy : recessiveSettings.getProxies() )
        {
            resetSourceLevelSet( proxy );
        }
        for ( org.apache.maven.settings.Profile profile : recessiveSettings.getProfiles() )
        {
            resetSourceLevelSet( profile );
        }

        return recessiveSettings;
    }

    private void resetSourceLevelSet( org.apache.maven.settings.TrackableBase trackable )
    {
        try
        {
            ReflectionUtils.setVariableValueInObject( trackable, "sourceLevelSet", Boolean.FALSE );
            getLog().debug( "sourceLevelSet: "
                                + ReflectionUtils.getValueIncludingSuperclasses( "sourceLevelSet", trackable ) );
        }
        catch ( IllegalAccessException e )
        {
            // noop
        }
    }

    private CharSequence resolveExternalJreVersion()
    {
        Artifact pluginArtifact = mojoExecution.getMojoDescriptor().getPluginDescriptor().getPluginArtifact();
        pluginArtifact.getFile();

        Commandline commandLine = new Commandline();
        commandLine.setExecutable( new File( javaHome, "bin/java" ).getAbsolutePath() );
        commandLine.createArg().setValue( "-cp" );
        commandLine.createArg().setFile( pluginArtifact.getFile() );
        commandLine.createArg().setValue( SystemPropertyPrinter.class.getName() );
        commandLine.createArg().setValue( "java.version" );

        final StringBuilder actualJreVersion = new StringBuilder();
        StreamConsumer consumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                actualJreVersion.append( line );
            }
        };
        try
        {
            CommandLineUtils.executeCommandLine( commandLine, consumer, null );
        }
        catch ( CommandLineException e )
        {
            getLog().warn( e.getMessage() );
        }
        return actualJreVersion;
    }

    /**
     * Runs the specified project.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param buildJob The build job to run, must not be <code>null</code>.
     * @param settingsFile The (already interpolated) user settings file for the build, may be <code>null</code> to use
     *            the current user settings.
     * @throws org.apache.maven.plugin.MojoExecutionException If the project could not be launched.
     */
    private void runBuild( File projectsDir, BuildJob buildJob, File settingsFile, File actualJavaHome,
                           CharSequence actualJreVersion )
        throws MojoExecutionException
    {
        File pomFile = new File( projectsDir, buildJob.getProject() );
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
                buildJob.setProject( buildJob.getProject() + File.separator + "pom.xml" );
            }
        }
        else
        {
            basedir = pomFile.getParentFile();
        }

        getLog().info( "Building: " + buildJob.getProject() );

        File interpolatedPomFile = null;
        if ( pomFile != null )
        {
            if ( filteredPomPrefix != null )
            {
                interpolatedPomFile = new File( basedir, filteredPomPrefix + pomFile.getName() );
                buildInterpolatedFile( pomFile, interpolatedPomFile );
            }
            else
            {
                interpolatedPomFile = pomFile;
            }
        }

        InvokerProperties invokerProperties = getInvokerProperties( basedir );

        // let's set what details we can
        buildJob.setName( invokerProperties.getJobName() );
        buildJob.setDescription( invokerProperties.getJobDescription() );

        try
        {
            int selection = getSelection( invokerProperties, actualJreVersion );
            if ( selection == 0 )
            {
                long milliseconds = System.currentTimeMillis();
                boolean executed;
                try
                {
                    // CHECKSTYLE_OFF: LineLength
                    executed = runBuild( basedir, interpolatedPomFile, settingsFile, actualJavaHome, invokerProperties );
                    // CHECKSTYLE_ON: LineLength
                }
                finally
                {
                    milliseconds = System.currentTimeMillis() - milliseconds;
                    buildJob.setTime( milliseconds / 1000.0 );
                }

                if ( executed )
                {
                    buildJob.setResult( BuildJob.Result.SUCCESS );

                    if ( !suppressSummaries )
                    {
                        getLog().info( "..SUCCESS " + formatTime( buildJob.getTime() ) );
                    }
                }
                else
                {
                    buildJob.setResult( BuildJob.Result.SKIPPED );

                    if ( !suppressSummaries )
                    {
                        getLog().info( "..SKIPPED " + formatTime( buildJob.getTime() ) );
                    }
                }
            }
            else
            {
                buildJob.setResult( BuildJob.Result.SKIPPED );

                StringBuilder message = new StringBuilder();
                if ( ( selection & SELECTOR_MAVENVERSION ) != 0 )
                {
                    message.append( "Maven version" );
                }
                if ( ( selection & SELECTOR_JREVERSION ) != 0 )
                {
                    if ( message.length() > 0 )
                    {
                        message.append( ", " );
                    }
                    message.append( "JRE version" );
                }
                if ( ( selection & SELECTOR_OSFAMILY ) != 0 )
                {
                    if ( message.length() > 0 )
                    {
                        message.append( ", " );
                    }
                    message.append( "OS" );
                }

                if ( !suppressSummaries )
                {
                    getLog().info( "..SKIPPED due to " + message.toString() );
                }

                // Abuse failureMessage, the field in the report which should contain the reason for skipping
                // Consider skipCode + I18N
                buildJob.setFailureMessage( "Skipped due to " + message.toString() );
            }
        }
        catch ( RunErrorException e )
        {
            buildJob.setResult( BuildJob.Result.ERROR );
            buildJob.setFailureMessage( e.getMessage() );

            if ( !suppressSummaries )
            {
                getLog().info( "..ERROR " + formatTime( buildJob.getTime() ) );
                getLog().info( "  " + e.getMessage() );
            }
        }
        catch ( RunFailureException e )
        {
            buildJob.setResult( e.getType() );
            buildJob.setFailureMessage( e.getMessage() );

            if ( !suppressSummaries )
            {
                getLog().info( "..FAILED " + formatTime( buildJob.getTime() ) );
                getLog().info( "  " + e.getMessage() );
            }
        }
        finally
        {
            if ( interpolatedPomFile != null && StringUtils.isNotEmpty( filteredPomPrefix ) )
            {
                interpolatedPomFile.delete();
            }
            writeBuildReport( buildJob );
        }
    }

    /**
     * Determines whether selector conditions of the specified invoker properties match the current environment.
     *
     * @param invokerProperties The invoker properties to check, must not be <code>null</code>.
     * @return <code>0</code> if the job corresponding to the properties should be run, otherwise a bitwise value
     *         representing the reason why it should be skipped.
     */
    private int getSelection( InvokerProperties invokerProperties, CharSequence actualJreVersion )
    {
        int selection = 0;
        if ( !SelectorUtils.isMavenVersion( invokerProperties.getMavenVersion(), actualMavenVersion ) )
        {
            selection |= SELECTOR_MAVENVERSION;
        }

        if ( !SelectorUtils.isJreVersion( invokerProperties.getJreVersion(), actualJreVersion.toString() ) )
        {
            selection |= SELECTOR_JREVERSION;
        }

        if ( !SelectorUtils.isOsFamily( invokerProperties.getOsFamily() ) )
        {
            selection |= SELECTOR_OSFAMILY;
        }

        return selection;
    }

    /**
     * Writes the XML report for the specified build job unless report generation has been disabled.
     *
     * @param buildJob The build job whose report should be written, must not be <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the report could not be written.
     */
    private void writeBuildReport( BuildJob buildJob )
        throws MojoExecutionException
    {
        if ( disableReports )
        {
            return;
        }

        String safeFileName = buildJob.getProject().replace( '/', '_' ).replace( '\\', '_' ).replace( ' ', '_' );
        if ( safeFileName.endsWith( "_pom.xml" ) )
        {
            safeFileName = safeFileName.substring( 0, safeFileName.length() - "_pom.xml".length() );
        }

        File reportFile = new File( reportsDirectory, "BUILD-" + safeFileName + ".xml" );
        try
        {
            FileOutputStream fos = new FileOutputStream( reportFile );
            try
            {
                Writer osw = new OutputStreamWriter( fos, buildJob.getModelEncoding() );
                BuildJobXpp3Writer writer = new BuildJobXpp3Writer();
                writer.write( osw, buildJob );
                osw.close();
            }
            finally
            {
                fos.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write build report " + reportFile, e );
        }
    }

    /**
     * Formats the specified build duration time.
     *
     * @param seconds The duration of the build.
     * @return The formatted time, never <code>null</code>.
     */
    private String formatTime( double seconds )
    {
        return secFormat.format( seconds );
    }

    /**
     * Runs the specified project.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param pomFile The (already interpolated) POM file, may be <code>null</code> for a POM-less Maven invocation.
     * @param settingsFile The (already interpolated) user settings file for the build, may be <code>null</code>. Will
     *            be merged with the settings file of the invoking Maven process.
     * @param invokerProperties The properties to use.
     * @return <code>true</code> if the project was launched or <code>false</code> if the selector script indicated that
     *         the project should be skipped.
     * @throws org.apache.maven.plugin.MojoExecutionException If the project could not be launched.
     * @throws org.apache.maven.shared.scriptinterpreter.RunFailureException If either a hook script or the build itself
     *             failed.
     */
    private boolean runBuild( File basedir, File pomFile, File settingsFile, File actualJavaHome,
                              InvokerProperties invokerProperties )
        throws MojoExecutionException, RunFailureException
    {
        if ( getLog().isDebugEnabled() && !invokerProperties.getProperties().isEmpty() )
        {
            Properties props = invokerProperties.getProperties();
            getLog().debug( "Using invoker properties:" );
            for ( String key : new TreeSet<String>( (Set) props.keySet() ) )
            {
                String value = props.getProperty( key );
                getLog().debug( "  " + key + " = " + value );
            }
        }

        List<String> goals = getGoals( basedir );

        List<String> profiles = getProfiles( basedir );

        Map<String, Object> context = new LinkedHashMap<String, Object>();

        FileLogger logger = setupLogger( basedir );
        try
        {
            try
            {
                scriptRunner.run( "selector script", basedir, selectorScript, context, logger, BuildJob.Result.SKIPPED,
                                  false );
            }
            catch ( RunErrorException e )
            {
                throw e;
            }
            catch ( RunFailureException e )
            {
                return false;
            }

            scriptRunner.run( "pre-build script", basedir, preBuildHookScript, context, logger,
                              BuildJob.Result.FAILURE_PRE_HOOK, false );

            final InvocationRequest request = new DefaultInvocationRequest();

            request.setLocalRepositoryDirectory( localRepositoryPath );

            request.setInteractive( false );

            request.setShowErrors( showErrors );

            request.setDebug( debug );

            request.setShowVersion( showVersion );

            if ( logger != null )
            {
                request.setErrorHandler( logger );

                request.setOutputHandler( logger );
            }

            if ( mavenHome != null )
            {
                invoker.setMavenHome( mavenHome );
                request.addShellEnvironment( "M2_HOME", mavenHome.getAbsolutePath() );
            }

            if ( mavenExecutable != null )
            {
                invoker.setMavenExecutable( new File( mavenExecutable ) );
            }

            if ( actualJavaHome != null )
            {
                request.setJavaHome( actualJavaHome );
            }

            if ( environmentVariables != null )
            {
                for ( Map.Entry<String, String> variable : environmentVariables.entrySet() )
                {
                    request.addShellEnvironment( variable.getKey(), variable.getValue() );
                }
            }

            for ( int invocationIndex = 1;; invocationIndex++ )
            {
                if ( invocationIndex > 1 && !invokerProperties.isInvocationDefined( invocationIndex ) )
                {
                    break;
                }

                request.setBaseDirectory( basedir );

                request.setPomFile( pomFile );

                request.setGoals( goals );

                request.setProfiles( profiles );

                request.setMavenOpts( mavenOpts );

                request.setOffline( false );

                request.setUserSettingsFile( settingsFile );

                Properties systemProperties =
                    getSystemProperties( basedir, invokerProperties.getSystemPropertiesFile( invocationIndex ) );
                request.setProperties( systemProperties );

                invokerProperties.configureInvocation( request, invocationIndex );

                if ( getLog().isDebugEnabled() )
                {
                    try
                    {
                        getLog().debug( "Using MAVEN_OPTS: " + request.getMavenOpts() );
                        getLog().debug( "Executing: " + new MavenCommandLineBuilder().build( request ) );
                    }
                    catch ( CommandLineConfigurationException e )
                    {
                        getLog().debug( "Failed to display command line: " + e.getMessage() );
                    }
                }

                InvocationResult result;

                try
                {
                    result = invoker.execute( request );
                }
                catch ( final MavenInvocationException e )
                {
                    getLog().debug( "Error invoking Maven: " + e.getMessage(), e );
                    throw new RunFailureException( "Maven invocation failed. " + e.getMessage(),
                                                   BuildJob.Result.FAILURE_BUILD );
                }

                verify( result, invocationIndex, invokerProperties, logger );
            }

            scriptRunner.run( "post-build script", basedir, postBuildHookScript, context, logger,
                              BuildJob.Result.FAILURE_POST_HOOK, true );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            if ( logger != null )
            {
                logger.close();
            }
        }
        return true;
    }

    /**
     * Initializes the build logger for the specified project.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The build logger or <code>null</code> if logging has been disabled.
     * @throws org.apache.maven.plugin.MojoExecutionException If the log file could not be created.
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

                getLog().debug( "Build log initialized in: " + outputLog );
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
     * @param filename The filename to the properties file to load, may be <code>null</code> to use the default path
     *            given by {@link #testPropertiesFile}.
     * @return The system properties to use, may be empty but never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the properties file exists but could not be read.
     */
    private Properties getSystemProperties( final File basedir, final String filename )
        throws MojoExecutionException
    {
        Properties collectedTestProperties = new Properties();

        if ( testProperties != null )
        {
            collectedTestProperties.putAll( testProperties );
        }

        if ( properties != null )
        {
            // MINVOKER-118: property can have empty value, which is not accepted by collectedTestProperties
            for ( Map.Entry<String, String> entry : properties.entrySet() )
            {
                if ( entry.getValue() != null )
                {
                    collectedTestProperties.put( entry.getKey(), entry.getValue() );
                }
            }
        }

        File propertiesFile = null;
        if ( filename != null )
        {
            propertiesFile = new File( basedir, filename );
        }
        else if ( testPropertiesFile != null )
        {
            propertiesFile = new File( basedir, testPropertiesFile );
        }

        if ( propertiesFile != null && propertiesFile.isFile() )
        {
            InputStream fin = null;
            try
            {
                fin = new FileInputStream( propertiesFile );

                Properties loadedProperties = new Properties();
                loadedProperties.load( fin );
                collectedTestProperties.putAll( loadedProperties );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error reading system properties from " + propertiesFile );
            }
            finally
            {
                IOUtil.close( fin );
            }
        }

        return collectedTestProperties;
    }

    /**
     * Verifies the invocation result.
     *
     * @param result The invocation result to check, must not be <code>null</code>.
     * @param invocationIndex The index of the invocation for which to check the exit code, must not be negative.
     * @param invokerProperties The invoker properties used to check the exit code, must not be <code>null</code>.
     * @param logger The build logger, may be <code>null</code> if logging is disabled.
     * @throws org.apache.maven.shared.scriptinterpreter.RunFailureException If the invocation result indicates a build
     *             failure.
     */
    private void verify( InvocationResult result, int invocationIndex, InvokerProperties invokerProperties,
                         FileLogger logger )
        throws RunFailureException
    {
        if ( result.getExecutionException() != null )
        {
            throw new RunFailureException( "The Maven invocation failed. "
                + result.getExecutionException().getMessage(), BuildJob.Result.ERROR );
        }
        else if ( !invokerProperties.isExpectedResult( result.getExitCode(), invocationIndex ) )
        {
            StringBuilder buffer = new StringBuilder( 256 );
            buffer.append( "The build exited with code " ).append( result.getExitCode() ).append( ". " );
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
            throw new RunFailureException( buffer.toString(), BuildJob.Result.FAILURE_BUILD );
        }
    }

    /**
     * Gets the goal list for the specified project.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The list of goals to run when building the project, may be empty but never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the profile file could not be read.
     */
    List<String> getGoals( final File basedir )
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
     * @throws org.apache.maven.plugin.MojoExecutionException If the profile file could not be read.
     */
    List<String> getProfiles( File basedir )
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

    /**
     * Gets the build jobs that should be processed. Note that the order of the returned build jobs is significant.
     *
     * @return The build jobs to process, may be empty but never <code>null</code>.
     * @throws java.io.IOException If the projects directory could not be scanned.
     */
    BuildJob[] getBuildJobs()
        throws IOException
    {
        BuildJob[] buildJobs;

        if ( ( pom != null ) && pom.exists() )
        {
            buildJobs = new BuildJob[] { new BuildJob( pom.getAbsolutePath(), BuildJob.Type.NORMAL ) };
        }
        else if ( invokerTest != null )
        {
            String[] testRegexes = StringUtils.split( invokerTest, "," );
            List<String> includes = new ArrayList<String>( testRegexes.length );
            List<String> excludes = new ArrayList<String>();

            for ( String regex : testRegexes )
            {
                // user just use -Dinvoker.test=MWAR191,MNG111 to use a directory thats the end is not pom.xml
                if ( regex.startsWith( "!" ) )
                {
                    excludes.add( regex.substring( 1 ) );
                }
                else
                {
                    includes.add( regex );
                }
            }

            // it would be nice if we could figure out what types these are... but perhaps
            // not necessary for the -Dinvoker.test=xxx t
            buildJobs = scanProjectsDirectory( includes, excludes, BuildJob.Type.DIRECT );
        }
        else
        {
            List<String> excludes =
                ( pomExcludes != null ) ? new ArrayList<String>( pomExcludes ) : new ArrayList<String>();
            if ( this.settingsFile != null )
            {
                String exclude = relativizePath( this.settingsFile, projectsDirectory.getCanonicalPath() );
                if ( exclude != null )
                {
                    excludes.add( exclude.replace( '\\', '/' ) );
                    getLog().debug( "Automatically excluded " + exclude + " from project scanning" );
                }
            }

            BuildJob[] setupPoms = scanProjectsDirectory( setupIncludes, excludes, BuildJob.Type.SETUP );
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Setup projects: " + Arrays.asList( setupPoms ) );
            }

            BuildJob[] normalPoms = scanProjectsDirectory( pomIncludes, excludes, BuildJob.Type.NORMAL );

            Map<String, BuildJob> uniquePoms = new LinkedHashMap<String, BuildJob>();
            for ( BuildJob setupPom : setupPoms )
            {
                uniquePoms.put( setupPom.getProject(), setupPom );
            }
            for ( BuildJob normalPom : normalPoms )
            {
                if ( !uniquePoms.containsKey( normalPom.getProject() ) )
                {
                    uniquePoms.put( normalPom.getProject(), normalPom );
                }
            }

            buildJobs = uniquePoms.values().toArray( new BuildJob[uniquePoms.size()] );
        }

        relativizeProjectPaths( buildJobs );

        return buildJobs;
    }

    /**
     * Scans the projects directory for projects to build. Both (POM) files and mere directories will be matched by the
     * scanner patterns. If the patterns match a directory which contains a file named "pom.xml", the results will
     * include the path to this file rather than the directory path in order to avoid duplicate invocations of the same
     * project.
     *
     * @param includes The include patterns for the scanner, may be <code>null</code>.
     * @param excludes The exclude patterns for the scanner, may be <code>null</code> to exclude nothing.
     * @param type The type to assign to the resulting build jobs, must not be <code>null</code>.
     * @return The build jobs matching the patterns, never <code>null</code>.
     * @throws java.io.IOException If the project directory could not be scanned.
     */
    private BuildJob[] scanProjectsDirectory( List<String> includes, List<String> excludes, String type )
        throws IOException
    {
        if ( !projectsDirectory.isDirectory() )
        {
            return new BuildJob[0];
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( projectsDirectory.getCanonicalFile() );
        scanner.setFollowSymlinks( false );
        if ( includes != null )
        {
            scanner.setIncludes( includes.toArray( new String[includes.size()] ) );
        }
        if ( excludes != null )
        {
            scanner.setExcludes( excludes.toArray( new String[excludes.size()] ) );
        }
        scanner.addDefaultExcludes();
        scanner.scan();

        Map<String, BuildJob> matches = new LinkedHashMap<String, BuildJob>();

        for ( String includedFile : scanner.getIncludedFiles() )
        {
            matches.put( includedFile, new BuildJob( includedFile, type ) );
        }

        for ( String includedDir : scanner.getIncludedDirectories() )
        {
            String includedFile = includedDir + File.separatorChar + "pom.xml";
            if ( new File( scanner.getBasedir(), includedFile ).isFile() )
            {
                matches.put( includedFile, new BuildJob( includedFile, type ) );
            }
            else
            {
                matches.put( includedDir, new BuildJob( includedDir, type ) );
            }
        }

        return matches.values().toArray( new BuildJob[matches.size()] );
    }

    /**
     * Relativizes the project paths of the specified build jobs against the directory specified by
     * {@link #projectsDirectory} (if possible). If a project path does not denote a sub path of the projects directory,
     * it is returned as is.
     *
     * @param buildJobs The build jobs whose project paths should be relativized, must not be <code>null</code> nor
     *            contain <code>null</code> elements.
     * @throws java.io.IOException If any path could not be relativized.
     */
    private void relativizeProjectPaths( BuildJob[] buildJobs )
        throws IOException
    {
        String projectsDirPath = projectsDirectory.getCanonicalPath();

        for ( BuildJob buildJob : buildJobs )
        {
            String projectPath = buildJob.getProject();

            File file = new File( projectPath );

            if ( !file.isAbsolute() )
            {
                file = new File( projectsDirectory, projectPath );
            }

            String relativizedPath = relativizePath( file, projectsDirPath );

            if ( relativizedPath == null )
            {
                relativizedPath = projectPath;
            }

            buildJob.setProject( relativizedPath );
        }
    }

    /**
     * Relativizes the specified path against the given base directory. Besides relativization, the returned path will
     * also be normalized, e.g. directory references like ".." will be removed.
     *
     * @param path The path to relativize, must not be <code>null</code>.
     * @param basedir The (canonical path of the) base directory to relativize against, must not be <code>null</code>.
     * @return The relative path in normal form or <code>null</code> if the input path does not denote a sub path of the
     *         base directory.
     * @throws java.io.IOException If the path could not be relativized.
     */
    private String relativizePath( File path, String basedir )
        throws IOException
    {
        String relativizedPath = path.getCanonicalPath();

        if ( relativizedPath.startsWith( basedir ) )
        {
            relativizedPath = relativizedPath.substring( basedir.length() );
            if ( relativizedPath.startsWith( File.separator ) )
            {
                relativizedPath = relativizedPath.substring( File.separator.length() );
            }

            return relativizedPath;
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the map-based value source used to interpolate POMs and other stuff.
     *
     * @return The map-based value source for interpolation, never <code>null</code>.
     */
    private Map<String, Object> getInterpolationValueSource()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        if ( interpolationsProperties != null )
        {
            props.putAll( (Map) interpolationsProperties );
        }
        if ( filterProperties != null )
        {
            props.putAll( filterProperties );
        }
        props.put( "basedir", this.project.getBasedir().getAbsolutePath() );
        props.put( "baseurl", toUrl( this.project.getBasedir().getAbsolutePath() ) );
        if ( settings.getLocalRepository() != null )
        {
            props.put( "localRepository", settings.getLocalRepository() );
            props.put( "localRepositoryUrl", toUrl( settings.getLocalRepository() ) );
        }
        return new CompositeMap( this.project, props );
    }

    /**
     * Converts the specified filesystem path to a URL. The resulting URL has no trailing slash regardless whether the
     * path denotes a file or a directory.
     *
     * @param filename The filesystem path to convert, must not be <code>null</code>.
     * @return The <code>file:</code> URL for the specified path, never <code>null</code>.
     */
    private static String toUrl( String filename )
    {
        /*
         * NOTE: Maven fails to properly handle percent-encoded "file:" URLs (WAGON-111) so don't use File.toURI() here
         * as-is but use the decoded path component in the URL.
         */
        String url = "file://" + new File( filename ).toURI().getPath();
        if ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.length() - 1 );
        }
        return url;
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
     * @throws java.io.IOException If the token file exists but could not be parsed.
     */
    private List<String> getTokens( File basedir, String filename, List<String> defaultTokens )
        throws IOException
    {
        List<String> tokens = ( defaultTokens != null ) ? defaultTokens : new ArrayList<String>();

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
     * @throws java.io.IOException If the token file could not be read.
     */
    private List<String> readTokens( final File tokenFile )
        throws IOException
    {
        List<String> result = new ArrayList<String>();

        BufferedReader reader = null;
        try
        {
            Map<String, Object> composite = getInterpolationValueSource();
            reader = new BufferedReader( new InterpolationFilterReader( newReader( tokenFile ), composite ) );

            String line;
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
    private List<String> collectListFromCSV( final String csv )
    {
        final List<String> result = new ArrayList<String>();

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
     * Interpolates the specified POM/settings file to a temporary file. The destination file may be same as the input
     * file, i.e. interpolation can be performed in-place.
     *
     * @param originalFile The XML file to interpolate, must not be <code>null</code>.
     * @param interpolatedFile The target file to write the interpolated contents of the original file to, must not be
     *            <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the target file could not be created.
     */
    void buildInterpolatedFile( File originalFile, File interpolatedFile )
        throws MojoExecutionException
    {
        getLog().debug( "Interpolate " + originalFile.getPath() + " to " + interpolatedFile.getPath() );

        try
        {
            String xml;

            Reader reader = null;
            try
            {
                // interpolation with token @...@
                Map<String, Object> composite = getInterpolationValueSource();
                reader = ReaderFactory.newXmlReader( originalFile );
                reader = new InterpolationFilterReader( reader, composite, "@", "@" );
                xml = IOUtil.toString( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            Writer writer = null;
            try
            {
                interpolatedFile.getParentFile().mkdirs();
                writer = WriterFactory.newXmlWriter( interpolatedFile );
                writer.write( xml );
                writer.flush();
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to interpolate file " + originalFile.getPath(), e );
        }
    }

    /**
     * Gets the (interpolated) invoker properties for an integration test.
     *
     * @param projectDirectory The base directory of the IT project, must not be <code>null</code>.
     * @return The invoker properties, may be empty but never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If an I/O error occurred during reading the properties.
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

            Interpolator interpolator = new RegexBasedInterpolator();
            interpolator.addValueSource( new MapBasedValueSource( getInterpolationValueSource() ) );
            // CHECKSTYLE_OFF: LineLength
            for ( String key : (Set<String>) ( (Map) props ).keySet() )
            {
                String value = props.getProperty( key );
                try
                {
                    value = interpolator.interpolate( value, "" );
                }
                catch ( InterpolationException e )
                {
                    throw new MojoExecutionException( "Failed to interpolate invoker properties: " + propertiesFile, e );
                }
                props.setProperty( key, value );
            }
            // CHECKSTYLE_ON: LineLength
        }
        return new InvokerProperties( props );
    }

    protected boolean isParallelRun()
    {
        return parallelThreads > 1;
    }

}
