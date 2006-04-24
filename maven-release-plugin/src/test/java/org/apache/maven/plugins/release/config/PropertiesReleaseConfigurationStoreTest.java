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

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * Test the properties store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PropertiesReleaseConfigurationStoreTest
    extends PlexusTestCase
{
    private PropertiesReleaseConfigurationStore store;

    public void testReadFromFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/release.properties" );
        store.setPropertiesFile( file );

        ReleaseConfiguration config = store.read();
        assertEquals( "Expected completedPhase of 'step1'", "step1", config.getCompletedPhase() );

        // TODO: assert other contents
    }

    public void testReadFromEmptyFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );
        store.setPropertiesFile( file );

        ReleaseConfiguration config = store.read();

        assertDefaultReleaseConfiguration( config );
    }

    public void testReadMissingFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );
        store.setPropertiesFile( file );

        ReleaseConfiguration config = store.read();

        assertDefaultReleaseConfiguration( config );
    }

    public void testMergeFromEmptyFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );
        store.setPropertiesFile( file );

        ReleaseConfiguration mergeConfiguration = createMergeConfiguration();
        ReleaseConfiguration config = store.read( mergeConfiguration );

        assertEquals( "Check configurations merged", mergeConfiguration, config );
    }

    public void testMergeFromMissingFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );
        store.setPropertiesFile( file );

        ReleaseConfiguration mergeConfiguration = createMergeConfiguration();
        ReleaseConfiguration config = store.read( mergeConfiguration );

        assertEquals( "Check configurations merged", mergeConfiguration, config );
    }

    private static void assertDefaultReleaseConfiguration( ReleaseConfiguration config )
    {
        assertNull( "Expected no completedPhase", config.getCompletedPhase() );

        // TODO: assert other default contents
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        store = (PropertiesReleaseConfigurationStore) lookup( ReleaseConfigurationStore.ROLE, "properties" );
    }

    public ReleaseConfiguration createMergeConfiguration()
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();

        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setUsername( "username" );
        // Not setting other optional SCM settings for brevity
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test-working-directory" ) );
        // Not setting non-override setting completedPhase

        return releaseConfiguration;
    }
}
