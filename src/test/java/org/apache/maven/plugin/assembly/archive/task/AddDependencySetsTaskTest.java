package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class AddDependencySetsTaskTest
    extends TestCase
{

    private MockManager mockManager = new MockManager();

    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException
    {
        MavenProject project = new MavenProject( new Model() );
        
        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );
        
        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.EMPTY_SET );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        mockManager.replayAll();
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), project, macTask.projectBuilder, macTask.dependencyResolver, logger );

        task.addDependencySet( ds, null, macTask.configSource, false );

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

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        macTask.expectArtifactGetFile();
        macTask.expectArtifactGetType( "jar" );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();
        
        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( macTask.artifact ) );

        if ( unpack )
        {
            macTask.expectAddArchivedFileSet( outputLocation + "/artifact/", null, null );
            macTask.expectModeChange( -1, -1, 8, 8, 2 );
        }
        else
        {
            macTask.expectAddFile( outputLocation + "/artifact", 8 );
        }

        macTask.expectCSGetFinalName( "final-name" );

        MavenProject depProject = new MavenProject( new Model() );

        macTask.expectBuildFromRepository( depProject );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), project, macTask.projectBuilder,
                                       macTask.dependencyResolver, logger );

        mockManager.replayAll();

        task.addDependencySet( ds, macTask.archiver, macTask.configSource, false );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
        throws ArchiveCreationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( macTask.artifact ) );
        
        project.setArtifacts( Collections.singleton( macTask.artifact ) );

        DependencySet dependencySet = new DependencySet();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), project, macTask.projectBuilder,
                                       macTask.dependencyResolver, logger );

        Set result = task.resolveDependencyArtifacts( dependencySet, macTask.configSource );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( macTask.artifact, result.iterator().next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude() throws ArchiveCreationException
    {
        MavenProject project = new MavenProject( new Model() );
        
        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        Set artifacts = new HashSet();

        MockAndControlForArtifact mac = new MockAndControlForArtifact();

        mac.enableGetGroupId( "group" );
        mac.enableGetArtifactId( "artifact" );
        mac.enableGetDependencyConflictId( "group:artifact:jar" );

        artifacts.add( mac.artifact );

        MockAndControlForArtifact mac2 = new MockAndControlForArtifact();

        mac2.enableGetGroupId( "group2" );
        mac2.enableGetArtifactId( "artifact2" );
        mac2.enableGetDependencyConflictId( "group2:artifact2:jar" );
        mac2.enableGetDependencyTrail( Collections.EMPTY_LIST );
        mac2.enableGetId( "group2:artifact2:1.0" );

        artifacts.add( mac2.artifact );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( artifacts );

        DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), project, null, macTask.dependencyResolver, logger );

        Set result = task.resolveDependencyArtifacts( dependencySet, macTask.configSource );

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

}
