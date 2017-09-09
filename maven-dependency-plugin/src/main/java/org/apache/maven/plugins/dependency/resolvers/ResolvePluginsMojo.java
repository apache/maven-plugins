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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

/**
 * Goal that resolves all project plugins and reports and their dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "resolve-plugins", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true )
public class ResolvePluginsMojo
    extends AbstractResolveMojo
{

    /**
     * Remote repositories which will be searched for plugins.
     */
    @Parameter( defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remotePluginRepositories;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through displaying the resolved version.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    @Override
    protected void doExecute()
        throws MojoExecutionException
    {
        try
        {
            // ideally this should either be DependencyCoordinates or DependencyNode
            final Set<Artifact> plugins = resolvePluginArtifacts();

            StringBuilder sb = new StringBuilder();
            sb.append( "\n" );
            sb.append( "The following plugins have been resolved:\n" );
            if ( plugins == null || plugins.isEmpty() )
            {
                sb.append( "   none\n" );
            }
            else
            {
                for ( Artifact plugin : plugins )
                {
                    String artifactFilename = null;
                    if ( outputAbsoluteArtifactFilename )
                    {
                        try
                        {
                            // we want to print the absolute file name here
                            artifactFilename = plugin.getFile().getAbsoluteFile().getPath();
                        }
                        catch ( NullPointerException e )
                        {
                            // ignore the null pointer, we'll output a null string
                            artifactFilename = null;
                        }
                    }

                    String id = plugin.toString();
                    sb.append( "   " + id + ( outputAbsoluteArtifactFilename ? ":" + artifactFilename : "" ) + "\n" );

                    if ( !excludeTransitive )
                    {
                        DefaultDependableCoordinate pluginCoordinate = new DefaultDependableCoordinate();
                        pluginCoordinate.setGroupId( plugin.getGroupId() );
                        pluginCoordinate.setArtifactId( plugin.getArtifactId() );
                        pluginCoordinate.setVersion( plugin.getVersion() );

                        for ( final Artifact artifact : resolveArtifactDependencies( pluginCoordinate ) )
                        {
                            artifactFilename = null;
                            if ( outputAbsoluteArtifactFilename )
                            {
                                try
                                {
                                    // we want to print the absolute file name here
                                    artifactFilename = artifact.getFile().getAbsoluteFile().getPath();
                                }
                                catch ( NullPointerException e )
                                {
                                    // ignore the null pointer, we'll output a null string
                                    artifactFilename = null;
                                }
                            }

                            id = artifact.toString();
                            sb.append( "      " + id + ( outputAbsoluteArtifactFilename ? ":" + artifactFilename : "" )
                                + "\n" );
                        }
                    }
                }
                sb.append( "\n" );

                String output = sb.toString();
                if ( outputFile == null )
                {
                    DependencyUtil.log( output, getLog() );
                }
                else
                {
                    DependencyUtil.write( output, outputFile, appendOutput, getLog() );
                }
            }
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( final ArtifactFilterException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactResolverException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( DependencyResolverException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * This method resolves the plugin artifacts from the project.
     *
     * @return set of resolved plugin artifacts.
     * @throws ArtifactFilterException in case of an error.
     * @throws ArtifactResolverException in case of an error.
     */
    protected Set<Artifact> resolvePluginArtifacts()
        throws ArtifactFilterException, ArtifactResolverException
    {
        final Set<Artifact> plugins = getProject().getPluginArtifacts();
        final Set<Artifact> reports = getProject().getReportArtifacts();

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        artifacts.addAll( reports );
        artifacts.addAll( plugins );

        final FilterArtifacts filter = getPluginArtifactsFilter();
        artifacts = filter.filter( artifacts );

        Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>( artifacts.size() );
        // final ArtifactFilter filter = getPluginFilter();
        for ( final Artifact artifact : new LinkedHashSet<Artifact>( artifacts ) )
        {
            // if ( !filter.include( artifact ) )
            // {
            // final String logStr =
            // String.format( " Plugin SKIPPED: %s", DependencyUtil.getFormattedFileName( artifact, false ) );
            //
            // if ( !silent )
            // {
            // this.getLog().info( logStr );
            // }
            //
            // artifacts.remove( artifact );
            // continue;
            // }

            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );

            buildingRequest.setRemoteRepositories( this.remotePluginRepositories );

            // resolve the new artifact
            resolvedArtifacts.add( getArtifactResolver().resolveArtifact( buildingRequest, artifact ).getArtifact() );
        }
        return artifacts;
    }

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return null;
    }
}
