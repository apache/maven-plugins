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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.dependency.AbstractDependencyMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.dependencies.collect.CollectorResult;
import org.apache.maven.shared.dependencies.collect.DependencyCollector;
import org.apache.maven.shared.dependencies.collect.DependencyCollectorException;

/**
 * Goal that resolves all project dependencies and then lists the repositories used by the build and by the transitive
 * dependencies
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: GoOfflineMojo.java 728546 2008-12-21 22:56:51Z bentmann $
 * @since 2.2
 */
@Mojo( name = "list-repositories", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class ListRepositoriesMojo
    extends AbstractDependencyMojo
{
    /**
     * Dependency collector, needed to resolve dependencies.
     */
    @Component( role = DependencyCollector.class )
    private DependencyCollector dependencyCollector;

    /**
     * Displays a list of the repositories used by this build.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    @Override
    protected void doExecute()
        throws MojoExecutionException
    {
        try
        {
            CollectorResult collectResult =
                dependencyCollector.collectDependencies( session.getProjectBuildingRequest(), getProject().getModel() );

            this.getLog().info( "Repositories used by this build:" );

            for ( ArtifactRepository repo : collectResult.getRemoteRepositories() )
            {
                this.getLog().info( repo.toString() );
            }
        }
        catch ( DependencyCollectorException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifacts", e );
        }
    }

}
