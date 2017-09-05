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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * <pre>
 * jmod hash ...
 * </pre>
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
// TODO: Reconsider resolution scope, phase ?
@Mojo( name = "hash", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE )
public class JModHashMojo
    extends AbstractJModMojo
{

    /**
     * <code>--class-path &lt;path&gt;</code> Application jar files|dir containing classes.
     */
    @Parameter
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
    private boolean dryRun;

    @Parameter
    private List<String> excludes;

    @Parameter
    private String mainClass;

    @Parameter
    private List<File> libs;

    @Parameter
    private String moduleVersion;

    /**
     * Define the modulepath for the <code>jmod</code> call. <code>--module-path &lt;path&gt;</code>
     * Must be a directory.
     */
    @Parameter( required = true )
    private File modulePath;

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

        Commandline cmd = createJModHashCommandLine();
        cmd.setExecutable( jModExecutable );

        // executeCommand( cmd, outputDirectory );

    }

    private Commandline createJModHashCommandLine()
    {
        Commandline cmd = new Commandline();

        cmd.createArg().setValue( "hash" );

        if ( dryRun )
        {
            cmd.createArg().setValue( "--dry-run" );
        }

        return cmd;
    }

}
