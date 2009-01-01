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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

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
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Flag whether to create checksums (MD5, SHA-1) or not.
     *
     * @parameter expression="${createChecksum}" default-value="false"
     */
    protected boolean createChecksum;

    /**
     * Digester for MD5.
     * 
     * @component role-hint="md5"
     */
    protected Digester md5Digester;

    /**
     * Digester for SHA-1.
     * 
     * @component role-hint="sha1"
     */
    protected Digester sha1Digester;

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
     * Installs the checksums for the specified file if this has been enabled in the plugin configuration.
     * 
     * @param originalFile The path to the file from which the checksums are generated, must not be <code>null</code>.
     * @param installedFile The base path from which the paths to the checksum files are derived, must not be
     *            <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    protected void installChecksums( File originalFile, File installedFile )
        throws MojoExecutionException
    {
        boolean signatureFile = installedFile.getName().endsWith( ".asc" );
        if ( createChecksum && !signatureFile )
        {
            installChecksum( originalFile, installedFile, md5Digester, ".md5" );
            installChecksum( originalFile, installedFile, sha1Digester, ".sha1" );
        }
    }

    /**
     * Installs a checksum for the specified file.
     * 
     * @param originalFile The path to the file from which the checksum is generated, must not be <code>null</code>.
     * @param installedFile The base path from which the path to the checksum files is derived by appending the given
     *            file extension, must not be <code>null</code>.
     * @param digester The checksum algorithm to use, must not be <code>null</code>.
     * @param ext The file extension (including the leading dot) to use for the checksum file, must not be
     *            <code>null</code>.
     * @throws MojoExecutionException If the checksum could not be installed.
     */
    private void installChecksum( File originalFile, File installedFile, Digester digester, String ext )
        throws MojoExecutionException
    {
        String checksum;
        getLog().debug( "Calculating " + digester.getAlgorithm() + " checksum for " + originalFile );
        try
        {
            checksum = digester.calc( originalFile );
        }
        catch ( DigesterException e )
        {
            throw new MojoExecutionException( "Failed to calculate " + digester.getAlgorithm() + " checksum for "
                + originalFile, e );
        }

        File checksumFile = new File( installedFile.getAbsolutePath() + ext );
        getLog().debug( "Installing checksum to " + checksumFile );
        try
        {
            checksumFile.getParentFile().mkdirs();
            FileUtils.fileWrite( checksumFile.getAbsolutePath(), "UTF-8", checksum );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to install checksum to " + checksumFile, e );
        }
    }

}
