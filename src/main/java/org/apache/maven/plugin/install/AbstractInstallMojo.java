package org.apache.maven.plugin.install;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * Common fields for installation mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractInstallMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     */
    @Component
    protected ArtifactInstaller installer;

    /**
     */
    @Parameter( property = "localRepository", required = true, readonly = true )
    protected ArtifactRepository localRepository;

    /**
     * Flag whether to create checksums (MD5, SHA-1) or not.
     *
     * @since 2.2
     */
    @Parameter( property = "createChecksum", defaultValue = "false" )
    protected boolean createChecksum;

    /**
     * Whether to update the metadata to make the artifact a release version.
     */
    @Parameter( property = "updateReleaseInfo", defaultValue = "false" )
    protected boolean updateReleaseInfo;

    protected final DualDigester digester = new DualDigester();
    /**
     * Gets the path of the specified artifact within the local repository. Note that the returned path need not exist
     * (yet).
     *
     * @param artifact The artifact whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact when installed, never <code>null</code>.
     */
    protected File getLocalRepoFile( Artifact artifact )
    {
        String path = localRepository.pathOf( artifact );
        return new File( localRepository.getBasedir(), path );
    }

    /**
     * Gets the path of the specified artifact metadata within the local repository. Note that the returned path need
     * not exist (yet).
     *
     * @param metadata The artifact metadata whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact metadata when installed, never <code>null</code>.
     */
    protected File getLocalRepoFile( ArtifactMetadata metadata )
    {
        String path = localRepository.pathOfLocalRepositoryMetadata( metadata, localRepository );
        return new File( localRepository.getBasedir(), path );
    }

    /**
     * Installs the checksums for the specified artifact if this has been enabled in the plugin configuration. This
     * method creates checksums for files that have already been installed to the local repo to account for on-the-fly
     * generated/updated files. For example, in Maven 2.0.4- the <code>ProjectArtifactMetadata</code> did not install
     * the original POM file (cf. MNG-2820). While the plugin currently requires Maven 2.0.6, we continue to hash the
     * installed POM for robustness with regard to future changes like re-introducing some kind of POM filtering.
     * 
     * @param artifact The artifact for which to create checksums, must not be <code>null</code>.
     * @param metadataFiles The set where additional metadata files will be registered for later checksum installation,
     *            must not be <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    protected void installChecksums( Artifact artifact, Collection<File> metadataFiles )
        throws MojoExecutionException
    {
        if ( !createChecksum )
        {
            return;
        }

        File artifactFile = getLocalRepoFile( artifact );
        installChecksums( artifactFile );

        @SuppressWarnings( "unchecked" )
        Collection<ArtifactMetadata> metadatas = artifact.getMetadataList();
        if ( metadatas != null )
        {
            for ( ArtifactMetadata metadata : metadatas )
            {
                File metadataFile = getLocalRepoFile( metadata );
                metadataFiles.add( metadataFile );
            }
        }
    }

    /**
     * Installs the checksums for the specified metadata files.
     * 
     * @param metadataFiles The collection of metadata files to install checksums for, must not be <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    protected void installChecksums( Collection<File> metadataFiles )
        throws MojoExecutionException
    {
        for ( File metadataFile : metadataFiles )
        {
            installChecksums( metadataFile );
        }
    }

    /**
     * Installs the checksums for the specified file (if it exists).
     *
     * @param installedFile The path to the already installed file in the local repo for which to generate checksums,
     *            must not be <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    private void installChecksums( File installedFile )
        throws MojoExecutionException
    {
        boolean signatureFile = installedFile.getName().endsWith( ".asc" );
        if ( installedFile.isFile() && !signatureFile )
        {

            getLog().debug( "Calculating checksums for " + installedFile );
            digester.calculate( installedFile );
            installChecksum( installedFile, ".md5", digester.getMd5() );
            installChecksum( installedFile, ".sha1", digester.getSha1() );
        }
    }

    /**
     * Installs a checksum for the specified file.
     *
     *
     *
     * @param installedFile The base path from which the path to the checksum files is derived by appending the given
     *            file extension, must not be <code>null</code>.
     * @param ext The file extension (including the leading dot) to use for the checksum file, must not be
     *            <code>null</code>.
     * @param checksum  the checksum to write
     * @throws MojoExecutionException If the checksum could not be installed.
     */
    private void installChecksum( File installedFile, String ext, String checksum )
        throws MojoExecutionException
    {
        File checksumFile = new File( installedFile.getAbsolutePath() + ext );
        getLog().debug( "Installing checksum to " + checksumFile );
        try
        {
            //noinspection ResultOfMethodCallIgnored
            checksumFile.getParentFile().mkdirs();
            FileUtils.fileWrite(checksumFile.getAbsolutePath(), "UTF-8", checksum );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to install checksum to " + checksumFile, e );
        }
    }

}
