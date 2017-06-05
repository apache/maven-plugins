package org.apache.maven.plugins.jlink;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * <pre>
 * Usage: jlink &lt;options&gt; --module-path &lt;modulepath&gt; --add-modules &lt;module&gt;[,&lt;module&gt;...]
 * Possible options include:
 *       --add-modules &lt;mod&gt;[,&lt;mod&gt;...]    Root modules to resolve
 *       --bind-services                   Link in service provider modules and
 *                                         their dependences
 *   -c, --compress=&lt;0|1|2&gt;                Enable compression of resources:
 *                                           Level 0: No compression
 *                                           Level 1: Constant string sharing
 *                                           Level 2: ZIP
 *       --disable-plugin &lt;pluginname&gt;     Disable the plugin mentioned
 *       --endian &lt;little|big&gt;             Byte order of generated jimage
 *                                         (default:native)
 *   -h, --help                            Print this help message
 *       --ignore-signing-information      Suppress a fatal error when signed
 *                                         modular JARs are linked in the image.
 *                                         The signature related files of the
 *                                         signed modular JARs are not copied to
 *                                         the runtime image.
 *       --launcher &lt;name&gt;=&lt;module&gt;[/&lt;mainclass&gt;]
 *                                         Add a launcher command of the given
 *                                         name for the module and the main class
 *                                         if specified
 *       --limit-modules &lt;mod&gt;[,&lt;mod&gt;...]  Limit the universe of observable
 *                                         modules
 *       --list-plugins                    List available plugins
 *   -p, --module-path &lt;path&gt;              Module path
 *       --no-header-files                 Exclude include header files
 *       --no-man-pages                    Exclude man pages
 *       --output &lt;path&gt;                   Location of output path
 *       --save-opts &lt;filename&gt;            Save jlink options in the given file
 *   -G, --strip-debug                     Strip debug information
 *       --suggest-providers [&lt;name&gt;,...]  Suggest providers that implement the
 *                                         given service types from the module path
 *   -v, --verbose                         Enable verbose tracing
 *       --version                         Version information
 *       &#64;&lt;filename&gt;                       Read options from file
 * </pre>
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// TODO: Think if ResultionScope is needed here? May be we need to reconsider package phase?
// May be it would be wise to put into PREPARE-PACKAGE and the generation of the final jimage in the package phase?
// Furthermore It could make sense so we can change the conf files if needed...
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "jlink", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true )
// CHECKSTYLE_ON: LineLength
public class JLinkMojo
    extends AbstractJLinkMojo
{
    /**
     * <code>-G, --strip-debug</code> strip debug information.
     */
    @Parameter( defaultValue = "false" )
    private boolean stripDebug;

    /**
     * <code>-c, --compress=&lt;0|1|2&gt;</code> Enabled compression of resources.
     */
    @Parameter
    private Integer compression;

    /**
     * Limit the univers of observable modules. <code>--limit-modules &lt;mod&gt;[,&lt;mod&gt;...]</code>
     */
    @Parameter
    private List<String> limitModules;

    /**
     * Root modules to resolve. <code>--add-modules &lt;mod&gt;[,&lt;mod&gt;...]</code>
     */
    @Parameter
    private List<String> addModules;

    /**
     * Custom plugin module path <code>--plugin-module-path &lt;modulepath&gt;</code>
     */
    @Parameter
    private File pluginModulePath;

    /**
     * <code>--output &lt;path&gt;</code>
     * </p>
     */
    // TODO: is this a good final location?
    @Parameter( defaultValue = "${project.build.directory}/jlink" )
    private File outputDirectory;

    /**
     * Byte order of generated jimage (default:native). <code>--endian &lt;little|big&gt;</code>.
     * </p>
     * TODO: Reconsider setting the default value? Hasn't that been set already?
     */
    @Parameter( defaultValue = "native" )
    private String endian;

    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true, required = true )
    private List<String> modulePaths;

    /**
     * Add the option <code>--bind-services</code> or not.
     */
    @Parameter( defaultValue = "false" )
    private boolean bindServices;

    /**
     * --disable-plugin pluginName.
     */
    @Parameter
    private String disablePlugin;

    /**
     * <code>--ignore-signing-information</code>
     */
    @Parameter( defaultValue = "false" )
    private boolean ignoreSigningInformation;

    /**
     * <code>--no-header-files</code>
     */
    @Parameter( defaultValue = "false" )
    private boolean noHeaderFiles;

    /**
     * <code>--no-man-pages</code>
     */
    @Parameter( defaultValue = "false" )
    private boolean noManPages;

    /**
     * --suggest-providers [<name>,...] Suggest providers that implement the given service types from the module path
     */
    @Parameter
    private List<String> suggestProviders;

    /**
     * <code>--verbose</code>
     */
    @Parameter( defaultValue = "false" )
    private boolean verbose;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        String jLinkExec;
        try
        {
            jLinkExec = getJLinkExecutable();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Unable to find jlink command: " + e.getMessage(), e );
        }

        getLog().info( "Toolchain in maven-jlink-plugin: jlink [ " + jLinkExec + " ]" );

        // TODO: Find a more better and cleaner way?
        File jLinkExecuteable = new File( jLinkExec );

        // Really Hacky...
        File jLinkParent = jLinkExecuteable.getParentFile().getParentFile();
        File jmodsFolder = new File( jLinkParent, "jmods" );

        getLog().debug( " Parent: " + jLinkParent.getAbsolutePath() );
        getLog().debug( " jmodsFolder: " + jmodsFolder.getAbsolutePath() );

        failIfParametersAreNotInTheirValidValueRanges();

        deleteOutputDirectoryIfItAlreadyExists();

        List<MavenProject> sortedProjects = getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            getLog().info( "MavenProject: " + mavenProject.getBasedir() );
        }

        List<Dependency> dependencies = getSession().getCurrentProject().getDependencies();

        List<MavenProject> modulesToAdd = new ArrayList<>();
        for ( Dependency dependency : dependencies )
        {
            // Should we only take care of module which have packaging "jmod"
            // what about other modules/packaging types like "jar" ?
            if ( "jmod".equals( dependency.getType() ) )
            {
                MavenProject mp = findDependencyInProjects( dependency );
                // TODO: What about module name != artifactId which has been
                // defined in module-info.java file!
                modulesToAdd.add( mp );
            }
        }

        if ( addModules == null )
        {
            addModules = new ArrayList<>();
        }

        for ( MavenProject mavenProject : modulesToAdd )
        {
            addModules.add( mavenProject.getArtifactId() );
        }

        if ( modulePaths == null )
        {
            modulePaths = new ArrayList<>();
        }

        // JDK mods folder
        modulePaths.add( jmodsFolder.getAbsolutePath() );

        for ( MavenProject mavenProject : modulesToAdd )
        {
            File output = new File( mavenProject.getBuild().getDirectory(), "jmods" );
            modulePaths.add( output.getAbsolutePath() );
        }

        // Synopsis
        // Usage: jlink <options> --module-path <modulepath> --add-modules <mods> --output <path>
        Commandline cmd;
        try
        {
            cmd = createJLinkCommandLine();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        cmd.setExecutable( jLinkExec );

        executeCommand( cmd, outputDirectory );

    }

    private MavenProject findDependencyInProjects( Dependency dep )
    {
        List<MavenProject> sortedProjects = getSession().getProjectDependencyGraph().getSortedProjects();
        MavenProject result = null;
        for ( MavenProject mavenProject : sortedProjects )
        {
            if ( dep.getGroupId().equals( mavenProject.getGroupId() )
                && dep.getArtifactId().equals( mavenProject.getArtifactId() )
                && dep.getVersion().equals( mavenProject.getVersion() ) )
            {
                result = mavenProject;
            }
        }
        return result;
    }

    private void failIfParametersAreNotInTheirValidValueRanges()
        throws MojoFailureException
    {
        if ( compression != null && ( compression < 0 || compression > 2 ) )
        {
            String message =
                "The given compression parameters " + compression + " is not in the valid value range from 0..2";
            getLog().error( message );
            throw new MojoFailureException( message );
        }

        // CHECK if this assumption here is correct?
        // if ( modulePaths != null && ( !modulePaths.isEmpty() ) )
        // {
        //
        // // FIXME: Need to check if the given paths exists? and if they are
        // // folders?
        // // String message = "The given module-paths parameter " + modulePath.getAbsolutePath()
        // // + " is not a directory or does not exist.";
        // // getLog().error( message );
        // // throw new MojoFailureException( message );
        // }
    }

    private void deleteOutputDirectoryIfItAlreadyExists()
        throws MojoExecutionException
    {
        if ( outputDirectory.exists() )
        {
            // Delete the output folder of JLink before we start
            // otherwise JLink will fail with a message "Error: directory already exists: ..."
            try
            {
                getLog().debug( "Deleting existing " + outputDirectory.getAbsolutePath() );
                FileUtils.forceDelete( outputDirectory );
            }
            catch ( IOException e )
            {
                getLog().error( "IOException", e );
                throw new MojoExecutionException( "Failure during deletion of " + outputDirectory.getAbsolutePath()
                    + " occured." );
            }
        }
    }

    Commandline createJLinkCommandLine()
        throws IOException
    {
        File file = new File( outputDirectory.getParentFile(), "jlinkArgs" );
        if ( !getLog().isDebugEnabled() )
        {
            file.deleteOnExit();
        }
        file.getParentFile().mkdirs();
        file.createNewFile();

        PrintStream argsFile = new PrintStream( file );

        if ( stripDebug )
        {
            argsFile.println( "--strip-debug" );
        }

        if ( bindServices )
        {
            argsFile.println( "--bind-services" );
        }

        if ( ignoreSigningInformation )
        {
            argsFile.println( "--ignore-signing-information" );
        }
        if ( compression != null )
        {
            argsFile.println( "--compression" );
            argsFile.println( compression );
        }

        if ( disablePlugin != null )
        {
            argsFile.println( "--disable-plugin" );
            argsFile.append( '"' ).append( disablePlugin ).println( '"' );

        }
        if ( modulePaths != null )
        {
            argsFile.println( "--module-path" );
            argsFile.append( '"' ).append( getColonSeparateList( modulePaths ).replace( "\\", "\\\\" ) ).println( '"' );
        }

        if ( noHeaderFiles )
        {
            argsFile.println( "--no-header-files" );
        }

        if ( noManPages )
        {
            argsFile.println( "--no-man-pages" );
        }

        if ( suggestProviders != null && !suggestProviders.isEmpty() )
        {
            argsFile.println( "--suggest-providers" );
            StringBuilder sb = getCommaSeparatedList( suggestProviders );
            argsFile.println( sb.toString() );
        }

        if ( limitModules != null && !limitModules.isEmpty() )
        {
            argsFile.println( "--limit-modules" );
            StringBuilder sb = getCommaSeparatedList( limitModules );
            argsFile.println( sb.toString() );
        }

        if ( addModules != null && !addModules.isEmpty() )
        {
            argsFile.println( "--add-modules" );
            StringBuilder sb = getCommaSeparatedList( addModules );
            argsFile.println( sb.toString() );
        }

        if ( outputDirectory != null )
        {
            argsFile.println( "--output" );
            argsFile.println( outputDirectory );
        }

        if ( verbose )
        {
            argsFile.println( "--verbose" );
        }
        argsFile.close();

        Commandline cmd = new Commandline();
        cmd.createArg().setValue( '@' + file.getAbsolutePath() );

        return cmd;
    }

    private String getColonSeparateList( List<String> modulePaths )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : modulePaths )
        {
            if ( sb.length() > 0 )
            {
                sb.append( SystemUtils.PATH_SEPARATOR );
            }
            sb.append( module );
        }
        return sb.toString();
    }

    private StringBuilder getCommaSeparatedList( List<String> modules )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : modules )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( module );
        }
        return sb;
    }
}
