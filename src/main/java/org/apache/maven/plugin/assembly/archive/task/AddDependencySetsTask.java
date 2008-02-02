package org.apache.maven.plugin.assembly.archive.task;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.UnpackOptions;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
public class AddDependencySetsTask
    implements ArchiverTask
{

    private static final List NON_ARCHIVE_DEPENDENCY_TYPES;

    static
    {
        List nonArch = new ArrayList();

        nonArch.add( "pom" );

        NON_ARCHIVE_DEPENDENCY_TYPES = Collections.unmodifiableList( nonArch );
    }

    private final List dependencySets;

    private final Logger logger;

    private final MavenProject project;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    private final DependencyResolver dependencyResolver;

    private String artifactExpressionPrefix = "artifact.";

    public AddDependencySetsTask( List dependencySets, MavenProject project, MavenProjectBuilder projectBuilder,
                                  DependencyResolver dependencyResolver, Logger logger )
    {
        this.dependencySets = dependencySets;
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
        this.logger = logger;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( ( dependencySets == null ) || dependencySets.isEmpty() )
        {
            logger.debug( "No dependency sets specified." );
            return;
        }

        List deps = project.getDependencies();
        if ( ( deps == null ) || deps.isEmpty() )
        {
            logger.debug( "Project " + project.getId() + " has no dependencies. Skipping dependency set addition." );
        }

        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = (DependencySet) i.next();

            addDependencySet( dependencySet, archiver, configSource );
        }
    }

    protected void addDependencySet( DependencySet dependencySet, Archiver archiver,
                                     AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        logger.info( "Processing DependencySet (output=" + dependencySet.getOutputDirectory() + ")" );

        if ( !dependencySet.isUseTransitiveDependencies() && dependencySet.isUseTransitiveFiltering() )
        {
            logger.warn( "DependencySet has nonsensical configuration: useTransitiveDependencies == false "
                         + "AND useTransitiveFiltering == true. Transitive filtering flag will be ignored." );
        }

        Set dependencyArtifacts = resolveDependencyArtifacts( dependencySet, configSource );

        logger.debug( "Adding " + dependencyArtifacts.size() + " dependency artifacts." );

        for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
        {
            Artifact depArtifact = (Artifact) j.next();

            MavenProject depProject;
            try
            {
                depProject = projectBuilder.buildFromRepository( depArtifact, configSource.getRemoteRepositories(),
                                                                 configSource.getLocalRepository(), true );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArchiveCreationException( "Error retrieving POM of module-dependency: " + depArtifact.getId()
                    + "; Reason: " + e.getMessage(), e );
            }

            if ( NON_ARCHIVE_DEPENDENCY_TYPES.contains( depArtifact.getType() ) )
            {
                addNonArchiveDependency( depArtifact, depProject, dependencySet, archiver, configSource );
            }
            else
            {
                AddArtifactTask task = new AddArtifactTask( depArtifact, logger );

                task.setProject( depProject );
                task.setArtifactExpressionPrefix( artifactExpressionPrefix );
                task.setOutputDirectory( dependencySet.getOutputDirectory(), defaultOutputDirectory );
                task.setFileNameMapping( dependencySet.getOutputFileNameMapping(), defaultOutputFileNameMapping );
                task.setDirectoryMode( dependencySet.getDirectoryMode() );
                task.setFileMode( dependencySet.getFileMode() );
                task.setUnpack( dependencySet.isUnpack() );

                UnpackOptions opts = dependencySet.getUnpackOptions();
                if ( dependencySet.isUnpack() && ( opts != null ) )
                {
                    task.setIncludes( opts.getIncludes() );
                    task.setExcludes( opts.getExcludes() );
                }

                task.execute( archiver, configSource );
            }
        }
    }

    protected Set resolveDependencyArtifacts( DependencySet dependencySet, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        ArtifactRepository localRepository = configSource.getLocalRepository();

        List additionalRemoteRepositories = configSource.getRemoteRepositories();

        Set dependencyArtifacts;
        try
        {
            dependencyArtifacts = dependencyResolver.resolveDependencies( project, dependencySet.getScope(),
                                                                          localRepository,
                                                                          additionalRemoteRepositories,
                                                                          dependencySet.isUseTransitiveDependencies() );

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

        if ( dependencySet.isUseProjectArtifact() )
        {
            Artifact projectArtifact = project.getArtifact();
            if ( ( projectArtifact != null ) && ( projectArtifact.getFile() != null ) )
            {
                dependencyArtifacts.add( projectArtifact );
            }
            else
            {
                logger.warn( "Cannot include project artifact: " + projectArtifact + "; it doesn't have an associated file or directory." );
            }
        }

        if ( dependencySet.isUseProjectAttachments() )
        {
            List attachments = project.getAttachedArtifacts();
            if ( attachments != null )
            {
                for ( Iterator attachmentIt = attachments.iterator(); attachmentIt.hasNext(); )
                {
                    Artifact attachment = (Artifact) attachmentIt.next();

                    if ( attachment.getFile() != null )
                    {
                        dependencyArtifacts.add( attachment );
                    }
                    else
                    {
                        logger.warn( "Cannot include attached artifact: " + project.getId() + " for project: "
                                     + project.getId() + "; it doesn't have an associated file or directory." );
                    }
                }
            }
        }

        if ( dependencySet.isUseTransitiveFiltering() )
        {
            logger.debug( "Filtering dependency artifacts USING transitive dependency path information." );
        }
        else
        {
            logger.debug( "Filtering dependency artifacts WITHOUT transitive dependency path information." );
        }

        FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                                     dependencySet.isUseStrictFiltering(), dependencySet.isUseTransitiveFiltering(),
                                     Collections.EMPTY_LIST, logger );

        return dependencyArtifacts;
    }

    protected void addNonArchiveDependency( Artifact depArtifact, MavenProject depProject, DependencySet dependencySet,
                                            Archiver archiver, AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        File source = depArtifact.getFile();

        String outputDirectory = dependencySet.getOutputDirectory();

        outputDirectory = AssemblyFormatUtils.getOutputDirectory( outputDirectory, configSource.getProject(),
                                                                  depProject, depProject.getBuild().getFinalName(),
                                                                  artifactExpressionPrefix );

        String destName = AssemblyFormatUtils.evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(),
                                                                       depArtifact, configSource.getProject(),
                                                                       depProject, artifactExpressionPrefix );

        String target;

        // omit the last char if ends with / or \\
        if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
        {
            target = outputDirectory + destName;
        }
        else
        {
            target = outputDirectory + "/" + destName;
        }

        try
        {
            int mode = TypeConversionUtils.modeToInt( dependencySet.getFileMode(), logger );
            if ( mode > -1 )
            {
                archiver.addFile( source, target, mode );
            }
            else
            {
                archiver.addFile( source, target );
            }
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
        }
    }

    public List getDependencySets()
    {
        return dependencySets;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public String getDefaultOutputDirectory()
    {
        return defaultOutputDirectory;
    }

    public void setDefaultOutputDirectory( String defaultOutputDirectory )
    {
        this.defaultOutputDirectory = defaultOutputDirectory;
    }

    public String getDefaultOutputFileNameMapping()
    {
        return defaultOutputFileNameMapping;
    }

    public void setDefaultOutputFileNameMapping( String defaultOutputFileNameMapping )
    {
        this.defaultOutputFileNameMapping = defaultOutputFileNameMapping;
    }

    public void setArtifactExpressionPrefix( String artifactExpressionPrefix )
    {
        this.artifactExpressionPrefix = artifactExpressionPrefix;
    }
}
