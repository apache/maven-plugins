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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProject;

public class TestUnpackDependenciesMojo2
    extends AbstractDependencyMojoTestCase
{

    private final String UNPACKABLE_FILE = "test.txt";

    private final String UNPACKABLE_FILE_PATH = "target/test-classes/unit/unpack-dependencies-test/" + UNPACKABLE_FILE;

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
        stubFactory.setUnpackableFile( mojo.getArchiverManager() );
        // i'm using one file repeatedly to archive so I can test the name
        // programmatically.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar + UNPACKABLE_FILE_PATH ) );

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

    public File getUnpackedFile( Artifact artifact )
    {
        File destDir = DependencyUtil.getFormattedOutputDirectory( mojo.isUseSubDirectoryPerScope(), mojo.isUseSubDirectoryPerType(), mojo
            .isUseSubDirectoryPerArtifact(), mojo.useRepositoryLayout, mojo.stripVersion, mojo.getOutputDirectory(),
                                                                   artifact );
        File unpacked = new File( destDir, DependencyArtifactStubFactory.getUnpackableFileName( artifact ) );
        assertTrue( unpacked.exists() );
        return unpacked;
    }

    public void testDontOverWriteRelease()
        throws MojoExecutionException, InterruptedException, IOException
    {

        Set artifacts = new HashSet();
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        artifacts.add( release );

        mojo.project.setArtifacts( artifacts );
        mojo.project.setDependencyArtifacts( artifacts );

        mojo.overWriteIfNewer = false;

        mojo.execute();

        assertUnpacked( release, false );
    }

    public void testOverWriteRelease()
        throws MojoExecutionException, InterruptedException, IOException
    {

        Set artifacts = new HashSet();
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        artifacts.add( release );

        mojo.project.setArtifacts( artifacts );
        mojo.project.setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = true;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        assertUnpacked( release, true );
    }

    public void testDontOverWriteSnap()
        throws MojoExecutionException, InterruptedException, IOException
    {

        Set artifacts = new HashSet();
        Artifact snap = stubFactory.getSnapshotArtifact();
        snap.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        artifacts.add( snap );

        mojo.project.setArtifacts( artifacts );
        mojo.project.setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = false;
        mojo.overWriteSnapshots = false;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        assertUnpacked( snap, false );
    }

    public void testOverWriteSnap()
        throws MojoExecutionException, InterruptedException, IOException
    {

        Set artifacts = new HashSet();
        Artifact snap = stubFactory.getSnapshotArtifact();
        snap.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        artifacts.add( snap );

        mojo.project.setArtifacts( artifacts );
        mojo.project.setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = false;
        mojo.overWriteSnapshots = true;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        assertUnpacked( snap, true );

    }

    public void testOverWriteIfNewer()
        throws MojoExecutionException, InterruptedException, IOException
    {

        Set artifacts = new HashSet();
        Artifact snap = stubFactory.getSnapshotArtifact();
        snap.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        artifacts.add( snap );

        mojo.project.setArtifacts( artifacts );
        mojo.project.setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = false;
        mojo.overWriteSnapshots = false;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        File unpackedFile = getUnpackedFile( snap );

        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        // set source to be newer and dest to be a known value.
        snap.getFile().setLastModified( time + 3000 );
        unpackedFile.setLastModified( time );
        // wait at least a second for filesystems that only record to the
        // nearest second.
        Thread.sleep( 1000 );

        assertEquals( time, unpackedFile.lastModified() );
        mojo.execute();
        System.gc();
        // make sure it didn't overwrite
        assertEquals( time, unpackedFile.lastModified() );

        mojo.overWriteIfNewer = true;

        mojo.execute();

        assertTrue( time != unpackedFile.lastModified() );
        
        System.gc();
    }

    public void assertUnpacked( Artifact artifact, boolean overWrite )
        throws InterruptedException, MojoExecutionException
    {
        File unpackedFile = getUnpackedFile( artifact );

        Thread.sleep( 100 );
        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        unpackedFile.setLastModified( time );
        // wait at least a second for filesystems that only record to the
        // nearest second.
        Thread.sleep( 1000 );

        assertEquals( time, unpackedFile.lastModified() );
        mojo.execute();

        if ( overWrite )
        {
            assertTrue( time != unpackedFile.lastModified() );
        }
        else
        {
            assertEquals( time, unpackedFile.lastModified() );
        }
    }
}
