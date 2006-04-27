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

import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckDependencySnapshotsPhaseTest
    extends AbstractReleaseTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );
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

    public void testSnapshotDependenciesInProjectOnlyMismatchedVersion()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "internal-differing-snapshot-dependencies" );

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

    private ReleaseConfiguration createConfigurationFromProjects( String path )
        throws Exception
    {
        return createConfigurationFromProjects( "check-dependencies/", path );
    }

}
