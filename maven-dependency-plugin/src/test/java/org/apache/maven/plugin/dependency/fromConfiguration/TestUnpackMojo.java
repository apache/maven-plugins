package org.apache.maven.plugin.dependency.fromConfiguration;

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
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.utils.markers.UnpackFileMarkerHandler;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.StubArtifactCollector;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugin.testing.stubs.StubArtifactResolver;
import org.apache.maven.project.MavenProject;

public class TestUnpackMojo
    extends AbstractDependencyMojoTestCase
{

    UnpackMojo mojo;

    public TestUnpackMojo()
    {
        super();
    }

    protected void setUp()
        throws Exception
    {
        super.setUp( "unpack", true );

        File testPom = new File( getBasedir(), "target/test-classes/unit/unpack-test/plugin-config.xml" );
        mojo = (UnpackMojo) lookupMojo( "unpack", testPom );
        mojo.setOutputDirectory( new File( this.testDir, "outputDirectory" ) );
        mojo.setMarkersDirectory( new File( this.testDir, "markers" ) );
        setSilent( mojo, true );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        // MavenProject project = mojo.getProject();
        // init classifier things
        // it needs to get the archivermanager
        stubFactory.setUnpackableFile( mojo.getArchiverManager() );
        // i'm using one file repeatedly to archive so I can test the name
        // programmatically.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar
            + "target/test-classes/unit/unpack-dependencies-test/test.txt" ) );

        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );
        mojo.setArtifactCollector( new StubArtifactCollector() );
        mojo.setUseJvmChmod( true );
    }

    public ArtifactItem getSingleArtifactItem( boolean removeVersion )
        throws MojoExecutionException
    {
        List<ArtifactItem> list = mojo.getProcessedArtifactItems( removeVersion );
        return list.get( 0 );
    }

    public void testGetArtifactItems()
        throws MojoExecutionException
    {

        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0" );

        ArrayList<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );

        mojo.setArtifactItems( list );

        ArtifactItem result = getSingleArtifactItem( false );
        assertEquals( mojo.getOutputDirectory(), result.getOutputDirectory() );

        File output = new File( mojo.getOutputDirectory(), "override" );
        item.setOutputDirectory( output );
        result = getSingleArtifactItem( false );
        assertEquals( output, result.getOutputDirectory() );
    }

    public void assertMarkerFiles( Collection<ArtifactItem> items, boolean exist )
    {
        for ( ArtifactItem item : items )
        {
            assertMarkerFile( exist, item );
        }
    }

    public void assertMarkerFile( boolean val, ArtifactItem item )
    {
        UnpackFileMarkerHandler handle = new UnpackFileMarkerHandler( item, mojo.getMarkersDirectory() );
        try
        {
            assertEquals( val, handle.isMarkerSet() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getLongMessage() );
        }
    }

    public void testUnpackFile()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertMarkerFiles( list, true );
    }
    
    public void testSkip()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setSkip( true );
        mojo.setArtifactItems( list );

        mojo.execute();

        assertMarkerFiles( list, false );
    }

    public void testUnpackToLocation()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = list.get( 0 );
        item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertMarkerFiles( list, true );
    }

    public void testMissingVersionNotFound()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        try
        {
            mojo.execute();
            fail( "Expected Exception Here." );
        }
        catch ( MojoExecutionException e )
        {
            // caught the expected exception.
        }
    }

    public List<Dependency> getDependencyList( ArtifactItem item )
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( item.getArtifactId() );
        dep.setClassifier( item.getClassifier() );
        dep.setGroupId( item.getGroupId() );
        dep.setType( item.getType() );
        dep.setVersion( "2.0-SNAPSHOT" );

        Dependency dep2 = new Dependency();
        dep2.setArtifactId( item.getArtifactId() );
        dep2.setClassifier( "classifier" );
        dep2.setGroupId( item.getGroupId() );
        dep2.setType( item.getType() );
        dep2.setVersion( "2.1" );

        List<Dependency> list = new ArrayList<Dependency>( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencies()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "2.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependenciesWithClassifier()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "war" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "2.1", item.getVersion() );
    }

    public List<Dependency> getDependencyMgtList( ArtifactItem item )
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( item.getArtifactId() );
        dep.setClassifier( item.getClassifier() );
        dep.setGroupId( item.getGroupId() );
        dep.setType( item.getType() );
        dep.setVersion( "3.0-SNAPSHOT" );

        Dependency dep2 = new Dependency();
        dep2.setArtifactId( item.getArtifactId() );
        dep2.setClassifier( "classifier" );
        dep2.setGroupId( item.getGroupId() );
        dep2.setType( item.getType() );
        dep2.setVersion( "3.1" );

        List<Dependency> list = new ArrayList<Dependency>( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencyMgt()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "3.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependencyMgtWithClassifier()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        mojo.execute();

        assertMarkerFile( true, item );
        assertEquals( "3.1", item.getVersion() );
    }

    public void testArtifactNotFound()
        throws Exception
    {
        dotestArtifactExceptions( false, true );
    }

    public void testArtifactResolutionException()
        throws Exception
    {
        dotestArtifactExceptions( true, false );
    }

    public void dotestArtifactExceptions( boolean are, boolean anfe )
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );
        item.setVersion( "1.0" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( null, are, anfe ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );

        try
        {
            mojo.execute();
            fail( "ExpectedException" );
        }
        catch ( MojoExecutionException e )
        {
            if ( are )
            {
                assertEquals( "Unable to resolve artifact.", e.getMessage() );
            }
            else
            {
                assertEquals( "Unable to find artifact.", e.getMessage() );
            }
        }
    }

    public void testNoArtifactItems()
    {
        try
        {
            mojo.getProcessedArtifactItems( false );
            fail( "Expected Exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "There are no artifactItems configured.", e.getMessage() );
        }

    }

    public void testUnpackDontOverWriteReleases()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        assertUnpacked( item, false );
    }

    public void testUnpackDontOverWriteSnapshots()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        assertUnpacked( item, false );
    }

    public void testUnpackOverWriteReleases()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( true );
        mojo.execute();

        assertUnpacked( item, true );
    }

    public void testUnpackOverWriteSnapshot()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( false );
        mojo.setOverWriteSnapshots( true );
        mojo.execute();

        assertUnpacked( item, true );
    }

    public void testUnpackOverWriteIfNewer()
        throws Exception
    {
        final long now = System.currentTimeMillis();
        
        setSilent( mojo, false );
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        assertTrue( artifact.getFile().setLastModified( now - 20000 ) );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = Collections.singletonList( item );
        mojo.setArtifactItems( list );
        mojo.setOverWriteIfNewer( true );
        mojo.execute();
        File unpackedFile = getUnpackedFile( item );

        // round down to the last second
        long time = now;
        time = time - ( time % 1000 );
        // go back 10 more seconds for linux
        time -= 10000;
        // set to known value
        assertTrue( unpackedFile.setLastModified( time ) );
        // set source to be newer was 4s but test is brittle on MacOS if less than 5s
        assertTrue( artifact.getFile().setLastModified( time + 5000 ) );

        // manually set markerfile (must match getMarkerFile in DefaultMarkerFileHandler)
        File marker = new File( mojo.getMarkersDirectory(), artifact.getId().replace( ':', '-' ) + ".marker" );
        assertTrue( marker.setLastModified( time ) );

        displayFile( "unpackedFile", unpackedFile );
        displayFile( "artifact    ", artifact.getFile() );
        displayFile( "marker      ", marker );
        System.out.println( "mojo.execute()" );
        mojo.execute();
        displayFile( "unpackedFile", unpackedFile );
        displayFile( "artifact    ", artifact.getFile() );
        displayFile( "marker      ", marker );
        System.out.println( "marker.lastModified() = " + marker.lastModified() );
        System.out.println( "unpackedFile.lastModified() = " + unpackedFile.lastModified() );
        assertTrue( "unpackedFile '" + unpackedFile + "' lastModified() == " + marker.lastModified() + ": should be different",
                    marker.lastModified() != unpackedFile.lastModified() );
    }

    public void testPurgeOutputDirectory()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact = stubFactory.getSnapshotArtifact();
        ArtifactItem item = new ArtifactItem(artifact);

        FileUtils.deleteDirectory(mojo.getOutputDirectory());
        mojo.getOutputDirectory().mkdirs();

        File file = new File(mojo.getOutputDirectory(), "file.txt");
        file.createNewFile();

        List<ArtifactItem> list = Collections.singletonList(item);
        mojo.setArtifactItems(list);
        mojo.setOverWriteIfNewer(true);
        mojo.setPurgeOutputDirectory(true);
        mojo.execute();

        assertFalse(
                "Output directory is not purged when purging is configured on global level.",
                file.exists());
    }

    public void testPurgeOutputDirectoryConfiguredInArtifact()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact = stubFactory.getSnapshotArtifact();
        ArtifactItem item = new ArtifactItem(artifact);
        item.setPurgeOutputDirectory(true);

        FileUtils.deleteDirectory(mojo.getOutputDirectory());
        mojo.getOutputDirectory().mkdirs();

        File file = new File(mojo.getOutputDirectory(), "file.txt");
        file.createNewFile();

        List<ArtifactItem> list = Collections.singletonList(item);
        mojo.setArtifactItems(list);
        mojo.setOverWriteIfNewer(true);
        mojo.execute();

        assertFalse(
                "Output directory is not purged when purging is configured on artifact level.",
                file.exists());
    }

    public void testPurgeOutputDirectoryNotConfigured()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact = stubFactory.getSnapshotArtifact();
        ArtifactItem item = new ArtifactItem(artifact);

        FileUtils.deleteDirectory(mojo.getOutputDirectory());
        mojo.getOutputDirectory().mkdirs();

        File file = new File(mojo.getOutputDirectory(), "file.txt");
        file.createNewFile();

        List<ArtifactItem> list = Collections.singletonList(item);
        mojo.setArtifactItems(list);
        mojo.setOverWriteIfNewer(true);
        mojo.execute();

        assertTrue(
                "Output directory is purged incorrectly when output directory purging is not configured.",
                file.exists());
    }

    public void testPurgeOutputDirectoryNoProcessing()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact = stubFactory.getSnapshotArtifact();
        ArtifactItem item = new ArtifactItem(artifact);

        // manually set markerfile (must match getMarkerFile in DefaultMarkerFileHandler)
        File marker = new File( mojo.getMarkersDirectory(), artifact.getId().replace( ':', '-' ) + ".marker" );
        marker.mkdirs();
        marker.createNewFile();

        FileUtils.deleteDirectory(mojo.getOutputDirectory());
        mojo.getOutputDirectory().mkdirs();

        File file = new File(mojo.getOutputDirectory(), "file.txt");
        file.createNewFile();

        List<ArtifactItem> list = Collections.singletonList(item);
        mojo.setArtifactItems(list);
        mojo.setPurgeOutputDirectory(true);
        mojo.setOverWriteIfNewer(false);
        mojo.execute();

        assertTrue(
                "Output directory is purged incorrectly when artifact does not have to unpack.",
                file.exists());
    }

    /**
     * Tests if artifact will be unpacked when no processing is required but the output folder is purged
     * for another artifact.
     */
    public void testUnpackWhenNoProcessingRequiredButOutputIsPurged()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);

        Artifact artifact1 =  stubFactory.createArtifact("group1", "artifact1", "version1", "scope1", "jar", "dev");
        ArtifactItem artifactItem1 = new ArtifactItem(artifact1);
        Artifact artifact2 =  stubFactory.createArtifact("group2", "artifact2", "version2", "scope2", "jar", "dev");
        ArtifactItem artifactItem2 = new ArtifactItem(artifact2);

        List<ArtifactItem> artifactItems = java.util.Arrays.asList(artifactItem1, artifactItem2);

        // manually set markerfile (must match getMarkerFile in DefaultMarkerFileHandler)
        File marker = new File( mojo.getMarkersDirectory(), artifact1.getId().replace( ':', '-' ) + ".marker" );
        marker.mkdirs();
        marker.createNewFile();

        FileUtils.deleteDirectory(mojo.getOutputDirectory());
        mojo.getOutputDirectory().mkdirs();

        mojo.setArtifactItems(artifactItems);
        mojo.setPurgeOutputDirectory(true);
        mojo.setOverWriteIfNewer(false);
        mojo.execute();

        File artifact1UnpackedFile = new File(
                artifactItem1.getOutputDirectory(), ArtifactStubFactory.getUnpackableFileName(artifact1));

        assertTrue(
                "Artifact not unpacked if output folder is purged because of a different artifact.",
                artifact1UnpackedFile.exists());

        File artifact2UnpackedFile = new File(
                artifactItem2.getOutputDirectory(), ArtifactStubFactory.getUnpackableFileName(artifact2));

        assertTrue(
                "Artifact not unpacked if output folder is purged.",
                artifact2UnpackedFile.exists());
    }

    public void testPurgeOutputDirectoryMultipleOutputFolders()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact1 = stubFactory.getSnapshotArtifact();
        Artifact artifact2 = stubFactory.getReleaseArtifact();

        ArtifactItem artifactItem1 = new ArtifactItem(artifact1);
        artifactItem1.setOutputDirectory(new File(mojo.getOutputDirectory(), "output1"));
        artifactItem1.getOutputDirectory().mkdirs();
        artifactItem1.setPurgeOutputDirectory(true);

        ArtifactItem artifactItem2 = new ArtifactItem(artifact2);
        artifactItem2.setOutputDirectory(new File(mojo.getOutputDirectory(), "output2"));
        artifactItem2.getOutputDirectory().mkdirs();
        artifactItem2.setPurgeOutputDirectory(true);

        ArtifactItem artifactItem3 = new ArtifactItem(artifact2);
        artifactItem3.setOutputDirectory(new File(mojo.getOutputDirectory(), "output3"));
        artifactItem3.getOutputDirectory().mkdirs();
        artifactItem3.setPurgeOutputDirectory(false);

        List<ArtifactItem> list = java.util.Arrays.asList(artifactItem1, artifactItem2, artifactItem3);

        File file1 = new File(artifactItem1.getOutputDirectory(), "file.txt");
        file1.createNewFile();

        File file2 = new File(artifactItem2.getOutputDirectory(), "file.txt");
        file2.createNewFile();

        File file3 = new File(artifactItem3.getOutputDirectory(), "file.txt");
        file3.createNewFile();

        mojo.setArtifactItems(list);
        mojo.setPurgeOutputDirectory(true);
        mojo.setOverWriteIfNewer(false);
        mojo.execute();

        assertFalse(
                "Output directory for artifact 1 is not purged in case of multiple artifacts.",
                file1.exists());

        assertFalse(
                "Output directory for artifact 2 is not purged in case of multiple artifacts.",
                file2.exists());

        assertTrue(
                "Output directory for artifact 3 is purged incorrectly in case of multiple artifacts.",
                file3.exists());
    }

    public void testPurgeOutputDirectoryConflictingSettings()
            throws Exception {
        setSilent(mojo, false);
        stubFactory.setCreateFiles(true);
        Artifact artifact1 = stubFactory.getSnapshotArtifact();
        Artifact artifact2 = stubFactory.getReleaseArtifact();

        ArtifactItem artifactItem1 = new ArtifactItem(artifact1);
        artifactItem1.setOutputDirectory(new File(mojo.getOutputDirectory(), "output1"));
        artifactItem1.setPurgeOutputDirectory(true);

        ArtifactItem artifactItem2 = new ArtifactItem(artifact2);
        artifactItem2.setOutputDirectory(new File(mojo.getOutputDirectory(), "output2"));
        artifactItem2.setPurgeOutputDirectory(true);

        ArtifactItem artifactItem3 = new ArtifactItem(artifact2);
        artifactItem3.setOutputDirectory(new File(mojo.getOutputDirectory(), "output2"));
        artifactItem3.setPurgeOutputDirectory(false);

        List<ArtifactItem> list = java.util.Arrays.asList(artifactItem1, artifactItem2, artifactItem3);

        mojo.setArtifactItems(list);
        mojo.setPurgeOutputDirectory(true);
        mojo.setOverWriteIfNewer(false);

        try {
            mojo.execute();
            fail("No exception is thrown when purging output directories with conflicting settings.");
        } catch (MojoExecutionException ex) {
            if (!ex.getMessage().contains("Conflicting settings for purgeOutputDirectory set for the artifacts")) {
                fail("Incorrect exception is thrown when purging output directories with conflicting settings.");
            }
        }
    }

    private void displayFile( String description, File file )
    {
        System.out.println( description + ' ' + DateFormatUtils.ISO_DATETIME_FORMAT.format( file.lastModified() ) + ' '
            + file.getPath().substring( getBasedir().length() ) );
    }
    
    public void assertUnpacked( ArtifactItem item, boolean overWrite )
        throws Exception
    {

        File unpackedFile = getUnpackedFile( item );

        Thread.sleep( 100 );
        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        unpackedFile.setLastModified( time );

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

    public File getUnpackedFile( ArtifactItem item )
    {
        File unpackedFile =
            new File( item.getOutputDirectory(),
                      DependencyArtifactStubFactory.getUnpackableFileName( item.getArtifact() ) );

        assertTrue( unpackedFile.exists() );
        return unpackedFile;

    }
}
