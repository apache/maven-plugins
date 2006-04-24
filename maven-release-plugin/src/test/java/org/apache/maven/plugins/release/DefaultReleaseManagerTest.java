package org.apache.maven.plugins.release;

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

import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.plugins.release.config.ReleaseConfigurationStore;
import org.apache.maven.plugins.release.config.ReleaseConfigurationStoreException;
import org.apache.maven.plugins.release.phase.ReleasePhase;
import org.apache.maven.plugins.release.phase.ReleasePhaseStub;
import org.codehaus.plexus.PlexusTestCase;
import org.jmock.cglib.Mock;
import org.jmock.core.constraint.IsSame;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.io.IOException;
import java.util.Map;

/**
 * Test the default release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultReleaseManagerTest
    extends PlexusTestCase
{
    private ReleaseConfigurationStore configStore;


    protected void setUp()
        throws Exception
    {
        super.setUp();

        configStore = (ReleaseConfigurationStore) lookup( ReleaseConfigurationStore.ROLE, "stub" );
    }

    public void testPrepareNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( null );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "step1" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "step3" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertFalse( "step2 not executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertFalse( "step3 not executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "foo" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareSimulateNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test-dryRun" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( null );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test-dryRun" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "step1" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test-dryRun" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "step3" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertFalse( "step3 not simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "test-dryRun" );

        ReleaseConfiguration releaseConfiguration = configStore.read();
        releaseConfiguration.setCompletedPhase( "foo" );

        releaseManager.prepare( releaseConfiguration );

        Map phases = container.lookupMap( ReleasePhase.ROLE );

        ReleasePhaseStub phase = (ReleasePhaseStub) phases.get( "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) phases.get( "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareUnknownPhaseConfigured()
        throws Exception
    {
        ReleaseManager releaseManager = (ReleaseManager) lookup( ReleaseManager.ROLE, "bad-phase-configured" );

        try
        {
            releaseManager.prepare( new ReleaseConfiguration() );
            fail( "Should have failed to find a phase" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
        }
    }

    public void testReleaseConfigurationStoreFailure()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/working-directory" ) );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.ROLE, "test" );

        Mock configStoreMock = new Mock( ReleaseConfigurationStore.class );
        configStoreMock.expects( new InvokeOnceMatcher() ).method( "read" ).with(
            new IsSame( releaseConfiguration ) ).will(
            new ThrowStub( new ReleaseConfigurationStoreException( "message", new IOException( "ioExceptionMsg" ) ) ) );

        releaseManager.setConfigStore( (ReleaseConfigurationStore) configStoreMock.proxy() );

        try
        {
            releaseManager.prepare( releaseConfiguration );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseConfigurationStoreException.class, e.getCause().getClass() );
        }
    }
}
