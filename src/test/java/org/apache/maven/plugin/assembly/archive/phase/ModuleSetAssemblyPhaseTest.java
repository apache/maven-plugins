package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForArtifact;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
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

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testCreateFileSet_ShouldUseModuleDirOnlyWhenOutDirIsNull()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        FileSet fs = new FileSet();

        ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        Model model = new Model();
        model.setArtifactId( "artifact" );

        MavenProject project = new MavenProject( model );

        File basedir = fileManager.createTempDir();

        project.setFile( new File( basedir, "pom.xml" ) );

        MockAndControlForArtifact macArtifact = new MockAndControlForArtifact( mm );

        macArtifact.expectIsSnapshot( false );
        macArtifact.expectGetClassifier( null );
        macArtifact.expectGetArtifactHandler();

        project.setArtifact( macArtifact.artifact );

        mm.replayAll();

        FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources, project );

        assertEquals( "artifact/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldPrependModuleDirWhenOutDirIsProvided()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        FileSet fs = new FileSet();
        fs.setOutputDirectory( "out" );

        ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        Model model = new Model();
        model.setArtifactId( "artifact" );

        MavenProject project = new MavenProject( model );

        File basedir = fileManager.createTempDir();

        project.setFile( new File( basedir, "pom.xml" ) );

        MockAndControlForArtifact macArtifact = new MockAndControlForArtifact( mm );

        macArtifact.expectIsSnapshot( false );
        macArtifact.expectGetClassifier( null );
        macArtifact.expectGetArtifactHandler();

        project.setArtifact( macArtifact.artifact );

        mm.replayAll();

        FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources, project );

        assertEquals( "artifact/out/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    public void testCreateFileSet_ShouldAddExcludesForSubModulesWhenExcludeSubModDirsIsTrue()
        throws AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        FileSet fs = new FileSet();

        ModuleSources sources = new ModuleSources();
        sources.setExcludeSubModuleDirectories( true );

        Model model = new Model();
        model.setArtifactId( "artifact" );
        
        model.addModule( "submodule" );

        MavenProject project = new MavenProject( model );

        File basedir = fileManager.createTempDir();

        project.setFile( new File( basedir, "pom.xml" ) );

        MockAndControlForArtifact macArtifact = new MockAndControlForArtifact( mm );

        macArtifact.expectIsSnapshot( false );
        macArtifact.expectGetClassifier( null );
        macArtifact.expectGetArtifactHandler();

        project.setArtifact( macArtifact.artifact );

        mm.replayAll();

        FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources, project );
        
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

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject module = createProject( "group", "module", "version", project );

        macTask.expectArtifactGetFile();
        module.setArtifact( macTask.artifact );

        List projects = new ArrayList();

        projects.add( project );
        projects.add( module );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectIsSnapshot( false );
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();

        int mode = Integer.parseInt( "777", 8 );

        macTask.expectAddFile( "out/artifact", mode );

        Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        ModuleSet ms = new ModuleSet();

        ModuleBinaries bin = new ModuleBinaries();

        bin.setOutputFileNameMapping( "artifact" );
        bin.setOutputDirectory( "out" );
        bin.setFileMode( "777" );
        bin.setUnpack( false );

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
        createPhase( null, null ).addModuleBinaries( null, null, null, null, false );
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
        project.setArtifact( macTask.artifact );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleBinaries( binaries, projects,
                                                                                                macTask.archiver,
                                                                                                macTask.configSource,
                                                                                                false );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        macTask.expectArtifactGetFile( true );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectIsSnapshot( false );
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();
        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource,
                                                       false );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifact()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        macTask.expectArtifactGetFile( true );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();
        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( binaries.getOutputDirectory() );
        ds.setOutputFileNameMapping( "${artifactId}" );
        ds.setFileMode( "777" );

        binaries.addDependencySet( ds );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        MockAndControlForArtifact macDepArtifact = new MockAndControlForArtifact( mm );

        macDepArtifact.expectGetClassifier( null );
        macDepArtifact.expectGetType( "jar" );
        macDepArtifact.expectIsSnapshot( false );
        macDepArtifact.expectGetArtifactHandler();
        macDepArtifact.expectGetArtifactId( "dep" );

        File artifactFile = fileManager.createTempFile();

        macDepArtifact.expectGetFile( artifactFile );

        macTask.expectAddFile( artifactFile, "out/dep", Integer.parseInt( "777", 8 ) );

        MavenProject depProject = createProject( "group", "dep", "version", null );

        depProject.setArtifact( macDepArtifact.artifact );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( macDepArtifact.artifact ) );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource, false );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifactUsingImpliedDepSet()
        throws ArchiveCreationException, AssemblyFormattingException, IOException,
        InvalidAssemblerConfigurationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        macTask.expectArtifactGetFile( true );
        macTask.expectArtifactGetArtifactId( "artifact" );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();
        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "${artifactId}" );
        binaries.setIncludeDependencies( true );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        MockAndControlForArtifact macDepArtifact = new MockAndControlForArtifact( mm );

        macDepArtifact.expectGetClassifier( null );
        macDepArtifact.expectGetType( "jar" );
        macDepArtifact.expectIsSnapshot( false );
        macDepArtifact.expectGetArtifactHandler();
        macDepArtifact.expectGetArtifactId( "dep" );

        File artifactFile = fileManager.createTempFile();

        macDepArtifact.expectGetFile( artifactFile );

        macTask.expectAddFile( artifactFile, "out/dep", Integer.parseInt( "777", 8 ) );

        MavenProject depProject = createProject( "group", "dep", "version", null );

        depProject.setArtifact( macDepArtifact.artifact );

        macTask.expectBuildFromRepository( depProject );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( macDepArtifact.artifact ) );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        ModuleSetAssemblyPhase phase = createPhase( overrideLogger, macTask );

        phase.addModuleBinaries( binaries, projects, macTask.archiver, macTask.configSource, false );

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

        List result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).collectExcludesFromQueuedArtifacts(
                                                                                                                     artifactIds,
                                                                                                                     excludes );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testCollectExcludesFromQueuedArtifacts_ShouldHandleNullExcludesList()
    {
        MockManager mm = new MockManager();

        mm.replayAll();

        Set artifactIds = Collections.singleton( "group:artifact:jar" );

        List result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).collectExcludesFromQueuedArtifacts(
                                                                                                                     artifactIds,
                                                                                                                     null );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testAddArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        macTask.expectArtifactGetFile( false );

        macTask.artifact.getId();
        macTask.artifactCtl.setReturnValue( "group:artifact:type:version" );

        mm.replayAll();

        try
        {
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addArtifact( macTask.artifact, null,
                                                                                              null, null, null, false );

            fail( "Expected ArchiveCreationException since artifact file is null." );
        }
        catch ( ArchiveCreationException e )
        {
            // expected
        }

        mm.verifyAll();
    }

    public void testAddArtifact_ShouldAddOneArtifact()
        throws AssemblyFormattingException, IOException, ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        macTask.expectArtifactGetFile();
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();

        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addArtifact( macTask.artifact, project,
                                                                                          macTask.archiver,
                                                                                          macTask.configSource,
                                                                                          binaries, false );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleSourceFileSets( null, null, null,
                                                                                                      null, false );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mm, fileManager );

        MavenProject project = createProject( "group", "artifact", "version", null );

        MockAndControlForArtifact macArtifact = new MockAndControlForArtifact( mm );

        macArtifact.expectIsSnapshot( false );
        macArtifact.expectGetClassifier( null );
        macArtifact.expectGetArtifactHandler();

        project.setArtifact( macArtifact.artifact );

        Set projects = Collections.singleton( project );

        ModuleSources sources = new ModuleSources();

        FileSet fs = new FileSet();
        fs.setDirectory( "/src" );
        fs.setDirectoryMode( "777" );
        fs.setFileMode( "777" );

        sources.addFileSet( fs );

        macTask.expectGetArchiveBaseDirectory();

        int mode = Integer.parseInt( "777", 8 );
        int[] modes = { -1, -1, mode, mode };

        macTask.expectAdditionOfSingleFileSet( project, project.getBasedir(), "final-name", false, modes, 1, true,
                                               false );

        mm.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleSourceFileSets( sources, projects, macTask.archiver, macTask.configSource,
                                                             false );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );

        List projects = Collections.singletonList( project );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), macTask ).getModuleProjects(
                                                                                                       moduleSet,
                                                                                                       macTask.configSource,
                                                                                                       true );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", null );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), macTask ).getModuleProjects(
                                                                                                       moduleSet,
                                                                                                       macTask.configSource,
                                                                                                       true );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", project );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), macTask ).getModuleProjects(
                                                                                                       moduleSet,
                                                                                                       macTask.configSource,
                                                                                                       true );

        assertFalse( moduleProjects.isEmpty() );

        MavenProject result = ( MavenProject ) moduleProjects.iterator().next();

        assertEquals( "artifact2", result.getArtifactId() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", project );
        MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), macTask ).getModuleProjects(
                                                                                                       moduleSet,
                                                                                                       macTask.configSource,
                                                                                                       true );

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

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mm );

        List macArtifacts = new ArrayList();

        MavenProject project = createProject( "group", "artifact", "version", null );
        macArtifacts.add( addArtifact( project, mm, false, false ) );

        MavenProject project2 = createProject( "group", "artifact2", "version", project );
        macArtifacts.add( addArtifact( project2, mm, true, false ) );

        ( ( MockAndControlForArtifact ) macArtifacts.get( 1 ) ).expectGetId( "group:artifact2:jar:version" );

        MavenProject project3 = createProject( "group", "artifact3", "version", project2 );
        macArtifacts.add( addArtifact( project3, mm, true, true ) );

        ( ( MockAndControlForArtifact ) macArtifacts.get( 2 ) ).expectGetId( "group:artifact3:jar:version" );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetProject( project );
        macTask.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        moduleSet.addExclude( "group:artifact2" );

        mm.replayAll();

        Set moduleProjects =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), macTask ).getModuleProjects(
                                                                                                       moduleSet,
                                                                                                       macTask.configSource,
                                                                                                       true );

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
            macArtifact.expectGetDependencyConflictId( project.getGroupId(), project.getArtifactId(),
                                                       project.getPackaging() );
        }

        if ( expectDepTrailCheck )
        {
            LinkedList depTrail = new LinkedList();

            MavenProject parent = project.getParent();
            while ( parent != null )
            {
                depTrail.addLast( ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() ) );

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
                MavenProject project = ( MavenProject ) iter.next();

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
                MavenProject project = ( MavenProject ) iter.next();

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
