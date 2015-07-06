package org.apache.maven.plugin.dependency.utils.resolvers;

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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DefaultArtifactsResolver
    implements ArtifactsResolver
{
    private ArtifactResolver resolver;

    private ProjectBuildingRequest buildingRequest;

    boolean stopOnFailure;

    public DefaultArtifactsResolver( ArtifactResolver theResolver, ProjectBuildingRequest buildingRequest,
                                     boolean theStopOnFailure )
    {
        this.resolver = theResolver;
        this.buildingRequest = buildingRequest;
        this.stopOnFailure = theStopOnFailure;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mojo.dependency.utils.resolvers.ArtifactsResolver#resolve(java.util.Set,
     *      org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Set<Artifact> resolve( Set<Artifact> artifacts, Log log )
        throws MojoExecutionException
    {

        Set<Artifact> resolvedArtifacts = new HashSet<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            try
            {
                artifact = resolver.resolveArtifact( buildingRequest, artifact ).getArtifact();
                resolvedArtifacts.add( artifact );
            }
            catch ( ArtifactResolverException ex )
            {
                // an error occurred during resolution, log it an continue
                log.debug( "error resolving: " + artifact.getId() );
                log.debug( ex );
                if ( stopOnFailure )
                {
                    throw new MojoExecutionException( "error resolving: " + artifact.getId(), ex );
                }
            }
        }
        return resolvedArtifacts;
    }

}
