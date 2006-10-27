package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class FilterUtilsTest
    extends TestCase
{

    private MockManager mockManager = new MockManager();

    private Logger logger;

    public void setUp()
    {
        clearAll();
    }

    private void clearAll()
    {
        mockManager.clear();

        logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
    }
    
    public void testFilterArtifacts_ShouldThrowExceptionUsingStrictModeWithUnmatchedInclude()
    {
        MockControl artifactCtl = MockControl.createControl( Artifact.class );
        Artifact artifact = (Artifact) artifactCtl.getMock();
        
        mockManager.add( artifactCtl );
        
        artifact.getGroupId();
        artifactCtl.setReturnValue( "group", MockControl.ONE_OR_MORE );
        
        artifact.getArtifactId();
        artifactCtl.setReturnValue( "artifact", MockControl.ONE_OR_MORE );
        
        artifact.getId();
        artifactCtl.setReturnValue( "group:artifact:type:version", MockControl.ONE_OR_MORE );

        artifact.getDependencyConflictId();
        artifactCtl.setReturnValue( "group:artifact:type", MockControl.ONE_OR_MORE );
        
        List includes = new ArrayList();
        
        includes.add( "other.group:other-artifact:type:version" );
        
        List excludes = Collections.EMPTY_LIST;
        
        Set artifacts = new HashSet();
        artifacts.add( artifact );
        
        mockManager.replayAll();
        
        try
        {
            FilterUtils.filterArtifacts( artifacts, includes, excludes, true, false, Collections.EMPTY_LIST, logger );
            
            fail( "Should fail because of unmatched include." );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            // expected.
        }
        
        mockManager.verifyAll();
    }

    public void testFilterArtifacts_ShouldNotRemoveArtifactDirectlyIncluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactInclusion( "group", "artifact", "group:artifact", null, null, null );
        verifyArtifactInclusion( "group", "artifact", "group:artifact:jar", null, null, null );
    }

    public void testFilterArtifacts_ShouldNotRemoveArtifactTransitivelyIncluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactInclusion( "group", "artifact", "group:dependentArtifact", null, Collections
            .singletonList( "group:dependentArtifact:jar:version" ), null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactTransitivelyExcluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:dependentArtifact", Collections
            .singletonList( "group:dependentArtifact:jar:version" ), null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactDirectlyExcluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:artifact", null, null );

        clearAll();

        verifyArtifactExclusion( "group", "artifact", null, "group:artifact:jar", null, null );
    }

    public void testFilterArtifacts_ShouldNotRemoveArtifactNotIncludedAndNotExcluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactInclusion( "group", "artifact", null, null, null, null );
        verifyArtifactInclusion( "group", "artifact", null, null, null, null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactExcludedByAdditionalFilter()
        throws InvalidAssemblerConfigurationException
    {
        ArtifactFilter filter = new ArtifactFilter()
        {

            public boolean include( Artifact artifact )
            {
                return false;
            }

        };

        verifyArtifactExclusion( "group", "artifact", "fail:fail", null, null, filter );
    }

    public void testFilterProjects_ShouldNotRemoveProjectDirectlyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:artifact", null, null );
        verifyProjectInclusion( "group", "artifact", "group:artifact:jar", null, null );
    }

    public void testFilterProjects_ShouldNotRemoveProjectTransitivelyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:dependentArtifact", null, Collections
            .singletonList( "group:dependentArtifact:jar:version" ) );
    }

    public void testFilterProjects_ShouldRemoveProjectTransitivelyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:dependentArtifact", Collections
            .singletonList( "group:dependentArtifact:jar:version" ) );
    }

    public void testFilterProjects_ShouldRemoveProjectDirectlyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:artifact", null );
        verifyProjectExclusion( "group", "artifact", null, "group:artifact:jar", null );
    }

    public void testFilterProjects_ShouldNotRemoveProjectNotIncludedAndNotExcluded()
    {
        verifyProjectInclusion( "group", "artifact", null, null, null );
        verifyProjectInclusion( "group", "artifact", null, null, null );
    }

    private void verifyArtifactInclusion( String groupId, String artifactId, String inclusionPattern,
                                          String exclusionPattern, List depTrail, ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true,
                                 additionalFilter );
    }

    private void verifyArtifactExclusion( String groupId, String artifactId, String inclusionPattern,
                                          String exclusionPattern, List depTrail, ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false,
                                 additionalFilter );
    }

    private void verifyArtifactFiltering( String groupId, String artifactId, String inclusionPattern,
                                          String exclusionPattern, List depTrail, boolean verifyInclusion,
                                          ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId, depTrail );

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

        FilterUtils.filterArtifacts( artifacts, inclusions, exclusions, false, depTrail != null, filters, logger );

        if ( verifyInclusion )
        {
            assertEquals( 1, artifacts.size() );
            assertEquals( mac.artifact.getDependencyConflictId(), ( (Artifact) artifacts.iterator().next() )
                .getDependencyConflictId() );
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

    private void verifyProjectInclusion( String groupId, String artifactId, String inclusionPattern,
                                         String exclusionPattern, List depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true );
    }

    private void verifyProjectExclusion( String groupId, String artifactId, String inclusionPattern,
                                         String exclusionPattern, List depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false );
    }

    private void verifyProjectFiltering( String groupId, String artifactId, String inclusionPattern,
                                         String exclusionPattern, List depTrail, boolean verifyInclusion )
    {
        ProjectWithArtifactMockControl pmac = new ProjectWithArtifactMockControl( groupId, artifactId, depTrail );

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

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        FilterUtils.filterProjects( projects, inclusions, exclusions, depTrail != null, logger );

        if ( verifyInclusion )
        {
            assertEquals( 1, projects.size() );
            assertEquals( pmac.getId(), ( (MavenProject) projects.iterator().next() ).getId() );
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

        ProjectWithArtifactMockControl( String groupId, String artifactId, List depTrail )
        {
            super( buildModel( groupId, artifactId ) );

            mac = new ArtifactMockAndControl( groupId, artifactId, depTrail );

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

        ArtifactMockAndControl( String groupId, String artifactId, List dependencyTrail )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.dependencyTrail = dependencyTrail;

            control = MockControl.createControl( Artifact.class );
            mockManager.add( control );

            artifact = (Artifact) control.getMock();

            // this is always enabled, for verification purposes.
            enableGetDependencyConflictId();
            enableGetGroupIdArtifactIdAndId();

            if ( dependencyTrail != null )
            {
                enableGetDependencyTrail();
            }
        }

        void enableGetDependencyTrail()
        {
            artifact.getDependencyTrail();
            control.setReturnValue( dependencyTrail, MockControl.ZERO_OR_MORE );
        }

        void enableGetDependencyConflictId()
        {
            artifact.getDependencyConflictId();
            control.setReturnValue( groupId + ":" + artifactId + ":jar", MockControl.ZERO_OR_MORE );
        }

        void enableGetGroupIdArtifactIdAndId()
        {
            artifact.getGroupId();
            control.setReturnValue( groupId, MockControl.ZERO_OR_MORE );

            artifact.getArtifactId();
            control.setReturnValue( artifactId, MockControl.ZERO_OR_MORE );

            artifact.getId();
            control.setReturnValue( groupId + ":" + artifactId + ":version:null:jar", MockControl.ZERO_OR_MORE );
        }
    }

}
