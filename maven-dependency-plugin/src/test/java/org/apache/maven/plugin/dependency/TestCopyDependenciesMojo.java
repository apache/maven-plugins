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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public class TestCopyDependenciesMojo
    extends AbstractDependencyMojoTestCase
{

    CopyDependenciesMojo mojo;

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "copy-dependencies", true );

        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-dependencies-test/plugin-config.xml" );
        mojo = (CopyDependenciesMojo) lookupMojo( "copy-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        // mojo.silent = true;

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

    public void assertNoMarkerFile( Artifact artifact )
    {
        DefaultFileMarkerHandler handle = new DefaultFileMarkerHandler( artifact, mojo.markersDirectory );
        try
        {
            assertFalse( handle.isMarkerSet() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getLongMessage() );
        }

    }

    public void testCopyFile()
        throws MojoExecutionException, IOException
    {
        File src = File.createTempFile( "copy", null );

        File dest = new File( mojo.outputDirectory, "toMe.jar" );

        assertFalse( dest.exists() );

        mojo.copyFile( src, dest );
        assertTrue( dest.exists() );
    }

    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testCopyDependenciesMojo()
        throws Exception
    {
        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );

            // there should be no markers for the copy mojo
            assertNoMarkerFile( artifact );
        }
    }

    public void testCopyDependenciesMojoStripVersion()
        throws Exception
    {
        mojo.stripVersion = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoNoTransitive()
        throws Exception
    {
        mojo.excludeTransitive = true;
        mojo.execute();
        Iterator iter = mojo.project.getDependencyArtifacts().iterator();

        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoExcludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), !file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );

        mojo.includeTypes = "jar";
        // if include is used, exclude should be ignored.
        mojo.excludeTypes = "jar";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoExcludeArtifactId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeArtifactIds = "one";
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), !file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeArtifactId()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeGroupId()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getGroupId().equals( "one" ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoExcludeGroupId()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( artifact.getGroupId().equals( "one" ), !file.exists() );
        }
    }

    public void testCopyDependenciesMojoExcludeClassifier()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeClassifiers = "one";
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), !file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeClassifier()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );

        mojo.includeClassifiers = "one";
        // if include is used, exclude should be ignored.
        mojo.excludeClassifiers = "one";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( true, false, false, mojo.outputDirectory,
                                                                      artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifact()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( false, true, false, mojo.outputDirectory,
                                                                      artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifactAndType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil
                .getFormattedOutputDirectory( true, true, false, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifactRemoveVersion()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.stripVersion = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, true );
            File folder = DependencyUtil
                .getFormattedOutputDirectory( false, true, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifactAndTypeRemoveVersion()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.stripVersion = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, true );
            File folder = DependencyUtil.getFormattedOutputDirectory( true, true, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCDMClassifier()
        throws Exception
    {
        dotestCopyDependenciesMojoClassifierType( "jdk14", null );
    }

    public void testCDMType()
        throws Exception
    {
        dotestCopyDependenciesMojoClassifierType( null, "sources" );
    }

    public void testCDMClassifierType()
        throws Exception
    {
        dotestCopyDependenciesMojoClassifierType( "jdk14", "sources" );
    }

    public void dotestCopyDependenciesMojoClassifierType( String testClassifier, String testType )
        throws Exception
    {
        mojo.classifier = testClassifier;
        mojo.type = testType;

        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();

            String useClassifier = artifact.getClassifier();
            String useType = artifact.getType();

            if ( StringUtils.isNotEmpty( testClassifier ) )
            {
                useClassifier = "-" + testClassifier;
                // type is only used if classifier is used.
                if ( StringUtils.isNotEmpty( testType ) )
                {
                    useType = testType;
                }
            }
            String fileName = artifact.getArtifactId() + "-" + artifact.getVersion() + useClassifier + "." + useType;
            File file = new File( mojo.outputDirectory, fileName );

            if ( !file.exists() )
            {
                fail( "Can't find:" + file.getAbsolutePath() );
            }

            // there should be no markers for the copy mojo
            assertNoMarkerFile( artifact );
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
        mojo.factory = DependencyTestUtils.getArtifactFactory();
        mojo.resolver = new StubArtifactResolver( null, are, anfe );
        mojo.local = new StubArtifactRepository( this.testDir.getAbsolutePath() );

        try
        {
            mojo.execute();
            fail( "ExpectedException" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    /*
     * public void testOverwrite() { stubFactory.setCreateFiles( false );
     * Artifact artifact = stubFactory.createArtifact( "test", "artifact", "1.0" );
     * 
     * File testFile = new File( getBasedir() + File.separatorChar +
     * "target/test-classes/unit/copy-dependencies-test/test.zip" ); }
     */

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

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( release, false ) );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
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

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( release, false ) );

        Thread.sleep( 100 );
        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        // wait at least a second for filesystems that only record to the
        // nearest second.
        Thread.sleep( 1000 );

        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
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

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( snap, false ) );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
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

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( snap, false ) );

        Thread.sleep( 100 );
        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        // wait at least a second for filesystems that only record to the
        // nearest second.
        Thread.sleep( 1000 );

        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
    }

    public void testGetDependencies()
        throws MojoExecutionException
    {
        assertEquals( mojo.getResolvedDependencies( true ).toString(), mojo.getDependencySets( true )
            .getResolvedDependencies().toString() );
    }

    public void testCopyDependenciesMojoExcludeProvidedScope()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getScope().equals( "provided" ), !file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }

    }

    public void testCopyDependenciesMojoExcludeSystemScope()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getScope().equals( "system" ), !file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }

    }

    public void testCopyDependenciesMojoExcludeCompileScope()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoExcludeTestScope()
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

    public void testCopyDependenciesMojoExcludeRuntimeScope()
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }
}
