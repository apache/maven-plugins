package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.easymock.MockControl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class FilterUtilsTest
    extends TestCase
{

    private MockManager mockManager = new MockManager();
    
    public void testFilterArtifacts_ShouldNotRemoveArtifactDirectlyIncluded()
    {
        verifyArtifactInclusion( "group", "artifact", "group:artifact", null, null, null );
        verifyArtifactInclusion( "group", "artifact", "group:artifact:jar", null, null, null );
    }

    public void testFilterArtifacts_ShouldNotRemoveArtifactTransitivelyIncluded()
    {
        verifyArtifactInclusion( "group", "artifact", "group:dependentArtifact", null, Collections.singletonList( "group:dependentArtifact" ), null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactTransitivelyExcluded()
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:dependentArtifact", Collections.singletonList( "group:dependentArtifact" ), null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactDirectlyExcluded()
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:artifact", null, null );
        verifyArtifactExclusion( "group", "artifact", null, "group:artifact:jar", null, null );
    }

    public void testFilterArtifacts_ShouldNotRemoveArtifactNotIncludedAndNotExcluded()
    {
        verifyArtifactInclusion( "group", "artifact", null, null, null, null, false );
        verifyArtifactInclusion( "group", "artifact", null, null, null, null, false );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactExcludedByAdditionalFilter()
    {
        ArtifactFilter filter = new ArtifactFilter()
        {

            public boolean include( Artifact artifact )
            {
                return false;
            }
            
        };
        
        verifyArtifactExclusion( "group", "artifact", "fail:fail", null, null, filter, false );
    }

    
    
    
    
    public void testFilterProjects_ShouldNotRemoveProjectDirectlyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:artifact", null, null );
        verifyProjectInclusion( "group", "artifact", "group:artifact:jar", null, null );
    }

    public void testFilterProjects_ShouldNotRemoveProjectTransitivelyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:dependentArtifact", null, Collections.singletonList( "group:dependentArtifact" ) );
    }

    public void testFilterProjects_ShouldRemoveProjectTransitivelyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:dependentArtifact", Collections.singletonList( "group:dependentArtifact" ) );
    }

    public void testFilterProjects_ShouldRemoveProjectDirectlyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:artifact", null );
        verifyProjectExclusion( "group", "artifact", null, "group:artifact:jar", null );
    }

    public void testFilterProjects_ShouldNotRemoveProjectNotIncludedAndNotExcluded()
    {
        verifyProjectInclusion( "group", "artifact", null, null, null, false );
        verifyProjectInclusion( "group", "artifact", null, null, null, false );
    }

    private void verifyArtifactInclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, ArtifactFilter additionalFilter )
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true, true, additionalFilter );
    }

    private void verifyArtifactInclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, ArtifactFilter additionalFilter, boolean enableExpectations )
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true, enableExpectations, additionalFilter );
    }

    private void verifyArtifactExclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, ArtifactFilter additionalFilter )
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false, true, additionalFilter );
    }

    private void verifyArtifactExclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, ArtifactFilter additionalFilter, boolean enableExpectations )
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false, enableExpectations, additionalFilter );
    }

    private void verifyArtifactFiltering( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, boolean verifyInclusion, boolean enableExpectations, ArtifactFilter additionalFilter )
    {
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId, depTrail, enableExpectations );
        
        mockManager.replayAll();

        List inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.EMPTY_LIST;
        }

        List exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.EMPTY_LIST;
        }
        
        List filters;
        if ( additionalFilter != null )
        {
            filters = Collections.singletonList( additionalFilter );
        }
        else
        {
            filters = Collections.EMPTY_LIST;
        }
        
        Set artifacts = new HashSet();
        artifacts.add( mac.artifact );

        FilterUtils.filterArtifacts( artifacts, inclusions, exclusions, depTrail != null, filters );

        if ( verifyInclusion )
        {
            assertEquals( 1, artifacts.size() );
            assertEquals( mac.artifact.getDependencyConflictId(), ((Artifact) artifacts.iterator().next()).getDependencyConflictId() );
        }
        else
        {
            // just make sure this trips, to meet the mock's expectations.
            mac.artifact.getDependencyConflictId();
            
            assertTrue( artifacts.isEmpty() );
        }

        mockManager.verifyAll();
        
        // get ready for multiple calls per test.
        mockManager.clear();
    }

    private void verifyProjectInclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true, true );
    }

    private void verifyProjectInclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, boolean enableExpectations )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true, enableExpectations );
    }

    private void verifyProjectExclusion( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false, true );
    }

    private void verifyProjectFiltering( String groupId, String artifactId, String inclusionPattern, String exclusionPattern, List depTrail, boolean verifyInclusion, boolean enableExpectations )
    {
        ProjectWithArtifactMockControl pmac = new ProjectWithArtifactMockControl( groupId, artifactId, depTrail, enableExpectations );

        mockManager.replayAll();
        
        // make sure the mock is satisfied...you can't disable this expectation.
        pmac.mac.artifact.getDependencyConflictId();

        Set projects = new HashSet();
        projects.add( pmac );

        List inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.EMPTY_LIST;
        }

        List exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.EMPTY_LIST;
        }

        FilterUtils.filterProjects( projects, inclusions, exclusions, depTrail != null );

        if ( verifyInclusion )
        {
            assertEquals( 1, projects.size() );
            assertEquals( pmac.getId(), ((MavenProject) projects.iterator().next()).getId() );
        }
        else
        {
            assertTrue( projects.isEmpty() );
        }

        mockManager.verifyAll();
        
        // get ready for multiple calls per test.
        mockManager.clear();
    }

    private static Model buildModel( String groupId, String artifactId )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );

        return model;
    }

    private final class ProjectWithArtifactMockControl
        extends MavenProject
    {
        ArtifactMockAndControl mac;

        ProjectWithArtifactMockControl( String groupId, String artifactId, List depTrail, boolean enableExpectations )
        {
            super( buildModel( groupId, artifactId ) );

            mac = new ArtifactMockAndControl( groupId, artifactId, depTrail, enableExpectations );

            setArtifact( mac.artifact );
        }

    }

    private final class ArtifactMockAndControl
    {
        MockControl control;

        Artifact artifact;

        String groupId;

        String artifactId;

        List dependencyTrail;

        ArtifactMockAndControl( String groupId, String artifactId, List dependencyTrail, boolean enableExpectations )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.dependencyTrail = dependencyTrail;

            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );

            artifact = (Artifact) control.getMock();

            // this is always enabled, for verification purposes.
            enableGetDependencyConflictId();
            
            if ( enableExpectations )
            {
                enableGetGroupIdAndArtifactId();

                if ( dependencyTrail != null )
                {
                    enableGetDependencyTrail();
                }
            }
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
