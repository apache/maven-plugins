package org.apache.maven.plugin.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which fails the build if Maven isn't the correct mavenVersion
 * 
 * @goal maven
 * @author Brian Fox
 * @phase process-sources
 */
public class MavenMojo
    extends abstractVersionEnforcer
{
    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.execution.RuntimeInformation}"
     * @required
     * @readonly
     */
    protected RuntimeInformation rti;

    /**
     * Specify the required Version of Maven.
     * Some examples are
     * <ul>
     *   <li><code>2.0.4</code> Version 2.0.4</li>
     *   <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not included)</li>
     *   <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
     *   <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
     *   <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and 2.1.1 or higher</li>
     * </ul>
     * 
     * @parameter expression="${enforcer.maven.version}" default-value=""
     * @required
     */
    private String mavenVersion = null;

    /**
     * Flag to warn only if the mavenVersion check fails.
     * 
     * @parameter expression="${enforcer.maven.warn}" default-value="false"
     */
    private boolean warn = false;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactVersion version = rti.getApplicationVersion();
        this.enforceVersion("Maven",this.mavenVersion,version,this.warn);
    }
}
