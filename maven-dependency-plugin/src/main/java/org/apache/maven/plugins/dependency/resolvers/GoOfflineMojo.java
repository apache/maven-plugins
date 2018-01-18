package org.apache.maven.plugins.dependency.resolvers;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;

/**
 * Goal that resolves all project dependencies, including plugins and reports and their dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "go-offline", requiresDependencyCollection = ResolutionScope.TEST, threadSafe = true )
@Execute( goal = "resolve-plugins" )
public class GoOfflineMojo
    extends AbstractResolveMojo
{

    /**
     * Include parent poms in the dependency resolution list.
     *
     * @since 3.0.3
     */
    @Parameter( property = "includeParents", defaultValue = "false" )
    private boolean includeParents;

    /**
     * Main entry into mojo. Gets the list of dependencies, filters them by the include/exclude parameters
     * provided and iterates through downloading the resolved version.
     * if the version is not present in the local repository.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    @Override
    protected void doExecute()
        throws MojoExecutionException
    {

        DependencyStatusSets results = this.getDependencySets( false, includeParents );

        ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
        for ( Artifact artifact : results.getResolvedDependencies() )
        {
            try
            {
                getArtifactResolver().resolveArtifact( buildingRequest, artifact );
                if ( !isSilent() )
                {
                    this.getLog().info( "Resolved: "
                            + DependencyUtil.getFormattedFileName( artifact, false ) );
                }
            }
            catch ( ArtifactResolverException e )
            {
                throw new MojoExecutionException( "Failed to resolve artifact: " + artifact, e );
            }
        }
    }

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return null;
    }
}
