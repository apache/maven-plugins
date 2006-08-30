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
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jmock.Mock;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckDependencySnapshotsPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );
    }

    public void testNoSnapshotDependencies()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "no-snapshot-dependencies" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-snapshot-dependencies" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReleasePluginNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "snapshot-release-plugin" );
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveDeclined()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveAccepted()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "yes" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "yes" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        assertTrue( true );
    }

    public void testSnapshotReleasePluginInteractiveInvalid()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "donkey" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "donkey" ) );

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveException()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }
    }

    public void testSnapshotDependenciesInProjectOnlyMismatchedVersion()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor =
            createDescriptorFromProjects( "internal-differing-snapshot-dependencies" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotManagedDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor =
            createDescriptorFromProjects( "unused-internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor =
            createDescriptorFromProjects( "unused-external-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-managed-snapshot-dependency" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-snapshot-dependencies" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesInsideAndOutsideProject()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor =
            createDescriptorFromProjects( "internal-and-external-snapshot-dependencies" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotReportPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "no-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsOutsideProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-snapshot-report-plugins" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReportPluginsInsideAndOutsideProject()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor =
            createDescriptorFromProjects( "internal-and-external-snapshot-report-plugins" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "no-snapshot-plugins" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-snapshot-plugins" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotManagedPluginInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "unused-internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "unused-external-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-managed-snapshot-plugin" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsOutsideProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-snapshot-plugins" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsInsideAndOutsideProject()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-and-external-snapshot-plugins" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotExternalParent()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-snapshot-parent/child" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testReleaseExternalParent()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-parent/child" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-snapshot-extension" );

        try
        {
            phase.execute( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, getReactorProjects() );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotInternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "internal-snapshot-extension" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testReleaseExternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = createDescriptorFromProjects( "external-extension" );

        phase.execute( releaseDescriptor, null, getReactorProjects() );

        phase.simulate( releaseDescriptor, null, getReactorProjects() );

        // successful execution is verification enough
        assertTrue( true );
    }

    private ReleaseDescriptor createDescriptorFromProjects( String path )
        throws Exception
    {
        return createDescriptorFromProjects( "check-dependencies/", path, true );
    }

}
