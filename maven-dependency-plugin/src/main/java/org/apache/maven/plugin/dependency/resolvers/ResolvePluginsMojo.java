/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com) 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.plugin.dependency.resolvers;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;

/**
 * Goal that resolves all project plugins and reports and their dependencies.
 *
 * @goal resolve-plugins
 * @phase generate-sources
 * @author brianf
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
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    private boolean excludeTransitive;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through displaying the resolved version.
     * 
     * @throws MojoExecutionException 
     *          with a message if an error occurs. 
     *
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            Set plugins = DependencyUtil.resolvePluginArtifacts( project, factory, local,
                                                                 remotePluginRepositories, resolver );
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Artifact plugin = (Artifact) i.next();
                if ( !silent )
                {
                    this.getLog().info( "Plugin Resolved: " + DependencyUtil.getFormattedFileName( plugin, false ) );
                }
                if ( !excludeTransitive )
                {
                    Set transitiveDependencies = DependencyUtil
                        .resolveArtifactDependencies( plugin, factory, local,
                                                      remoteRepos, resolver, mavenProjectBuilder );
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
}
