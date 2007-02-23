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
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public class TestUnpackDependenciesMojo
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

    public void assertUnpacked( Artifact artifact )
    {
        assertUnpacked( true, artifact );
    }

    public void assertUnpacked( boolean val, Artifact artifact )
    {
        File folder = DependencyUtil.getFormattedOutputDirectory( mojo.useSubDirectoryPerType,
                                                                  mojo.useSubDirectoryPerArtifact,
                                                                  mojo.useRepositoryLayout, mojo.stripVersion,
                                                                  mojo.outputDirectory, artifact );

        File destFile = new File( folder, ArtifactStubFactory.getUnpackableFileName( artifact ) );

        assertEquals( val, destFile.exists() );
        assertMarkerFile( val, artifact );
    }

    public void assertMarkerFile( boolean val, Artifact artifact )
    {
        DefaultFileMarkerHandler handle = new DefaultFileMarkerHandler( artifact, mojo.markersDirectory );
        try
        {
            assertEquals( val, handle.isMarkerSet() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getLongMessage() );
        }
    }

    public void testMojo()
        throws Exception
    {
        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testNoTransitive()
        throws Exception
    {
        mojo.excludeTransitive = true;
        mojo.execute();
        Iterator iter = mojo.project.getDependencyArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testExcludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();

            assertUnpacked( !artifact.getType().equalsIgnoreCase( "jar" ), artifact );
        }
    }

    public void testExcludeProvidedScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeScope = "provided";
        // mojo.silent = false;

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !artifact.getScope().equals( "provided" ), artifact );
        }

    }

    public void testExcludeSystemScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeScope = "system";
        // mojo.silent = false;

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !artifact.getScope().equals( "system" ), artifact );
        }

    }

    public void testExcludeCompileScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !saf.include( artifact ), artifact );
        }
    }

    public void testExcludeTestScope()
        throws IOException
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeScope = "test";

        try
        {
            mojo.execute();
            fail( "expected an exception" );
        }
        catch ( MojoExecutionException e )
        {

        }

    }

    public void testExcludeRuntimeScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !saf.include( artifact ), artifact );
        }
    }

    public void testIncludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );

        mojo.includeTypes = "jar";
        // if include is used, exclude should be ignored.
        mojo.excludeTypes = "jar";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();

            assertUnpacked( artifact.getType().equalsIgnoreCase( "jar" ), artifact );
        }
    }

    public void testSubPerType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifact()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactAndType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactRemoveVersion()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.stripVersion = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactAndTypeRemoveVersion()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.stripVersion = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact );
        }
    }

    public void testIncludeCompileScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeTestScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "test";

        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeRuntimeScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeprovidedScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "provided";

        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ), artifact );
        }
    }

    public void testIncludesystemScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "system";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ), artifact );
        }
    }

    public void testIncludeArtifactId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );

        mojo.includeArtifactIds = "one";
        // if include is used, exclude should be ignored.
        mojo.excludeArtifactIds = "one";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( artifact.getArtifactId().equals( "one" ), artifact );
        }
    }

    public void testExcludeArtifactId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeArtifactIds = "one";
        mojo.execute();

        // test - get all direct dependencies and verify that they exist if they
        // do not have a classifier of "one"
        // then delete the file and at the end, verify the folder is empty.
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !artifact.getArtifactId().equals( "one" ), artifact );
        }
    }

    public void testExcludeGroupId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeGroupIds = "one";
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertUnpacked( !artifact.getGroupId().equals( "one" ), artifact );
        }
    }

    public void testIncludeGroupId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeGroupIds = "one";
        // if include is used, exclude should be ignored.
        mojo.excludeGroupIds = "one";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            // Testing with artifact id because group id is not in filename
            assertUnpacked( artifact.getGroupId().equals( "one" ), artifact );
        }
    }

    public void testCDMClassifier()
        throws Exception
    {
        dotestClassifierType( "jdk14", null );
    }

    public void testCDMType()
        throws Exception
    {
        dotestClassifierType( null, "zip" );
    }

    public void testCDMClassifierType()
        throws Exception
    {
        dotestClassifierType( "jdk14", "war" );
    }

    public void dotestClassifierType( String testClassifier, String testType )
        throws Exception
    {
        mojo.classifier = testClassifier;
        mojo.type = testType;
        mojo.factory = DependencyTestUtils.getArtifactFactory();
        mojo.resolver = new StubArtifactResolver( stubFactory, false, false );
        mojo.local = new StubArtifactRepository( this.testDir.getAbsolutePath() );

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();

            String useClassifier = artifact.getClassifier();
            String useType = artifact.getType();

            if ( StringUtils.isNotEmpty( testClassifier ) )
            {
                useClassifier = testClassifier;
                // type is only used if classifier is used.
                if ( StringUtils.isNotEmpty( testType ) )
                {
                    useType = testType;
                }
            }
            Artifact unpacked = stubFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact
                .getVersion(), Artifact.SCOPE_COMPILE, useType, useClassifier );
            assertUnpacked( unpacked );
        }
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
        mojo.classifier = "jdk";
        mojo.type = "java-sources";
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
        }
    }

    public File getUnpackedFile( Artifact artifact )
    {
        File destDir = DependencyUtil.getFormattedOutputDirectory( mojo.isUseSubDirectoryPerType(), mojo
            .isUseSubDirectoryPerArtifact(), mojo.useRepositoryLayout, mojo.stripVersion, mojo.getOutputDirectory(),
                                                                   artifact );
        File unpacked = new File( destDir, ArtifactStubFactory.getUnpackableFileName( artifact ) );
        assertTrue( unpacked.exists() );
        return unpacked;
    }

    public DefaultFileMarkerHandler getUnpackedMarkerHandler( Artifact artifact )
    {
        return new DefaultFileMarkerHandler( artifact, mojo.getMarkersDirectory() );
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

        // make sure it didn't overwrite
        assertEquals( time, unpackedFile.lastModified() );

        mojo.overWriteIfNewer = true;

        mojo.execute();

        assertTrue( time != unpackedFile.lastModified() );
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
