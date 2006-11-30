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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * Goal that resolves all project plugins and reports and their dependencies.
 * 
 * @goal resolve-plugins
 * @phase generate-sources
 * @author brianf
 * @since 2.0
 */
public class ResolvePluginsMojo
    extends AbstractResolveMojo
{

    /**
     * Remote repositories which will be searched for plugins.
     * 
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remotePluginRepositories;

    /**
     * If we should exclude transitive dependencies
     * 
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    private boolean excludeTransitive;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * displaying the resolved version.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            Set plugins = resolvePluginArtifacts();
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Artifact plugin = (Artifact) i.next();
                if ( !silent )
                {
                    this.getLog().info( "Plugin Resolved: " + DependencyUtil.getFormattedFileName( plugin, false ) );
                }
                if ( !excludeTransitive )
                {
                    Set transitiveDependencies = this.resolveArtifactDependencies( plugin);
                    if ( !silent )
                    {
                        for ( Iterator transIter = transitiveDependencies.iterator(); transIter.hasNext(); )
                        {
                            this.getLog().info(
                                                "    Plugin Dependency Resolved: "
                                                    + DependencyUtil.getFormattedFileName( (Artifact) transIter.next(),
                                                                                           false ) );
                        }
                    }
                }
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }

    }

    /**
     * This method resolves the plugin artifacts from the project.
     * 
     * @param project
     *            The POM.
     * @param artifactFactory
     *            component to build artifact objects.
     * @param localRepository
     *            where to resolve artifacts.
     * @param remotePluginRepositories
     *            list of remote repositories used to resolve plugins.
     * @param artifactResolver
     *            component used to resolve artifacts.
     * 
     * @return set of resolved plugin artifacts.
     * 
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     */
    protected Set resolvePluginArtifacts()
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set plugins = project.getPluginArtifacts();
        Set reports = project.getReportArtifacts();

        Set artifacts = new HashSet();
        artifacts.addAll( reports );
        artifacts.addAll( plugins );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            // resolve the new artifact
            this.resolver.resolve( artifact, this.remotePluginRepositories, this.local );
        }
        return artifacts;
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
