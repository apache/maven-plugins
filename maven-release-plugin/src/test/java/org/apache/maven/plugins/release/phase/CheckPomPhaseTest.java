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
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the POM verification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckPomPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-poms" );
    }

    public void testCorrectlyConfigured()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testMissingUrl()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Should have failed to execute" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Should have failed to simulate" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }
}
