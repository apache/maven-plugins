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
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugin.testing.stubs.StubArtifactResolver;
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

        Set<Artifact> artifacts = this.stubFactory.getScopedArtifacts();
        Set<Artifact> directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
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
    public void testMojo()
        throws Exception
    {
        mojo.execute();
        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );

            // there should be no markers for the copy mojo
            assertNoMarkerFile( artifact );
        }
    }

    public void testStripVersion()
        throws Exception
    {
        mojo.stripVersion = true;
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testNoTransitive()
        throws Exception
    {
        mojo.excludeTransitive = true;
        mojo.execute();
        Iterator<Artifact> iter = mojo.project.getDependencyArtifacts().iterator();

        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testExcludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), !file.exists() );
        }
    }

    public void testIncludeType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeTypes = "jar";
        mojo.excludeTypes = "jar";
        //shouldn't get anything.

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeTypes = "";
        mojo.execute();

        iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), file.exists() );
        }
    }


    public void testExcludeArtifactId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeArtifactIds = "one";
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), !file.exists() );
        }
    }

    public void testIncludeArtifactId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeArtifactIds = "one";
        mojo.excludeArtifactIds = "one";
        //shouldn't get anything

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeArtifactIds = "";
        mojo.execute();

        iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), file.exists() );
        }
    }

    public void testIncludeGroupId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeGroupIds = "one";
        mojo.excludeGroupIds = "one";
        //shouldn't get anything

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeGroupIds = "";
        mojo.execute();

        iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getGroupId().equals( "one" ), file.exists() );
        }

    }

    public void testExcludeGroupId()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeGroupIds = "one";
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( artifact.getGroupId().equals( "one" ), !file.exists() );
        }
    }
    public void testExcludeMultipleGroupIds()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeGroupIds = "one,two";
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( artifact.getGroupId().equals( "one" ) || artifact.getGroupId().equals( "two" ), !file.exists() );
        }
    }

    public void testExcludeClassifier()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeClassifiers = "one";
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), !file.exists() );
        }
    }

    public void testIncludeClassifier()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeClassifiers = "one";
        mojo.excludeClassifiers = "one";
        //shouldn't get anything

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeClassifiers = "";
        mojo.execute();

        iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), file.exists() );
        }

    }

    public void testSubPerType()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( false, true, false, false, false, mojo.outputDirectory,
                                                                      artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
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
        dotestClassifierType( null, "sources" );
    }

    public void testCDMClassifierType()
        throws Exception
    {
        dotestClassifierType( "jdk14", "sources" );
    }

    public void dotestClassifierType( String testClassifier, String testType )
        throws Exception
    {
        mojo.classifier = testClassifier;
        mojo.type = testType;

        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();

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

        Set<Artifact> artifacts = new HashSet<Artifact>();
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

        Set<Artifact> artifacts = new HashSet<Artifact>();
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

        Set<Artifact> artifacts = new HashSet<Artifact>();
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

        Set<Artifact> artifacts = new HashSet<Artifact>();
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

    public void testExcludeProvidedScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "provided";
        // mojo.silent = false;

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getScope().equals( "provided" ), !file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }

    }

    public void testExcludeSystemScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "system";
        // mojo.silent = false;

        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getScope().equals( "system" ), !file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }

    }

    public void testExcludeCompileScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }

    public void testExcludeTestScope()
        throws IOException
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
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
        mojo.project.setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyPom()
        throws Exception
    {
        mojo.setCopyPom( true );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );

        Set<Artifact> set = new HashSet<Artifact>();
        set.add( stubFactory.createArtifact( "org.apache.maven", "maven-artifact", "2.0.7", Artifact.SCOPE_COMPILE ) );
        mojo.project.setArtifacts( set );
        mojo.execute();

        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName.substring( 0, fileName.length() - 4 ) + ".pom" );
            assertTrue( file.exists() );
        }
    }
    
    public void testPrependGroupId() 
        throws Exception
    {
        mojo.prependGroupId = true;
        mojo.execute();
    
        Iterator<Artifact> iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }
}
