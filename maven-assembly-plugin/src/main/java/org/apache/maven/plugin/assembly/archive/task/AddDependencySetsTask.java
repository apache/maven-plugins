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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.UnpackOptions;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.logging.Logger;

/**
 * @version $Id$
 */
public class AddDependencySetsTask
    implements ArchiverTask
{

    private static final List<String> NON_ARCHIVE_DEPENDENCY_TYPES;

    static
    {
        final List<String> nonArch = new ArrayList<String>();

        nonArch.add( "pom" );

        NON_ARCHIVE_DEPENDENCY_TYPES = Collections.unmodifiableList( nonArch );
    }

    private final List<DependencySet> dependencySets;

    private final Logger logger;

    private final MavenProject project;

    private MavenProject moduleProject;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    private Artifact moduleArtifact;

    private final Set<Artifact> resolvedArtifacts;

    private final ArchiverManager archiverManager;

    public AddDependencySetsTask( final List<DependencySet> dependencySets, final Set<Artifact> resolvedArtifacts,
                                  final MavenProject project, final MavenProjectBuilder projectBuilder,
                                  final ArchiverManager archiverManager, final Logger logger )
    {
        this.dependencySets = dependencySets;
        this.resolvedArtifacts = resolvedArtifacts;
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.archiverManager = archiverManager;
        this.logger = logger;
    }

    public void execute( final Archiver archiver, final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( ( dependencySets == null ) || dependencySets.isEmpty() )
        {
            logger.debug( "No dependency sets specified." );
            return;
        }

        @SuppressWarnings( "unchecked" )
        final List<Dependency> deps = project.getDependencies();
        if ( ( deps == null ) || deps.isEmpty() )
        {
            logger.debug( "Project " + project.getId() + " has no dependencies. Skipping dependency set addition." );
        }

        for (final DependencySet dependencySet : dependencySets) {
            addDependencySet(dependencySet, archiver, configSource);
        }
    }

    protected void addDependencySet( final DependencySet dependencySet, final Archiver archiver,
                                     final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        logger.debug( "Processing DependencySet (output=" + dependencySet.getOutputDirectory() + ")" );

        if ( !dependencySet.isUseTransitiveDependencies() && dependencySet.isUseTransitiveFiltering() )
        {
            logger.warn( "DependencySet has nonsensical configuration: useTransitiveDependencies == false "
                            + "AND useTransitiveFiltering == true. Transitive filtering flag will be ignored." );
        }

        final Set<Artifact> dependencyArtifacts = resolveDependencyArtifacts( dependencySet );

        boolean filterContents = false;
        final UnpackOptions opts = dependencySet.getUnpackOptions();
        if ( dependencySet.isUnpack() && opts != null && ( opts.isFiltered() || opts.getLineEnding() != null ) )
        {
            filterContents = true;
        }
        else if ( dependencyArtifacts.size() > 1 )
        {
            checkMultiArtifactOutputConfig( dependencySet );
        }

        logger.debug( "Adding " + dependencyArtifacts.size() + " dependency artifacts." );

        for (final Artifact depArtifact : dependencyArtifacts) {
            MavenProject depProject;
            try {
                depProject =
                        projectBuilder.buildFromRepository(depArtifact, configSource.getRemoteRepositories(),
                                configSource.getLocalRepository());
            } catch (final ProjectBuildingException e) {
                logger.debug("Error retrieving POM of module-dependency: " + depArtifact.getId() + "; Reason: "
                        + e.getMessage() + "\n\nBuilding stub project instance.");

                depProject = buildProjectStub(depArtifact);
            }

            if (NON_ARCHIVE_DEPENDENCY_TYPES.contains(depArtifact.getType())) {
                addNonArchiveDependency(depArtifact, depProject, dependencySet, archiver, configSource);
            } else {
                if (filterContents) {
                    addFilteredUnpackedArtifact(dependencySet, depArtifact, depProject, archiver, configSource);
                } else {
                    addNormalArtifact(dependencySet, depArtifact, depProject, archiver, configSource);
                }
            }
        }
    }

    private void checkMultiArtifactOutputConfig( final DependencySet dependencySet )
    {
        String dir = dependencySet.getOutputDirectory();
        if ( dir == null )
        {
            dir = defaultOutputDirectory;
        }

        String mapping = dependencySet.getOutputFileNameMapping();
        if ( mapping == null )
        {
            mapping = defaultOutputFileNameMapping;
        }

        if ( ( dir == null || !dir.contains("${")) && ( mapping == null || !mapping.contains("${")) )
        {
            logger.warn( "NOTE: Your assembly specifies a dependencySet that matches multiple artifacts, but specifies a concrete output format. "
                            + "THIS MAY RESULT IN ONE OR MORE ARTIFACTS BEING OBSCURED!\n\nOutput directory: '"
                            + dir
                            + "'\nOutput filename mapping: '" + mapping + "'" );
        }
    }

    private void addFilteredUnpackedArtifact( final DependencySet dependencySet, final Artifact depArtifact,
                                              final MavenProject depProject, final Archiver archiver,
                                              final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        logger.debug( "Adding dependency artifact " + depArtifact.getId() + " after filtering the unpacked contents." );

        final StringBuilder sb =
            new StringBuilder().append( depArtifact.getGroupId() )
                               .append( "_" )
                               .append( depArtifact.getArtifactId() )
                               .append( "_" )
                               .append( depArtifact.getVersion() );

        final String classifier = depArtifact.getClassifier();
        if ( classifier != null )
        {
            sb.append( "_" ).append( classifier );
        }

        sb.append( "." ).append( depArtifact.getType() );

        final File dir = new File( configSource.getWorkingDirectory(), sb.toString() );
        if ( dir.exists() )
        {
            logger.debug( "NOT unpacking: " + depArtifact.getId() + ". Directory already exists in workdir:\n\t"
                            + dir.getAbsolutePath() );
        }
        else
        {
            dir.mkdirs();

            UnArchiver unarchiver;
            try
            {
                unarchiver = archiverManager.getUnArchiver( depArtifact.getFile() );
            }
            catch ( final NoSuchArchiverException e )
            {
                throw new ArchiveCreationException( "Failed to retrieve un-archiver for: " + depArtifact.getId()
                                + ". Dependency filtering cannot proceed.", e );
            }

            unarchiver.setDestDirectory( dir );
            unarchiver.setOverwrite( true );
            unarchiver.setSourceFile( depArtifact.getFile() );
            unarchiver.setIgnorePermissions( configSource.isIgnorePermissions() );

            try
            {
                unarchiver.extract();
            }
            catch ( final ArchiverException e )
            {
                throw new ArchiveCreationException( "Failed to unpack dependency archive: " + depArtifact.getId()
                                + ". Dependency filtering cannot proceed.", e );
            }
        }

        final UnpackOptions opts = dependencySet.getUnpackOptions();

        final FileSet fs = new FileSet();
        fs.setDirectory( dir.getAbsolutePath() );
        fs.setDirectoryMode( dependencySet.getDirectoryMode() );
        fs.setExcludes( opts.getExcludes() );
        fs.setFileMode( dependencySet.getFileMode() );
        fs.setFiltered( opts.isFiltered() );
        fs.setIncludes( opts.getIncludes() );

        String outDir = dependencySet.getOutputDirectory();
        if ( outDir == null )
        {
            outDir = defaultOutputDirectory;
        }

        String filenameMapping = dependencySet.getOutputFileNameMapping();
        if ( filenameMapping == null )
        {
            filenameMapping = defaultOutputFileNameMapping;
        }

        filenameMapping =
            AssemblyFormatUtils.evaluateFileNameMapping( filenameMapping, depArtifact, configSource.getProject(),
                                                         moduleProject, moduleArtifact, depProject, configSource );

        final String outputLocation = new File( outDir, filenameMapping ).getPath();

        fs.setOutputDirectory( outputLocation );

        fs.setLineEnding( opts.getLineEnding() );
        fs.setUseDefaultExcludes( opts.isUseDefaultExcludes() );

        final AddFileSetsTask task = new AddFileSetsTask( fs );
        task.setProject( depProject );
        task.setModuleProject( moduleProject );
        task.setLogger( logger );

        task.execute( archiver, configSource );
    }

    private void addNormalArtifact( final DependencySet dependencySet, final Artifact depArtifact,
                                    final MavenProject depProject, final Archiver archiver,
                                    final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        logger.debug( "Adding dependency artifact " + depArtifact.getId() + "." );

        final AddArtifactTask task = new AddArtifactTask( depArtifact, logger );

        task.setProject( depProject );
        task.setModuleProject( moduleProject );
        task.setModuleArtifact( moduleArtifact );
        task.setOutputDirectory( dependencySet.getOutputDirectory(), defaultOutputDirectory );
        task.setFileNameMapping( dependencySet.getOutputFileNameMapping(), defaultOutputFileNameMapping );

        final int dirMode = TypeConversionUtils.modeToInt( dependencySet.getDirectoryMode(), logger );
        if ( dirMode != -1 )
        {
            task.setDirectoryMode( dirMode );
        }

        final int fileMode = TypeConversionUtils.modeToInt( dependencySet.getFileMode(), logger );
        if ( fileMode != -1 )
        {
            task.setFileMode( fileMode );
        }

        task.setUnpack( dependencySet.isUnpack() );

        final UnpackOptions opts = dependencySet.getUnpackOptions();
        if ( dependencySet.isUnpack() && ( opts != null ) )
        {
            task.setIncludes( opts.getIncludes() );
            task.setExcludes( opts.getExcludes() );
        }

        task.execute( archiver, configSource );
    }

    private MavenProject buildProjectStub( final Artifact depArtifact )
    {
        final Model model = new Model();
        model.setGroupId( depArtifact.getGroupId() );
        model.setArtifactId( depArtifact.getArtifactId() );
        model.setVersion( depArtifact.getBaseVersion() );
        model.setPackaging( depArtifact.getType() );

        model.setDescription( "Stub for " + depArtifact.getId() );

        final MavenProject project = new MavenProject( model );
        project.setArtifact( depArtifact );

        return project;
    }

    protected Set<Artifact> resolveDependencyArtifacts( final DependencySet dependencySet )
        throws InvalidAssemblerConfigurationException
    {
        final Set<Artifact> dependencyArtifacts = new LinkedHashSet<Artifact>();
        if ( resolvedArtifacts != null )
        {
            dependencyArtifacts.addAll( resolvedArtifacts );
        }

        if ( dependencySet.isUseProjectArtifact() )
        {
            final Artifact projectArtifact = project.getArtifact();
            if ( ( projectArtifact != null ) && ( projectArtifact.getFile() != null ) )
            {
                dependencyArtifacts.add( projectArtifact );
            }
            else
            {
                logger.warn( "Cannot include project artifact: " + projectArtifact
                                + "; it doesn't have an associated file or directory." );
            }
        }

        if ( dependencySet.isUseProjectAttachments() )
        {
            @SuppressWarnings( "unchecked" )
            final List<Artifact> attachments = project.getAttachedArtifacts();
            if ( attachments != null )
            {
                for (final Artifact attachment : attachments) {
                    if (attachment.getFile() != null) {
                        dependencyArtifacts.add(attachment);
                    } else {
                        logger.warn("Cannot include attached artifact: " + project.getId() + " for project: "
                                + project.getId() + "; it doesn't have an associated file or directory.");
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

        final ScopeArtifactFilter filter = new ScopeArtifactFilter( dependencySet.getScope() );

        FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                                     dependencySet.isUseStrictFiltering(), dependencySet.isUseTransitiveFiltering(),
                                     logger, filter );

        return dependencyArtifacts;
    }

    protected void addNonArchiveDependency( final Artifact depArtifact, final MavenProject depProject,
                                            final DependencySet dependencySet, final Archiver archiver,
                                            final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        final File source = depArtifact.getFile();

        String outputDirectory = dependencySet.getOutputDirectory();

        outputDirectory =
            AssemblyFormatUtils.getOutputDirectory( outputDirectory, configSource.getProject(), moduleProject,
                                                    depProject, depProject.getBuild().getFinalName(), configSource );

        final String destName =
            AssemblyFormatUtils.evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(), depArtifact,
                                                         configSource.getProject(), moduleProject, moduleArtifact,
                                                         depProject, configSource );

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
            final int mode = TypeConversionUtils.modeToInt( dependencySet.getFileMode(), logger );
            if ( mode > -1 )
            {
                archiver.addFile( source, target, mode );
            }
            else
            {
                archiver.addFile( source, target );
            }
        }
        catch ( final ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
        }
    }

    public List<DependencySet> getDependencySets()
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

    public void setDefaultOutputDirectory( final String defaultOutputDirectory )
    {
        this.defaultOutputDirectory = defaultOutputDirectory;
    }

    public String getDefaultOutputFileNameMapping()
    {
        return defaultOutputFileNameMapping;
    }

    public void setDefaultOutputFileNameMapping( final String defaultOutputFileNameMapping )
    {
        this.defaultOutputFileNameMapping = defaultOutputFileNameMapping;
    }

    public MavenProject getModuleProject()
    {
        return moduleProject;
    }

    public void setModuleProject( final MavenProject moduleProject )
    {
        this.moduleProject = moduleProject;
    }

    public void setModuleArtifact( final Artifact moduleArtifact )
    {
        this.moduleArtifact = moduleArtifact;
    }

    public Artifact getModuleArtifact()
    {
        return moduleArtifact;
    }
}
