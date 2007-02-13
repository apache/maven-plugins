package org.apache.maven.plugin.install;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

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
     * @parameter expression="${component.org.apache.maven.artifact.installer.ArtifactInstaller}"
     * @required
     * @readonly
     */
    protected ArtifactInstaller installer;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Flag Whether to create checksums(MD5, SHA1) or not.
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

    protected void installCheckSum( File file, boolean isPom )
        throws MojoExecutionException
    {
        installCheckSum( file, null, isPom );
    }

    protected void installCheckSum( File file, Artifact artifact, boolean isPom )
        throws MojoExecutionException
    {
        try
        {
            getLog().info( "Creating Checksums..." );

            String md5Sum = getChecksum( file, "MD5" );
            String sha1Sum = getChecksum( file, "SHA-1" );

            File temp = File.createTempFile( "maven-md5-checksum", null );
            temp.deleteOnExit();
            FileUtils.fileWrite( temp.getAbsolutePath(), md5Sum );

            File tempSha1 = File.createTempFile( "maven-sha1-checksum", null );
            tempSha1.deleteOnExit();
            FileUtils.fileWrite( tempSha1.getAbsolutePath(), sha1Sum );

            File destination = null;

            if ( isPom )
            {
                destination = file;
            }
            else
            {
                String localPath = localRepository.pathOf( artifact );
                destination = new File( localRepository.getBasedir(), localPath );
            }

            if ( !destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }

            getLog().debug( "Installing checksum for " + destination );

            FileUtils.copyFile( temp, new File( destination + ".md5" ) );
            FileUtils.copyFile( tempSha1, new File( destination + ".sha1" ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating checksum", e );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new MojoExecutionException( "Error in algorithm", e );
        }
        catch ( DigesterException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    protected String getChecksum( File file, String algo )
        throws NoSuchAlgorithmException, DigesterException
    {
        if ( "MD5".equals( algo ) )
        {
            return md5Digester.calc( file );
        }
        else if ( "SHA-1".equals( algo ) )
        {
            return sha1Digester.calc( file );
        }
        else
        {
            throw new NoSuchAlgorithmException( "No support for algorithm " + algo + "." );
        }
    }
}
