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
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.DestFileFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Goal that copies the project dependencies from the repository to a defined
 * location.
 *
 * @goal copy-dependencies
 * @requiresDependencyResolution test
 * @phase process-sources
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 1.0
 */
public class CopyDependenciesMojo
    extends AbstractFromDependenciesMojo
{

    /**
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * @component
     */
    protected ArtifactRepositoryFactory repositoryFactory;

    /**
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * calling copyArtifact.
     *
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     *
     * @see #getDependencies
     * @see #copyArtifact(Artifact, boolean)
     */
    public void execute()
        throws MojoExecutionException
    {
        DependencyStatusSets dss = getDependencySets( this.failOnMissingClassifierArtifact );
        Set<Artifact> artifacts = dss.getResolvedDependencies();

        if ( !useRepositoryLayout )
        {
            for ( Artifact artifact : artifacts )
            {
	    		copyArtifact( artifact, this.stripVersion, this.prependGroupId );
            }
        }
        else
        {
            try
            {
                ArtifactRepository targetRepository = repositoryFactory.createDeploymentArtifactRepository(
                        "local",
                        outputDirectory.toURL().toExternalForm(),
                        (ArtifactRepositoryLayout) repositoryLayouts.get( "default" ),
                        false /*uniqueVersion*/ );
                for ( Artifact artifact : artifacts )
                {
                    installArtifact( artifact, targetRepository );
                }
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "Could not create outputDirectory repository", e );
            }
        }

        Set<Artifact> skippedArtifacts = dss.getSkippedDependencies();
        for ( Artifact artifact : skippedArtifacts )
        {
            getLog().info( artifact.getFile().getName() + " already exists in destination." );
        }

        if ( isCopyPom() )
        {
            copyPoms( getOutputDirectory(), artifacts, this.stripVersion );
            copyPoms( getOutputDirectory(), skippedArtifacts, this.stripVersion );  // Artifacts that already exist may not already have poms.
        }
    }

    private void installArtifact( Artifact artifact, ArtifactRepository targetRepository )
    {
        try
        {
            if ( "pom".equals( artifact.getType() ) )
            {
                installer.install( artifact.getFile(), artifact, targetRepository );
                installBaseSnapshot( artifact, targetRepository );
            }
            else
            {
                installer.install( artifact.getFile(), artifact, targetRepository );
                installBaseSnapshot( artifact, targetRepository );

                if ( isCopyPom() )
                {
                    Artifact pomArtifact = getResolvedPomArtifact( artifact );
                    if ( pomArtifact.getFile() != null && pomArtifact.getFile().exists() )
                    {
                        installer.install( pomArtifact.getFile(), pomArtifact, targetRepository );
                        installBaseSnapshot( pomArtifact, targetRepository );
                    }
                }
            }
        }
        catch ( ArtifactInstallationException e )
        {
            getLog().info( e.getMessage() );
        }
    }

    private void installBaseSnapshot( Artifact artifact, ArtifactRepository targetRepository )
            throws ArtifactInstallationException
    {
        if ( artifact.isSnapshot() && !artifact.getBaseVersion().equals( artifact.getVersion() ) )
        {
            Artifact baseArtifact = this.factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getBaseVersion(), artifact.getScope(), artifact.getType() );
            installer.install( artifact.getFile(), baseArtifact, targetRepository );
        }
    }

    /**
     * Copies the Artifact after building the destination file name if
     * overridden. This method also checks if the classifier is set and adds it
     * to the destination file name if needed.
     *
     * @param artifact
     *            representing the object to be copied.
     * @param removeVersion
     *            specifies if the version should be removed from the file name
     *            when copying.
     * @param prependGroupId
     *            specifies if the groupId should be prepend to the file while copying.
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     *
     * @see DependencyUtil#copyFile(File, File, Log)
     * @see DependencyUtil#getFormattedFileName(Artifact, boolean)
     */
    protected void copyArtifact( Artifact artifact, boolean removeVersion, boolean prependGroupId )
        throws MojoExecutionException
    {

        String destFileName = DependencyUtil.getFormattedFileName( artifact, removeVersion, prependGroupId);

        File destDir;
        destDir = DependencyUtil.getFormattedOutputDirectory( useSubDirectoryPerScope, useSubDirectoryPerType, useSubDirectoryPerArtifact,
                                                              useRepositoryLayout, stripVersion, outputDirectory,
                                                              artifact );
        File destFile = new File( destDir, destFileName );

        copyFile( artifact.getFile(), destFile );
    }

    /**
     * Copy the pom files associated with the artifacts.
     */
    public void copyPoms( File destDir, Set<Artifact> artifacts, boolean removeVersion )
        throws MojoExecutionException

    {
        for ( Artifact artifact : artifacts )
        {
            Artifact pomArtifact = getResolvedPomArtifact( artifact );

            // Copy the pom
            if ( pomArtifact.getFile() != null && pomArtifact.getFile().exists() )
            {
                File pomDestFile = new File( destDir, DependencyUtil.getFormattedFileName( pomArtifact, removeVersion,
                                                                                           prependGroupId) );
                if ( ! pomDestFile.exists() )
                {
                    copyFile( pomArtifact.getFile(), pomDestFile );
                }
            }
        }
    }

    protected Artifact getResolvedPomArtifact( Artifact artifact )
    {
        Artifact pomArtifact = this.factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                            artifact.getVersion(), "", "pom" );
        // Resolve the pom artifact using repos
        try
        {
            this.resolver.resolve( pomArtifact, this.remoteRepos, this.getLocal() );
        }
        catch ( Exception e )
        {
            getLog().info( e.getMessage() );
        }
        return pomArtifact;
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return new DestFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                   this.useSubDirectoryPerArtifact, this.useSubDirectoryPerType, this.useSubDirectoryPerScope,
                                   this.useRepositoryLayout, this.stripVersion, this.outputDirectory );
    }
}
