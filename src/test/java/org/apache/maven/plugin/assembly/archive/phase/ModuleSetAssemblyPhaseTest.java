package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

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

    public void testExecute_ShouldSkipIfNoModuleSetsFound()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        createPhase( null, null ).execute( assembly, null, null );
    }

    public void testExecute_ShouldAddOneModuleSetWithOneModuleInIt()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
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
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();
        macTask.expectArtifactGetDependencyConflictId( "group:module:jar" );

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
        bin.setIncludeDependencies( false );
        
        ms.setBinaries( bin );
        
        assembly.addModuleSet( ms );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        
        mm.replayAll();

        createPhase( logger, null ).execute( assembly, macTask.archiver, macTask.configSource );
        
        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldReturnImmediatelyWhenBinariesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        createPhase( null, null ).addModuleBinaries( null, null, null, null, false );
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        
        macLogger.expectDebug( false, true );
        
        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        macTask.expectArtifactGetFile( true );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();
        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        macTask.artifact.getDependencyConflictId();
        macTask.artifactCtl.setReturnValue( "group:artifact:jar", MockControl.ONE_OR_MORE );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        createPhase( macLogger.logger, null ).addModuleBinaries( binaries, projects, macTask.archiver,
                                                                 macTask.configSource, false );

        mm.verifyAll();
    }

    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndWithOneDepArtifact()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        // MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        //        
        // macLogger.expectWarn();
        // macLogger.expectDebug( true, true );

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        macTask.expectArtifactGetFile( true );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();
        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        macTask.artifact.getDependencyConflictId();
        macTask.artifactCtl.setReturnValue( "group:artifact:jar", MockControl.ONE_OR_MORE );

        macTask.configSource.getRemoteRepositories();
        macTask.configSourceCtl.setReturnValue( null, MockControl.ONE_OR_MORE );

        macTask.configSource.getLocalRepository();
        macTask.configSourceCtl.setReturnValue( null, MockControl.ONE_OR_MORE );

        // macTask.artifact.getId();
        // macTask.artifactCtl.setReturnValue( "group:artifact:jar:version", MockControl.ONE_OR_MORE );

        ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( true );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        MockAndControlForArtifact macDepArtifact = new MockAndControlForArtifact( mm );

        macDepArtifact.expectGetClassifier();
        macDepArtifact.expectGetArtifactHandler();
        macDepArtifact.expectGetDependencyConflictId( "group", "dep", "jar" );
        macDepArtifact.expectGetFile();

        macTask.expectAddFile( macDepArtifact.artifactFile, "out/artifact", Integer.parseInt( "777", 8 ) );

        MavenProject depProject = createProject( "group", "dep", "version", null );

        depProject.setArtifact( macDepArtifact.artifact );

        MockAndControlForProjectBuilder macPB = new MockAndControlForProjectBuilder( mm );

        macPB.expectBuildFromRepository( macDepArtifact.artifact, depProject );

        project.setArtifacts( new HashSet( Collections.singleton( macDepArtifact.artifact ) ) );

        Set projects = Collections.singleton( project );

        mm.replayAll();

        Logger overrideLogger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( overrideLogger, macPB.projectBuilder ).addModuleBinaries( binaries, projects, macTask.archiver,
                                                                               macTask.configSource, false );

        mm.verifyAll();
    }

    private final class MockAndControlForProjectBuilder
    {
        MavenProjectBuilder projectBuilder;

        MockControl control;

        public MockAndControlForProjectBuilder( MockManager mm )
        {
            control = MockControl.createControl( MavenProjectBuilder.class );
            mm.add( control );

            projectBuilder = ( MavenProjectBuilder ) control.getMock();
        }

        void expectBuildFromRepository( Artifact artifact, MavenProject result )
        {
            try
            {
                projectBuilder.buildFromRepository( artifact, null, null );
            }
            catch ( ProjectBuildingException e )
            {
                Assert.fail( "should never happen" );
            }

            control.setMatcher( FIRST_PARAM_MATCHER );
            control.setReturnValue( result, MockControl.ONE_OR_MORE );
        }
    }

    private static final ArgumentsMatcher FIRST_PARAM_MATCHER = new ArgumentsMatcher()
    {

        public boolean matches( Object[] test, Object[] args )
        {
            if ( test.length < 1 && args.length < 1 )
            {
                return true;
            }

            if ( test[0] == null && args[0] == null )
            {
                return true;
            }

            if ( test[0].equals( args[0] ) )
            {
                return true;
            }

            return false;
        }

        public String toString( Object[] args )
        {
            return "with first argument "
                            + ( args.length > 0 ? " of \'" + String.valueOf( args[0] ) + "\'" : "missing" );
        }

    };

    public void testCollectExcludesFromQueuedArtifacts_ShouldAddOneExclusion()
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        List excludes = new ArrayList();

        // nothing up this sleeve...
        assertTrue( excludes.isEmpty() );

        mm.replayAll();

        Set artifactIds = Collections.singleton( "group:artifact:jar" );

        List result = createPhase( macLogger.logger, null ).collectExcludesFromQueuedArtifacts( artifactIds, excludes );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testCollectExcludesFromQueuedArtifacts_ShouldHandleNullExcludesList()
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        mm.replayAll();

        Set artifactIds = Collections.singleton( "group:artifact:jar" );

        List result = createPhase( macLogger.logger, null ).collectExcludesFromQueuedArtifacts( artifactIds, null );

        assertEquals( 1, result.size() );

        assertEquals( "group:artifact:jar", result.get( 0 ) );

        mm.verifyAll();
    }

    public void testAddArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws AssemblyFormattingException, IOException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );
        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        macTask.expectArtifactGetFile( false );

        macTask.artifact.getId();
        macTask.artifactCtl.setReturnValue( "group:artifact:type:version" );

        mm.replayAll();

        try
        {
            createPhase( macLogger.logger, null ).addArtifact( macTask.artifact, null, null, null, null, false );

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
        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( macTask.artifact );

        macTask.expectArtifactGetFile();
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetClassifier( null );
        macTask.expectGetArtifactHandler();

        macTask.expectAddFile( "out/artifact", Integer.parseInt( "777", 8 ) );

        ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );

        mm.replayAll();

        createPhase( macLogger.logger, null ).addArtifact( macTask.artifact, project, macTask.archiver,
                                                           macTask.configSource, binaries, false );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );

        mm.replayAll();

        createPhase( macLogger.logger, null ).addModuleSourceFileSets( null, null, null, null, false );

        mm.verifyAll();
    }

    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        MockManager mm = new MockManager();

        MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mm, fileManager );

        MavenProject project = createProject( "group", "artifact", "version", null );

        Set projects = Collections.singleton( project );

        ModuleSources sources = new ModuleSources();
        sources.setDirectory( "/src" );
        sources.setDirectoryMode( "777" );
        sources.setFileMode( "777" );

        macTask.expectGetArchiveBaseDirectory();

        int mode = Integer.parseInt( "777", 8 );
        int[] modes = { -1, -1, mode, mode };

        macTask.expectAdditionOfSingleFileSet( project, project.getBasedir(), "final-name", false, modes, 1, true,
                                               false );

        mm.replayAll();
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleSourceFileSets( sources, projects, macTask.archiver,
                                                                       macTask.configSource, false );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );

        List projects = Collections.singletonList( project );

        macCS.expectGetProject( project );
        macCS.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects = createPhase( macLogger.logger, null ).getModuleProjects( moduleSet, macCS.configSource );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", null );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macCS.expectGetProject( project );
        macCS.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects = createPhase( macLogger.logger, null ).getModuleProjects( moduleSet, macCS.configSource );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", project );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );

        macCS.expectGetProject( project );
        macCS.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects = createPhase( macLogger.logger, null ).getModuleProjects( moduleSet, macCS.configSource );

        assertFalse( moduleProjects.isEmpty() );

        MavenProject result = ( MavenProject ) moduleProjects.iterator().next();

        assertEquals( "artifact2", result.getArtifactId() );

        mm.verifyAll();
    }

    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

        MavenProject project = createProject( "group", "artifact", "version", null );
        MavenProject project2 = createProject( "group", "artifact2", "version", project );
        MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        List projects = new ArrayList();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macCS.expectGetProject( project );
        macCS.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        mm.replayAll();

        Set moduleProjects = createPhase( macLogger.logger, null ).getModuleProjects( moduleSet, macCS.configSource );

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

        MockAndControlForLogger macLogger = new MockAndControlForLogger( mm );
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource( mm );

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

        macCS.expectGetProject( project );
        macCS.expectGetReactorProjects( projects );

        ModuleSet moduleSet = new ModuleSet();

        moduleSet.addExclude( "group:artifact2" );

        mm.replayAll();

        Set moduleProjects = createPhase( macLogger.logger, null ).getModuleProjects( moduleSet, macCS.configSource );

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

    private ModuleSetAssemblyPhase createPhase( Logger logger, MavenProjectBuilder projectBuilder )
    {
        ModuleSetAssemblyPhase phase = new ModuleSetAssemblyPhase( projectBuilder );

        phase.enableLogging( logger );

        return phase;
    }

    private final class MockAndControlForArtifact
    {
        Artifact artifact;

        MockControl control;

        File artifactFile;

        ArtifactHandler handler;

        MockControl handlerControl;

        private final MockManager mm;

        MockAndControlForArtifact( MockManager mm )
        {
            this.mm = mm;
            control = MockControl.createControl( Artifact.class );
            mm.add( control );

            artifact = ( Artifact ) control.getMock();
        }

        void expectGetClassifier()
        {
            artifact.getClassifier();
            control.setReturnValue( null, MockControl.ONE_OR_MORE );
        }

        void expectGetArtifactHandler()
        {
            handlerControl = MockControl.createControl( ArtifactHandler.class );
            mm.add( handlerControl );

            handler = ( ArtifactHandler ) handlerControl.getMock();

            artifact.getArtifactHandler();
            control.setReturnValue( handler, MockControl.ONE_OR_MORE );
        }

        void expectGetFile()
            throws IOException
        {
            this.artifactFile = fileManager.createTempFile();

            artifact.getFile();

            control.setReturnValue( artifactFile, MockControl.ONE_OR_MORE );
        }

        void expectGetId( String id )
        {
            artifact.getId();
            control.setReturnValue( id, MockControl.ONE_OR_MORE );
        }

        void expectGetDependencyTrail( List depTrail )
        {
            artifact.getDependencyTrail();
            control.setReturnValue( new LinkedList( depTrail ), MockControl.ONE_OR_MORE );
        }

        void expectGetDependencyConflictId( String groupId, String artifactId, String packaging )
        {
            artifact.getDependencyConflictId();
            control.setReturnValue( groupId + ":" + artifactId + ":" + packaging, MockControl.ONE_OR_MORE );
        }

        void expectGetGroupId( String groupId )
        {
            artifact.getGroupId();
            control.setReturnValue( groupId, MockControl.ONE_OR_MORE );
        }

        void expectGetArtifactId( String artifactId )
        {
            artifact.getArtifactId();
            control.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
        }
    }

    private final class MockAndControlForLogger
    {
        Logger logger;

        MockControl control;

        public MockAndControlForLogger( MockManager mockManager )
        {
            control = MockControl.createControl( Logger.class );
            mockManager.add( control );

            logger = ( Logger ) control.getMock();
        }

        public void expectWarn()
        {
            logger.warn( null );
            control.setMatcher( MockControl.ALWAYS_MATCHER );
            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }

        public void expectDebug( boolean debugCheck, boolean debugEnabled )
        {
            if ( debugCheck )
            {
                logger.isDebugEnabled();
                control.setReturnValue( debugEnabled, MockControl.ONE_OR_MORE );
            }

            if ( !debugCheck || debugEnabled )
            {
                logger.debug( null );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
                control.setVoidCallable( MockControl.ONE_OR_MORE );
            }
        }
    }

    private final class MockAndControlForConfigSource
    {
        AssemblerConfigurationSource configSource;

        MockControl control;

        MockAndControlForConfigSource( MockManager mockManager )
        {
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = ( AssemblerConfigurationSource ) control.getMock();
        }

        public void expectGetReactorProjects( List projects )
        {
            configSource.getReactorProjects();
            control.setReturnValue( projects, MockControl.ONE_OR_MORE );
        }

        public void expectGetProject( MavenProject project )
        {
            configSource.getProject();
            control.setReturnValue( project, MockControl.ONE_OR_MORE );
        }
    }

}
