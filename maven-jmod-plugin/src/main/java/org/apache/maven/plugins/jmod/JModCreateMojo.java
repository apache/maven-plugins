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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult.ModuleNameSource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * The <code>create</code> goal is intended to create <code>jmod</code> files which can be used for later linking via
 * <a href="https://maven.apache.org/plugins/maven-jlink-plugin/">maven-jlink-plugin</a>. The <code>jmod</code> files
 * can not be used as usual dependencies on the classpath only in relationship with <code>maven-jlink-plugin</code>.
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "create", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true )
// CHECKSTYLE_ON: LineLength
public class JModCreateMojo
    extends AbstractJModMojo
{
    private static final String JMODS = "jmods";

    private List<String> classpathElements;

    private List<String> modulepathElements;

    private Map<String, JavaModuleDescriptor> pathElements;

    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true, required = true )
    private List<String> compilePath;

    @Component
    private LocationManager locationManager;

    /**
     * <code>--class-path &lt;path&gt;</code> Application jar files/directory containing classes. Specifies a class path
     * whose content will be copied into the resulting <code>jmod</code> file.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private List<String> classPath;

    /**
     * Specifies one or more directories containing native commands to be copied. The given directories are relative to
     * the current base directory. If no entry is defined the default is <code>src/main/cmds</code> used.
     * 
     * <pre>
     * &lt;cmds&gt;
     *   &lt;cmd&gt;...&lt;/cmd&gt;
     *   &lt;cmd&gt;...&lt;/cmd&gt;
     *   .
     *   .
     * &lt;/cmds&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>bin</code> within the jmod
     * file.
     * </p>
     * <code>JMod</code> command line equivalent: <code>--cmds &lt;path&gt;</code>.
     */
    @Parameter
    private List<String> cmds;

    private static final String DEFAULT_CMD_DIRECTORY = "src/main/cmds";

    /**
     * Specifies one or more directories containing configuration files to be copied. Location of user-editable config
     * files. If no configuration is given the <code>src/main/configs</code> location is used as default. If this
     * directory does not exist the whole will be ignored.
     * 
     * <pre>
     * &lt;configs&gt;
     *   &lt;config&gt;...&lt;/config&gt;
     *   &lt;config&gt;...&lt;/config&gt;
     *   .
     *   .
     * &lt;/configs&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>config</code> within the jmod
     * file.
     * </p>
     * jmod command line equivalent: <code>--config &lt;path&gt;</code>.
     */
    @Parameter
    private List<String> configs;

    private static final String DEFAULT_CONFIG_DIRECTORY = "src/main/configs";

    /**
     * Exclude files matching the pattern list. Each element using one the following forms: &lt;glob-pattern&gt;,
     * glob:&lt;glob-pattern&gt; or regex:&lt;regex-pattern&gt;
     * 
     * <pre>
     * &lt;excludes&gt;
     *   &lt;exclude&gt;...&lt;/exclude&gt;
     *   &lt;exclude&gt;...&lt;/exclude&gt;
     *   .
     *   .
     * &lt;/excludes&gt;
     * </pre>
     */
    @Parameter
    private List<String> excludes;

    /**
     * Define the main class which is recorded in the <code>module-info.class</code> file.
     */
    @Parameter
    private String mainClass;

    /**
     * Specifies one or more directories containing native libraries to be copied (The given directories are relative to
     * project base directory). If no configuration is given in <<pom.xml>> file the location <code>src/main/libs</code>
     * will be used. If the default location does not exist the whole configuration will be ignored.
     * 
     * <pre>
     * &lt;libs&gt;
     *   &lt;lib&gt;...&lt;/lib&gt;
     *   &lt;lib&gt;...&lt;/lib&gt;
     *   .
     *   .
     * &lt;/libs&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>lib</code> within the jmod
     * file.
     * </p>
     */
    @Parameter
    private List<String> libs;

    private static final String DEFAULT_LIB_DIRECTORY = "src/main/libs";

    /**
     * Define the module version of the jmod file.
     */
    @Parameter( defaultValue = "${project.version}" )
    private String moduleVersion;

    /**
     * <code>--do-not-resolve-by-default</code> Exclude from the default root set of modules
     */
    @Parameter( defaultValue = "false" )
    private boolean doNotResolveByDefault;

    /**
     * Define the locations of header files. The default location is <code>src/main/headerfiles</code>. If the the
     * default location does not exist in the current project it will be ignored. The given directories are relative to
     * the project base directory. If an entry is defined the definition of all locations is needed.
     * 
     * <pre>
     * &lt;headerFiles&gt;
     *   &lt;headerFile&gt;...&lt;/headerFile&gt;
     *   &lt;headerFile&gt;...&lt;/headerFile&gt;
     *   .
     *   .
     * &lt;/headerFiles&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>includes</code> within the
     * jmod file.
     * </p>
     * jmod command line equivalent <code>--header-files &lt;path&gt;</code>
     */
    @Parameter
    private List<String> headerFiles;

    private static final String DEFAULT_HEADER_FILES_DIRECTORY = "src/main/headerfiles";

    /**
     * Define the locations of man pages. The default location is <code>src/main/manpages</code>. The given man pages
     * locations are relative to the project base directory.
     * 
     * <pre>
     * &lt;manPages&gt;
     *   &lt;manPage&gt;...&lt;/manPage&gt;
     *   &lt;manPage&gt;...&lt;/manPage&gt;
     *   .
     *   .
     * &lt;/manPages&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>man</code> within the jmod
     * file.
     * </p>
     * jmod command line equivalent <code>--man-pages &lt;path&gt;</code>
     */
    @Parameter
    private List<String> manPages;

    private static final String DEFAULT_MAN_PAGES_DIRECTORY = "src/main/manpages";

    /**
     * This is only the name of the jmod file in the target directory.
     */
    @Parameter( defaultValue = "${project.artifactId}", required = true, readonly = true )
    private String moduleName;

    /**
     * Define the location of legal notices. The default location is <code>src/main/legalnotices</code>. The given man
     * pages locations are relative to the project base directory.
     * 
     * <pre>
     * &lt;legalNotices&gt;
     *   &lt;legalNotice&gt;...&lt;/legalNotice&gt;
     *   &lt;legalNotice&gt;...&lt;/legalNotice&gt;
     *   .
     *   .
     * &lt;/legalNotices&gt;
     * </pre>
     * <p>
     * All files from those directories will be copied into the resulting directory <code>legal</code> within the jmod
     * file.
     * </p>
     * jmod command line equivalent <code>--legal-notices &lt;path&gt;</code>
     */
    @Parameter
    private List<String> legalNotices;

    private static final String DEFAULT_LEGAL_NOTICES_DIRECTORY = "src/main/legalnotices";

    /**
     * <code>--target-platform &lt;target-platform&gt;</code> Target platform TODO: Which values are valid?
     */
    @Parameter
    private String targetPlatform;

    /**
     * Hint for a tool to issue a warning if the module is resolved. The valid values are:
     * <ul>
     * <li>deprecated</li>
     * <li>deprecated-for-removal</li>
     * <li>incubating</li>
     * </ul>
     */
    @Parameter
    private String warnIfResolved;

    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    private File targetClassesDirectory;

    @Parameter( defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File outputDirectory;

    private List<String> modulePaths;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        String jModExecutable;
        try
        {
            jModExecutable = getJModExecutable();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Unable to find jmod command: " + e.getMessage(), e );
        }

        File jModExecuteableFile = new File( jModExecutable );
        File jModExecutableParent = jModExecuteableFile.getParentFile().getParentFile();
        File jmodsFolderJDK = new File( jModExecutableParent, JMODS );
        getLog().debug( "Parent: " + jModExecutableParent.getAbsolutePath() );
        getLog().debug( "jmodsFolder: " + jmodsFolderJDK.getAbsolutePath() );

        /*
         * this.moduleName = extract module name from module-info.java if not defined extract the package name as
         * default
         */
        preparePaths();

        failIfParametersAreNotInTheirValidValueRanges();

        getLog().info( "Toolchain in maven-jmod-plugin: jmod [ " + jModExecutable + " ]" );

        // We need to put the resulting x.jmod files into jmods folder otherwise is
        // seemed to be not working.
        // Check why?
        File modsFolder = new File( outputDirectory, "jmods" );
        File resultingJModFile = new File( modsFolder, moduleName + ".jmod" );

        deleteOutputIfAlreadyExists( resultingJModFile );

        // create the jmods folder...
        modsFolder.mkdirs();

        this.modulePaths = new ArrayList<>();
        for ( Entry<String, JavaModuleDescriptor> item : pathElements.entrySet() )
        {
            // Isn't there a better solution?
            if ( item.getValue() == null )
            {
                String message = "The given dependency " + item.getKey()
                    + " does not have a module-info.java file. So it can't be linked.";
                getLog().error( message );
                throw new MojoFailureException( message );
            }
            getLog().debug( "pathElements Item:" + item.getKey() + " v:" + item.getValue().name() );
            getLog().info( " -> module: " + item.getValue().name() + " ( " + item.getKey() + " )" );
            // We use the real module name and not the artifact Id...
            this.modulePaths.add( item.getKey() );
        }
        // The jmods directory of the JDK
        this.modulePaths.add( jmodsFolderJDK.getAbsolutePath() );

        Commandline cmd;
        try
        {
            cmd = createJModCreateCommandLine( resultingJModFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        cmd.setExecutable( jModExecutable );

        executeCommand( cmd, outputDirectory );

        if ( projectHasAlreadySetAnArtifact() )
        {
            throw new MojoExecutionException( "You have to use a classifier "
                + "to attach supplemental artifacts to the project instead of replacing them." );
        }

        getProject().getArtifact().setFile( resultingJModFile );
    }

    private void deleteOutputIfAlreadyExists( File resultingJModFile )
        throws MojoFailureException
    {
        if ( resultingJModFile.exists() && resultingJModFile.isFile() )
        {
            try
            {
                getLog().debug( "Deleting the existing " + resultingJModFile.getAbsolutePath() + " file." );
                FileUtils.forceDelete( resultingJModFile );
            }
            catch ( IOException e )
            {
                String message = "Failure during deleting of file " + resultingJModFile.getAbsolutePath();
                getLog().error( message );
                throw new MojoFailureException( message );
            }
        }
    }

    private void failIfParametersAreNotInTheirValidValueRanges()
        throws MojoFailureException
    {
        // if ( moduleName == null || ( moduleName != null && moduleName.trim().isEmpty() ) )
        // {
        // String message = "A moduleName must be given.";
        // getLog().error( message );
        // throw new MojoFailureException( message );
        // }

        if ( warnIfResolved != null )
        {
            String x = warnIfResolved.toLowerCase().trim();
            if ( !"deprecated".equals( x ) && "deprecated-for-removal".equals( x ) && "incubating".equals( x ) )
            {
                String message = "The parameter warnIfResolved does not contain a valid value. "
                    + "Valid values are 'deprecated', 'deprecated-for-removal' or 'incubating'.";
                getLog().error( message );
                throw new MojoFailureException( message );

            }
        }

        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( cmds, DEFAULT_CMD_DIRECTORY ),
                                                 "cmd" );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( configs,
                                                                                     DEFAULT_CONFIG_DIRECTORY ),
                                                 "config" );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( libs, DEFAULT_LIB_DIRECTORY ),
                                                 "lib" );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( headerFiles,
                                                                                     DEFAULT_HEADER_FILES_DIRECTORY ),
                                                 "headerFile" );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( legalNotices,
                                                                                     DEFAULT_LEGAL_NOTICES_DIRECTORY ),
                                                 "legalNotice" );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault( manPages,
                                                                                     DEFAULT_MAN_PAGES_DIRECTORY ),
                                                 "manPage" );
    }

    private void throwExceptionIfNotExistOrNotADirectory( List<String> configurations, String partialMessage )
        throws MojoFailureException
    {
        for ( String configLocation : configurations )
        {
            File dir = new File( configLocation );
            if ( !dir.exists() || !dir.isDirectory() )
            {
                String message = "The directory " + configLocation + " for " + partialMessage
                    + " parameter does not exist " + "or is not a directory. ";
                getLog().error( message );
                throw new MojoFailureException( message );
            }
        }
    }

    private List<File> getCompileClasspathElements( MavenProject project )
    {
        List<File> list = new ArrayList<File>( project.getArtifacts().size() + 1 );

        list.add( new File( project.getBuild().getOutputDirectory() ) );

        for ( Artifact a : project.getArtifacts() )
        {
            list.add( a.getFile() );
        }
        return list;
    }

    private void preparePaths()
    {
        assert compilePath != null;

        boolean hasModuleDescriptor = false;
        // Assuming that the module-info.java is already compiled by compiler plugin so only
        // check if the module-info.class file exists.
        File moduleInfo = new File( targetClassesDirectory, "module-info.class" );

        if ( moduleInfo.exists() && moduleInfo.isFile() )
        {
            getLog().debug( "We have found a module-info.class file." );
            hasModuleDescriptor = true;
        }

        if ( hasModuleDescriptor )
        {
            // For now only allow named modules. Once we can create a graph with ASM we can specify exactly the modules
            // and we can detect if auto modules are used. In that case, MavenProject.setFile() should not be used, so
            // you cannot depend on this project and so it won't be distributed.

            modulepathElements = new ArrayList<String>();
            classpathElements = new ArrayList<String>();
            pathElements = new LinkedHashMap<String, JavaModuleDescriptor>();

            ResolvePathsResult<File> resolvePathsResult;
            try
            {
                Collection<File> dependencyArtifacts = getCompileClasspathElements( getProject() );

                ResolvePathsRequest<File> request = ResolvePathsRequest.withFiles( dependencyArtifacts );

                Toolchain toolchain = getToolchain();
                if ( toolchain != null && toolchain instanceof DefaultJavaToolChain )
                {
                    request.setJdkHome( new File( ( (DefaultJavaToolChain) toolchain ).getJavaHome() ) );
                }

                resolvePathsResult = locationManager.resolvePaths( request );

                JavaModuleDescriptor moduleDescriptor = resolvePathsResult.getMainModuleDescriptor();

                for ( Map.Entry<File, ModuleNameSource> entry : resolvePathsResult.getModulepathElements().entrySet() )
                {
                    if ( ModuleNameSource.FILENAME.equals( entry.getValue() ) )
                    {
                        final String message = "Required filename-based automodules detected. "
                            + "Please don't publish this project to a public artifact repository!";

                        if ( moduleDescriptor.exports().isEmpty() )
                        {
                            // application
                            getLog().info( message );
                        }
                        else
                        {
                            // library
                            writeBoxedWarning( message );
                        }
                        break;
                    }
                }

                for ( Map.Entry<File, JavaModuleDescriptor> entry : resolvePathsResult.getPathElements().entrySet() )
                {
                    pathElements.put( entry.getKey().getPath(), entry.getValue() );
                }

                for ( File file : resolvePathsResult.getClasspathElements() )
                {
                    classpathElements.add( file.getPath() );
                }

                for ( File file : resolvePathsResult.getModulepathElements().keySet() )
                {
                    modulepathElements.add( file.getPath() );
                }
            }
            catch ( IOException e )
            {
                getLog().warn( e.getMessage() );
            }

        }
        else
        {
            classpathElements = compilePath;
            modulepathElements = Collections.emptyList();
        }
    }

    private Commandline createJModCreateCommandLine( File resultingJModFile )
        throws IOException
    {
        File file = new File( outputDirectory, "jmodCreateArgs" );
        if ( !getLog().isDebugEnabled() )
        {
            file.deleteOnExit();
        }
        file.getParentFile().mkdirs();
        file.createNewFile();

        PrintStream argsFile = new PrintStream( file );

        argsFile.println( "create" );

        if ( moduleVersion != null )
        {
            argsFile.println( "--module-version" );
            argsFile.println( moduleVersion );
        }

        if ( classPath != null )
        {
            argsFile.println( "--class-path" );
            argsFile.println( getPlatformSeparatedList( classPath ) );
        }

        if ( excludes != null && !excludes.isEmpty() )
        {
            argsFile.println( "--exclude" );
            String commaSeparatedList = getCommaSeparatedList( excludes );
            argsFile.append( '"' ).append( commaSeparatedList.replace( "\\", "\\\\" ) ).println( '"' );
        }

        List<String> configList = handleConfigurationListWithDefault( configs, DEFAULT_CONFIG_DIRECTORY );
        if ( !configList.isEmpty() )
        {
            argsFile.println( "--config" );
            // Should we quote the paths?
            argsFile.println( getPlatformSeparatedList( configList ) );
        }

        List<String> cmdsList = handleConfigurationListWithDefault( cmds, DEFAULT_CMD_DIRECTORY );
        if ( !cmdsList.isEmpty() )
        {
            argsFile.println( "--cmds" );
            argsFile.println( getPlatformSeparatedList( cmdsList ) );
        }

        List<String> libsList = handleConfigurationListWithDefault( libs, DEFAULT_LIB_DIRECTORY );
        if ( !libsList.isEmpty() )
        {
            argsFile.println( "--libs" );
            argsFile.println( getPlatformSeparatedList( libsList ) );
        }

        List<String> headerFilesList =
            handleConfigurationListWithDefault( headerFiles, DEFAULT_HEADER_FILES_DIRECTORY );
        if ( !headerFilesList.isEmpty() )
        {
            argsFile.println( "--header-files" );
            argsFile.println( getPlatformSeparatedList( headerFilesList ) );
        }

        List<String> legalNoticesList =
            handleConfigurationListWithDefault( legalNotices, DEFAULT_LEGAL_NOTICES_DIRECTORY );
        if ( !legalNoticesList.isEmpty() )
        {
            argsFile.println( "--legal-notices" );
            argsFile.println( getPlatformSeparatedList( legalNoticesList ) );
        }

        List<String> manPagesList = handleConfigurationListWithDefault( manPages, DEFAULT_MAN_PAGES_DIRECTORY );
        if ( !manPagesList.isEmpty() )
        {
            argsFile.println( "--man-pages" );
            argsFile.println( getPlatformSeparatedList( manPagesList ) );
        }

        if ( modulePaths != null )
        {
            //@formatter:off
            argsFile.println( "--module-path" );
            argsFile
              .append( '"' )
              .append( getPlatformSeparatedList( modulePaths ).replace( "\\", "\\\\" ) ) 
              .println( '"' );
            //@formatter:off
        }

        if ( targetPlatform != null )
        {
            argsFile.println( "--target-platform" );
            argsFile.println( targetPlatform );
        }

        if ( warnIfResolved != null )
        {
            argsFile.println( "--warn-if-resolved" );
            argsFile.println( warnIfResolved );
        }

        if ( doNotResolveByDefault )
        {
            argsFile.println( "--do-not-resolve-by-default" );
        }

        argsFile.println( resultingJModFile.getAbsolutePath() );
        argsFile.close();

        Commandline cmd = new Commandline();
        cmd.createArg().setValue( '@' + file.getAbsolutePath() );

        return cmd;
    }

    private boolean isConfigurationDefinedInPOM( List<String> configuration )
    {
        return configuration != null && !configuration.isEmpty();
    }

    private List<String> handleConfigurationListWithDefault( List<String> configuration, String defaultLocation )
    {
        List<String> commands = new ArrayList<String>();
        if ( isConfigurationDefinedInPOM( configuration ) )
        {
            commands.addAll( configuration );
        }
        else
        {
            if ( doDefaultsExist( defaultLocation ) )
            {
                commands.add( defaultLocation );
            }
        }

        commands = resolveAgainstProjectBaseDir( commands );
        return commands;
    }

    private List<String> resolveAgainstProjectBaseDir( List<String> relativeDirectories )
    {
        List<String> result = new ArrayList<>();

        for ( String configLocation : relativeDirectories )
        {
            File dir = new File( getProject().getBasedir(), configLocation );
            result.add( dir.getAbsolutePath() );
        }
        return result;
    }

    private boolean doDefaultsExist( String defaultLocation )
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), defaultLocation );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private String getPlatformSeparatedList( List<String> paths )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : paths )
        {
            if ( sb.length() > 0 )
            {
                sb.append( File.pathSeparatorChar );
            }
            sb.append( module );
        }
        return sb.toString();
    }

    private void writeBoxedWarning( String message )
    {
        String line = StringUtils.repeat( "*", message.length() + 4 );
        getLog().warn( line );
        getLog().warn( "* " + MessageUtils.buffer().strong( message )  + " *" );
        getLog().warn( line );
    }

}
