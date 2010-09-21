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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.ModuleBinaries;
import org.apache.maven.plugin.assembly.model.ModuleSet;
import org.apache.maven.plugin.assembly.model.ModuleSources;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the &lt;moduleSets/&gt; top-level section of the assembly descriptor.
 * 
 * @version $Id$
 */
@Component( role = AssemblyArchiverPhase.class, hint = "module-sets" )
public class ModuleSetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    @Requirement
    private MavenProjectBuilder projectBuilder;

    public ModuleSetAssemblyPhase()
    {
        // needed for plexus
    }

    public ModuleSetAssemblyPhase( final MavenProjectBuilder projectBuilder, final Logger logger )
    {
        this.projectBuilder = projectBuilder;
        enableLogging( logger );
    }

    /**
     * {@inheritDoc}
     */
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource, final AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final List<ModuleSet> moduleSets = assembly.getModuleSets();

        for ( final Iterator<ModuleSet> i = moduleSets.iterator(); i.hasNext(); )
        {
            final ModuleSet moduleSet = i.next();

            validate( moduleSet, configSource );

            final Set<MavenProject> moduleProjects = getModuleProjects( moduleSet, configSource, getLogger() );

            final ModuleSources sources = moduleSet.getSources();
            addModuleSourceFileSets( sources, moduleProjects, archiver, configSource );

            final ModuleBinaries binaries = moduleSet.getBinaries();
            addModuleBinaries( binaries, moduleProjects, archiver, configSource, context );
        }
    }

    private void validate( final ModuleSet moduleSet, final AssemblerConfigurationSource configSource )
    {
        if ( ( moduleSet.getSources() == null ) && ( moduleSet.getBinaries() == null ) )
        {
            getLogger().warn( "Encountered ModuleSet with no sources or binaries specified. Skipping." );
        }

        if ( moduleSet.isUseAllReactorProjects() && !moduleSet.isIncludeSubModules() )
        {
            getLogger().warn( "includeSubModules == false is incompatible with useAllReactorProjects. Ignoring."
                                              + "\n\nTo refactor, remove the <includeSubModules/> flag, and use the <includes/> "
                                              + "and <excludes/> sections to fine-tune the modules included." );
        }

        final List<MavenProject> projects = configSource.getReactorProjects();
        if ( projects != null && projects.size() > 1 && projects.indexOf( configSource.getProject() ) == 0
                        && moduleSet.getBinaries() != null )
        {
            getLogger().warn( "[DEPRECATION] moduleSet/binaries section detected in root-project assembly."
                                              + "\n\nMODULE BINARIES MAY NOT BE AVAILABLE FOR THIS ASSEMBLY!"
                                              + "\n\n To refactor, move this assembly into a child project and use the flag "
                                              + "<useAllReactorProjects>true</useAllReactorProjects> in each moduleSet." );
        }

        if ( moduleSet.getSources() != null )
        {
            final ModuleSources sources = moduleSet.getSources();
            if ( isDeprecatedModuleSourcesConfigPresent( sources ) )
            {
                getLogger().warn( "[DEPRECATION] Use of <moduleSources/> as a file-set is deprecated. "
                                                  + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
            }
            else if ( !sources.isUseDefaultExcludes() )
            {
                getLogger().warn( "[DEPRECATION] Use of directoryMode, fileMode, or useDefaultExcludes "
                                                  + "elements directly within <moduleSources/> are all deprecated. "
                                                  + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
            }
        }
    }

    protected void addModuleBinaries( final ModuleBinaries binaries, final Set<MavenProject> projects,
                                      final Archiver archiver, final AssemblerConfigurationSource configSource,
                                      final AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( binaries == null )
        {
            return;
        }

        final Set<MavenProject> moduleProjects = new HashSet<MavenProject>( projects );

        for ( final Iterator<MavenProject> it = moduleProjects.iterator(); it.hasNext(); )
        {
            final MavenProject project = it.next();

            if ( "pom".equals( project.getPackaging() ) )
            {
                final String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                getLogger().debug( "Excluding POM-packaging module: " + projectId );

                it.remove();
            }
        }

        final String classifier = binaries.getAttachmentClassifier();

        final Map<MavenProject, Artifact> chosenModuleArtifacts = new HashMap<MavenProject, Artifact>();

        for ( final Iterator<MavenProject> j = moduleProjects.iterator(); j.hasNext(); )
        {
            final MavenProject project = j.next();

            Artifact artifact = null;

            if ( classifier == null )
            {
                getLogger().debug( "Processing binary artifact for module project: " + project.getId() );

                artifact = project.getArtifact();
            }
            else
            {
                getLogger().debug( "Processing binary attachment: " + classifier + " for module project: "
                                                   + project.getId() );

                @SuppressWarnings( "unchecked" )
                final List<Artifact> attachments = project.getAttachedArtifacts();
                if ( ( attachments != null ) && !attachments.isEmpty() )
                {
                    for ( final Iterator<Artifact> attachmentIterator = attachments.iterator(); attachmentIterator.hasNext(); )
                    {
                        final Artifact attachment = attachmentIterator.next();

                        if ( classifier.equals( attachment.getClassifier() ) )
                        {
                            artifact = attachment;
                            break;
                        }
                    }
                }

                if ( artifact == null )
                {
                    throw new InvalidAssemblerConfigurationException( "Cannot find attachment with classifier: "
                                    + classifier + " in module project: " + project.getId()
                                    + ". Please exclude this module from the module-set." );
                }
            }

            chosenModuleArtifacts.put( project, artifact );
            addModuleArtifact( artifact, project, archiver, configSource, binaries );
        }

        final List<DependencySet> depSets = getDependencySets( binaries );

        if ( depSets != null )
        {
            for ( final Iterator<DependencySet> it = depSets.iterator(); it.hasNext(); )
            {
                final DependencySet ds = it.next();

                // NOTE: Disabling useProjectArtifact flag, since module artifact has already been handled!
                ds.setUseProjectArtifact( false );
            }

            // FIXME: This will produce unpredictable results when module dependencies have a version conflict.
            getLogger().warn( "NOTE: Currently, inclusion of module dependencies may produce unpredictable "
                                              + "results if a version conflict occurs." );

            for ( final Iterator<MavenProject> it = moduleProjects.iterator(); it.hasNext(); )
            {
                final MavenProject moduleProject = it.next();

                getLogger().debug( "Processing binary dependencies for module project: " + moduleProject.getId() );

                final AddDependencySetsTask task =
                    new AddDependencySetsTask( depSets, context.getResolvedArtifacts(), moduleProject, projectBuilder,
                                               getLogger() );

                task.setModuleProject( moduleProject );
                task.setModuleArtifact( chosenModuleArtifacts.get( moduleProject ) );
                task.setDefaultOutputDirectory( binaries.getOutputDirectory() );
                task.setDefaultOutputFileNameMapping( binaries.getOutputFileNameMapping() );

                task.execute( archiver, configSource );
            }
        }
    }

    public static List<DependencySet> getDependencySets( final ModuleBinaries binaries )
    {
        List<DependencySet> depSets = binaries.getDependencySets();

        if ( ( ( depSets == null ) || depSets.isEmpty() ) && binaries.isIncludeDependencies() )
        {
            final DependencySet impliedDependencySet = new DependencySet();

            impliedDependencySet.setOutputDirectory( binaries.getOutputDirectory() );
            impliedDependencySet.setOutputFileNameMapping( binaries.getOutputFileNameMapping() );
            impliedDependencySet.setFileMode( binaries.getFileMode() );
            impliedDependencySet.setDirectoryMode( binaries.getDirectoryMode() );
            impliedDependencySet.setExcludes( binaries.getExcludes() );
            impliedDependencySet.setIncludes( binaries.getIncludes() );
            impliedDependencySet.setUnpack( binaries.isUnpack() );
            // unpackOptions is handled in the first stage of dependency-set handling, below.

            depSets = Collections.singletonList( impliedDependencySet );
        }

        return depSets;
    }

    // protected List<String> collectExcludesFromQueuedArtifacts( final Set visitedArtifacts, final List binaryExcludes
    // )
    // {
    // List excludes = binaryExcludes;
    //
    // if ( excludes == null )
    // {
    // excludes = new ArrayList();
    // }
    // else
    // {
    // excludes = new ArrayList( excludes );
    // }
    //
    // for ( final Iterator it = visitedArtifacts.iterator(); it.hasNext(); )
    // {
    // excludes.add( it.next() );
    // }
    //
    // return excludes;
    // }

    protected void addModuleArtifact( final Artifact artifact, final MavenProject project, final Archiver archiver,
                                      final AssemblerConfigurationSource configSource, final ModuleBinaries binaries )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( artifact.getFile() == null )
        {
            throw new ArchiveCreationException( "Artifact: " + artifact.getId()
                            + " (included by module) does not have an artifact with a file. "
                            + "Please ensure the package phase is run before the assembly is generated." );
        }

        final AddArtifactTask task = new AddArtifactTask( artifact, getLogger() );

        task.setFileNameMapping( binaries.getOutputFileNameMapping() );
        task.setOutputDirectory( binaries.getOutputDirectory() );
        task.setProject( project );
        task.setModuleProject( project );
        task.setModuleArtifact( artifact );

        final int dirMode = TypeConversionUtils.modeToInt( binaries.getDirectoryMode(), getLogger() );
        if ( dirMode != -1 )
        {
            task.setDirectoryMode( dirMode );
        }

        final int fileMode = TypeConversionUtils.modeToInt( binaries.getFileMode(), getLogger() );
        if ( fileMode != -1 )
        {
            task.setFileMode( fileMode );
        }

        task.setUnpack( binaries.isUnpack() );

        if ( binaries.isUnpack() && binaries.getUnpackOptions() != null )
        {
            task.setIncludes( binaries.getUnpackOptions()
                                      .getIncludes() );
            task.setExcludes( binaries.getUnpackOptions()
                                      .getExcludes() );
        }

        task.execute( archiver, configSource );
    }

    protected void addModuleSourceFileSets( final ModuleSources sources, final Set<MavenProject> moduleProjects,
                                            final Archiver archiver, final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( sources == null )
        {
            return;
        }

        final List<FileSet> fileSets = new ArrayList<FileSet>();

        if ( isDeprecatedModuleSourcesConfigPresent( sources ) )
        {
            final FileSet fs = new FileSet();
            fs.setOutputDirectory( sources.getOutputDirectory() );
            fs.setIncludes( sources.getIncludes() );
            fs.setExcludes( sources.getExcludes() );
            fs.setUseDefaultExcludes( sources.isUseDefaultExcludes() );

            fileSets.add( fs );
        }

        List<FileSet> subFileSets = sources.getFileSets();

        if ( ( subFileSets == null ) || subFileSets.isEmpty() )
        {
            final FileSet fs = new FileSet();
            fs.setDirectory( "src" );

            subFileSets = Collections.singletonList( fs );
        }

        fileSets.addAll( subFileSets );

        for ( final Iterator<MavenProject> j = moduleProjects.iterator(); j.hasNext(); )
        {
            final MavenProject moduleProject = j.next();

            getLogger().info( "Processing sources for module project: " + moduleProject.getId() );

            final List<FileSet> moduleFileSets = new ArrayList<FileSet>();

            for ( final Iterator<FileSet> fsIterator = fileSets.iterator(); fsIterator.hasNext(); )
            {
                final FileSet fileSet = fsIterator.next();

                moduleFileSets.add( createFileSet( fileSet, sources, moduleProject, configSource ) );
            }

            final AddFileSetsTask task = new AddFileSetsTask( moduleFileSets );

            task.setProject( moduleProject );
            task.setModuleProject( moduleProject );
            task.setLogger( getLogger() );

            task.execute( archiver, configSource );
        }
    }

    /**
     * Determine whether the deprecated file-set configuration directly within the ModuleSources object is present.
     */
    protected boolean isDeprecatedModuleSourcesConfigPresent( final ModuleSources sources )
    {
        boolean result = false;

        if ( sources.getOutputDirectory() != null )
        {
            result = true;
        }
        else if ( ( sources.getIncludes() != null ) && !sources.getIncludes()
                                                               .isEmpty() )
        {
            result = true;
        }
        else if ( ( sources.getExcludes() != null ) && !sources.getExcludes()
                                                               .isEmpty() )
        {
            result = true;
        }

        return result;
    }

    protected FileSet createFileSet( final FileSet fileSet, final ModuleSources sources,
                                     final MavenProject moduleProject, final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        final FileSet fs = new FileSet();

        String sourcePath = fileSet.getDirectory();

        final File moduleBasedir = moduleProject.getBasedir();

        if ( sourcePath != null )
        {
            final File sourceDir = new File( sourcePath );

            if ( !sourceDir.isAbsolute() )
            {
                sourcePath = new File( moduleBasedir, sourcePath ).getAbsolutePath();
            }
        }
        else
        {
            sourcePath = moduleBasedir.getAbsolutePath();
        }

        fs.setDirectory( sourcePath );
        fs.setDirectoryMode( fileSet.getDirectoryMode() );

        final List<String> excludes = new ArrayList<String>();

        final List<String> originalExcludes = fileSet.getExcludes();
        if ( ( originalExcludes != null ) && !originalExcludes.isEmpty() )
        {
            excludes.addAll( originalExcludes );
        }

        if ( sources.isExcludeSubModuleDirectories() )
        {
            @SuppressWarnings( "unchecked" )
            final List<String> modules = moduleProject.getModules();
            for ( final Iterator<String> moduleIterator = modules.iterator(); moduleIterator.hasNext(); )
            {
                final String moduleSubPath = moduleIterator.next();

                excludes.add( moduleSubPath + "/**" );
            }
        }

        fs.setExcludes( excludes );
        fs.setFiltered( fileSet.isFiltered() );
        fs.setFileMode( fileSet.getFileMode() );
        fs.setIncludes( fileSet.getIncludes() );
        fs.setLineEnding( fileSet.getLineEnding() );

        String destPathPrefix = "";
        if ( sources.isIncludeModuleDirectory() )
        {
            destPathPrefix =
                AssemblyFormatUtils.evaluateFileNameMapping( sources.getOutputDirectoryMapping(),
                                                             moduleProject.getArtifact(), configSource.getProject(),
                                                             moduleProject, moduleProject.getArtifact(), moduleProject,
                                                             configSource );

            if ( !destPathPrefix.endsWith( "/" ) )
            {
                destPathPrefix += "/";
            }
        }

        String destPath = fileSet.getOutputDirectory();

        if ( destPath == null )
        {
            destPath = destPathPrefix;
        }
        else
        {
            destPath = destPathPrefix + destPath;
        }

        destPath =
            AssemblyFormatUtils.getOutputDirectory( destPath, configSource.getProject(), moduleProject, moduleProject,
                                                    configSource.getFinalName(), configSource );

        fs.setOutputDirectory( destPath );

        getLogger().debug( "module source directory is: " + sourcePath );
        getLogger().debug( "module dest directory is: " + destPath + " (assembly basedir may be prepended)" );

        return fs;
    }

    public static Set<MavenProject> getModuleProjects( final ModuleSet moduleSet,
                                                       final AssemblerConfigurationSource configSource,
                                                       final Logger logger )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();
        Set<MavenProject> moduleProjects = null;

        if ( moduleSet.isUseAllReactorProjects() )
        {
            if ( !moduleSet.isIncludeSubModules() )
            {
                moduleProjects = new HashSet<MavenProject>( configSource.getReactorProjects() );
            }

            project = configSource.getReactorProjects()
                                  .get( 0 );
        }

        if ( moduleProjects == null )
        {
            try
            {
                moduleProjects =
                    ProjectUtils.getProjectModules( project, configSource.getReactorProjects(),
                                                    moduleSet.isIncludeSubModules(), logger );
            }
            catch ( final IOException e )
            {
                throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId()
                                + ": " + e.getMessage(), e );
            }
        }

        FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), true, logger );
        return moduleProjects;
    }

}
