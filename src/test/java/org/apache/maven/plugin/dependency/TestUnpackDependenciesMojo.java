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
package org.apache.maven.plugin.dependency;

import java.io.File;
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

public class TestUnpackDependenciesMojo
    extends AbstractDependencyMojoTestCase
{

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
        stubFactory.setUnpackableFile( mojo.archiverManager );
        // i'm using one file repeatedly to archive so I can test the name
        // programmatically.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar
            + "target/test-classes/unit/unpack-dependencies-test/test.txt" ) );

        assertNotNull( mojo );
        assertNotNull( mojo.project );
        MavenProject project = mojo.project;

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
                                                                  mojo.outputDirectory, artifact );

        File destFile = new File( folder, stubFactory.getUnpackableFileName( artifact ) );

     /*
         * System.out.println( "Checking file: " + destFile.getPath() ); if (
         * val != destFile.exists() ) { System.out.println( "FAIL!" ); }
         */
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

    public void testUnpackDependenciesMojo()
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

    public void testUnpackDependenciesMojoNoTransitive()
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

    public void testUnpackDependenciesMojoExcludeType()
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

    public void testUnpackDependenciesMojoIncludeType()
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

    public void testUnpackDependenciesMojoSubPerType()
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

    public void testUnpackDependenciesMojoSubPerArtifact()
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

    public void testUnpackDependenciesMojoSubPerArtifactAndType()
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

    public void testUnpackDependenciesMojoIncludeCompileScope()
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

    public void testUnpackDependenciesMojoIncludeTestScope()
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

    public void testUnpackDependenciesMojoIncludeRuntimeScope()
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

    public void testUnpackDependenciesMojoIncludeprovidedScope()
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

    public void testUnpackDependenciesMojoIncludesystemScope()
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

    public void testCDMClassifier()
        throws Exception
    {
        dotestUnpackDependenciesMojoClassifierType( "jdk14", null );
    }

    public void testCDMType()
        throws Exception
    {
        dotestUnpackDependenciesMojoClassifierType( null, "zip" );
    }

    public void testCDMClassifierType()
        throws Exception
    {
        dotestUnpackDependenciesMojoClassifierType( "jdk14", "war" );
    }

    public void dotestUnpackDependenciesMojoClassifierType( String testClassifier, String testType )
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
     * public void testDontOverWriteRelease() throws MojoExecutionException,
     * InterruptedException, IOException {
     * 
     * Set artifacts = new HashSet(); Artifact release =
     * stubFactory.getReleaseArtifact(); release.getFile().setLastModified(
     * System.currentTimeMillis() - 2000 );
     * 
     * artifacts.add( release );
     * 
     * mojo.project.setArtifacts( artifacts );
     * mojo.project.setDependencyArtifacts( artifacts );
     * 
     * mojo.overWriteIfNewer = false;
     * 
     * mojo.execute();
     * 
     * File copiedFile = new File( mojo.outputDirectory,
     * DependencyUtil.getFormattedFileName( release, false ) );
     * 
     * Thread.sleep( 100 ); long time = System.currentTimeMillis();
     * copiedFile.setLastModified( time ); Thread.sleep( 100 );
     * 
     * mojo.execute();
     * 
     * assertEquals( time, copiedFile.lastModified() ); }
     * 
     * public void testOverWriteRelease() throws MojoExecutionException,
     * InterruptedException, IOException {
     * 
     * Set artifacts = new HashSet(); Artifact release =
     * stubFactory.getReleaseArtifact(); release.getFile().setLastModified(
     * System.currentTimeMillis() - 2000 );
     * 
     * artifacts.add( release );
     * 
     * mojo.project.setArtifacts( artifacts );
     * mojo.project.setDependencyArtifacts( artifacts );
     * 
     * mojo.overWriteReleases = true; mojo.overWriteIfNewer = false;
     * 
     * mojo.execute();
     * 
     * File copiedFile = new File( mojo.outputDirectory,
     * DependencyUtil.getFormattedFileName( release, false ) );
     * 
     * Thread.sleep( 100 ); long time = System.currentTimeMillis();
     * copiedFile.setLastModified( time ); Thread.sleep( 100 );
     * 
     * mojo.execute();
     * 
     * assertTrue( time < copiedFile.lastModified() ); }
     * 
     * public void testDontOverWriteSnap() throws MojoExecutionException,
     * InterruptedException, IOException {
     * 
     * Set artifacts = new HashSet(); Artifact snap =
     * stubFactory.getSnapshotArtifact(); snap.getFile().setLastModified(
     * System.currentTimeMillis() - 2000 );
     * 
     * artifacts.add( snap );
     * 
     * mojo.project.setArtifacts( artifacts );
     * mojo.project.setDependencyArtifacts( artifacts );
     * 
     * mojo.overWriteReleases = false; mojo.overWriteSnapshots = false;
     * mojo.overWriteIfNewer = false;
     * 
     * mojo.execute();
     * 
     * File copiedFile = new File( mojo.outputDirectory,
     * DependencyUtil.getFormattedFileName( snap, false ) );
     * 
     * Thread.sleep( 100 ); long time = System.currentTimeMillis();
     * copiedFile.setLastModified( time ); Thread.sleep( 100 );
     * 
     * mojo.execute();
     * 
     * assertEquals( time, copiedFile.lastModified() ); }
     * 
     * public void testOverWriteSnap() throws MojoExecutionException,
     * InterruptedException, IOException {
     * 
     * Set artifacts = new HashSet(); Artifact snap =
     * stubFactory.getSnapshotArtifact(); snap.getFile().setLastModified(
     * System.currentTimeMillis() - 2000 );
     * 
     * artifacts.add( snap );
     * 
     * mojo.project.setArtifacts( artifacts );
     * mojo.project.setDependencyArtifacts( artifacts );
     * 
     * mojo.overWriteReleases = false; mojo.overWriteSnapshots = true;
     * mojo.overWriteIfNewer = false;
     * 
     * mojo.execute();
     * 
     * File copiedFile = new File( mojo.outputDirectory,
     * DependencyUtil.getFormattedFileName( snap, false ) );
     * 
     * Thread.sleep( 100 ); long time = System.currentTimeMillis();
     * copiedFile.setLastModified( time ); Thread.sleep( 100 );
     * 
     * mojo.execute();
     * 
     * assertTrue( time < copiedFile.lastModified() ); }
     * 
     * public void testGetDependencies() throws MojoExecutionException {
     * assertSame( mojo.getDependencies( true ), mojo.getDependencySets( true
     * ).getResolvedDependencies() ); }
     */
}