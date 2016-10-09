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

import java.io.IOException;
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
 * Usage: jlink &lt;options&gt; --module-path &lt;modulepath&gt; --add-modules &lt;mods&gt; --output &lt;path&gt;
 * Possible options include:
 *  -h, --help                        Print this help message
 *  -p &lt;modulepath&gt;
 *  --module-path &lt;modulepath&gt;        Module path
 *  --limit-modules &lt;mod&gt;[,&lt;mod&gt;...]  Limit the universe of observable modules
 *  --add-modules &lt;mod&gt;[,&lt;mod&gt;...]    Root modules to resolve
 *  --output &lt;path&gt;                   Location of output path
 *  --endian &lt;little|big&gt;             Byte order of generated jimage (default:native)
 *  --version                         Version information
 *  --save-opts &lt;filename&gt;            Save jlink options in the given file
 *  -G, --strip-debug                 Strip debug information
 *  -c, --compress=&lt;0|1|2&gt;            Enable compression of resources (level 2)
 *  --plugin-module-path &lt;modulepath&gt; Custom plugin module path
 *  --list-plugins                    List available plugins
 *  &#64;&lt;filename&gt;                       Read options from file
 * </pre>
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// TODO: Think if ResultionScope is needed here? May be we need to reconsider package phase?
// May be it would be wise to put into PREPARE-PACKAGE and the generation of the final jimage in the package phase?
// Furthermore It could make sense so we can change the conf files if needed...
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "jlink", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true )
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
     * Define the modulepath for the <code>JLink</code> call. <code>--module-path &lt;modulepath&gt;</code> TODO: The
     * default should be the jmods folder of the JDK...
     */
    @Parameter
    private List<String> modulePaths;

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
     * TODO: Think about the default value? I'm not sure if something different would be better?
     */
    @Parameter( defaultValue = "${project.build.directory}/link-result" )
    private File outputDirectory;

    /**
     * Byte order of generated jimage (default:native). <code>--endian &lt;little|big&gt;</code>.
     * </p>
     * TODO: Reconsider setting the default value? Hasn't that been set already?
     */
    @Parameter( defaultValue = "native" )
    private String endian;

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
        Commandline cmd = createJLinkCommandLine();
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
    {
        Commandline cmd = new Commandline();

        if ( stripDebug )
        {
            cmd.createArg().setValue( "--strip-debug" );
        }

        if ( compression != null )
        {
            cmd.createArg().setValue( "--compression" );
            cmd.createArg().setValue( compression.toString() );
        }

        if ( modulePaths != null )
        {
            cmd.createArg().setValue( "--module-path" );
            StringBuilder sb = getColonSeparateList( modulePaths );
            cmd.createArg().setValue( sb.toString() );
        }

        if ( limitModules != null && !limitModules.isEmpty() )
        {
            cmd.createArg().setValue( "--limit-modules" );
            StringBuilder sb = getCommaSeparatedList( limitModules );
            cmd.createArg().setValue( sb.toString() );
        }

        if ( addModules != null && !addModules.isEmpty() )
        {
            cmd.createArg().setValue( "--add-modules" );
            StringBuilder sb = getCommaSeparatedList( addModules );
            cmd.createArg().setValue( sb.toString() );
        }

        if ( outputDirectory != null )
        {
            cmd.createArg().setValue( "--output" );
            cmd.createArg().setFile( outputDirectory );
        }

        return cmd;
    }

    private StringBuilder getColonSeparateList( List<String> modulePaths )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : modulePaths )
        {
            if ( sb.length() > 0 )
            {
                // FIXME: Check this ?
                if ( SystemUtils.IS_OS_WINDOWS )
                {
                    sb.append( ';' );
                }
                else
                {
                    sb.append( ':' );
                }
            }
            sb.append( module );
        }
        return sb;
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
