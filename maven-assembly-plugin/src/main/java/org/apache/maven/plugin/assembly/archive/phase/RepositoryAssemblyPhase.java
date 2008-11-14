package org.apache.maven.plugin.assembly.archive.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;
import org.apache.maven.shared.repository.model.RepositoryInfo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="repositories"
 */
public class RepositoryAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    /**
     * @plexus.requirement
     */
    private RepositoryAssembler repositoryAssembler;

    /**
     * @plexus.requirement
     */
    private DependencyResolver dependencyResolver;

    public RepositoryAssemblyPhase()
    {
        // used for plexus.
    }

    // introduced for testing.
    public RepositoryAssemblyPhase( RepositoryAssembler repositoryAssembler, DependencyResolver resolver )
    {
        this.repositoryAssembler = repositoryAssembler;
        dependencyResolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        execute( assembly, archiver, configSource, new DefaultAssemblyContext() );
    }

    /**
     * {@inheritDoc}
     */
    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource,
                         AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        List repositoriesList = assembly.getRepositories();

        File tempRoot = configSource.getTemporaryRootDirectory();

        for ( Iterator i = repositoriesList.iterator(); i.hasNext(); )
        {
            Repository repository = (Repository) i.next();

            resolveDependencies( repository, configSource, context );

            String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( repository.getOutputDirectory(), configSource.getProject(),
                                                        null, configSource.getFinalName(),
                                                        configSource );

            File repositoryDirectory = new File( tempRoot, outputDirectory );

            if ( !repositoryDirectory.exists() )
            {
                repositoryDirectory.mkdirs();
            }

            try
            {
                getLogger().debug( "Assembling repository to: " + repositoryDirectory );
                repositoryAssembler.buildRemoteRepository( repositoryDirectory, wrap( repository ), wrap( configSource ) );
                getLogger().debug( "Finished assembling repository to: " + repositoryDirectory );
            }
            catch ( RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assemble repository: " + e.getMessage(), e );
            }

            AddDirectoryTask task = new AddDirectoryTask( repositoryDirectory );

            int dirMode = TypeConversionUtils.modeToInt( repository.getDirectoryMode(), getLogger() );
            if ( dirMode != -1 )
            {
                task.setDirectoryMode( dirMode );
            }
            
            int fileMode = TypeConversionUtils.modeToInt( repository.getFileMode(), getLogger() );
            if ( fileMode != -1 )
            {
                task.setFileMode( fileMode );
            }
            
            task.setUseDefaultExcludes( repository.isUseDefaultExcludes() );
            task.setOutputDirectory( outputDirectory );

            task.execute( archiver, configSource );
        }
    }

    private void resolveDependencies( Repository repository, AssemblerConfigurationSource configSource,
                                      AssemblyContext context )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        ArtifactRepository localRepository = configSource.getLocalRepository();

        List additionalRemoteRepositories = configSource.getRemoteRepositories();

        Set dependencyArtifacts;
        try
        {
            // NOTE: hard-coding to resolve artifacts transitively, since this is meant to be a self-contained repository...
            dependencyArtifacts =
                dependencyResolver.resolveDependencies( project, repository.getScope(), context.getManagedVersionMap(),
                                                        localRepository, additionalRemoteRepositories, true );

            if ( ( dependencyArtifacts != null ) && !dependencyArtifacts.isEmpty() )
            {
                dependencyArtifacts = new LinkedHashSet( dependencyArtifacts );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }

        project.setDependencyArtifacts( dependencyArtifacts );
    }

    private RepositoryBuilderConfigSource wrap( AssemblerConfigurationSource configSource )
    {
        return new RepoBuilderConfigSourceWrapper( configSource );
    }

    private RepositoryInfo wrap( Repository repository )
    {
        return new RepoInfoWrapper( repository );
    }

}
