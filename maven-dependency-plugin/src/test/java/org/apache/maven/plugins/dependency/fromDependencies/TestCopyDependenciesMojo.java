package org.apache.maven.plugins.dependency.fromDependencies;

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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

public class TestCopyDependenciesMojo
    extends AbstractDependencyMojoTestCase
{

    CopyDependenciesMojo mojo;

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "copy-dependencies", true, false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-dependencies-test/plugin-config.xml" );
        mojo = (CopyDependenciesMojo) lookupMojo( "copy-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        // mojo.silent = true;

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();

        MavenSession session = newMavenSession( project );
        setVariableValueToObject( mojo, "session", session );

        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) session.getRepositorySession();

        repoSession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( stubFactory.getWorkingDir() ) );

        Set<Artifact> artifacts = this.stubFactory.getScopedArtifacts();
        Set<Artifact> directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );
        mojo.markersDirectory = new File( this.testDir, "markers" );

        ArtifactHandlerManager manager = lookup( ArtifactHandlerManager.class );
        setVariableValueToObject( mojo, "artifactHandlerManager", manager );
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

        copyFile( mojo, src, dest );
        assertTrue( dest.exists() );
    }

    /**
     * tests the proper discovery and configuration of the mojo
     *
     * @throws Exception in case of an error.
     */
    public void testMojo()
        throws Exception
    {
        mojo.execute();
        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
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

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testStripClassifier()
        throws Exception
    {
        mojo.stripClassifier = true;
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false, false, false, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testUseBaseVersion()
        throws Exception
    {
        mojo.useBaseVersion = true;
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false, false, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testNoTransitive()
        throws Exception
    {
        mojo.excludeTransitive = true;
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getDependencyArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testExcludeType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), !file.exists() );
        }
    }

    public void testIncludeType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeTypes = "jar";
        mojo.excludeTypes = "jar";
        // shouldn't get anything.

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeTypes = "";
        mojo.execute();

        artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), file.exists() );
        }
    }

    public void testExcludeArtifactId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeArtifactIds = "one";
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), !file.exists() );
        }
    }

    public void testIncludeArtifactId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeArtifactIds = "one";
        mojo.excludeArtifactIds = "one";
        // shouldn't get anything

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeArtifactIds = "";
        mojo.execute();

        artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getArtifactId().equals( "one" ), file.exists() );
        }
    }

    public void testIncludeGroupId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeGroupIds = "one";
        mojo.excludeGroupIds = "one";
        // shouldn't get anything

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeGroupIds = "";
        mojo.execute();

        artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getGroupId().equals( "one" ), file.exists() );
        }

    }

    public void testExcludeGroupId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeGroupIds = "one";
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( artifact.getGroupId().equals( "one" ), !file.exists() );
        }
    }

    public void testExcludeMultipleGroupIds()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeGroupIds = "one,two";
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( artifact.getGroupId().equals( "one" ) || artifact.getGroupId().equals( "two" ),
                          !file.exists() );
        }
    }

    public void testExcludeClassifier()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeClassifiers = "one";
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), !file.exists() );
        }
    }

    public void testIncludeClassifier()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getClassifiedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeClassifiers = "one";
        mojo.excludeClassifiers = "one";
        // shouldn't get anything

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertFalse( file.exists() );
        }

        mojo.excludeClassifiers = "";
        mojo.execute();

        artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getClassifier().equals( "one" ), file.exists() );
        }

    }

    public void testSubPerType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( false, true, false, false, false,
                                                                      mojo.outputDirectory, artifact );
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

        for ( Artifact artifact : mojo.getProject().getArtifacts() )
        {
            String type = testType != null ? testType : artifact.getType();

            stubFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                        artifact.getScope(), type, testClassifier );

        }

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
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
     * public void testOverwrite() { stubFactory.setCreateFiles( false ); Artifact artifact =
     * stubFactory.createArtifact( "test", "artifact", "1.0" ); File testFile = new File( getBasedir() +
     * File.separatorChar + "target/test-classes/unit/copy-dependencies-test/test.zip" ); }
     */

    public void testDontOverWriteRelease()
        throws MojoExecutionException, InterruptedException, IOException, MojoFailureException
    {

        Set<Artifact> artifacts = new HashSet<Artifact>();
        Artifact release = stubFactory.getReleaseArtifact();
        assertTrue( release.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        artifacts.add( release );

        mojo.getProject().setArtifacts( artifacts );
        mojo.getProject().setDependencyArtifacts( artifacts );

        mojo.overWriteIfNewer = false;

        mojo.execute();

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( release, false ) );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        assertTrue( copiedFile.setLastModified( time ) );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
    }

    /**
     * Test that the given artifact is copied, and the copy is overwritten if the Mojo is executed again.
     *
     * @throws Exception On errors
     */

    public void testOverWriteRelease()
        throws Exception
    {

        Set<Artifact> artifacts = new HashSet<Artifact>();
        Artifact release = stubFactory.getReleaseArtifact();

        assertTrue( release.getFile().setLastModified( 1000L ) );
        assertEquals( 1000L, release.getFile().lastModified() );

        artifacts.add( release );

        mojo.getProject().setArtifacts( artifacts );
        mojo.getProject().setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = true;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( release, false ) );

        assertTrue( copiedFile.setLastModified( 2000L ) );
        assertEquals( 2000L, copiedFile.lastModified() );

        mojo.execute();

        long timeCopyNow = copiedFile.lastModified();
        assertEquals( 1000L, timeCopyNow );
    }

    public void testDontOverWriteSnap()
        throws MojoExecutionException, InterruptedException, IOException, MojoFailureException
    {

        Set<Artifact> artifacts = new HashSet<Artifact>();
        Artifact snap = stubFactory.getSnapshotArtifact();
        assertTrue( snap.getFile().setLastModified( System.currentTimeMillis() - 2000 ) );

        artifacts.add( snap );

        mojo.getProject().setArtifacts( artifacts );
        mojo.getProject().setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = false;
        mojo.overWriteSnapshots = false;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( snap, false ) );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        assertTrue( copiedFile.setLastModified( time ) );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
    }

    /**
     * Test that the given artifact is copied, and the copy is overwritten if the Mojo is executed again.
     *
     * @throws Exception On errors
     */

    public void testOverWriteSnap()
        throws Exception
    {

        Set<Artifact> artifacts = new HashSet<Artifact>();
        Artifact snap = stubFactory.getSnapshotArtifact();

        assertTrue( snap.getFile().setLastModified( 1000L ) );
        assertEquals( 1000L, snap.getFile().lastModified() );

        artifacts.add( snap );

        mojo.getProject().setArtifacts( artifacts );
        mojo.getProject().setDependencyArtifacts( artifacts );

        mojo.overWriteReleases = false;
        mojo.overWriteSnapshots = true;
        mojo.overWriteIfNewer = false;

        mojo.execute();

        File copiedFile = new File( mojo.outputDirectory, DependencyUtil.getFormattedFileName( snap, false ) );

        assertTrue( copiedFile.setLastModified( 2000L ) );
        assertEquals( 2000L, copiedFile.lastModified() );

        mojo.execute();

        long timeCopyNow = copiedFile.lastModified();
        assertEquals( 1000L, timeCopyNow );
    }

    public void testGetDependencies()
        throws MojoExecutionException
    {
        assertEquals( mojo.getResolvedDependencies( true ).toString(),
                      mojo.getDependencySets( true ).getResolvedDependencies().toString() );
    }

    public void testExcludeProvidedScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "provided";
        // mojo.silent = false;

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
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
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "system";
        // mojo.silent = false;

        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
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
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }

    public void testExcludeTestScope()
        throws IOException, MojoFailureException
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
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
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.excludeScope );

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( !saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyPom()
        throws Exception
    {
        mojo.setCopyPom( true );

        Set<Artifact> set = new HashSet<Artifact>();
        set.add( stubFactory.createArtifact( "org.apache.maven", "maven-artifact", "2.0.7", Artifact.SCOPE_COMPILE ) );
        stubFactory.createArtifact( "org.apache.maven", "maven-artifact", "2.0.7", Artifact.SCOPE_COMPILE, "pom",
                                    null );
        mojo.getProject().setArtifacts( set );
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName.substring( 0, fileName.length() - 4 ) + ".pom" );
            assertTrue( file + " doesn't exist", file.exists() );
        }
    }

    public void testPrependGroupId()
        throws Exception
    {
        mojo.prependGroupId = true;
        mojo.execute();

        Set<Artifact> artifacts = mojo.getProject().getArtifacts();
        for ( Artifact artifact : artifacts )
        {
            String fileName = DependencyUtil.getFormattedFileName( artifact, false, true );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }
}
