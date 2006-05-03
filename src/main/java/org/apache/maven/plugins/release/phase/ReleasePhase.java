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
import org.apache.maven.plugins.release.config.ReleaseConfiguration;

/**
 * A phase in the release cycle.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleasePhase
{
    /**
     * The Plexus role.
     */
    String ROLE = ReleasePhase.class.getName();

    /**
     * Execute the phase.
     *
     * @param releaseConfiguration the configuration to use
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     */
    void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Simulate the phase, but don't make any changes to the project.
     *
     * @param releaseConfiguration the configuration to use
     * @throws ReleaseExecutionException an exception during the execution of the phase
     * @throws ReleaseFailureException   a failure during the execution of the phase
     */
    void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Clean up after a phase if it leaves any additional files in the checkout.
     *
     * @param config the configuration to use
     */
    void clean( ReleaseConfiguration config );
}
