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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.ModuleBinaries;
import org.apache.maven.plugin.assembly.model.ModuleSet;
import org.apache.maven.plugin.assembly.model.ModuleSources;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ModuleSetAssemblyPhaseTest
    extends TestCase
{

    private final TestFileManager fileManager = new TestFileManager( "module-set-phase.test.", "" );

    private final Logger logger = new ConsoleLogger( Logger.LEVEL_INFO, "test" );

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchOutputDir()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setOutputDirectory( "outdir" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchInclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addInclude( "**/included.txt" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchExclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addExclude( "**/excluded.txt" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchFileMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setFileMode( "777" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchDirMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setDirectoryMode( "777" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testCreateFileSet_ShouldUseModuleDirOnlyWhenOutDirIsNull()
        throws AssemblyFormattingException
    {
        final MockManager mm = new MockManager();

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final File basedir = fileManager.createTempDir();

        final MavenProject artifactProject = new MavenProject( new Model() );

        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources,
                                                                                                artifactProject,
                                                                                                macTask.configSource );

        assertEquals( "artifact/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldPrependModuleDirWhenOutDirIsProvided()
        throws AssemblyFormattingException
    {
        final MockManager mm = new MockManager();

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();
        fs.setOutputDirectory( "out" );

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final MavenProject artifactProject = new MavenProject( new Model() );

        final File basedir = fileManager.createTempDir();

        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources,
                                                                                                artifactProject,
                                                                                                macTask.configSource );

        assertEquals( "artifact/out/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldAddExcludesForSubModulesWhenExcludeSubModDirsIsTrue()
        throws AssemblyFormattingException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setExcludeSubModuleDirectories( true );

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        model.addModule( "submodule" );

        final MavenProject project = new MavenProject( model );

        final File basedir = fileManager.createTempDir();

        project.setFile( new File( basedir, "pom.xml" ) );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        project.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources, project,
                                                                                                macTask.configSource );

        assertEquals( 1, result.getExcludes()
                               .size() );
        assertEquals( "submodule/**", result.getExcludes()
                                            .get( 0 ) );

        mm.verifyAll();
    }

    public void testExecute_ShouldSkipIfNoModuleSetsFound()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        createPhase( null, null ).execute( assembly, null, null, new DefaultAssemblyContext() );
    }

    public void testExecute_ShouldAddOneModuleSetWithOneModuleInIt()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        final MavenProject module = createProject( "group", "module", "version", project );

        final ArtifactMock moduleArtifactMock = new ArtifactMock( mm, "group", "module", "version", "jar", false );
        final File moduleArtifactFile = moduleArtifactMock.setNewFile();
        module.setArtifact( moduleArtifactMock.getArtifact() );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        projects.add( module );

        macTask.expectGetReactorProjects( projects );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        macTask.expectAddFile( moduleArtifactFile, "out/artifact", mode );

        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        final ModuleSet ms = new ModuleSet();

        final ModuleBinaries bin = new ModuleBinaries();

        bin.setOutputFileNameMapping( "artifact" );
        bin.setOutputDirectory( "out" );
        bin.setFileMode( "777" );
        bin.setUnpack( false );
        bin.setIncludeDependencies( false );

        ms.setBinaries( bin );

        assembly.addModuleSet( ms );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mm.replayAll();

        createPhase( logger, null ).execute( assembly, macTask.archiver, macTask.configSource,
                                             new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldReturnImmediatelyWhenBinariesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        createPhase( null, null ).addModuleBinaries( null, null, null, null, new DefaultAssemblyContext() );
    }

    public void testAddModuleBinaries_ShouldFilterPomModule()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setPackaging( "pom" );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "pom", false );
        project.setArtifact( artifactMock.getArtifact() );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleBinaries( binaries,
                                                                                                projects,
                                                                                                macTask.archiver,
                                                                                                macTask.configSource,
                                                                                                new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleAttachmentArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", "test", false );
        final File artifactFile = artifactMock.setNewFile();

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.addAttachedArtifact( artifactMock.getArtifact() );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                       new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldFailWhenOneModuleDoesntHaveAttachmentWithMatchingClassifier()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", "test", false );
        artifactMock.setNewFile();

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        try
        {
            createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                           new DefaultAssemblyContext() );

            fail( "Should throw an invalid configuration exception because of module with missing attachment." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            // should throw this because of missing attachment.
        }

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );
        final File artifactFile = artifactMock.setNewFile();

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                       new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifact()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );
        final File artifactFile = artifactMock.setNewFile();

        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macTask.expectGetSession( null );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final DependencySet ds = new DependencySet();
        ds.setUseProjectArtifact( false );
        ds.setOutputDirectory( binaries.getOutputDirectory() );
        ds.setOutputFileNameMapping( "${artifact.artifactId}" );
        ds.setFileMode( "777" );

        binaries.addDependencySet( ds );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        final ArtifactMock depArtifactMock = new ArtifactMock( mm, "group", "dep", "1", "jar", false );
        final File depArtifactFile = depArtifactMock.setNewFile();

        macTask.expectAddFile( depArtifactFile, "out/dep",
                               TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        final MavenProject depProject = createProject( "group", "dep", "version", null );
        depProject.setArtifact( depArtifactMock.getArtifact() );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        final Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        final DefaultAssemblyContext context = new DefaultAssemblyContext();
        context.setResolvedArtifacts( Collections.singleton( depArtifactMock.getArtifact() ) );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource, context );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifactUsingImpliedDepSet()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        final ArtifactMock moduleArtifactMock = new ArtifactMock( mm, "group", "artifact", "0", "jar", false );
        final File moduleArtifactFile = moduleArtifactMock.setNewFile();

        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        macTask.expectAddFile( moduleArtifactFile, "out/artifact", mode );
        macTask.expectGetSession( null );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "${artifact.artifactId}" );
        binaries.setIncludeDependencies( true );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( moduleArtifactMock.getArtifact() );

        final ArtifactMock depArtifactMock = new ArtifactMock( mm, "group", "dep", "1", "jar", false );
        final File depArtifactFile = depArtifactMock.setNewFile();

        macTask.expectAddFile( depArtifactFile, "out/dep", mode );

        final MavenProject depProject = createProject( "group", "dep", "version", null );
        depProject.setArtifact( depArtifactMock.getArtifact() );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );

        final Set<MavenProject> projects = Collections.singleton( project );

        mm.replayAll();

        final Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        final DefaultAssemblyContext context = new DefaultAssemblyContext();
        context.setResolvedArtifacts( Collections.singleton( depArtifactMock.getArtifact() ) );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource, context );

        mm.verifyAll();
    }

    // public void testCollectExcludesFromQueuedArtifacts_ShouldAddOneExclusion()
    // {
    // MockManager mm = new MockManager();
    //
    // List excludes = new ArrayList();
    //
    // // nothing up this sleeve...
    // assertTrue( excludes.isEmpty() );
    //
    // mm.replayAll();
    //
    // Set artifactIds = Collections.singleton( "group:artifact:jar" );
    //
    // List result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null )
    // .collectExcludesFromQueuedArtifacts( artifactIds, excludes );
    //
    // assertEquals( 1, result.size() );
    //
    // assertEquals( "group:artifact:jar", result.get( 0 ) );
    //
    // mm.verifyAll();
    // }
    //
    // public void testCollectExcludesFromQueuedArtifacts_ShouldHandleNullExcludesList()
    // {
    // MockManager mm = new MockManager();
    //
    // mm.replayAll();
    //
    // Set artifactIds = Collections.singleton( "group:artifact:jar" );
    //
    // List result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null )
    // .collectExcludesFromQueuedArtifacts( artifactIds, null );
    //
    // assertEquals( 1, result.size() );
    //
    // assertEquals( "group:artifact:jar", result.get( 0 ) );
    //
    // mm.verifyAll();
    // }

    public void testAddModuleArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws AssemblyFormattingException, IOException
    {
        final MockManager mm = new MockManager();

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "type", false );
        artifactMock.setNullFile();

        mm.replayAll();

        try
        {
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleArtifact( artifactMock.getArtifact(),
                                                                                                    null, null, null,
                                                                                                    null );

            fail( "Expected ArchiveCreationException since artifact file is null." );
        }
        catch ( final ArchiveCreationException e )
        {
            // expected
        }

        mm.verifyAll();
    }

    public void testAddModuleArtifact_ShouldAddOneArtifact()
        throws AssemblyFormattingException, IOException, ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "type", false );
        final File artifactFile = artifactMock.setNewFile();

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );

        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        final ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleArtifact( artifactMock.getArtifact(),
                                                                                                project,
                                                                                                macTask.archiver,
                                                                                                macTask.configSource,
                                                                                                binaries );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final MockManager mm = new MockManager();

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleSourceFileSets( null, null, null,
                                                                                                      null );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final MockManager mm = new MockManager();

        final MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mm, fileManager );

        final MavenProject project = createProject( "group", "artifact", "version", null );

        macTask.expectGetProject( project );

        final ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        project.setArtifact( artifactMock.getArtifact() );

        final Set<MavenProject> projects = Collections.singleton( project );

        final ModuleSources sources = new ModuleSources();

        final FileSet fs = new FileSet();
        fs.setDirectory( "/src" );
        fs.setDirectoryMode( "777" );
        fs.setFileMode( "777" );

        sources.addFileSet( fs );

        macTask.expectGetArchiveBaseDirectory();

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        final int[] modes = { -1, -1, mode, mode };

        macTask.expectAdditionOfSingleFileSet( project, project.getBasedir(), "final-name", false, modes, 1, true,
                                               false );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleSourceFileSets( sources, projects, macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        final List<MavenProject> projects = Collections.singletonList( project );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", null );

        final List<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );

        final List<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertFalse( moduleProjects.isEmpty() );

        final MavenProject result = moduleProjects.iterator()
                                                  .next();

        assertEquals( "artifact2", result.getArtifactId() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );
        final MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        final List<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertEquals( 2, moduleProjects.size() );

        final List<MavenProject> check = new ArrayList<MavenProject>();
        check.add( project2 );
        check.add( project3 );

        verifyResultIs( check, moduleProjects );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldExcludeModuleAndDescendentsTransitively()
        throws ArchiveCreationException
    {
        final MockManager mm = new MockManager();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        final List<ArtifactMock> macArtifacts = new ArrayList<ArtifactMock>();

        macArtifacts.add( addArtifact( project, mm, false, false ) );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );
        macArtifacts.add( addArtifact( project2, mm, true, false ) );
        final MavenProject project3 = createProject( "group", "artifact3", "version", project2 );
        macArtifacts.add( addArtifact( project3, mm, true, true ) );

        final List<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        moduleSet.addExclude( "group:artifact2" );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    private ArtifactMock addArtifact( final MavenProject project, final MockManager mm,
                                      final boolean expectIdentityChecks, final boolean expectDepTrailCheck )
    {
        final ArtifactMock macArtifact =
            new ArtifactMock( mm, project.getGroupId(), project.getArtifactId(), project.getVersion(),
                              project.getPackaging(), false );

        if ( expectDepTrailCheck )
        {
            final LinkedList<String> depTrail = new LinkedList<String>();

            MavenProject parent = project.getParent();
            while ( parent != null )
            {
                depTrail.addLast( parent.getId() );

                parent = parent.getParent();
            }

            macArtifact.setDependencyTrail( depTrail );
        }

        project.setArtifact( macArtifact.getArtifact() );

        return macArtifact;
    }

    private void verifyResultIs( final List<MavenProject> check, final Set<MavenProject> moduleProjects )
    {
        boolean failed = false;

        final Set<MavenProject> checkTooMany = new HashSet<MavenProject>( moduleProjects );
        checkTooMany.removeAll( check );

        if ( !checkTooMany.isEmpty() )
        {
            failed = true;

            System.out.println( "Unexpected projects in output: " );

            for (final MavenProject project : checkTooMany) {
                System.out.println(project.getId());
            }
        }

        final Set<MavenProject> checkTooFew = new HashSet<MavenProject>( check );
        checkTooFew.removeAll( moduleProjects );

        if ( !checkTooFew.isEmpty() )
        {
            failed = true;

            System.out.println( "Expected projects missing from output: " );

            for (final MavenProject project : checkTooMany) {
                System.out.println(project.getId());
            }
        }

        if ( failed )
        {
            Assert.fail( "See system output for more information." );
        }
    }

    private MavenProject createProject( final String groupId, final String artifactId, final String version,
                                        final MavenProject parentProject )
    {
        final Model model = new Model();
        model.setArtifactId( artifactId );
        model.setGroupId( groupId );
        model.setVersion( version );

        final MavenProject project = new MavenProject( model );

        File pomFile;
        if ( parentProject == null )
        {
            final File basedir = fileManager.createTempDir();
            pomFile = new File( basedir, "pom.xml" );
        }
        else
        {
            final File parentBase = parentProject.getBasedir();
            pomFile = new File( parentBase, artifactId + "/pom.xml" );

            parentProject.getModel()
                         .addModule( artifactId );
            project.setParent( parentProject );
        }

        project.setFile( pomFile );

        return project;
    }

    private ModuleSetAssemblyPhase createPhase( final Logger logger,
                                                final MockAndControlForAddDependencySetsTask macTask )
    {
        MavenProjectBuilder projectBuilder = null;

        if ( macTask != null )
        {
            projectBuilder = macTask.projectBuilder;
        }

        final ModuleSetAssemblyPhase phase = new ModuleSetAssemblyPhase( projectBuilder, logger );

        return phase;
    }

}
