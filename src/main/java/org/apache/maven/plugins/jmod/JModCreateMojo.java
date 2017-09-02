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
     * Specifies one or more directories containing native commands to be copied. <code>JMod</code> command line
     * equivalent: <code>--cmds <path></code>.
     */
    @Parameter( defaultValue = "${project.basedir}/src/main/cmds" )
    private List<String> cmds;

    /**
     * Specifies one or more directories containing configuration files to be copied. <code>--config &lt;path&gt;</code>
     * Location of user-editable config files. TODO: Implement the handling. Should we use src/main/resources for this?
     * or better something different? What about filtering? I think a first approach is to use
     * <code>src/main/config</code>.
     * 
     * <pre>
     * &lt;configs&gt;
     *   &lt;config&gt;...&lt;/config&gt;
     *   &lt;config&gt;...&lt;/config&gt;
     *   .
     *   .
     * &lt;/configs&gt;
     * </pre>
     */
    @Parameter
    private List<String> configs;

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
     * Specifies one or more directories containing native libraries to be copied. Location of native libraries.
     * <code>--libs &lt;path&gt;</code>
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
    private File libs;

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

    @Parameter( defaultValue = "false" )
    private boolean doNotResolveByDefault;

    /**
     * Define the locations of header files.
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
     * jmod command line equivalent <code>--header-files &lt;path&gt;</code>
     */
    @Parameter
    private List<String> headerFiles;

    /**
     * Define the locations of man pages.
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
     * jmod command line equivalent <code>--man-pages &lt;path&gt;</code>
     */
    @Parameter
    private List<String> manPages;

    /**
     * The moduleName. The default is to use the <code>artifactId</code>.
     */
    @Parameter( defaultValue = "${project.artifactId}", required = true )
    private String moduleName;

    /**
     * Define the location of legal notices.
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
     * jmod command line equivalent <code>--legal-notices &lt;path&gt;</code>
     */
    @Parameter
    private List<String> legalNotices;

    /**
     * --target-platform <target-platform> Target platform
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

        if ( configs != null && !configs.isEmpty() )
        {
            argsFile.println( "--config" );
            StringBuilder sb = getPlatformSeparatedList( configs );
            // Should we quote the paths?
            argsFile.println( sb.toString() );
        }

        if ( excludes != null && !excludes.isEmpty() )
        {
            argsFile.println( "--exclude" );
            String commaSeparatedList = getCommaSeparatedList( excludes );
            argsFile.append( '"' ).append( commaSeparatedList.replace( "\\", "\\\\" ) ).println( '"' );
        }

        if ( cmds != null && !cmds.isEmpty() )
        {
            argsFile.println( "--cmds" );
            StringBuilder sb = getPlatformSeparatedList( cmds );
            argsFile.println( sb.toString() );
        }

        if ( headerFiles != null && !headerFiles.isEmpty() )
        {
            argsFile.println( "--header-files" );
            StringBuilder sb = getPlatformSeparatedList( headerFiles );
            argsFile.println( sb.toString() );
        }

        if ( legalNotices != null && !legalNotices.isEmpty() )
        {
            argsFile.println( "--legal-notices" );
            StringBuilder sb = getPlatformSeparatedList( legalNotices );
            argsFile.println( sb.toString() );
        }

        if ( manPages != null && !manPages.isEmpty() )
        {
            argsFile.println( "--man-pages" );
            StringBuilder sb = getPlatformSeparatedList( manPages );
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
