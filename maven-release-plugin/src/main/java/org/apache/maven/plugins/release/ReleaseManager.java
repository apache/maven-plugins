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

import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.List;

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
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Prepare a release.
     *
     * @param releaseDescriptor the configuration to pass to the preparation steps
     * @param settings          the settings.xml configuration
     * @param reactorProjects   the reactor projects
     * @param resume            resume a previous release, if the properties file exists
     * @param dryRun            do not commit any changes to the file system or SCM
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects, boolean resume,
                           boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Perform a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param settings          the settings.xml configuration
     * @param checkoutDirectory the location to checkout to and build from
     * @param goals             the goals to execute
     * @param useReleaseProfile whether to use the release profile from the super POM or not
     * @throws ReleaseExecutionException if there is a problem performing the release
     * @throws ReleaseFailureException   if there is a problem performing the release
     */
    void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                           File checkoutDirectory, String goals, boolean useReleaseProfile )
        throws ReleaseExecutionException, ReleaseFailureException;

    /**
     * Clean a release.
     *
     * @param releaseDescriptor the configuration to use for release
     * @param reactorProjects   the reactor projects
     */
    void clean( ReleaseDescriptor releaseDescriptor, List reactorProjects );

    void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects, boolean resume,
                           boolean dryRun, ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    ReleaseResult prepareWithResult( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects, boolean resume,
                           boolean dryRun, ReleaseManagerListener listener );

    void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                           File checkoutDirectory, String goals, boolean useReleaseProfile,
                           ReleaseManagerListener listener )
        throws ReleaseExecutionException, ReleaseFailureException;

    ReleaseResult performWithResult( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                           File checkoutDirectory, String goals, boolean useReleaseProfile,
                           ReleaseManagerListener listener );
}
