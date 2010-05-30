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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojo;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;

/**
 * Goal that resolves all project dependencies and then lists the repositories
 * used by the build and by the transitive dependencies
 *
 * @goal list-repositories
 * @requiresDependencyResolution test
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: GoOfflineMojo.java 728546 2008-12-21 22:56:51Z bentmann $
 * @since 2.2
 */
public class ListRepositoriesMojo
    extends AbstractDependencyMojo
{
	/**
     * Displays a list of the repositories used by this build.
	 * @throws MojoExecutionException
	 *             with a message if an error occurs.
	 *
	 */
    public void execute()
        throws MojoExecutionException
	{
		try
		{
			ArtifactResolutionResult result = this.artifactCollector.collect(
					project.getArtifacts(), project.getArtifact(), this.getLocal(),
					this.remoteRepos, this.artifactMetadataSource,
					new ScopeArtifactFilter( Artifact.SCOPE_TEST ),
					new ArrayList() );
			HashSet repos = new HashSet();
            for ( Iterator i = result.getArtifactResolutionNodes().iterator(); i.hasNext(); )
			{
				ResolutionNode node = (ResolutionNode) i.next();
                repos.addAll( node.getRemoteRepositories() );
			}

            this.getLog().info( "Repositories Used by this build:" );
            for ( Iterator i = repos.iterator(); i.hasNext(); )
            {
                this.getLog().info( i.next().toString() );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifacts", e );
        }
	}
}
