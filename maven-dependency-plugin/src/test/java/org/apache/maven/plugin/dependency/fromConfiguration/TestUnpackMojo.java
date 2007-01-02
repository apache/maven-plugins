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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
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
        // mojo.silent = true;

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
    }

    public ArtifactItem getSingleArtifactItem( boolean removeVersion )
        throws MojoExecutionException
    {
        ArrayList list = mojo.getProcessedArtifactItems( removeVersion );
        return (ArtifactItem) list.get( 0 );
    }

    public void testGetArtifactItems()
        throws MojoExecutionException
    {

        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0" );

        ArrayList list = new ArrayList( 1 );
        list.add( item );

        mojo.setArtifactItems( list );

        ArtifactItem result = getSingleArtifactItem( false );
        assertEquals( mojo.getOutputDirectory(), result.getOutputDirectory() );

        File output = new File( mojo.getOutputDirectory(), "override" );
        item.setOutputDirectory( output );
        result = getSingleArtifactItem( false );
        assertEquals( output, result.getOutputDirectory() );
    }

    public void assertMarkerFiles( Collection items, boolean exist )
    {
        Iterator iter = items.iterator();
        while ( iter.hasNext() )
        {
            assertMarkerFile( exist, (ArtifactItem) iter.next() );
        }
    }

    public void assertMarkerFile( boolean val, ArtifactItem item )
    {
        DefaultFileMarkerHandler handle = new DefaultFileMarkerHandler( item.getArtifact(), mojo.getMarkersDirectory() );
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
        throws IOException, MojoExecutionException
    {
        ArrayList list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertMarkerFiles( list, true );
    }

    public void testUnpackToLocation()
        throws IOException, MojoExecutionException
    {
        ArrayList list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = (ArtifactItem) list.get( 0 );
        item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertMarkerFiles( list, true );
    }

    public void testMissingVersionNotFound()
        throws MojoExecutionException
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        ArrayList list = new ArrayList();
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

    public List getDependencyList( ArtifactItem item )
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

        List list = new ArrayList( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencies()
        throws MojoExecutionException
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        ArrayList list = new ArrayList();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "2.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependenciesWithClassifier()
        throws MojoExecutionException
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "war" );

        ArrayList list = new ArrayList();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "2.1", item.getVersion() );
    }

    public List getDependencyMgtList( ArtifactItem item )
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

        List list = new ArrayList( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencyMgt()
        throws MojoExecutionException
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

        ArrayList list = new ArrayList();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        mojo.execute();
        assertMarkerFile( true, item );
        assertEquals( "3.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependencyMgtWithClassifier()
        throws MojoExecutionException
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

        ArrayList list = new ArrayList();
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

        ArrayList list = new ArrayList();
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
        throws IOException, MojoExecutionException, InterruptedException
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        ArrayList list = new ArrayList( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        assertUnpacked( item, false );
    }

    public void testUnpackDontOverWriteSnapshots()
        throws IOException, MojoExecutionException, InterruptedException
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        ArrayList list = new ArrayList( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        assertUnpacked( item, false );
    }

    public void testUnpackOverWriteReleases()
        throws IOException, MojoExecutionException, InterruptedException
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        ArrayList list = new ArrayList( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( true );
        mojo.execute();

        assertUnpacked( item, true );
    }

    public void testUnpackOverWriteSnapshot()
        throws IOException, MojoExecutionException, InterruptedException
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        ArrayList list = new ArrayList( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( false );
        mojo.setOverWriteSnapshots( true );
        mojo.execute();

        assertUnpacked( item, true );
    }

    public void testUnpackOverWriteIfNewer()
        throws IOException, MojoExecutionException, InterruptedException
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        ArrayList list = new ArrayList( 1 );
        list.add( item );
        mojo.setArtifactItems( list );
        mojo.setOverWriteIfNewer( true );
        mojo.execute();

        Thread.sleep( 100 );
        File unpackedFile = getUnpackedFile( item );

        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );

        // set to known value
        unpackedFile.setLastModified( time );

        // set source to be newer
        artifact.getFile().setLastModified( time + 4000 );
        assertTrue( time == unpackedFile.lastModified() );
        mojo.execute();

        assertTrue( time != unpackedFile.lastModified() );
    }

    public void assertUnpacked( ArtifactItem item, boolean overWrite )
        throws InterruptedException, MojoExecutionException
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
        File unpackedFile = new File( item.getOutputDirectory(), ArtifactStubFactory.getUnpackableFileName( item
            .getArtifact() ) );
        
        assertTrue(unpackedFile.exists());
        return unpackedFile;

    }
}
