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

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;
import org.apache.maven.plugin.dependency.utils.filters.ResolveFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.SourcesFileMarkerHandler;

/**
 * Goal that resolves the project source dependencies from the repository.
 * 
 * @goal sources
 * @phase generate-sources
 * @requiresDependencyResolution test
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0-alpha2
 */
public class ResolveDependencySourcesMojo
    extends AbstractResolveMojo
{

    private static final String SOURCE_TYPE = "java-source";

    private static final String SOURCE_CLASSIFIER = "sources";

    /**
     * Only used to store results for integration test validation
     */
    DependencyStatusSets results;
    
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
        this.classifier = SOURCE_CLASSIFIER;
        this.type = SOURCE_TYPE;
        // get sets of dependencies
        results = this.getDependencySets( false );
        
        SourcesFileMarkerHandler handler = new SourcesFileMarkerHandler(this.markersDirectory);
        handler.setResolved( true );
        
        Iterator iter = results.getResolvedDependencies().iterator();
        while (iter.hasNext())
        {
            Artifact artifact = (Artifact) iter.next();
            handler.setArtifact( artifact );
            handler.setMarker();
        }
        
        handler.setResolved( false );
        iter = results.getUnResolvedDependencies().iterator();
        while (iter.hasNext())
        {
            Artifact artifact = (Artifact) iter.next();
            handler.setArtifact( artifact );
            handler.setMarker();
        }

        results.logStatus( getLog(), outputAbsoluteArtifactFilename, false );
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new ResolveFileFilter( new SourcesFileMarkerHandler( this.markersDirectory ) );
    }
}
