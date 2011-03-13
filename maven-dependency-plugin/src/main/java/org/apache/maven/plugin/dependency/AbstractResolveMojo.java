package org.apache.maven.plugin.dependency;

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

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 *
 */
public abstract class AbstractResolveMojo
    extends AbstractDependencyFilterMojo
{
    /**
     * Project builder -- builds a model from a pom.xml
     *
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * If specified, this parameter will cause the dependencies to be written to the path specified, instead of writing
     * to the console.
     *
     * @parameter expression="${outputFile}"
     * @since 2.0
     */
    protected File outputFile;

    /**
     * This method resolves the dependency artifacts from the project.
     *
     * @param theProject
     *            The POM.
     * @return resolved set of dependency artifacts.
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */
    
	/**
	 * Whether to append outputs into the output file or overwrite it.
	 * 
	 * @parameter expression="${appendOutput}" default-value="false"
	 * @since 2.2
	 */
	protected boolean appendOutput;
    
    protected Set<Artifact> resolveDependencyArtifacts( MavenProject theProject )
        throws ArtifactResolutionException, ArtifactNotFoundException, InvalidDependencyVersionException
    {
        Set<Artifact> artifacts =
            theProject.createArtifacts( this.factory, Artifact.SCOPE_TEST,
                                        new ScopeArtifactFilter( Artifact.SCOPE_TEST ) );

        for ( Artifact artifact : artifacts )
        {
            // resolve the new artifact
            this.resolver.resolve( artifact, this.remoteRepos, this.getLocal() );
        }
        return artifacts;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact
     *            the artifact used to retrieve dependencies
     *
     * @return resolved set of dependencies
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     */
    protected Set<Artifact> resolveArtifactDependencies( Artifact artifact )
        throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException,
        InvalidDependencyVersionException
    {
        Artifact pomArtifact = this.factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact
            .getVersion(), "", "pom" );

        MavenProject pomProject = mavenProjectBuilder.buildFromRepository( pomArtifact, this.remoteRepos, this.getLocal() );

        return resolveDependencyArtifacts( pomProject );
    }
}
