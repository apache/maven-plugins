package org.apache.maven.plugin.assembly.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.easymock.MockControl;

import junit.framework.TestCase;


public class AssemblyScopeArtifactFilterTest
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();
    
    public void testScopesShouldIncludeArtifactWithSameScope()
    {
        verifyIncluded( Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE );
        verifyIncluded( Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED );
        verifyIncluded( Artifact.SCOPE_RUNTIME, Artifact.SCOPE_RUNTIME );
        verifyIncluded( Artifact.SCOPE_SYSTEM, Artifact.SCOPE_SYSTEM );
        verifyIncluded( Artifact.SCOPE_TEST, Artifact.SCOPE_TEST );
        verifyIncluded( null, null );
    }
    
    public void testCompileScopeShouldIncludeOnlyArtifactsWithNullSystemProvidedOrCompileScopes()
    {
        String scope = Artifact.SCOPE_COMPILE;
        
        verifyIncluded( scope, null );
        verifyIncluded( scope, Artifact.SCOPE_COMPILE );
        verifyIncluded( scope, Artifact.SCOPE_PROVIDED );
        verifyIncluded( scope, Artifact.SCOPE_SYSTEM );

        verifyExcluded( scope, Artifact.SCOPE_RUNTIME );
        verifyExcluded( scope, Artifact.SCOPE_TEST );
    }
    
    public void testRuntimeScopeShouldIncludeOnlyArtifactsWithNullRuntimeOrCompileScopes()
    {
        String scope = Artifact.SCOPE_RUNTIME;
        
        verifyIncluded( scope, null );
        verifyIncluded( scope, Artifact.SCOPE_COMPILE );
        verifyIncluded( scope, Artifact.SCOPE_RUNTIME );
        
        verifyExcluded( scope, Artifact.SCOPE_PROVIDED );
        verifyExcluded( scope, Artifact.SCOPE_SYSTEM );
        verifyExcluded( scope, Artifact.SCOPE_TEST );
    }
    
    public void testTestScopeShouldIncludeAllScopes()
    {
        String scope = Artifact.SCOPE_TEST;
        
        verifyIncluded( scope, null );
        verifyIncluded( scope, Artifact.SCOPE_COMPILE );
        verifyIncluded( scope, Artifact.SCOPE_RUNTIME );
        
        verifyIncluded( scope, Artifact.SCOPE_PROVIDED );
        verifyIncluded( scope, Artifact.SCOPE_SYSTEM );
        verifyIncluded( scope, Artifact.SCOPE_TEST );
    }
    
    public void testProvidedScopeShouldIncludeOnlyArtifactsWithNullOrProvidedScopes()
    {
        String scope = Artifact.SCOPE_PROVIDED;
        
        verifyIncluded( scope, null );
        verifyExcluded( scope, Artifact.SCOPE_COMPILE );
        verifyExcluded( scope, Artifact.SCOPE_RUNTIME );
        
        verifyIncluded( scope, Artifact.SCOPE_PROVIDED );
        
        verifyExcluded( scope, Artifact.SCOPE_SYSTEM );
        verifyExcluded( scope, Artifact.SCOPE_TEST );
    }
    
    public void testSystemScopeShouldIncludeOnlyArtifactsWithNullOrSystemScopes()
    {
        String scope = Artifact.SCOPE_SYSTEM;
        
        verifyIncluded( scope, null );
        verifyExcluded( scope, Artifact.SCOPE_COMPILE );
        verifyExcluded( scope, Artifact.SCOPE_RUNTIME );
        verifyExcluded( scope, Artifact.SCOPE_PROVIDED );
        
        verifyIncluded( scope, Artifact.SCOPE_SYSTEM );
        
        verifyExcluded( scope, Artifact.SCOPE_TEST );
    }
    
    private void verifyIncluded( String filterScope, String artifactScope )
    {
        ArtifactMockAndControl mac = new ArtifactMockAndControl( artifactScope );
        
        mockManager.replayAll();
        
        ArtifactFilter filter = new AssemblyScopeArtifactFilter( filterScope );
        
        assertTrue( "Artifact scope: " + artifactScope + " NOT included using filter scope: " + filterScope, filter.include( mac.artifact ) );
        
        mockManager.verifyAll();
        
        // enable multiple calls to this method within a single test.
        mockManager.clear();
    }

    private void verifyExcluded( String filterScope, String artifactScope )
    {
        ArtifactMockAndControl mac = new ArtifactMockAndControl( artifactScope );
        
        mockManager.replayAll();
        
        ArtifactFilter filter = new AssemblyScopeArtifactFilter( filterScope );
        
        assertFalse( "Artifact scope: " + artifactScope + " NOT excluded using filter scope: " + filterScope, filter.include( mac.artifact ) );
        
        mockManager.verifyAll();
        
        // enable multiple calls to this method within a single test.
        mockManager.clear();
    }

    private final class ArtifactMockAndControl
    {
        Artifact artifact;
        MockControl control;
        private final String scope;
        
        ArtifactMockAndControl( String scope )
        {
            this.scope = scope;
            
            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );
            
            artifact = (Artifact) control.getMock();
            
            enableGetScope();
            enableGetId();
        }
        
        void enableGetScope()
        {
            artifact.getScope();
            control.setReturnValue( scope, MockControl.ONE_OR_MORE );
        }
        
        void enableGetId()
        {
            artifact.getId();
            control.setReturnValue( "group:artifact:type:version", MockControl.ZERO_OR_MORE );
        }
    }

}
