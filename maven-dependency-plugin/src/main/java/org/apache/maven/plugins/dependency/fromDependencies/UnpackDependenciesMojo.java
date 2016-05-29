package org.apache.maven.plugins.dependency.fromDependencies;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.filters.MarkerFileFilter;
import org.apache.maven.plugins.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.io.File;

/**
 * Goal that unpacks the project dependencies from the repository to a defined
 * location.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo( name = "unpack-dependencies", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true )
public class UnpackDependenciesMojo
    extends AbstractFromDependenciesMojo
{
    /**
     * A comma separated list of file patterns to include when unpacking the
     * artifact.  i.e. <code>**\/*.xml,**\/*.properties</code>
     * NOTE: Excludes patterns override the includes.
     * (component code = <code>return isIncluded( name ) AND !isExcluded( name );</code>)
     *
     * @since 2.0
     */
    @Parameter( property = "mdep.unpack.includes" )
    private String includes;

    /**
     * A comma separated list of file patterns to exclude when unpacking the
     * artifact.  i.e. <code>**\/*.xml,**\/*.properties</code>
     * NOTE: Excludes patterns override the includes.
     * (component code = <code>return isIncluded( name ) AND !isExcluded( name );</code>)
     *
     * @since 2.0
     */
    @Parameter( property = "mdep.unpack.excludes" )
    private String excludes;

    /**
     * Encoding of artifact.
     *
     */
    @Parameter( property = "mdep.unpack.encoding" )
    private String encoding;

    /**
     * Main entry into mojo. This method gets the dependencies and iterates
     * through each one passing it to DependencyUtil.unpackFile().
     *
     * @throws MojoExecutionException with a message if an error occurs.
     * @see #getDependencies
     * @see DependencyUtil#unpackFile(Artifact, File, File, ArchiverManager,
     *      Log)
     */
    @Override
    protected void doExecute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( this.failOnMissingClassifierArtifact );

        for ( Artifact artifact : dss.getResolvedDependencies() )
        {
            File destDir;
            destDir = DependencyUtil.getFormattedOutputDirectory( useSubDirectoryPerScope, useSubDirectoryPerType,
                                                                  useSubDirectoryPerArtifact, useRepositoryLayout,
                                                                  stripVersion, outputDirectory, artifact );
            unpack( artifact, destDir, getIncludes(), getExcludes(), getEncoding() );
            DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler( artifact, this.markersDirectory );
            handler.setMarker();
        }

        for ( Artifact artifact : dss.getSkippedDependencies() )
        {
            getLog().info( artifact.getId() + " already exists in destination." );
        }
    }

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new MarkerFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                     new DefaultFileMarkerHandler( this.markersDirectory ) );
    }

    /**
     * @return Returns a comma separated list of excluded items
     */
    public String getExcludes()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.excludes );
    }

    /**
     * @param excludes A comma separated list of items to exclude
     *                 i.e. <code>**\/*.xml, **\/*.properties</code>
     */
    public void setExcludes( String excludes )
    {
        this.excludes = excludes;
    }

    /**
     * @return Returns a comma separated list of included items
     */
    public String getIncludes()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.includes );
    }

    /**
     * @param includes A comma separated list of items to include
     *                 i.e. <code>**\/*.xml, **\/*.properties</code>
     */
    public void setIncludes( String includes )
    {
        this.includes = includes;
    }

    /**
     * @param encoding The encoding to set.
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }    

    /**
     * @return Returns the encoding.
     */
    public String getEncoding()
    {
        return this.encoding;
    }
}
