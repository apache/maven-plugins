/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
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
package org.apache.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author brianf
 * 
 */
public abstract class AbstractDependencyMojo
    extends AbstractMojo
{
    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    protected org.apache.maven.artifact.factory.ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected org.apache.maven.artifact.repository.ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected java.util.List remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    protected ArchiverManager archiverManager;

    /**
     * POM
     * 
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * Contains the full list of projects in the reactor.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * If the plugin should be silent.
     * 
     * @parameter expression="${silent}" default-value="false"
     */
    protected boolean silent;

    private Log log;

    /**
     * @return Returns the log.
     */
    public Log getLog()
    {
        if ( silent )
        {
            log = new SilentLog();
        }
        else
        {
            log = super.getLog();
        }

        return this.log;
    }

    /**
     * @return Returns the archiverManager.
     */
    public ArchiverManager getArchiverManager()
    {
        return this.archiverManager;
    }

    /**
     * Does the actual copy of the file and logging.
     * 
     * @param artifact
     *            represents the file to copy.
     * @param destFile
     *            file name of destination file.
     * @param log
     *            to use for output.
     * @param overWriteSnapshots
     *            force Over Writing of the file
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     */
    protected boolean copyFile( File artifact, File destFile, boolean overWrite )
        throws MojoExecutionException
    {
        Log theLog = this.getLog();
        boolean result = false;
        if ( !destFile.exists() || overWrite )
        {
            try
            {
                theLog.info( "Copying " + artifact.getAbsolutePath() + " to " + destFile );
                FileUtils.copyFile( artifact, destFile );
                result = true;
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Error copying artifact from " + artifact + " to " + destFile, e );
            }
        }
        else
        {
            theLog.info( artifact.getName() + " already exists." );
        }

        return result;
    }
    
    /**
     * Unpacks the archive file, checking for a marker file to see if it should
     * unpack again. Creates the marker file after unpacking.
     * 
     * @param Artifact
     *            File to be unpacked.
     * @param unpackDirectory
     *            Location where to put the unpacked files. 
     *            
     *  @deprecated Please now use unpack()
     * @see unpack()
     */
    protected void unpackFile( Artifact artifact, File unpackDirectory, File markersDirectory,
                                   boolean overWrite )
        throws MojoExecutionException
    {
        Log theLog = getLog();
        markersDirectory.mkdirs();

        File markerFile = new File( markersDirectory, artifact.getId().replace( ':', '-' ) + ".unpacked" );

        if ( !markerFile.exists() || overWrite )
        {
            try
            {
                unpackDirectory.mkdirs();

                unpack( artifact.getFile(), unpackDirectory);

                // create marker file
                markerFile.getParentFile().mkdirs();
                markerFile.createNewFile();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error creating marker file: " + markerFile );
            }
        }
        else
        {
            theLog.info( artifact.getFile().getName() + " already unpacked." );
        }
    }

    /**
     * Unpacks the archive file.
     * 
     * @param file
     *            File to be unpacked.
     * @param location
     *            Location where to put the unpacked files.
     */
    protected void unpack( File file, File location)
        throws MojoExecutionException
    {

        try
        {
            location.mkdirs();

            UnArchiver unArchiver;

            unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Unknown archiver type", e );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + location + "\r\n"
                + e.toString(), e );
        }
        catch ( ArchiverException e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + location + "\r\n"
                + e.toString(), e );
        }
    }

}
