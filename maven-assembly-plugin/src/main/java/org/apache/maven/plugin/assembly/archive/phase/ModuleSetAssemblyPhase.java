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
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase" role-hint="module-sets"
 */
public class ModuleSetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement
     */
    private DependencyResolver dependencyResolver;

    public ModuleSetAssemblyPhase()
    {
        // needed for plexus
    }

    public ModuleSetAssemblyPhase( MavenProjectBuilder projectBuilder, DependencyResolver dependencyResolver,
                                   Logger logger )
    {
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;

        enableLogging( logger );
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        List moduleSets = assembly.getModuleSets();

        for ( Iterator i = moduleSets.iterator(); i.hasNext(); )
        {
            ModuleSet moduleSet = ( ModuleSet ) i.next();

            Set moduleProjects = getModuleProjects( moduleSet, configSource, moduleSet.isIncludeSubModules() );

            ModuleSources sources = moduleSet.getSources();
            ModuleBinaries binaries = moduleSet.getBinaries();

            if ( ( sources == null ) && ( binaries == null ) )
            {
                getLogger().warn( "Encountered ModuleSet with no sources or binaries specified. Skipping." );
                continue;
            }

            addModuleSourceFileSets( moduleSet.getSources(), moduleProjects, archiver, configSource );

            addModuleBinaries( moduleSet.getBinaries(), moduleProjects, archiver, configSource );
        }
    }

    protected void addModuleBinaries( ModuleBinaries binaries, Set projects, Archiver archiver,
                                      AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( binaries == null )
        {
            return;
        }

        Set moduleProjects = new HashSet( projects );

        for ( Iterator it = moduleProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = ( MavenProject ) it.next();

            if ( "pom".equals( project.getPackaging() ) )
            {
                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                getLogger().debug( "Excluding POM-packaging module: " + projectId );

                it.remove();
            }
        }

        String classifier = binaries.getAttachmentClassifier();

        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject project = ( MavenProject ) j.next();

            Artifact artifact = null;

            if ( classifier == null )
            {
                getLogger().debug( "Processing binary artifact for module project: " + project.getId() );

                artifact = project.getArtifact();
            }
            else
            {
                getLogger().debug( "Processing binary attachment: " + classifier + " for module project: " + project.getId() );

                List attachments = project.getAttachedArtifacts();
                if ( ( attachments != null ) && !attachments.isEmpty() )
                {
                    for ( Iterator attachmentIterator = attachments.iterator(); attachmentIterator.hasNext(); )
                    {
                        Artifact attachment = (Artifact) attachmentIterator.next();

                        if ( classifier.equals( attachment.getClassifier() ) )
                        {
                            artifact = attachment;
                            break;
                        }
                    }
                }

                if ( artifact == null )
                {
                    throw new InvalidAssemblerConfigurationException( "Cannot find attachment with classifier: " + classifier + " in module project: " + project.getId() + ". Please exclude this module from the module-set." );
                }
            }

            addModuleArtifact( artifact, project, archiver, configSource, binaries );
        }

        List depSets = binaries.getDependencySets();

        if ( ( ( depSets == null ) || depSets.isEmpty() ) && binaries.isIncludeDependencies() )
        {
            DependencySet impliedDependencySet = new DependencySet();

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

        if ( depSets != null )
        {
            for ( Iterator it = depSets.iterator(); it.hasNext(); )
            {
                DependencySet ds = (DependencySet) it.next();
                
                // NOTE: Disabling useProjectArtifact flag, since module artifact has already been handled!
                ds.setUseProjectArtifact( false );
            }

            // FIXME: This will produce unpredictable results when module dependencies have a version conflict.
            getLogger().warn(
                              "NOTE: Currently, inclusion of module dependencies may produce unpredictable "
                                              + "results if a version conflict occurs." );

            for ( Iterator it = moduleProjects.iterator(); it.hasNext(); )
            {
                MavenProject moduleProject = ( MavenProject ) it.next();

                getLogger().debug( "Processing binary dependencies for module project: " + moduleProject.getId() );

                AddDependencySetsTask task =
                    new AddDependencySetsTask( depSets, moduleProject, projectBuilder, dependencyResolver, getLogger() );

                task.setArtifactExpressionPrefix( "module." );
                task.setDefaultOutputDirectory( binaries.getOutputDirectory() );
                task.setDefaultOutputFileNameMapping( binaries.getOutputFileNameMapping() );

                task.execute( archiver, configSource );
            }
        }
    }

    protected List collectExcludesFromQueuedArtifacts( Set visitedArtifacts, List binaryExcludes )
    {
        List excludes = binaryExcludes;

        if ( excludes == null )
        {
            excludes = new ArrayList();
        }
        else
        {
            excludes = new ArrayList( excludes );
        }

        for ( Iterator it = visitedArtifacts.iterator(); it.hasNext(); )
        {
            excludes.add( it.next() );
        }

        return excludes;
    }

    protected void addModuleArtifact( Artifact artifact, MavenProject project, Archiver archiver,
                                AssemblerConfigurationSource configSource, ModuleBinaries binaries )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( artifact.getFile() == null )
        {
            throw new ArchiveCreationException( "Artifact: " + artifact.getId()
                            + " (included by module) does not have an artifact with a file. "
                            + "Please ensure the package phase is run before the assembly is generated." );
        }

        AddArtifactTask task = new AddArtifactTask( artifact, getLogger() );

        task.setArtifactExpressionPrefix( "module." );
        task.setFileNameMapping( binaries.getOutputFileNameMapping() );
        task.setOutputDirectory( binaries.getOutputDirectory() );
        task.setProject( project );
        task.setDirectoryMode( binaries.getDirectoryMode() );
        task.setFileMode( binaries.getFileMode() );
        task.setUnpack( binaries.isUnpack() );
        if ( binaries.isUnpack() ) {
            task.setIncludes( binaries.getUnpackOptions().getIncludes() );
            task.setExcludes( binaries.getUnpackOptions().getExcludes() );
        }

        task.execute( archiver, configSource );
    }

    protected void addModuleSourceFileSets( ModuleSources sources, Set moduleProjects, Archiver archiver,
                                            AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( sources == null )
        {
            return;
        }

        List fileSets = new ArrayList();

        if ( isDeprecatedModuleSourcesConfigPresent( sources ) )
        {
            FileSet fs = new FileSet();
            fs.setOutputDirectory( sources.getOutputDirectory() );
            fs.setIncludes( sources.getIncludes() );
            fs.setExcludes( sources.getExcludes() );
            fs.setUseDefaultExcludes( sources.isUseDefaultExcludes() );

            fileSets.add( fs );
        }

        List subFileSets = sources.getFileSets();

        if ( ( subFileSets == null ) || subFileSets.isEmpty() )
        {
            FileSet fs = new FileSet();
            fs.setDirectory( "src" );

            subFileSets = Collections.singletonList( fs );
        }

        fileSets.addAll( subFileSets );

        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject moduleProject = ( MavenProject ) j.next();

            getLogger().info( "Processing sources for module project: " + moduleProject.getId() );
            
            List moduleFileSets = new ArrayList();

            for ( Iterator fsIterator = fileSets.iterator(); fsIterator.hasNext(); )
            {
                FileSet fileSet = ( FileSet ) fsIterator.next();

                moduleFileSets.add( createFileSet( fileSet, sources, moduleProject, configSource ) );
            }
            
            AddFileSetsTask task = new AddFileSetsTask( moduleFileSets );

            task.setArtifactExpressionPrefix( "module." );
            task.setProject( moduleProject );
            task.setLogger( getLogger() );

            task.execute( archiver, configSource );
        }
    }

    /**
     * Determine whether the deprecated file-set configuration directly within the ModuleSources object is present.
     */
    protected boolean isDeprecatedModuleSourcesConfigPresent( ModuleSources sources )
    {
        boolean result = false;

        if ( sources.getOutputDirectory() != null )
        {
            result = true;
        }
        else if ( ( sources.getIncludes() != null ) && !sources.getIncludes().isEmpty() )
        {
            result = true;
        }
        else if ( ( sources.getExcludes() != null ) && !sources.getExcludes().isEmpty() )
        {
            result = true;
        }

        if ( result )
        {
            getLogger().warn(
                              "[DEPRECATION] Use of <moduleSources/> as a file-set is deprecated. "
                                              + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
        }
        else if ( !sources.isUseDefaultExcludes() )
        {
            getLogger().warn(
                              "[DEPRECATION] Use of directoryMode, fileMode, or useDefaultExcludes "
                                              + "elements directly within <moduleSources/> are all deprecated. "
                                              + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
        }

        return result;
    }

    protected FileSet createFileSet( FileSet fileSet, ModuleSources sources, MavenProject moduleProject, AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String sourcePath = fileSet.getDirectory();

        File moduleBasedir = moduleProject.getBasedir();

        if ( sourcePath != null )
        {
            File sourceDir = new File( sourcePath );

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

        List excludes = new ArrayList();

        List originalExcludes = fileSet.getExcludes();
        if ( ( originalExcludes != null ) && !originalExcludes.isEmpty() )
        {
            excludes.addAll( originalExcludes );
        }

        if ( sources.isExcludeSubModuleDirectories() )
        {
            List modules = moduleProject.getModules();
            for ( Iterator moduleIterator = modules.iterator(); moduleIterator.hasNext(); )
            {
                String moduleSubPath = ( String ) moduleIterator.next();

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
                                                             moduleProject.getArtifact(), configSource.getProject(), moduleProject, "module." );

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

        destPath = AssemblyFormatUtils.getOutputDirectory( destPath, configSource.getProject(), moduleProject, configSource.getFinalName(), "module." );

        fs.setOutputDirectory( destPath );

        getLogger().debug( "module source directory is: " + sourcePath );
        getLogger().debug( "module dest directory is: " + destPath + " (assembly basedir may be prepended)" );

        return fs;
    }

    protected Set getModuleProjects( ModuleSet moduleSet, AssemblerConfigurationSource configSource,
                                     boolean includeSubModules )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        Set moduleProjects;
        try
        {
            moduleProjects =
                ProjectUtils.getProjectModules( project, configSource.getReactorProjects(), includeSubModules,
                                                getLogger() );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId() + ": "
                            + e.getMessage(), e );
        }

        FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), true, getLogger() );
        return moduleProjects;
    }

}
