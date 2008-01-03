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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ResolveFileFilter;
import org.apache.maven.plugin.dependency.utils.markers.SourcesFileMarkerHandler;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Goal that resolves the project dependencies from the repository.
 * 
 * @goal resolve
 * @requiresDependencyResolution test
 * @phase generate-sources
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0
 */
public class ResolveDependenciesMojo
    extends AbstractResolveMojo
{

    /**
     * If we should display the scope when resolving
     * 
     * @parameter expression="${mdep.outputScope}" default-value="true"
     * @since 2.0-alpha-2
     */
    protected boolean outputScope;

    /**
     * Only used to store results for integration test validation
     */
    DependencyStatusSets results;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through displaying the resolved version.
     * 
     * @throws MojoExecutionException with a message if an error occurs.
     */
    public void execute()
        throws MojoExecutionException
    {
        // get sets of dependencies
        results = this.getDependencySets( false );

        String output = results.getOutput( outputAbsoluteArtifactFilename, outputScope );
        try
        {
            if ( outputFile == null )
            {
                DependencyUtil.log( output, getLog() );
            }
            else
            {
                DependencyUtil.write( output, outputFile, getLog() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(e.getMessage(),e);
        }
    }

    /**
     * @return Returns the results.
     */
    public DependencyStatusSets getResults()
    {
        return this.results;
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new ResolveFileFilter( new SourcesFileMarkerHandler( this.markersDirectory ) );
    }
}
