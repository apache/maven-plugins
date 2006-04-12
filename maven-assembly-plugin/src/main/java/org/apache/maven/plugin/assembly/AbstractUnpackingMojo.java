package org.apache.maven.plugin.assembly;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Base routines for assembly and unpack goals.
 *
 * @version $Id$
 */
public abstract class AbstractUnpackingMojo
    extends AbstractMojo
{
    protected static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * The output directory of the assembled distribution file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The filename of the assembled distribution file.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * @parameter expression="${projectModulesOnly}" default-value="false"
     */
    protected boolean projectModulesOnly = false;

    /**
     * Directory to unpack JARs into if needed
     *
     * @parameter expression="${project.build.directory}/assembly/work"
     * @required
     */
    protected File workDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */

    protected ArchiverManager archiverManager;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${localRepository}"
     */
    protected ArtifactRepository localRepository;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * @parameter expression="${classifier}"
     * @deprecated Please use the Assembly's id for classifier instead
     */
    protected String classifier;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;


    /**
     * Retrieves all artifact dependencies.
     *
     * @return A HashSet of artifacts
     */
    protected Set getDependencies()
        throws MojoExecutionException
    {
        return new HashSet( getDependenciesMap().values() );
    }

    /**
     * Retrieves an includes list generated from the existing depedencies in a project.
     *
     * @return A List of includes
     * @throws MojoExecutionException
     */
    protected List getDependenciesIncludeList()
        throws MojoExecutionException
    {
        List includes = new ArrayList();

        for ( Iterator i = getDependencies().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( project.getArtifactId().equals( a.getArtifactId() ))
            {
                continue;
            }

            includes.add( a.getGroupId() + ":" + a.getArtifactId() );
        }

        return includes;
    }

    /**
     * Retrieves all artifact dependencies in a Map keyed by conflict id.
     *
     * @return A Map of artifacts
     */
    protected Map getDependenciesMap()
        throws MojoExecutionException
    {
        Map dependencies = new HashMap();

        MavenProject project = getExecutedProject();

        // TODO: this is not mediating dependencies versions - first wins. Is there a way we can do that properly from here?
        if ( project != null )
        {
            Artifact artifact = project.getArtifact();

            if ( artifact.getFile() != null )
            {
                String key = artifact.getDependencyConflictId();

                dependencies.put( key, artifact );
            }
        }

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) i.next();

            Artifact artifact = reactorProject.getArtifact();

            if ( artifact.getFile() != null )
            {
                String key = artifact.getDependencyConflictId();

                if ( !dependencies.containsKey( key ) )
                {
                    dependencies.put( key, artifact );
                }
            }

            for ( Iterator j = reactorProject.getArtifacts().iterator(); j.hasNext(); )
            {
                artifact = (Artifact) j.next();

                String key = artifact.getDependencyConflictId();

                if ( !dependencies.containsKey( key ) )
                {
                    dependencies.put( key, artifact );
                }
            }
        }

        return dependencies;
    }

    protected Set getModules()
        throws MojoExecutionException, MojoFailureException
    {
        Map dependencies = new HashMap();

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) i.next();

            Artifact artifact = reactorProject.getArtifact();

            try
            {
                artifactResolver.resolve( artifact, project.getRemoteArtifactRepositories(), localRepository );
            }
            catch ( ArtifactNotFoundException e )
            {
                //TODO: Is there a better way to get the artifact if it is not yet installed in the repo?
                //reactorProject.getArtifact().getFile() is returning null
                //tried also the project.getArtifact().getFile() but returning same result
                File fileArtifact = new File( reactorProject.getBuild().getDirectory() + File.separator +
                    reactorProject.getBuild().getFinalName() + "." + reactorProject.getPackaging() );

                getLog().info( "Artifact ( " + artifact.getFile().getName() + " ) not found " +
                    "in any repository, resolving thru modules..." );

                artifact.setFile( fileArtifact );

                if ( fileArtifact.exists() )
                {
                    if ( artifact.getType().equals( "pom" ) )
                    {
                        continue;
                    }

                    addModuleArtifact( dependencies, artifact );
                }
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException( "Failed to resolve artifact", e );
            }

            if ( artifact.getFile() != null )
            {
                if ( artifact.getType().equals( "pom" ) )
                {
                    continue;
                }

                addModuleArtifact( dependencies, artifact );
            }
        }
        return new HashSet( dependencies.values() );
    }

    private void addModuleArtifact( Map dependencies, Artifact artifact )
    {
        String key = artifact.getDependencyConflictId();

        if ( !dependencies.containsKey( key ) )
        {
            dependencies.put( key, artifact );
        }
    }

    protected abstract MavenProject getExecutedProject();

    /**
     * Unpacks the archive file.
     *
     * @param file     File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    protected void unpack( File file, File location )
        throws MojoExecutionException, NoSuchArchiverException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver = this.archiverManager.getUnArchiver( archiveExt );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

    public String getClassifier()
    {
        return classifier;
    }
}
