package org.apache.maven.plugin.dependency;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.util.Set;

import org.apache.maven.project.MavenProject;

public class TestIncludeExcludeUnpackDependenciesMojo
	extends AbstractDependencyMojoTestCase
{
	private final String PACKED_FILE = "test.zip";

	private final String UNPACKED_FILE_PREFIX = "test";
	private final String UNPACKED_FILE_SUFFIX = ".txt";

	private final String PACKED_FILE_PATH = "target/test-classes/unit/unpack-dependencies-test/" + PACKED_FILE;

	UnpackDependenciesMojo mojo;

    protected void setUp()
        throws Exception
    {
    	// required for mojo lookups to work
        super.setUp( "unpack-dependencies", true );

        File testPom = new File( getBasedir(), "target/test-classes/unit/unpack-dependencies-test/plugin-config.xml" );
        mojo = (UnpackDependenciesMojo) lookupMojo( "unpack-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        // mojo.silent = true;

        // it needs to get the archivermanager
        //stubFactory.setUnpackableFile( mojo.getArchiverManager() );
        // i'm using one file repeatedly to archive so I can test the name
        // programmatically.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar + PACKED_FILE_PATH ) );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );
        mojo.markersDirectory = new File( this.testDir, "markers" );

    }

    protected void tearDown()
    {
        super.tearDown();

        mojo = null;
        System.gc();
    }

    private void assertUnpacked( boolean unpacked, String fileName )
    {
        File destFile = new File( mojo.getOutputDirectory().getAbsolutePath(), fileName );
        assertEquals( unpacked, destFile.exists() );
    }

    /**
     * This test will validate that only the 1 and 11 files get unpacked
     * @throws Exception
     */
    public void testUnpackIncludesManyFiles()
		throws Exception
	{
        mojo.setIncludes( "**/*1" + UNPACKED_FILE_SUFFIX );
        mojo.execute();
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    /**
     * This test will verify only the 2 file gets unpacked
     * @throws Exception
     */
    public void testUnpackIncludesSingleFile()
    	throws Exception
	{
        mojo.setIncludes( "**/test2" + UNPACKED_FILE_SUFFIX );
        mojo.execute();
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    /**
     * This test will verify all files get unpacked
     * @throws Exception
     */
    public void testUnpackIncludesAllFiles()
    	throws Exception
	{
        mojo.setIncludes( "**/*" );
        mojo.execute();
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    /**
     * This test will validate that only the 2 and 3 files get unpacked
     * @throws Exception
     */
    public void testUnpackExcludesManyFiles()
		throws Exception
	{
        mojo.setExcludes( "**/*1" + UNPACKED_FILE_SUFFIX );
        mojo.execute();
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    /**
     * This test will verify only the 1, 11 & 3 files get unpacked
     * @throws Exception
     */
    public void testUnpackExcludesSingleFile()
    	throws Exception
	{
        mojo.setExcludes( "**/test2" + UNPACKED_FILE_SUFFIX );
        mojo.execute();
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    /**
     * This test will verify no files get unpacked
     * @throws Exception
     */
    public void testUnpackExcludesAllFiles()
    	throws Exception
	{
        mojo.setExcludes( "**/*" );
        mojo.execute();
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( false, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}

    public void testNoIncludeExcludes()
    	throws Exception
	{
        mojo.execute();
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 1 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 11 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 2 + UNPACKED_FILE_SUFFIX );
        assertUnpacked( true, UNPACKED_FILE_PREFIX + 3 + UNPACKED_FILE_SUFFIX );
	}
}
