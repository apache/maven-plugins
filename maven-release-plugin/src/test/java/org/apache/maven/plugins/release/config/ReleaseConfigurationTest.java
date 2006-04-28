package org.apache.maven.plugins.release.config;

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

import junit.framework.TestCase;
import org.apache.maven.settings.Settings;

import java.io.File;

/**
 * ReleaseConfiguration Tester.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseConfigurationTest
    extends TestCase
{
    public void testMergeConfigurationSourceEmpty()
    {
        ReleaseConfiguration mergeConfiguration = createReleaseConfiguration();

        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.merge( mergeConfiguration );

        assertEquals( "Check merge", mergeConfiguration, releaseConfiguration );
    }

    public void testMergeConfigurationDestEmpty()
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        releaseConfiguration.merge( new ReleaseConfiguration() );

        ReleaseConfiguration expectedConfiguration = createReleaseConfiguration( releaseConfiguration.getSettings(),
                                                                                 releaseConfiguration.getWorkingDirectory() );
        assertEquals( "Check merge", expectedConfiguration, releaseConfiguration );
    }

    public void testMergeConfiguration()
    {
        Settings settings = new Settings();
        File workingDirectory = new File( "." );

        ReleaseConfiguration mergeConfiguration =
            createMergeConfiguration( settings, workingDirectory, "completed-phase-merge" );

        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();
        releaseConfiguration.merge( mergeConfiguration );

        ReleaseConfiguration expectedConfiguration = createMergeConfiguration( releaseConfiguration.getSettings(),
                                                                               releaseConfiguration.getWorkingDirectory(),
                                                                               releaseConfiguration.getCompletedPhase() );
        assertEquals( "Check merge", expectedConfiguration, releaseConfiguration );
    }

    private static ReleaseConfiguration createMergeConfiguration( Settings settings, File workingDirectory,
                                                                  String completedPhase )
    {
        ReleaseConfiguration mergeConfiguration = new ReleaseConfiguration();
        mergeConfiguration.setUrl( "scm-url-merge" );
        mergeConfiguration.setCompletedPhase( completedPhase );
        mergeConfiguration.setPassphrase( "passphrase-merge" );
        mergeConfiguration.setPassword( "password-merge" );
        mergeConfiguration.setPrivateKey( "private-key-merge" );
        mergeConfiguration.setSettings( settings );
        mergeConfiguration.setTagBase( "tag-base-merge" );
        mergeConfiguration.setReleaseLabel( "tag-merge" );
        mergeConfiguration.setUsername( "username-merge" );
        mergeConfiguration.setWorkingDirectory( workingDirectory );
        return mergeConfiguration;
    }

    private static ReleaseConfiguration createReleaseConfiguration()
    {
        Settings settings = new Settings();
        File workingDirectory = new File( "." );

        return createReleaseConfiguration( settings, workingDirectory );
    }

    private static ReleaseConfiguration createReleaseConfiguration( Settings settings, File workingDirectory )
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setCompletedPhase( "completed-phase" );
        releaseConfiguration.setPassphrase( "passphrase" );
        releaseConfiguration.setPassword( "password" );
        releaseConfiguration.setPrivateKey( "private-key" );
        releaseConfiguration.setSettings( settings );
        releaseConfiguration.setTagBase( "tag-base" );
        releaseConfiguration.setReleaseLabel( "tag" );
        releaseConfiguration.setUsername( "username" );
        releaseConfiguration.setWorkingDirectory( workingDirectory );
        return releaseConfiguration;
    }
}
