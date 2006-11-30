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


package org.apache.maven.plugin.dependency.resolvers;

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

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;

/**
 * Goal that resolves the project source dependencies from the repository.
 * 
 * @goal sources
 * @phase generate-sources
 * @requiresDependencyResolution test
 * @author brianf
 * @since 2.0
 */
public class ResolveDependencySourcesMojo
    extends AbstractResolveMojo
{

    private static final String SOURCE_TYPE = "java-source";

    private static final String SOURCE_CLASSIFIER = "sources";

    /**
     * If the plugin should exclude Transitive dependencies.
     * 
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    private boolean excludeTransitive;

    /**
     * The mojo compares artifact groupIds against this string using
     * string.startsWith to see if they should be resolved.
     * 
     * @parameter expression="${groupId}" default-value=""
     */
    private String groupId;

    /**
     * Directory to store flag files
     * 
     * @parameter expression="${dependency.resolveMarkersDirectory}"
     *            default-value="${project.build.directory}/dependency-maven-plugin-markers"
     * @required
     */
    private File markersDirectory;

    /**
     * Use Marker Files
     * 
     * @parameter expression="${dependency.useMarkers}" default-value="false"
     */
    private boolean useMarkers;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * resolving the source jars.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     */
    public void execute()
        throws MojoExecutionException
    {
        // Loop through all artifacts
        Set artifacts;
        if ( !excludeTransitive )
        {
            artifacts = project.getArtifacts();
        }
        else
        {
            artifacts = project.getDependencyArtifacts();
        }

        if ( groupId != null && !"".equals( groupId ) )
        {
            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( !artifact.getGroupId().startsWith( groupId ) )
                {
                    iter.remove();
                }
            }
        }

    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
