package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AddDependencySetsTaskTest
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();

    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource();

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        MockAndControlForLogger macLog = new MockAndControlForLogger();

        macLog.expectInfo( "Processing DependencySet" );

        mockManager.replayAll();

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), project, null, macLog.logger );
        
        task.addDependencySet( ds, null, macCS.configSource, false );

        mockManager.verifyAll();
    }

    public void testAddDependencySet_ShouldAddOneDependencyFromProjectWithoutUnpacking()
        throws AssemblyFormattingException, ArchiveCreationException, IOException
    {
        verifyOneDependencyAdded( "out", false );
    }

    public void testAddDependencySet_ShouldAddOneDependencyFromProjectUnpacked()
        throws AssemblyFormattingException, ArchiveCreationException, IOException
    {
        verifyOneDependencyAdded( "out", true );
    }

    private void verifyOneDependencyAdded( String outputLocation, boolean unpack )
        throws AssemblyFormattingException, ArchiveCreationException, IOException
    {
        MavenProject project = new MavenProject( new Model() );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "artifact" );
        ds.setUnpack( unpack );
        ds.setScope( Artifact.SCOPE_COMPILE );

        ds.setDirectoryMode( Integer.toString( 8, 8 ) );
        ds.setFileMode( Integer.toString( 8, 8 ) );

        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mockManager );

        macTask.expectArtifactGetFile();
        macTask.expectArtifactGetScope( Artifact.SCOPE_COMPILE );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();

        if ( unpack )
        {
            macTask.expectAddArchivedFileSet( outputLocation + "/artifact/", null, null );
            macTask.expectModeChange( -1, -1, 8, 8, 2 );
        }
        else
        {
            macTask.expectAddFile( outputLocation + "/artifact", 8 );
        }

        project.setArtifacts( Collections.singleton( macTask.artifact ) );

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource();

        macCS.enableGetFinalName( "final-name" );
        macCS.enableGetRepositories( null, Collections.EMPTY_LIST );
        
        MockAndControlForLogger macLog = new MockAndControlForLogger();

        macLog.expectInfo( "Processing DependencySet" );

        MockAndControlForProjectBuilder macPB = new MockAndControlForProjectBuilder();
        
        MavenProject depProject = new MavenProject( new Model() );
        
        macPB.expectBuildFromRepository( depProject );

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), project, macPB.projectBuilder, macLog.logger );
        
        mockManager.replayAll();

        task.addDependencySet( ds, macTask.archiver, macCS.configSource, false );

        mockManager.verifyAll();
    }
    
    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForArtifact mac = new MockAndControlForArtifact();

        mac.expectGetScope( Artifact.SCOPE_COMPILE );

        project.setArtifacts( Collections.singleton( mac.artifact ) );

        DependencySet dependencySet = new DependencySet();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        MockAndControlForProjectBuilder macPB = new MockAndControlForProjectBuilder();

        mockManager.replayAll();

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), project, macPB.projectBuilder, logger );
        
        Set result = task.getDependencyArtifacts( project, dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude()
    {
        MavenProject project = new MavenProject( new Model() );

        Set artifacts = new HashSet();

        MockAndControlForArtifact mac = new MockAndControlForArtifact();

        mac.enableGetGroupId( "group" );
        mac.enableGetArtifactId( "artifact" );
        mac.enableGetDependencyConflictId( "group:artifact:jar" );
        mac.expectGetScope( Artifact.SCOPE_COMPILE );

        artifacts.add( mac.artifact );

        MockAndControlForArtifact mac2 = new MockAndControlForArtifact();

        mac2.enableGetGroupId( "group2" );
        mac2.enableGetArtifactId( "artifact2" );
        mac2.enableGetDependencyConflictId( "group2:artifact2:jar" );
        mac2.expectGetScope( Artifact.SCOPE_COMPILE );
        mac2.enableGetDependencyTrail( Collections.EMPTY_LIST );
        mac2.enableGetId( "group2:artifact2:1.0" );

        artifacts.add( mac2.artifact );

        project.setArtifacts( artifacts );

        DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), project, null, logger );
        
        Set result = task.getDependencyArtifacts( project, dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );

        mockManager.verifyAll();
    }

    private final class MockAndControlForArtifact
    {
        Artifact artifact;

        MockControl control;

        public MockAndControlForArtifact()
        {
            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );

            artifact = ( Artifact ) control.getMock();
        }

        public void enableGetId( String id )
        {
            artifact.getId();
            control.setReturnValue( id, MockControl.ONE_OR_MORE );
        }

        public void enableGetDependencyTrail( List dependencyTrail )
        {
            artifact.getDependencyTrail();
            control.setReturnValue( dependencyTrail, MockControl.ONE_OR_MORE );
        }

        public void enableGetDependencyConflictId( String conflictId )
        {
            artifact.getDependencyConflictId();
            control.setReturnValue( conflictId, MockControl.ONE_OR_MORE );
        }

        public void enableGetArtifactId( String artifactId )
        {
            artifact.getArtifactId();
            control.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
        }

        public void enableGetGroupId( String groupId )
        {
            artifact.getGroupId();
            control.setReturnValue( groupId, MockControl.ONE_OR_MORE );
        }

        public void expectGetScope( String scope )
        {
            artifact.getScope();
            control.setReturnValue( scope, MockControl.ONE_OR_MORE );
        }
    }

    private final class MockAndControlForConfigSource
    {
        AssemblerConfigurationSource configSource;

        MockControl control;

        MockAndControlForConfigSource()
        {
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = ( AssemblerConfigurationSource ) control.getMock();
        }

        public void enableGetRepositories( ArtifactRepository localRepo, List remoteRepos )
        {
            configSource.getLocalRepository();
            control.setReturnValue( localRepo, MockControl.ONE_OR_MORE );
            
            configSource.getRemoteRepositories();
            control.setReturnValue( remoteRepos, MockControl.ONE_OR_MORE );
        }

        public void enableGetFinalName( String finalName )
        {
            configSource.getFinalName();
            control.setReturnValue( finalName, MockControl.ONE_OR_MORE );
        }

        void enableGetProject( MavenProject project )
        {
            configSource.getProject();
            control.setReturnValue( project, MockControl.ONE_OR_MORE );
        }
    }

    private final class MockAndControlForLogger
    {
        Logger logger;

        MockControl control;

        MockAndControlForLogger()
        {
            control = MockControl.createControl( Logger.class );
            mockManager.add( control );

            logger = ( Logger ) control.getMock();
            
            logger.isDebugEnabled();
            control.setReturnValue( true, MockControl.ZERO_OR_MORE );
            
            logger.debug( null );
            control.setMatcher( MockControl.ALWAYS_MATCHER );
            control.setVoidCallable( MockControl.ZERO_OR_MORE );
        }

        void expectInfo( String message )
        {
            logger.info( message );
            control.setVoidCallable( MockControl.ONE_OR_MORE );
        }
    }
    
    private final class MockAndControlForProjectBuilder
    {
        MavenProjectBuilder projectBuilder;
        
        MockControl control;
        
        public MockAndControlForProjectBuilder()
        {
            control = MockControl.createControl( MavenProjectBuilder.class );
            mockManager.add( control );
            
            projectBuilder = ( MavenProjectBuilder ) control.getMock();
        }

        public void expectBuildFromRepository( MavenProject project )
        {
            try
            {
                projectBuilder.buildFromRepository( null, null, null );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
                control.setReturnValue( project, MockControl.ONE_OR_MORE );
            }
            catch ( ProjectBuildingException e )
            {
                Assert.fail( "should never happen" );
            }
        }
    }

}
