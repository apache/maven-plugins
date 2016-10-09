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
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * <pre>
 * jmod create ...
 * </pre>
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// TODO: Reconsider resolution scope, phase ?
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "create", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true )
// CHECKSTYLE_ON: LineLength
public class JModCreateMojo
    extends AbstractJModMojo
{

    /**
     * <code>--class-path &lt;path&gt;</code> Application jar files|dir containing classes.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private List<String> classPath;

    /**
     * <code>--class-path &lt;path&gt;</code> Application jar files|dir containing classes.
     */
    @Parameter
    private List<String> cmds;

    /**
     * <code>--config &lt;path&gt;</code> Location of user-editable config files.
     */
    @Parameter
    private File config;

    @Parameter
    private List<String> excludes;

    @Parameter
    private String mainClass;

    @Parameter
    private List<File> libs;

    @Parameter( defaultValue = "${project.version}" )
    private String moduleVersion;

    /**
     * <code>--os-arch &lt;os-arch&gt;</code> Operating system architecture.
     */
    @Parameter
    private String osArch;

    /**
     * <code>--os-name &lt;os-name&gt;</code> Operating system name.
     */
    @Parameter
    private String osName;

    /**
     * <code>--os-version &lt;os-version&gt;</code> Operating system version.
     */
    @Parameter
    private String osVersion;

    /**
     * Define the modulepath for the <code>jmod</code> call. <code>--module-path &lt;path&gt;</code>
     */
    // TODO: check if this target/classes folder?
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File modulePath;

    @Parameter( defaultValue = "${project.artifactId}", required = true )
    private String moduleName;

    @Parameter( defaultValue = "${project.build.directory}" )
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

        // We need to put the resulting x.jmod files into jmods folder.
        // Check why?
        File modsFolder = new File( outputDirectory, "jmods" );
        File resultingJModFile = new File( modsFolder, moduleName + ".jmod" );

        deleteOutputIfAlreadyExists( resultingJModFile );

        // create the jmods folder...
        modsFolder.mkdirs();

        Commandline cmd = createJModCreateCommandLine( resultingJModFile );
        cmd.setExecutable( jModExecutable );

        executeCommand( cmd, outputDirectory );

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
    }

    private Commandline createJModCreateCommandLine( File resultingJModFile )
    {
        Commandline cmd = new Commandline();

        cmd.createArg().setValue( "create" );

        if ( moduleVersion != null )
        {
            cmd.createArg().setValue( "--module-version" );
            cmd.createArg().setValue( moduleVersion );
        }

        cmd.createArg().setValue( "--os-arch" );
        if ( osArch != null )
        {
            cmd.createArg().setValue( osArch );
        }
        else
        {
            cmd.createArg().setValue( SystemUtils.OS_ARCH );
        }

        cmd.createArg().setValue( "--os-name" );
        if ( osName != null )
        {
            cmd.createArg().setValue( osName );
        }
        else
        {
            cmd.createArg().setValue( SystemUtils.OS_NAME );
        }

        cmd.createArg().setValue( "--os-version" );
        if ( osVersion != null )
        {
            cmd.createArg().setValue( osVersion );
        }
        else
        {
            cmd.createArg().setValue( SystemUtils.OS_VERSION );
        }

        if ( classPath != null )
        {
            cmd.createArg().setValue( "--class-path" );
            StringBuilder sb = getColonSeparateList( classPath );
            cmd.createArg().setValue( sb.toString() );
        }

        // Can not be overwritten...
        // TODO: Good idea?
        cmd.createArg().setFile( resultingJModFile );
        return cmd;
    }

    private StringBuilder getColonSeparateList( List<String> paths )
    {
        StringBuilder sb = new StringBuilder();
        for ( String module : paths )
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

}
