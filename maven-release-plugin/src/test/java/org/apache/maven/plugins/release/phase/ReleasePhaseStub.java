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

import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.settings.Settings;

import java.util.List;

/**
 * Test stub for testing if a phase is executed.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleasePhaseStub
    implements ReleasePhase
{
    /**
     * Whether the phase was simulated.
     */
    private boolean simulated;

    /**
     * Whether the phase was executed.
     */
    private boolean executed;

    /**
     * Whether the phase was cleaned.
     */
    private boolean cleaned;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        executed = true;

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        simulated = true;

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult clean( List reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        cleaned = true;

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public boolean isExecuted()
    {
        return executed;
    }

    public boolean isSimulated()
    {
        return simulated;
    }

    public boolean isCleaned()
    {
        return cleaned;
    }
}
