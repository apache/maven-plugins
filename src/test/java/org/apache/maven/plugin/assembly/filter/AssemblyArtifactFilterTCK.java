package org.apache.maven.plugin.assembly.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;


public abstract class AssemblyArtifactFilterTCK
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();
    
    protected abstract ArtifactFilter createFilter( List patterns );

    protected abstract ArtifactFilter createFilter( List patterns, boolean actTransitively );
    
    public void testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId );
        
        mockManager.replayAll();
        
        ArtifactFilter filter = createFilter( Collections.singletonList( groupId + ":" + artifactId ) );
        
        if ( reverse )
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        else
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }

    public void testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId );
        
        mockManager.replayAll();
        
        ArtifactFilter filter = createFilter( Collections.singletonList( groupId + ":" + artifactId + ":jar" ) );
        
        if ( reverse )
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        else
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }
    
    public void testShouldNotIncludeWhenGroupIdDiffers(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId );
        
        mockManager.replayAll();
        List patterns = new ArrayList();
        
        
        patterns.add( "otherGroup:" + artifactId + ":jar" );
        patterns.add( "otherGroup:" + artifactId );
        
        ArtifactFilter filter = createFilter( patterns );

        if ( reverse )
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        else
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }
    
    public void testShouldNotIncludeWhenArtifactIdDiffers(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId );
        
        mockManager.replayAll();
        
        List patterns = new ArrayList();
        
        patterns.add( groupId + "otherArtifact:jar" );
        patterns.add( groupId + "otherArtifact" );
        
        ArtifactFilter filter = createFilter( patterns );

        if ( reverse )
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        else
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }
    
    public void testShouldNotIncludeWhenBothIdElementsDiffer(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId );
        
        mockManager.replayAll();
        
        List patterns = new ArrayList();
        
        patterns.add( "otherGroup:otherArtifact:jar" );
        patterns.add( "otherGroup:otherArtifact" );
        
        ArtifactFilter filter = createFilter( patterns );

        if ( reverse )
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        else
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }
    
    public void testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled(boolean reverse)
    {
        String groupId = "group";
        String artifactId = "artifact";
        
        String depTrailItem = "otherGroup:otherArtifact";
        List depTrail = Collections.singletonList( depTrailItem );
        
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId, depTrail );
        
        mockManager.replayAll();
        
        ArtifactFilter filter = createFilter( depTrail, true );

        if ( reverse )
        {
            assertFalse( filter.include( mac.artifact ) );
        }
        else
        {
            assertTrue( filter.include( mac.artifact ) );
        }
        
        mockManager.verifyAll();
    }
    
    private final class ArtifactMockAndControl
    {
        MockControl control;
        Artifact artifact;
        String groupId;
        String artifactId;
        List dependencyTrail;
        
        ArtifactMockAndControl( String groupId, String artifactId )
        {
            this( groupId, artifactId, null );
        }
        
        ArtifactMockAndControl( String groupId, String artifactId, List dependencyTrail )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.dependencyTrail = dependencyTrail;
            
            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );
            
            artifact = (Artifact) control.getMock();
            
            enableGetDependencyConflictId();
            enableGetGroupIdAndArtifactId();
            enableGetId();
            
            if ( dependencyTrail != null )
            {
                enableGetDependencyTrail();
            }
        }
        
        void enableGetId()
        {
            artifact.getId();
            control.setReturnValue( groupId + ":" + artifactId + ":type:version", MockControl.ZERO_OR_MORE );
        }
        
        void enableGetDependencyTrail()
        {
            artifact.getDependencyTrail();
            control.setReturnValue( dependencyTrail, MockControl.ONE_OR_MORE );
        }

        void enableGetDependencyConflictId()
        {
            artifact.getDependencyConflictId();
            control.setReturnValue( groupId + ":" + artifactId + ":jar", MockControl.ONE_OR_MORE );
        }
        
        void enableGetGroupIdAndArtifactId()
        {
            artifact.getGroupId();
            control.setReturnValue( groupId, MockControl.ONE_OR_MORE );
            
            artifact.getArtifactId();
            control.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
        }
    }

}
