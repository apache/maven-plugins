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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
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
import org.codehaus.plexus.util.IOUtil;

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
     * If we should exclude transitive dependencies
     */
    @Parameter( property = "excludeTransitive", defaultValue = "false" )
    private boolean excludeTransitive;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * displaying the resolved version.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    @Override
    protected void doExecute()
        throws MojoExecutionException
    {
        Writer outputWriter = null;

        try
        {
            // ideally this should either be DependencyCoordinates or DependencyNode
            final Set<Artifact> plugins = resolvePluginArtifacts();

            if ( this.outputFile != null )
            {
                outputFile.getParentFile()
                          .mkdirs();

                outputWriter = new FileWriter( outputFile );
            }

            for ( final Artifact plugin : plugins )
            {
                String logStr = "Plugin Resolved: " + DependencyUtil.getFormattedFileName( plugin, false );
                if ( !isSilent() )
                {
                    this.getLog().info( logStr );
                }

                if ( outputWriter != null )
                {
                    outputWriter.write( logStr );
                    outputWriter.write( "\n" );
                }

                if ( !excludeTransitive )
                {
                    DefaultDependableCoordinate pluginCoordinate = new DefaultDependableCoordinate();
                    pluginCoordinate.setGroupId( plugin.getGroupId() );
                    pluginCoordinate.setArtifactId( plugin.getArtifactId() );
                    pluginCoordinate.setVersion( plugin.getVersion() );
                    
                    for ( final Artifact artifact : resolveArtifactDependencies( pluginCoordinate ) )
                    {
                        logStr =
                            "    Plugin Dependency Resolved: " + DependencyUtil.getFormattedFileName( artifact, false );

                        if ( !isSilent() )
                        {
                            this.getLog().info( logStr );
                        }

                        if ( outputWriter != null )
                        {
                            outputWriter.write( logStr );
                            outputWriter.write( "\n" );
                        }
                    }
                }
            }

            if ( outputWriter != null )
            {
                outputWriter.close();
                outputWriter = null;
            }
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( final ArtifactFilterException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ArtifactResolverException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( DependencyResolverException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        finally
        {
            IOUtil.close( outputWriter );
        }

    }

    /**
     * This method resolves the plugin artifacts from the project.
     *
     * @return set of resolved plugin artifacts.
     * @throws ArtifactFilterException 
     * @throws ArtifactResolverException 
     */
    protected Set<Artifact> resolvePluginArtifacts()
        throws ArtifactFilterException, ArtifactResolverException
    {
        final Set<Artifact> plugins = getProject().getPluginArtifacts();
        final Set<Artifact> reports = getProject().getReportArtifacts();

        Set<Artifact> artifacts = new HashSet<Artifact>();
        artifacts.addAll( reports );
        artifacts.addAll( plugins );

        final FilterArtifacts filter = getPluginArtifactsFilter();
        artifacts = filter.filter( artifacts );
        
        Set<Artifact> resolvedArtifacts = new HashSet<Artifact>( artifacts.size() );
        //        final ArtifactFilter filter = getPluginFilter();
        for ( final Artifact artifact : new HashSet<Artifact>( artifacts ) )
        {
            // if ( !filter.include( artifact ) )
            // {
            //     final String logStr =
            //     String.format( "    Plugin SKIPPED: %s", DependencyUtil.getFormattedFileName( artifact, false ) );
            //
            //     if ( !silent )
            //     {
            //         this.getLog().info( logStr );
            //     }
            //
            //     artifacts.remove( artifact );
            //     continue;
            // }

            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
            
            buildingRequest.setRemoteRepositories( this.remotePluginRepositories );
            
            // resolve the new artifact
            resolvedArtifacts.add( getArtifactResolver().resolveArtifact( buildingRequest, artifact ) .getArtifact() );
        }
        return artifacts;
    }

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return null;
    }
}
