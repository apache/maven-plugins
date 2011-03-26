package org.apache.maven.plugin.dependency.resolvers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.codehaus.plexus.util.IOUtil;

/**
 * Goal that resolves all project plugins and reports and their dependencies.
 * 
 * @goal resolve-plugins
 * @phase generate-sources
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0
 */
public class ResolvePluginsMojo
    extends AbstractResolveMojo
{

    /**
     * Remote repositories which will be searched for plugins.
     * 
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remotePluginRepositories;

    /**
     * If we should exclude transitive dependencies
     * 
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    private boolean excludeTransitive;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * displaying the resolved version.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     */
    public void execute()
        throws MojoExecutionException
    {
        Writer outputWriter = null;

        try
        {
            Set<Artifact> plugins = resolvePluginArtifacts();

            if ( this.outputFile != null )
            {
                outputFile.getParentFile().mkdirs();

                outputWriter = new FileWriter( outputFile );
            }

            for ( Artifact plugin : plugins )
            {
                String logStr = "Plugin Resolved: " + DependencyUtil.getFormattedFileName( plugin, false );
                if ( !silent )
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
                    for ( Artifact artifact : resolveArtifactDependencies( plugin ) )
                    {
                        logStr =
                            "    Plugin Dependency Resolved: " + DependencyUtil.getFormattedFileName( artifact, false );

                        if ( !silent )
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
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( "Nested:", e );
        }
        catch ( InvalidDependencyVersionException e )
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
     * @param project
     *            The POM.
     * @param artifactFactory
     *            component to build artifact objects.
     * @param localRepository
     *            where to resolve artifacts.
     * @param remotePluginRepositories
     *            list of remote repositories used to resolve plugins.
     * @param artifactResolver
     *            component used to resolve artifacts.
     * 
     * @return set of resolved plugin artifacts.
     * 
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     */
    protected Set<Artifact> resolvePluginArtifacts()
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<Artifact> plugins = project.getPluginArtifacts();
        Set<Artifact> reports = project.getReportArtifacts();

        Set<Artifact> artifacts = new HashSet<Artifact>();
        artifacts.addAll( reports );
        artifacts.addAll( plugins );

        for ( Artifact artifact : artifacts )
        {
            // resolve the new artifact
            this.resolver.resolve( artifact, this.remotePluginRepositories, this.getLocal() );
        }
        return artifacts;
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return null;
    }
}
