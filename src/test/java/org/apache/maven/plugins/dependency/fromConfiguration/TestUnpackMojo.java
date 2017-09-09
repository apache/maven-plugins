package org.apache.maven.plugins.dependency.fromConfiguration;

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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugins.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugins.dependency.utils.markers.UnpackFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

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
        super.setUp( "unpack", true, false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/unpack-test/plugin-config.xml" );
        mojo = (UnpackMojo) lookupMojo( "unpack", testPom );
        mojo.setOutputDirectory( new File( this.testDir, "outputDirectory" ) );
        mojo.setMarkersDirectory( new File( this.testDir, "markers" ) );
        mojo.setSilent( true );

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

        mojo.setUseJvmChmod( true );

        MavenSession session = newMavenSession( mojo.getProject() );
        setVariableValueToObject( mojo, "session", session );

        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) session.getRepositorySession();

        repoSession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( stubFactory.getWorkingDir() ) );
    }

    public ArtifactItem getSingleArtifactItem( boolean removeVersion )
        throws MojoExecutionException
    {
        List<ArtifactItem> list = mojo.getProcessedArtifactItems( removeVersion );
        return list.get( 0 );
    }

    public void testGetArtifactItems()
        throws Exception
    {

        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0" );

        ArrayList<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( createArtifact( item ) );

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

    public void testUnpackToLocationWhereLocationCannotBeCreatedThrowsException()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = list.get( 0 );
        item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );

        mojo.setArtifactItems( list );
        final File currentFile = mojo.getOutputDirectory();

        // pretend that the output directory cannot be found event after mkdirs has been called by the mojo
        // ifor instance in the case when the outputDirectory cannot be created because of permissions on the
        // parent of the output directory
        mojo.setOutputDirectory( new File( currentFile.getAbsolutePath() )
        {

            private static final long serialVersionUID = -8559876942040177020L;

            @Override
            public boolean exists()
            {
                // this file will always report that it does not exist
                return false;
            }
        } );
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
        project.setDependencies( createArtifacts( getDependencyList( item ) ) );

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
        project.setDependencies( createArtifacts( getDependencyList( item ) ) );

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
        project.setDependencies( createArtifacts( getDependencyList( item ) ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( createArtifacts( getDependencyMgtList( item ) ) );

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
        project.setDependencies( createArtifacts( getDependencyList( item ) ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "jar" );

        stubFactory.createArtifact( "groupId", "artifactId-2", VersionRange.createFromVersion( "3.0-SNAPSHOT" ), null,
                                    "jar", "classifier", false );
        stubFactory.createArtifact( "groupId", "artifactId-2", VersionRange.createFromVersion( "3.1" ), null, "jar",
                                    "classifier", false );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( createArtifacts( getDependencyMgtList( item ) ) );

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

        try
        {
            mojo.execute();
            fail( "ExpectedException" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "Unable to find/resolve artifact.", e.getMessage() );
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
        assertTrue( release.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        ArtifactItem item = new ArtifactItem( createArtifact( release ) );

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
        assertTrue( artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        ArtifactItem item = new ArtifactItem( createArtifact( artifact ) );

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
        assertTrue( release.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        ArtifactItem item = new ArtifactItem( createArtifact( release ) );

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
        assertTrue( artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        ArtifactItem item = new ArtifactItem( createArtifact( artifact ) );

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

        mojo.setSilent( false );
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        assertTrue( artifact.getFile().setLastModified( now - 20000 ) );

        ArtifactItem item = new ArtifactItem( createArtifact( artifact ) );

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
        assertTrue( "unpackedFile '" + unpackedFile + "' lastModified() == " + marker.lastModified()
            + ": should be different", marker.lastModified() != unpackedFile.lastModified() );
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
        assertTrue( unpackedFile.setLastModified( time ) );

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
        File unpackedFile = new File( item.getOutputDirectory(),
                                      DependencyArtifactStubFactory.getUnpackableFileName( item.getArtifact() ) );

        assertTrue( unpackedFile.exists() );
        return unpackedFile;

    }

    // respects the createUnpackableFile flag of the ArtifactStubFactory
    private List<Dependency> createArtifacts( List<Dependency> items )
        throws IOException
    {
        for ( Dependency item : items )
        {
            String classifier = "".equals( item.getClassifier() ) ? null : item.getClassifier();
            stubFactory.createArtifact( item.getGroupId(), item.getArtifactId(),
                                        VersionRange.createFromVersion( item.getVersion() ), null, item.getType(),
                                        classifier, item.isOptional() );
        }
        return items;
    }

    private Artifact createArtifact( Artifact art )
        throws IOException
    {
        String classifier = "".equals( art.getClassifier() ) ? null : art.getClassifier();
        stubFactory.createArtifact( art.getGroupId(), art.getArtifactId(),
                                    VersionRange.createFromVersion( art.getVersion() ), null, art.getType(), classifier,
                                    art.isOptional() );
        return art;
    }

    private ArtifactItem createArtifact( ArtifactItem item )
        throws IOException
    {
        String classifier = "".equals( item.getClassifier() ) ? null : item.getClassifier();
        stubFactory.createArtifact( item.getGroupId(), item.getArtifactId(), item.getVersion(), null, item.getType(),
                                    classifier );
        return item;
    }
}
