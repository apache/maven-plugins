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

import java.io.File;

/**
 * Release management classes.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ReleaseManager
{
    /**
     * The Plexus role.
     */
    String ROLE = ReleaseManager.class.getName();

    /**
     * Prepare a release.
     *
     * @param releaseConfiguration
     */
    void prepare( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException;

    /**
     * Perform a release.
     *
     * @param releaseConfiguration the configuration to use for release
     * @param checkoutDirectory    the location to checkout to and build from
     * @param goals                the goals to execute
     * @throws ReleaseExecutionException if there is a problem performing the release
     */
    void perform( ReleaseConfiguration releaseConfiguration, File checkoutDirectory, String goals )
        throws ReleaseExecutionException;

    /**
     * Clean a release.
     */
    void clean();
}
