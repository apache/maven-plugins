/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.workspace.EclipseWorkspaceWriter;

/**
 * Configures The following Eclipse Workspace features:
 * <ul>
 * <li>Adds the classpath variable MAVEN_REPO to Eclipse.</li>
 * <li>Optionally load Eclipse code style file via a URL.</li>
 * </ul>
 * 
 * @goal configure-workspace
 * @requiresProject false
 */
public class ConfigureWorkspaceMojo
    extends AbstractWorkspaceMojo
{
    /**
     * Point to a URL containing code styles content.
     * 
     * @parameter expression="${eclipse.workspaceCodeStylesURL}"
     */
    private String workspaceCodeStylesURL;

    /**
     * Name of a profile in <code>workspaceCodeStylesURL</code> to activate. Default is the first profile name in the
     * code style file in <code>workspaceCodeStylesURL</code>
     * 
     * @parameter expression="${eclipse.workspaceActiveCodeStyleProfileName}"
     */
    private String workspaceActiveCodeStyleProfileName;

    public void execute()
        throws MojoExecutionException
    {
        WorkspaceConfiguration config = new WorkspaceConfiguration();
        config.setWorkspaceDirectory( new File( this.getWorkspace() ) );
        config.setLocalRepository( this.getLocalRepository() );

        if ( this.workspaceCodeStylesURL != null )
        {
            try
            {
                config.setCodeStylesURL( new URL( workspaceCodeStylesURL ) );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            config.setActiveStyleProfileName( workspaceActiveCodeStyleProfileName );

        }

        new EclipseWorkspaceWriter().init( this.getLog(), config ).write();
    }

}
