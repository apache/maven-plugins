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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * The <code>create</code> goals is intended to create <code>jmod</code> files which can be used for later linking via
 * <code>maven-jlink-plugin</code>. The JMod files can not be used as usual dependencies on the classpath only in
 * relationship with maven-jlink-plugin. JMOD files can be used at compile time and link time, but not at run time.
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// TODO: Reconsider resolution scope.
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "create", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true )
// CHECKSTYLE_ON: LineLength
public class JModCreateMojo
    extends AbstractJModMojo
{

    /**
     * <code>--class-path &lt;path&gt;</code> Application jar files/directory containing classes. Specifies a class path
     * whose content will be copied into the resulting JMOD file.
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
     * 
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
     * 
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
     * Define the modulepath for the <code>jmod</code> call. <code>--module-path &lt;path&gt;</code>
     */
    // TODO: check if this target/classes folder?
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File modulePath;

    /**
     * <code>--do-not-resolve-by-default</code> Exclude from the default root set of modules
     */
    @Parameter( defaultValue = "false" )
    private boolean doNotResolveByDefault;

    /**
     * Define the locations of header files. The default location is <code>src/main/headerfiles</code>. Best is to put
     * the header files into the default location. If the directories do not exist the configuration will be ignored.
     * The given directories are relative to the project base directory.
     * 
     * <pre>
     * &lt;headerFiles&gt;
     *   &lt;headerFile&gt;...&lt;/headerFile&gt;
     *   &lt;headerFile&gt;...&lt;/headerFile&gt;
     *   .
     *   .
     * &lt;/headerFiles&gt;
     * </pre>
     * 
     * jmod command line equivalent <code>--header-files &lt;path&gt;</code> TODO: Define default location.
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
     * 
     * jmod command line equivalent <code>--man-pages &lt;path&gt;</code> TODO: Define default location.
     */
    @Parameter
    private List<String> manPages;

    private static final String DEFAULT_MAN_PAGES_DIRECTORY = "src/main/manpages";

    /**
     * The moduleName. The default is to use the <code>artifactId</code>.
     */
    @Parameter( defaultValue = "${project.artifactId}", required = true )
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
     * 
     * jmod command line equivalent <code>--legal-notices &lt;path&gt;</code> TODO: Define default location.
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

    /**
     * Do not change this. (TODO!)
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File outputDirectory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        failIfParametersAreNotInTheirValidValueRanges();

        String jModExecutable;
        try
        {
            jModExecutable = getJModExecutable();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Unable to find jmod command: " + e.getMessage(), e );
        }

        getLog().info( "Toolchain in maven-jmod-plugin: jmod [ " + jModExecutable + " ]" );

        // We need to put the resulting x.jmod files into jmods folder otherwise is
        // seemed to be not working.
        // Check why?
        File modsFolder = new File( outputDirectory, "jmods" );
        File resultingJModFile = new File( modsFolder, moduleName + ".jmod" );

        deleteOutputIfAlreadyExists( resultingJModFile );

        // create the jmods folder...
        modsFolder.mkdirs();

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
        if ( modulePath != null && !modulePath.isDirectory() )
        {
            String message = "The given module-path parameter " + modulePath.getAbsolutePath() + " is not a directory.";
            getLog().error( message );
            throw new MojoFailureException( message );
        }

        if ( moduleName != null && moduleName.trim().isEmpty() )
        {
            String message = "A moduleName must be given.";
            getLog().error( message );
            throw new MojoFailureException( message );
        }

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

        List<String> handleConfigurationListWithDefault =
            handleConfigurationListWithDefault( cmds, DEFAULT_CMD_DIRECTORY );
        throwExceptionIfNotExistOrNotADirectory( handleConfigurationListWithDefault, "cmd" );
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
            File dir = new File( getProject().getBasedir(), configLocation );
            if ( !dir.exists() || !dir.isDirectory() )
            {
                String message = "The directory " + configLocation + " for " + partialMessage
                    + " parameter does not exist " + "or is not a directory. ";
                getLog().error( message );
                throw new MojoFailureException( message );
            }
        }
    }

    private Commandline createJModCreateCommandLine( File resultingJModFile )
        throws IOException
    {
        File file = new File( outputDirectory, "jmodArgs" );
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
            StringBuilder sb = getPlatformSeparatedList( classPath );
            argsFile.println( sb.toString() );
        }

        if ( excludes != null && !excludes.isEmpty() )
        {
            argsFile.println( "--exclude" );
            String commaSeparatedList = getCommaSeparatedList( excludes );
            argsFile.append( '"' ).append( commaSeparatedList.replace( "\\", "\\\\" ) ).println( '"' );
        }

        List<String> configList = handleConfigs();
        if ( !configList.isEmpty() )
        {
            List<String> configAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : configList )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                configAbsoluteList.add( f.getCanonicalPath() );
            }
            argsFile.println( "--config" );
            StringBuilder sb = getPlatformSeparatedList( configAbsoluteList );
            // Should we quote the paths?
            argsFile.println( sb.toString() );
        }

        List<String> commands = handleCmds();
        if ( !commands.isEmpty() )
        {
            List<String> cmdsAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : commands )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                cmdsAbsoluteList.add( f.getCanonicalPath() );
            }

            argsFile.println( "--cmds" );
            StringBuilder sb = getPlatformSeparatedList( cmdsAbsoluteList );
            argsFile.println( sb.toString() );
        }

        List<String> localLibs = handleLibs();
        if ( !localLibs.isEmpty() )
        {
            List<String> libsAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : localLibs )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                libsAbsoluteList.add( f.getCanonicalPath() );
            }

            argsFile.println( "--libs" );
            StringBuilder sb = getPlatformSeparatedList( libsAbsoluteList );
            argsFile.println( sb.toString() );
        }

        List<String> localHeaderFiles = handleHeaderFiles();
        if ( !localHeaderFiles.isEmpty() )
        {
            List<String> headFilesAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : localHeaderFiles )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                headFilesAbsoluteList.add( f.getCanonicalPath() );
            }

            argsFile.println( "--header-files" );
            StringBuilder sb = getPlatformSeparatedList( headFilesAbsoluteList );
            argsFile.println( sb.toString() );
        }

        List<String> localLegalNotices = handleLegalNotices();
        if ( !localLegalNotices.isEmpty() )
        {
            List<String> legalNoticesAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : localLegalNotices )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                legalNoticesAbsoluteList.add( f.getCanonicalPath() );
            }

            argsFile.println( "--legal-notices" );
            StringBuilder sb = getPlatformSeparatedList( legalNoticesAbsoluteList );
            argsFile.println( sb.toString() );
        }

        List<String> localManPages = handleManPages();
        if ( !localManPages.isEmpty() )
        {
            List<String> manPagesAbsoluteList = new ArrayList<String>();
            for ( String realiveDirectory : localManPages )
            {
                File f = new File( getProject().getBasedir(), realiveDirectory );
                manPagesAbsoluteList.add( f.getCanonicalPath() );
            }

            argsFile.println( "--man-pages" );
            StringBuilder sb = getPlatformSeparatedList( manPagesAbsoluteList );
            argsFile.println( sb.toString() );
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

    private boolean havingConfigurationDefinedInPOM( List<String> configuration )
    {
        return configuration != null && !configuration.isEmpty();
    }

    private List<String> handleConfigurationListWithDefault( List<String> configuration, String defaultLocation )
    {
        List<String> commands = new ArrayList<String>();
        if ( havingConfigurationDefinedInPOM( configuration ) )
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
        return commands;
    }

    /**
     * Check if a configuration is given for cmds in pom file than take that. Otherwise check if the default location
     * exists if yes than take that otherwise the resulting list will be emtpy.
     * 
     * @return
     */
    private List<String> handleCmds()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingCmdsDefinedInPOM() )
        {
            commands.addAll( cmds );
        }
        else
        {
            if ( doCmdDefaultsExist() )
            {
                commands.add( DEFAULT_CMD_DIRECTORY );
            }
        }
        return commands;
    }

    private List<String> handleConfigs()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingConfigsDefinedInPOM() )
        {
            commands.addAll( configs );
        }
        else
        {
            if ( doConfigsDefaultsExist() )
            {
                commands.add( DEFAULT_CONFIG_DIRECTORY );
            }
        }
        return commands;
    }

    private List<String> handleLibs()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingLibsDefinedInPOM() )
        {
            commands.addAll( this.libs );
        }
        else
        {
            if ( doLibsDefaultsExist() )
            {
                commands.add( DEFAULT_LIB_DIRECTORY );
            }
        }
        return commands;
    }

    private List<String> handleHeaderFiles()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingHeaderFilesDefinedInPOM() )
        {
            commands.addAll( headerFiles );
        }
        else
        {
            if ( doLibsDefaultsExist() )
            {
                commands.add( DEFAULT_HEADER_FILES_DIRECTORY );
            }
        }
        return commands;
    }

    private List<String> handleLegalNotices()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingLegalNoticesDefinedInPOM() )
        {
            commands.addAll( legalNotices );
        }
        else
        {
            if ( doLegalNoticesDefaultsExist() )
            {
                commands.add( DEFAULT_LEGAL_NOTICES_DIRECTORY );
            }
        }
        return commands;
    }

    private List<String> handleManPages()
    {
        List<String> commands = new ArrayList<String>();
        if ( havingManPagesDefinedInPOM() )
        {
            commands.addAll( manPages );
        }
        else
        {
            if ( doManPagesDefaultsExist() )
            {
                commands.add( DEFAULT_MAN_PAGES_DIRECTORY );
            }
        }
        return commands;
    }

    private boolean havingCmdsDefinedInPOM()
    {
        return cmds != null && !cmds.isEmpty();
    }

    private boolean havingConfigsDefinedInPOM()
    {
        return configs != null && !configs.isEmpty();
    }

    private boolean havingLibsDefinedInPOM()
    {
        return libs != null && !libs.isEmpty();
    }

    private boolean havingHeaderFilesDefinedInPOM()
    {
        return headerFiles != null && !headerFiles.isEmpty();
    }

    private boolean havingLegalNoticesDefinedInPOM()
    {
        return legalNotices != null && !legalNotices.isEmpty();
    }

    private boolean havingManPagesDefinedInPOM()
    {
        return manPages != null && !manPages.isEmpty();
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

    private boolean doCmdDefaultsExist()
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), DEFAULT_CMD_DIRECTORY );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private boolean doConfigsDefaultsExist()
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), DEFAULT_CONFIG_DIRECTORY );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private boolean doLibsDefaultsExist()
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), DEFAULT_LIB_DIRECTORY );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private boolean doLegalNoticesDefaultsExist()
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), DEFAULT_LEGAL_NOTICES_DIRECTORY );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private boolean doManPagesDefaultsExist()
    {
        boolean result = false;
        File dir = new File( getProject().getBasedir(), DEFAULT_MAN_PAGES_DIRECTORY );
        if ( dir.exists() && dir.isDirectory() )
        {
            result = true;
        }
        return result;
    }

    private StringBuilder getPlatformSeparatedList( List<String> paths )
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
        return sb;
    }

}
