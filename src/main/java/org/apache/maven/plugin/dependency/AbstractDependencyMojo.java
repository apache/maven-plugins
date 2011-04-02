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
import java.lang.reflect.Field;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencySilentLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: AbstractDependencyMojo.java 552528
 *          2007-07-02 16:12:47Z markh $
 */
public abstract class AbstractDependencyMojo
    extends AbstractMojo
{
    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * Artifact collector, needed to resolve dependencies.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactCollector"
     * @required
     * @readonly
     */
    protected ArtifactCollector artifactCollector;

    /**
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     *            hint="maven"
     * @required
     * @readonly
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component
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
    protected List<MavenProject> reactorProjects;

    /**
     * If the plugin should be silent.
     *
     * @optional
     * @since 2.0
     * @parameter expression="${silent}"
     *            default-value="false"
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
            log = new DependencySilentLog();
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
     * @param artifact represents the file to copy.
     * @param destFile file name of destination file.
     *
     * @throws MojoExecutionException with a message if an
     *             error occurs.
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

    protected void unpack( File file, File location )
        throws MojoExecutionException
    {
        unpack( file, location, null, null );
    }

    /**
     * Unpacks the archive file.
     *
     * @param file File to be unpacked.
     * @param location Location where to put the unpacked files.
     * @param includes Comma separated list of file patterns to include i.e. <code>**&#47;.xml,
     *            **&#47;*.properties</code>
     * @param excludes Comma separated list of file patterns to exclude i.e. <code>**&#47;*.xml,
     *            **&#47;*.properties</code>
     */
    protected void unpack( File file, File location, String includes, String excludes )
        throws MojoExecutionException
    {
        try
        {
            logUnpack( file, location, includes, excludes );

            location.mkdirs();

            UnArchiver unArchiver;

            unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            if ( StringUtils.isNotEmpty( excludes ) || StringUtils.isNotEmpty( includes ) )
            {
                // Create the selectors that will filter
                // based on include/exclude parameters
                // MDEP-47
                IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[] { new IncludeExcludeFileSelector() };

                if ( StringUtils.isNotEmpty( excludes ) )
                {
                    selectors[0].setExcludes( excludes.split( "," ) );
                }

                if ( StringUtils.isNotEmpty( includes ) )
                {
                    selectors[0].setIncludes( includes.split( "," ) );
                }

                unArchiver.setFileSelectors( selectors );
            }
            if ( this.silent )
            {
                silenceUnarchiver( unArchiver );
            }

            unArchiver.extract();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Unknown archiver type", e );
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
        // dangerous but handle any errors. It's the only way to silence the unArchiver.
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
    public ArtifactFactory getFactory()
    {
        return this.factory;
    }

    /**
     * @param factory The factory to set.
     */
    public void setFactory( ArtifactFactory factory )
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
    protected ArtifactRepository getLocal()
    {
        return this.local;
    }

    /**
     * @param local The local to set.
     */
    public void setLocal( ArtifactRepository local )
    {
        this.local = local;
    }

    /**
     * @return Returns the remoteRepos.
     */
    public List<ArtifactRepository> getRemoteRepos()
    {
        return this.remoteRepos;
    }

    /**
     * @param remoteRepos The remoteRepos to set.
     */
    public void setRemoteRepos( List<ArtifactRepository> remoteRepos )
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
     * @param resolver The resolver to set.
     */
    public void setResolver( ArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    /**
     * @param archiverManager The archiverManager to set.
     */
    public void setArchiverManager( ArchiverManager archiverManager )
    {
        this.archiverManager = archiverManager;
    }

    /**
     * @return Returns the artifactCollector.
     */
    public ArtifactCollector getArtifactCollector()
    {
        return this.artifactCollector;
    }

    /**
     * @param theArtifactCollector The artifactCollector to set.
     */
    public void setArtifactCollector( ArtifactCollector theArtifactCollector )
    {
        this.artifactCollector = theArtifactCollector;
    }

    /**
     * @return Returns the artifactMetadataSource.
     */
    public ArtifactMetadataSource getArtifactMetadataSource()
    {
        return this.artifactMetadataSource;
    }

    /**
     * @param theArtifactMetadataSource The artifactMetadataSource to set.
     */
    public void setArtifactMetadataSource( ArtifactMetadataSource theArtifactMetadataSource )
    {
        this.artifactMetadataSource = theArtifactMetadataSource;
    }

    private void logUnpack( File file, File location, String includes, String excludes )
    {
        if ( !getLog().isInfoEnabled() )
        {
            return;
        }

        StringBuffer msg = new StringBuffer();
        msg.append( "Unpacking " );
        msg.append( file );
        msg.append( " to " );
        msg.append( location );

        if ( includes != null && excludes != null )
        {
            msg.append( " with includes \"" );
            msg.append( includes );
            msg.append( "\" and excludes \"" );
            msg.append( excludes );
            msg.append( "\"" );
        }
        else if ( includes != null )
        {
            msg.append( " with includes \"" );
            msg.append( includes );
            msg.append( "\"" );
        }
        else if ( excludes != null )
        {
            msg.append( " with excludes \"" );
            msg.append( excludes );
            msg.append( "\"" );
        }

        getLog().info( msg.toString() );
    }
}
