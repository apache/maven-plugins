package org.apache.maven.plugin.assembly.archive.phase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

public class DependencySetAssemblyPhaseTest
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();
    
    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
    {
        MavenProject project = new MavenProject( new Model() );
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl();
        
        mac.enableGetScope( Artifact.SCOPE_COMPILE );
        
        project.setArtifacts( Collections.singleton( mac.artifact ) );
        
        DependencySet dependencySet = new DependencySet();
        
        DependencySetAssemblyPhase phase = new DependencySetAssemblyPhase();
        phase.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        mockManager.replayAll();
        
        Set result = phase.getDependencyArtifacts( project, dependencySet );
        
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );
        
        mockManager.verifyAll();
    }
    
    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude()
    {
        MavenProject project = new MavenProject( new Model() );
        
        Set artifacts = new HashSet();
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl();
        
        mac.enableGetGroupId( "group" );
        mac.enableGetArtifactId( "artifact" );
        mac.enableGetDependencyConflictId( "group:artifact:jar" );
        mac.enableGetScope( Artifact.SCOPE_COMPILE );
        
        artifacts.add( mac.artifact );
        
        ArtifactMockAndControl mac2 = new ArtifactMockAndControl();
        
        mac2.enableGetGroupId( "group2" );
        mac2.enableGetArtifactId( "artifact2" );
        mac2.enableGetDependencyConflictId( "group2:artifact2:jar" );
        mac2.enableGetScope( Artifact.SCOPE_COMPILE );
        mac2.enableGetDependencyTrail( Collections.EMPTY_LIST );
        mac2.enableGetId( "group2:artifact2:1.0" );
        
        artifacts.add( mac2.artifact );
        
        project.setArtifacts( artifacts );
        
        DependencySet dependencySet = new DependencySet();
        
        dependencySet.addInclude( "group:artifact" );
        
        DependencySetAssemblyPhase phase = new DependencySetAssemblyPhase();
        phase.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        mockManager.replayAll();
        
        Set result = phase.getDependencyArtifacts( project, dependencySet );
        
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );
        
        mockManager.verifyAll();
    }
    
    private final class ArtifactMockAndControl
    {
        Artifact artifact;
        MockControl control;
        
        public ArtifactMockAndControl()
        {
            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );
            
            artifact = (Artifact) control.getMock();
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

        public void enableGetScope( String scope )
        {
            artifact.getScope();
            control.setReturnValue( scope, MockControl.ONE_OR_MORE );
        }
    }

}
