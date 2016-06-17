package org.apache.maven.plugins.assembly.utils;

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

import org.hamcrest.Matchers;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.classextension.EasyMockSupport;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.expect;

public class FilterUtilsTest
    extends TestCase
{

    private final EasyMockSupport mockManager = new EasyMockSupport();

    private Logger logger;

    private static Model buildModel( final String groupId, final String artifactId )
    {
        final Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );

        return model;
    }

    @Override
    public void setUp()
    {
        clearAll();
    }

    private void clearAll()
    {
        logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
    }

    public void testFilterArtifacts_ShouldThrowExceptionUsingStrictModeWithUnmatchedInclude()
    {
        final Artifact artifact = mockManager.createMock( Artifact.class );

        expect( artifact.getGroupId() ).andReturn( "group" ).atLeastOnce();

        expect( artifact.getArtifactId() ).andReturn( "artifact" ).atLeastOnce();

        expect( artifact.getId() ).andReturn( "group:artifact:type:version" ).atLeastOnce();

        expect( artifact.getDependencyConflictId() ).andReturn( "group:artifact:type" ).atLeastOnce();

        final List<String> includes = new ArrayList<String>();

        includes.add( "other.group:other-artifact:type:version" );

        final List<String> excludes = Collections.emptyList();

        final Set<Artifact> artifacts = new HashSet<Artifact>();
        artifacts.add( artifact );

        mockManager.replayAll();

        try
        {
            FilterUtils.filterArtifacts( artifacts, includes, excludes, true, false, logger );

            fail( "Should fail because of unmatched include." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
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
        verifyArtifactInclusion( "group", "artifact", "group:dependentArtifact", null,
                                 Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ),
                                 null );
    }

    public void testFilterArtifacts_ShouldRemoveArtifactTransitivelyExcluded()
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:dependentArtifact",
                                 Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ),
                                 null );
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
        final ArtifactFilter filter = new ArtifactFilter()
        {

            public boolean include( final Artifact artifact )
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
        verifyProjectInclusion( "group", "artifact", "group:dependentArtifact", null,
                                Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ) );
    }

    public void testFilterProjects_ShouldRemoveProjectTransitivelyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:dependentArtifact",
                                Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ) );
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

    public void testTransitiveScopes()
    {
        Assert.assertThat( FilterUtils.newScopeFilter( "compile" ).getIncluded(),
                           Matchers.containsInAnyOrder( "compile", "provided", "system" ) );

        Assert.assertThat( FilterUtils.newScopeFilter( "provided" ).getIncluded(),
                           Matchers.containsInAnyOrder( "provided" ) );

        Assert.assertThat( FilterUtils.newScopeFilter( "system" ).getIncluded(),
                           Matchers.containsInAnyOrder( "system" ) );

        Assert.assertThat( FilterUtils.newScopeFilter( "runtime" ).getIncluded(),
                           Matchers.containsInAnyOrder( "compile", "runtime" ) );

        Assert.assertThat( FilterUtils.newScopeFilter( "test" ).getIncluded(),
                           Matchers.containsInAnyOrder( "compile", "provided", "runtime", "system", "test" ) );

    }
    
    private void verifyArtifactInclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true,
                                 additionalFilter );
    }

    private void verifyArtifactExclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false,
                                 additionalFilter );
    }

    private void verifyArtifactFiltering( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final boolean verifyInclusion, final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        final ArtifactMockAndControl mac = new ArtifactMockAndControl( groupId, artifactId, depTrail );

        mockManager.replayAll();

        List<String> inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.emptyList();
        }

        List<String> exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.emptyList();
        }

        final Set<Artifact> artifacts = new HashSet<Artifact>();
        artifacts.add( mac.artifact );

        FilterUtils.filterArtifacts( artifacts, inclusions, exclusions, false, depTrail != null, logger,
                                     additionalFilter );

        if ( verifyInclusion )
        {
            assertEquals( 1, artifacts.size() );
            assertEquals( mac.artifact.getDependencyConflictId(),
                          artifacts.iterator().next().getDependencyConflictId() );
        }
        else
        {
            // just make sure this trips, to meet the mock's expectations.
            mac.artifact.getDependencyConflictId();

            assertTrue( artifacts.isEmpty() );
        }

        mockManager.verifyAll();

        // get ready for multiple calls per test.
        mockManager.resetAll();
    }

    private void verifyProjectInclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true );
    }

    private void verifyProjectExclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false );
    }

    private void verifyProjectFiltering( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail,
                                         final boolean verifyInclusion )
    {
        final ProjectWithArtifactMockControl pmac = new ProjectWithArtifactMockControl( groupId, artifactId, depTrail );

        mockManager.replayAll();

        // make sure the mock is satisfied...you can't disable this expectation.
        pmac.mac.artifact.getDependencyConflictId();

        final Set<MavenProject> projects = new HashSet<MavenProject>();
        projects.add( pmac );

        List<String> inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.emptyList();
        }

        List<String> exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.emptyList();
        }

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        Set<MavenProject> result =
            FilterUtils.filterProjects( projects, inclusions, exclusions, depTrail != null, logger );

        if ( verifyInclusion )
        {
            assertEquals( 1, result.size() );
            assertEquals( pmac.getId(), result.iterator().next().getId() );
        }
        else
        {
            assertTrue( result.isEmpty() );
        }

        mockManager.verifyAll();

        // get ready for multiple calls per test.
        mockManager.resetAll();
    }

    private final class ProjectWithArtifactMockControl
        extends MavenProject
    {
        final ArtifactMockAndControl mac;

        ProjectWithArtifactMockControl( final String groupId, final String artifactId, final List<String> depTrail )
        {
            super( buildModel( groupId, artifactId ) );

            mac = new ArtifactMockAndControl( groupId, artifactId, depTrail );

            setArtifact( mac.artifact );

            setVersion( "1.0" );
        }

    }

    private final class ArtifactMockAndControl
    {
        final Artifact artifact;

        final String groupId;

        final String artifactId;

        final List<String> dependencyTrail;

        ArtifactMockAndControl( final String groupId, final String artifactId, final List<String> dependencyTrail )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.dependencyTrail = dependencyTrail;

            artifact = mockManager.createMock( Artifact.class );

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
            expect( artifact.getDependencyTrail() ).andReturn( dependencyTrail ).anyTimes();
        }

        void enableGetDependencyConflictId()
        {
            expect( artifact.getDependencyConflictId() ).andReturn( groupId + ":" + artifactId + ":jar" ).anyTimes();
        }

        void enableGetGroupIdArtifactIdAndId()
        {
            expect( artifact.getGroupId() ).andReturn( groupId ).anyTimes();

            expect( artifact.getArtifactId() ).andReturn( artifactId ).anyTimes();

            expect( artifact.getId() ).andReturn( groupId + ":" + artifactId + ":version:null:jar" ).anyTimes();
        }
    }

}
