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
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForArtifact;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
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

    private TestFileManager fileManager = new TestFileManager( "module-set-phase.test.", "" );
    
    private Logger logger = new ConsoleLogger( Logger.LEVEL_INFO, "test" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchOutputDir()
    {
        ModuleSources sources = new ModuleSources();
        sources.setOutputDirectory( "outdir" );

        ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchInclude()
    {
        ModuleSources sources = new ModuleSources();
        sources.addInclude( "**/included.txt" );

        ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchExclude()
    {
        ModuleSources sources = new ModuleSources();
        sources.addExclude( "**/excluded.txt" );

        ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchFileMode()
    {
        ModuleSources sources = new ModuleSources();
        sources.setFileMode( "777" );

        ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchDirMode()
    {
        ModuleSources sources = new ModuleSources();
        sources.setDirectoryMode( "777" );

        ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    public void testCreateFileSet_ShouldUseModuleDirOnlyWhenOutDirIsNull()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        Model model = new Model();
        model.setArtifactId( "artifact" );

        MavenProject project = new MavenProject( model );

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );

        FileSet fs = new FileSet();

        ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        File basedir = fileManager.createTempDir();

        MavenProject artifactProject = new MavenProject( new Model() );
        
        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        FileSet result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs,
                                                                                                             sources,
                                                                                                             artifactProject,
                                                                                                             macTask.configSource );

        assertEquals( "artifact/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldPrependModuleDirWhenOutDirIsProvided()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        Model model = new Model();
        model.setArtifactId( "artifact" );

        MavenProject project = new MavenProject( model );

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );
        
        FileSet fs = new FileSet();
        fs.setOutputDirectory( "out" );

        ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        MavenProject artifactProject = new MavenProject( new Model() );

        File basedir = fileManager.createTempDir();

        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        FileSet result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs,
                                                                                                             sources,
                                                                                                             artifactProject,
                                                                                                             macTask.configSource );

        assertEquals( "artifact/out/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldAddExcludesForSubModulesWhenExcludeSubModDirsIsTrue()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        macTask.expectGetFinalName( null );
        
        FileSet fs = new FileSet();

        ModuleSources sources = new ModuleSources();
        sources.setExcludeSubModuleDirectories( true );

        Model model = new Model();
        model.setArtifactId( "artifact" );

        model.addModule( "submodule" );

        MavenProject project = new MavenProject( model );

        File basedir = fileManager.createTempDir();

        project.setFile( new File( basedir, "pom.xml" ) );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        project.setArtifact( artifactMock.getArtifact() );

        mm.replayAll();

        FileSet result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs,
                                                                                                             sources,
                                                                                                             project,
                                                                                                             macTask.configSource );

        assertEquals( 1, result.getExcludes().size() );
        assertEquals( "submodule/**", result.getExcludes().get( 0 ) );

        mm.verifyAll();
    }

    public void testExecute_ShouldSkipIfNoModuleSetsFound()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        createPhase( null, null ).execute( assembly, null, null );
    }

    public void testExecute_ShouldAddOneModuleSetWithOneModuleInIt()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        MavenProject module = createProject( "group", "module", "version", project );

        ArtifactMock moduleArtifactMock = new ArtifactMock( mm, "group", "module", "version", "jar", false );
        File moduleArtifactFile = moduleArtifactMock.setNewFile();
        module.setArtifact( moduleArtifactMock.getArtifact() );

        List projects = new ArrayList();

        projects.add( module );

        macTask.expectGetReactorProjects( projects );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        
        int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        macTask.expectAddFile( moduleArtifactFile, "out/artifact", mode );

        Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        ModuleSet ms = new ModuleSet();

        ModuleBinaries bin = new ModuleBinaries();

        bin.setOutputFileNameMapping( "artifact" );
        bin.setOutputDirectory( "out" );
        bin.setFileMode( "777" );
        bin.setUnpack( false );
        bin.setIncludeDependencies( false );

        ms.setBinaries( bin );

        assembly.addModuleSet( ms );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mm.replayAll();

        createPhase( logger, null ).execute( assembly, macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldReturnImmediatelyWhenBinariesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        createPhase( null, null ).addModuleBinaries( null, null, null, null, null );
    }

    public void testAddModuleBinaries_ShouldFilterPomModule()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setPackaging( "pom" );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "pom", false );
        project.setArtifact( artifactMock.getArtifact() );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleBinaries( binaries, projects,
                                                                                                macTask.archiver,
                                                                                                macTask.configSource,
                                                                                                new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleAttachmentArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", "test", false );
        File artifactFile = artifactMock.setNewFile();

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact", TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        
        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.addAttachedArtifact( artifactMock.getArtifact() );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                       new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldFailWhenOneModuleDoesntHaveAttachmentWithMatchingClassifier()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", "test", false );
        artifactMock.setNewFile();

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        try
        {
            createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                           new DefaultAssemblyContext() );

            fail( "Should throw an invalid configuration exception because of module with missing attachment." );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            // should throw this because of missing attachment.
        }

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact", TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        
        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                       new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifact()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact", TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macTask.expectGetSession( null );
        
        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        DependencySet ds = new DependencySet();
        ds.setUseProjectArtifact( false );
        ds.setOutputDirectory( binaries.getOutputDirectory() );
        ds.setOutputFileNameMapping( "${artifact.artifactId}" );
        ds.setFileMode( "777" );

        binaries.addDependencySet( ds );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        ArtifactMock depArtifactMock = new ArtifactMock( mm, "group", "dep", "1", "jar", false );
        File depArtifactFile = depArtifactMock.setNewFile();

        macTask.expectAddFile( depArtifactFile, "out/dep", TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        MavenProject depProject = createProject( "group", "dep", "version", null );
        depProject.setArtifact( depArtifactMock.getArtifact() );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( depArtifactMock.getArtifact() ) );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                 new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifactUsingImpliedDepSet()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        ArtifactMock moduleArtifactMock = new ArtifactMock( mm, "group", "artifact", "0", "jar", false );
        File moduleArtifactFile = moduleArtifactMock.setNewFile();

        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );

        int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        macTask.expectAddFile( moduleArtifactFile, "out/artifact", mode );
        macTask.expectGetSession( null );
        
        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "${artifact.artifactId}" );
        binaries.setIncludeDependencies( true );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( moduleArtifactMock.getArtifact() );

        ArtifactMock depArtifactMock = new ArtifactMock( mm, "group", "dep", "1", "jar", false );
        File depArtifactFile = depArtifactMock.setNewFile();

        macTask.expectAddFile( depArtifactFile, "out/dep", mode );

        MavenProject depProject = createProject( "group", "dep", "version", null );
        depProject.setArtifact( depArtifactMock.getArtifact() );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( depArtifactMock.getArtifact() ) );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                 new DefaultAssemblyContext() );

        mm.verifyAll();
    }

    public void testCollectExcludesFromQueuedArtifacts_ShouldAddOneExclusion()
    {
        MockManager mm = new MockManager();

        List excludes = new ArrayList();

        // nothing up this sleeve...
        assertTrue( excludes.isEmpty() );

        mm.replayAll();

        Set artifactIds = Collections.singleton( "group:artifact:jar" );

        List result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null )
            .collectExcludesFromQueuedArtifacts( artifactIds, excludes );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testCollectExcludesFromQueuedArtifacts_ShouldHandleNullExcludesList()
    {
        MockManager mm = new MockManager();

        mm.replayAll();

        Set artifactIds = Collections.singleton( "group:artifact:jar" );

        List result = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null )
            .collectExcludesFromQueuedArtifacts( artifactIds, null );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testAddModuleArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "type", false );
        artifactMock.setNullFile();

        mm.replayAll();

        try
        {
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleArtifact( artifactMock.getArtifact(), null,
                                                                                              null, null, null );

            fail( "Expected ArchiveCreationException since artifact file is null." );
        }
        catch ( ArchiveCreationException e )
        {
            // expected
        }

        mm.verifyAll();
    }

    public void testAddModuleArtifact_ShouldAddOneArtifact()
        throws AssemblyFormattingException, IOException, ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "type", false );
        File artifactFile = artifactMock.setNewFile();

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifactMock.getArtifact() );

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );

        macTask.expectAddFile( artifactFile, "out/artifact", TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        
        ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleArtifact( artifactMock.getArtifact(), project,
                                                                                          macTask.archiver,
                                                                                          macTask.configSource,
                                                                                          binaries );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleSourceFileSets( null, null, null,
                                                                                                      null );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mm, fileManager );

        MavenProject project = createProject( "group", "artifact", "version", null );

        macTask.expectGetProject( project );

        ArtifactMock artifactMock = new ArtifactMock( mm, "group", "artifact", "version", "jar", false );

        project.setArtifact( artifactMock.getArtifact() );

        Set projects = Collections.singleton( project );

        ModuleSources sources = new ModuleSources();

        FileSet fs = new FileSet();
        fs.setDirectory( "/src" );
        fs.setDirectoryMode( "777" );
        fs.setFileMode( "777" );

        sources.addFileSet( fs );

        macTask.expectGetArchiveBaseDirectory();

        int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        int[] modes = { -1, -1, mode, mode };

        macTask.expectAdditionOfSingleFileSet( project, project.getBasedir(), "final-name", false, modes, 1, true,
                                               false );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleSourceFileSets( sources, projects, macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        List projects = Collections.singletonList( project );

        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        Set moduleProjects = ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        MavenProject project2 = createProject( "group", "artifact2", "version", null );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        Set moduleProjects = ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        MavenProject project2 = createProject( "group", "artifact2", "version", project );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        Set moduleProjects = ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertFalse( moduleProjects.isEmpty() );

        MavenProject result = (MavenProject) moduleProjects.iterator().next();

        assertEquals( "artifact2", result.getArtifactId() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        MavenProject project2 = createProject( "group", "artifact2", "version", project );
        MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        Set moduleProjects = ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertEquals( 2, moduleProjects.size() );

        List check = new ArrayList();
        check.add( project2 );
        check.add( project3 );

        verifyResultIs( check, moduleProjects );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldExcludeModuleAndDescendentsTransitively()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm, project );

        List macArtifacts = new ArrayList();

        macArtifacts.add( addArtifact( project, mm, false, false ) );

        MavenProject project2 = createProject( "group", "artifact2", "version", project );
        macArtifacts.add( addArtifact( project2, mm, true, false ) );

        ( (MockAndControlForArtifact) macArtifacts.get( 1 ) ).expectGetId( "group:artifact2:jar:version" );
//        ( (MockAndControlForArtifact) macArtifacts.get( 1 ) ).expectGetDependencyTrail( Collections
//            .singletonList( "group:artifact:jar:version" ) );

        MavenProject project3 = createProject( "group", "artifact3", "version", project2 );
        macArtifacts.add( addArtifact( project3, mm, true, true ) );

        ( (MockAndControlForArtifact) macArtifacts.get( 2 ) ).expectGetId( "group:artifact3:jar:version" );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        moduleSet.addExclude( "group:artifact2" );

        mm.replayAll();

        Set moduleProjects = ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    private MockAndControlForArtifact addArtifact( MavenProject project, MockManager mm, boolean expectIdentityChecks,
                                                   boolean expectDepTrailCheck )
    {
        MockAndControlForArtifact macArtifact = new MockAndControlForArtifact( mm );

        if ( expectIdentityChecks )
        {
            macArtifact.expectGetArtifactId( project.getArtifactId() );
            macArtifact.expectGetGroupId( project.getGroupId() );
            macArtifact.expectGetDependencyConflictId( project.getGroupId(), project.getArtifactId(), project
                .getPackaging() );
        }

        if ( expectDepTrailCheck )
        {
            LinkedList depTrail = new LinkedList();

            MavenProject parent = project.getParent();
            while ( parent != null )
            {
                depTrail.addLast( parent.getId() );

                parent = parent.getParent();
            }

            macArtifact.expectGetDependencyTrail( depTrail );
        }

        project.setArtifact( macArtifact.artifact );

        return macArtifact;
    }

    private void verifyResultIs( List check, Set moduleProjects )
    {
        boolean failed = false;

        Set checkTooMany = new HashSet( moduleProjects );
        checkTooMany.removeAll( check );

        if ( !checkTooMany.isEmpty() )
        {
            failed = true;

            System.out.println( "Unexpected projects in output: " );

            for ( Iterator iter = checkTooMany.iterator(); iter.hasNext(); )
            {
                MavenProject project = (MavenProject) iter.next();

                System.out.println( project.getId() );
            }
        }

        Set checkTooFew = new HashSet( check );
        checkTooFew.removeAll( moduleProjects );

        if ( !checkTooFew.isEmpty() )
        {
            failed = true;

            System.out.println( "Expected projects missing from output: " );

            for ( Iterator iter = checkTooMany.iterator(); iter.hasNext(); )
            {
                MavenProject project = (MavenProject) iter.next();

                System.out.println( project.getId() );
            }
        }

        if ( failed )
        {
            Assert.fail( "See system output for more information." );
        }
    }

    private MavenProject createProject( String groupId, String artifactId, String version, MavenProject parentProject )
    {
        Model model = new Model();
        model.setArtifactId( artifactId );
        model.setGroupId( groupId );
        model.setVersion( version );

        MavenProject project = new MavenProject( model );

        File pomFile;
        if ( parentProject == null )
        {
            File basedir = fileManager.createTempDir();
            pomFile = new File( basedir, "pom.xml" );
        }
        else
        {
            File parentBase = parentProject.getBasedir();
            pomFile = new File( parentBase, artifactId + "/pom.xml" );

            parentProject.getModel().addModule( artifactId );
            project.setParent( parentProject );
        }

        project.setFile( pomFile );

        return project;
    }

    private ModuleSetAssemblyPhase createPhase( Logger logger, MockAndControlForAddDependencySetsTask macTask )
    {
        MavenProjectBuilder projectBuilder = null;
        DependencyResolver dependencyResolver = null;

        if ( macTask != null )
        {
            projectBuilder = macTask.projectBuilder;
            dependencyResolver = macTask.dependencyResolver;
        }

        ModuleSetAssemblyPhase phase = new ModuleSetAssemblyPhase( projectBuilder, dependencyResolver, logger );

        return phase;
    }

}
