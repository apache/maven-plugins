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
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

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
import org.codehaus.plexus.util.ReflectionUtils;

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
     * @optional
     * @since 2.0
     * @parameter expression="${silent}" default-value="false"
     */
    public boolean silent;

    /**
     * Output absolute filename for resolved artifacts
     * 
     * @optional
     * @since 2.0
     * @parameter expression="${outputAbsoluteArtifactFilename}"
     *            default-value="false"
     */
    protected boolean outputAbsoluteArtifactFilename;

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
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     */
    protected void copyFile( File artifact, File destFile )
        throws MojoExecutionException
    {
        Log theLog = this.getLog();
        try
        {
            theLog.info( "Copying "
                + ( this.outputAbsoluteArtifactFilename ? artifact.getAbsolutePath() : artifact.getName() ) + " to "
                + destFile );
            FileUtils.copyFile( artifact, destFile );

        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error copying artifact from " + artifact + " to " + destFile, e );
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
    protected void unpack( File file, File location )
        throws MojoExecutionException
    {

        try
        {
            location.mkdirs();

            UnArchiver unArchiver;

            unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            if (this.silent)
            {
                silenceUnarchiver(unArchiver);
            }
            
            unArchiver.extract();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Unknown archiver type", e );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "Error unpacking file: "
                + ( this.outputAbsoluteArtifactFilename ? file.getAbsolutePath() : file.getName() ) + " to: "
                + location + "\r\n" + e.toString(), e );
        }
        catch ( ArchiverException e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + location + "\r\n"
                + e.toString(), e );
        }
    }

    private void silenceUnarchiver( UnArchiver unArchiver )
    {
        // dangerous but handle any errors. It's the only way to silence the
        // unArchiver.
        try
        {
            Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( "logger", unArchiver.getClass() );

            field.setAccessible( true );

            field.set( unArchiver, this.getLog() );
        }
        catch ( Exception e )
        {
            // was a nice try. Don't bother logging because the log is silent.
        }
    }

    /**
     * @return Returns the factory.
     */
    public org.apache.maven.artifact.factory.ArtifactFactory getFactory()
    {
        return this.factory;
    }

    /**
     * @param factory
     *            The factory to set.
     */
    public void setFactory( org.apache.maven.artifact.factory.ArtifactFactory factory )
    {
        this.factory = factory;
    }

    /**
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @return Returns the local.
     */
    public org.apache.maven.artifact.repository.ArtifactRepository getLocal()
    {
        return this.local;
    }

    /**
     * @param local
     *            The local to set.
     */
    public void setLocal( org.apache.maven.artifact.repository.ArtifactRepository local )
    {
        this.local = local;
    }

    /**
     * @return Returns the remoteRepos.
     */
    public java.util.List getRemoteRepos()
    {
        return this.remoteRepos;
    }

    /**
     * @param remoteRepos
     *            The remoteRepos to set.
     */
    public void setRemoteRepos( java.util.List remoteRepos )
    {
        this.remoteRepos = remoteRepos;
    }

    /**
     * @return Returns the resolver.
     */
    public org.apache.maven.artifact.resolver.ArtifactResolver getResolver()
    {
        return this.resolver;
    }

    /**
     * @param resolver
     *            The resolver to set.
     */
    public void setResolver( org.apache.maven.artifact.resolver.ArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    /**
     * @param archiverManager
     *            The archiverManager to set.
     */
    public void setArchiverManager( ArchiverManager archiverManager )
    {
        this.archiverManager = archiverManager;
    }
}
