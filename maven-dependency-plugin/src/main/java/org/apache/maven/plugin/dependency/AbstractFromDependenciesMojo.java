/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.maven.plugin.dependency;

import java.io.File;

/**
 * Abstract Parent class used by mojos that get Artifact information from the
 * project dependencies.
 * 
 * @author brianf
 * 
 */
public abstract class AbstractFromDependenciesMojo
    extends AbstractDependencyFilterMojo
{

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     * 
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/dependency"
     * @optional
     * @since 1.0
     */
    protected File outputDirectory;

    /**
     * Place each type of file in a separate subdirectory. (example
     * /outputDirectory/jars /outputDirectory/wars etc)
     * @since 2.0
     * @parameter expression="${useSubDirectoryPerType}" default-value="false"
     * @optional
     */
    protected boolean useSubDirectoryPerType;

    /**
     * Place each file in a separate subdirectory. (example
     * /outputDirectory/junit-junit-3.8.1)
     * @since 2.0
     * @parameter expression="${useSubDirectoryPerArtifact}"
     *            default-value="false"
     * @optional
     */
    protected boolean useSubDirectoryPerArtifact;

}
