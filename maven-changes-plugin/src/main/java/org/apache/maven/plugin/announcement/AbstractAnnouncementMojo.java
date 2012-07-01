package org.apache.maven.plugin.announcement;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract superclass for announcement mojos.
 *
 * @version $Id$
 * @since 2.3
 */
public abstract class AbstractAnnouncementMojo
    extends AbstractMojo
{
    /**
     * The current project base directory.
     *
     * @since 2.1
     */
    @Parameter( property = "basedir", required = true )
    protected String basedir;

    /**
     * The Maven Session.
     *
     * @since 2.3
     */
    @Component
    protected MavenSession mavenSession;

    /**
     * This will cause the execution to be run only at the top of a given module
     * tree. That is, run in the project contained in the same folder where the
     * mvn execution was launched.
     *
     * @since 2.3
     */
    @Parameter( property = "announcement.runOnlyAtExecutionRoot", defaultValue = "false" )
    protected boolean runOnlyAtExecutionRoot;

    /**
     * Returns <code>true</code> if the current project is located at the
     * Execution Root Directory (where mvn was launched).
     *
     * @return <code>true</code> if the current project is at the Execution Root
     */
    protected boolean isThisTheExecutionRoot()
    {
        getLog().debug( "Root Folder:" + mavenSession.getExecutionRootDirectory() );
        getLog().debug( "Current Folder:" + basedir );
        boolean result = mavenSession.getExecutionRootDirectory().equalsIgnoreCase( basedir.toString() );
        if ( result )
        {
            getLog().debug( "This is the execution root." );
        }
        else
        {
            getLog().debug( "This is NOT the execution root." );
        }
        return result;
    }
}
