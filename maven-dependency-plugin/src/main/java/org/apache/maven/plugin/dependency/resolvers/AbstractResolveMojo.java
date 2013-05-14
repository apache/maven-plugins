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

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public abstract class AbstractResolveMojo
    extends AbstractDependencyFilterMojo
{
    /**
     * Project builder -- builds a model from a pom.xml
     */
    @Component
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * If specified, this parameter will cause the dependencies to be written to the path specified, instead of writing
     * to the console.
     *
     * @since 2.0
     */
    @Parameter( property = "outputFile" )
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
     * @since 2.2
     */
    @Parameter( property = "appendOutput", defaultValue = "false" )
    protected boolean appendOutput;

    /**
     * Don't resolve plugins that are in the current reactor. 
     * Only works for plugins at the moment.
     * 
     * @since 2.7
     */
    @Parameter( property = "excludeReactor", defaultValue = "true" )
    protected boolean excludeReactor;

    /**
     * <i>not used in this goal</i>
     */
    @Parameter
    protected boolean useJvmChmod = true;

    /**
     * <i>not used in this goal</i>
     */
    @Parameter
    protected boolean ignorePermissions;

    protected FilterArtifacts getPluginArtifactsFilter()
    {
        if ( excludeReactor )
        {
            final StringBuilder exAids = new StringBuilder();
            if ( this.excludeArtifactIds != null )
            {
                exAids.append( this.excludeArtifactIds );
            }

            for ( final MavenProject rp : reactorProjects )
            {
                if ( !"maven-plugin".equals( rp.getPackaging() ) )
                {
                    continue;
                }

                if ( exAids.length() > 0 )
                {
                    exAids.append( "," );
                }

                exAids.append( rp.getArtifactId() );
            }

            this.excludeArtifactIds = exAids.toString();
        }

        final FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter( new ScopeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeScope ),
                                           DependencyUtil.cleanToBeTokenizedString( this.excludeScope ) ) );

        filter.addFilter( new TypeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeTypes ),
                                          DependencyUtil.cleanToBeTokenizedString( this.excludeTypes ) ) );

        filter.addFilter( new ClassifierFilter( DependencyUtil.cleanToBeTokenizedString( this.includeClassifiers ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeClassifiers ) ) );

        filter.addFilter( new GroupIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeGroupIds ),
                                             DependencyUtil.cleanToBeTokenizedString( this.excludeGroupIds ) ) );

        filter.addFilter( new ArtifactIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeArtifactIds ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeArtifactIds ) ) );

        return filter;
    }

    protected Set<Artifact> resolveDependencyArtifacts( final MavenProject theProject )
        throws ArtifactResolutionException, ArtifactNotFoundException, InvalidDependencyVersionException
    {
        final Set<Artifact> artifacts =
            theProject.createArtifacts( this.factory, Artifact.SCOPE_TEST,
                                        new ScopeArtifactFilter( Artifact.SCOPE_TEST ) );

        for ( final Artifact artifact : artifacts )
        {
            // resolve the new artifact
            this.resolver.resolve( artifact, this.remoteRepos, this.getLocal() );
        }
        return artifacts;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the artifact used to retrieve dependencies
     * @return resolved set of dependencies
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     *
     */
    protected Set<Artifact> resolveArtifactDependencies( final Artifact artifact )
        throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException,
        InvalidDependencyVersionException
    {
        final Artifact pomArtifact =
            this.factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "",
                                         "pom" );

        final MavenProject pomProject =
            mavenProjectBuilder.buildFromRepository( pomArtifact, this.remoteRepos, this.getLocal() );

        return resolveDependencyArtifacts( pomProject );
    }
}
