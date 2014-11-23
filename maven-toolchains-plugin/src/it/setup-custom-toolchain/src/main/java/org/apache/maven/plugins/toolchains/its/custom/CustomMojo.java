package org.apache.maven.plugins.toolchains.its.custom;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * A sample Mojo that uses the custom toolchain.
 * 
 * @author Hervé Boutemy
 */
@Mojo( name = "tool" )
public class CustomMojo
    extends AbstractMojo
{

    @Component
    private ToolchainManager toolchainManager;

    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    public void execute()
        throws MojoExecutionException
    {
        // get the custom toolchain
        CustomToolchain toolchain = (CustomToolchain) toolchainManager.getToolchainFromBuildContext( "custom", session );

        if ( toolchain == null )
        {
            throw new MojoExecutionException( "Could not find 'custom' toolchain: please check maven-toolchains-plugin configuration." );
        }

        getLog().info( "Found 'custom' toolchain in build context." );

        // get a tool from the toolchain
        String path = toolchain.findTool( "tool" );

        getLog().info( "Found expected tool named 'tool' at following location: " + path );
    }
}