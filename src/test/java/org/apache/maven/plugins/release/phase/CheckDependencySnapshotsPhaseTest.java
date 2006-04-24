package org.apache.maven.plugins.release.phase;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckDependencySnapshotsPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    private MavenProjectBuilder projectBuilder;

    private ArtifactRepository localRepository;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        String localRepoPath = getTestFile( "target/local-repository" ).getAbsolutePath().replace( '\\', '/' );
        localRepository = new DefaultArtifactRepository( "local", "file://" + localRepoPath, layout );
    }

    public void testNoSnapshotDependencies()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "no-snapshot-dependencies" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "internal-snapshot-dependencies" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotManagedDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-managed-snapshot-dependency" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedDependency()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "unused-internal-managed-snapshot-dependency" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedDependency()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "unused-external-managed-snapshot-dependency" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedDependency()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "external-managed-snapshot-dependency" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-snapshot-dependencies" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotDependenciesInsideAndOutsideProject()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-and-external-snapshot-dependencies" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testNoSnapshotReportPlugins()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "no-snapshot-report-plugins" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsInProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-snapshot-report-plugins" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsOutsideProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "external-snapshot-report-plugins" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotReportPluginsInsideAndOutsideProject()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-and-external-snapshot-report-plugins" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testNoSnapshotPlugins()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "no-snapshot-plugins" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotPluginsInProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "internal-snapshot-plugins" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotManagedPluginInProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-managed-snapshot-plugin" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedPlugin()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "unused-internal-managed-snapshot-plugin" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedPlugin()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "unused-external-managed-snapshot-plugin" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedPlugin()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "external-managed-snapshot-plugin" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotPluginsOutsideProjectOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-snapshot-plugins" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotPluginsInsideAndOutsideProject()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "internal-and-external-snapshot-plugins" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotExternalParent()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-snapshot-parent/child" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testReleaseExternalParent()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-parent/child" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalExtension()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-snapshot-extension" );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSnapshotInternalExtension()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "internal-snapshot-extension" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testReleaseExternalExtension()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "external-extension" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    private Map createManagedVersionMap( String projectId, DependencyManagement dependencyManagement,
                                         ArtifactFactory artifactFactory )
        throws ProjectBuildingException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(),
                                                                                  d.getClassifier(), d.getScope() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    private ReleaseConfiguration createConfigurationFromProjects( String path )
        throws Exception
    {
        Stack projectFiles = new Stack();
        projectFiles.push( getTestFile( "target/test-classes/projects/" + path + "/pom.xml" ) );

        List projects = new ArrayList();
        while ( !projectFiles.isEmpty() )
        {
            File file = (File) projectFiles.pop();

            MavenProject project = projectBuilder.build( file, localRepository, null );

            for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
            {
                String module = (String) i.next();

                projectFiles.push( new File( file.getParentFile(), module + "/pom.xml" ) );
            }

            projects.add( project );
        }

        List repos = Collections.singletonList( new DefaultArtifactRepository( "central", getTestFile(
            "src/test/remote-repository" ).toURL().toExternalForm(), new DefaultRepositoryLayout() ) );

        ProjectSorter sorter = new ProjectSorter( projects );

        projects = sorter.getSortedProjects();

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        ArtifactCollector artifactCollector = (ArtifactCollector) lookup( ArtifactCollector.class.getName() );
        ArtifactMetadataSource artifactMetadataSource = (ArtifactMetadataSource) lookup( ArtifactMetadataSource.ROLE );

        // pass back over and resolve dependencies - can't be done earlier as the order may not be correct
        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            project.setRemoteArtifactRepositories( repos );
            project.setPluginArtifactRepositories( repos );

            Artifact projectArtifact = project.getArtifact();

            Map managedVersions = createManagedVersionMap(
                ArtifactUtils.versionlessKey( projectArtifact.getGroupId(), projectArtifact.getArtifactId() ),
                project.getDependencyManagement(), artifactFactory );

            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );

            ArtifactResolutionResult result = artifactCollector.collect( project.getDependencyArtifacts(),
                                                                         projectArtifact, managedVersions,
                                                                         localRepository, repos, artifactMetadataSource,
                                                                         null, Collections.EMPTY_LIST );

            project.setArtifacts( result.getArtifacts() );
        }

        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setReactorProjects( projects );

        return releaseConfiguration;
    }

}
