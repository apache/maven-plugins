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
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugins.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

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
        super.setUp( "unpack-dependencies", true, false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/unpack-dependencies-test/plugin-config.xml" );
        mojo = (UnpackDependenciesMojo) lookupMojo( "unpack-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        mojo.setUseJvmChmod( true );
        // mojo.silent = true;

        // it needs to get the archivermanager
        stubFactory.setUnpackableFile( mojo.getArchiverManager() );
        // i'm using one file repeatedly to archive so I can test the name
        // programmatically.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar + UNPACKABLE_FILE_PATH ) );

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

    protected void tearDown()
    {
        super.tearDown();

        mojo = null;
        System.gc();
    }

    public void assertUnpacked( Artifact artifact )
    {
        assertUnpacked( true, artifact );
    }

    public void assertUnpacked( boolean val, Artifact artifact )
    {
        File folder =
            DependencyUtil.getFormattedOutputDirectory( mojo.useSubDirectoryPerScope, mojo.useSubDirectoryPerType,
                                                        mojo.useSubDirectoryPerArtifact, mojo.useRepositoryLayout,
                                                        mojo.stripVersion, mojo.outputDirectory, artifact );

        File destFile = new File( folder, DependencyArtifactStubFactory.getUnpackableFileName( artifact ) );

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
        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testNoTransitive()
        throws Exception
    {
        mojo.excludeTransitive = true;
        mojo.execute();
        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getDependencyArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testExcludeType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !artifact.getType().equalsIgnoreCase( "jar" ), artifact );
        }
    }

    public void testExcludeProvidedScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeScope = "provided";
        // mojo.silent = false;

        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !artifact.getScope().equals( "provided" ), artifact );
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

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !artifact.getScope().equals( "system" ), artifact );
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

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !saf.include( artifact ), artifact );
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

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !saf.include( artifact ), artifact );
        }
    }

    public void testIncludeType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );

        mojo.includeTypes = "jar";
        mojo.excludeTypes = "jar";
        // shouldn't get anything

        mojo.execute();

        Iterator<Artifact> iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();

            assertUnpacked( false, artifact );
        }

        mojo.excludeTypes = "";
        mojo.execute();

        iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();

            assertUnpacked( artifact.getType().equalsIgnoreCase( "jar" ), artifact );
        }
    }

    public void testSubPerType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifact()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactAndType()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactRemoveVersion()
        throws Exception
    {
        mojo.useSubDirectoryPerArtifact = true;
        mojo.stripVersion = true;
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testSubPerArtifactAndTypeRemoveVersion()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getTypedArchiveArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.useSubDirectoryPerArtifact = true;
        mojo.useSubDirectoryPerType = true;
        mojo.stripVersion = true;
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( artifact );
        }
    }

    public void testIncludeCompileScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeTestScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeScope = "test";

        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeRuntimeScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( saf.include( artifact ), artifact );
        }
    }

    public void testIncludeprovidedScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeScope = "provided";

        mojo.execute();
        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ), artifact );
        }
    }

    public void testIncludesystemScope()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.includeScope = "system";

        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ), artifact );
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

        Iterator<Artifact> iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            assertUnpacked( false, artifact );
        }
        mojo.excludeArtifactIds = "";
        mojo.execute();

        iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            assertUnpacked( artifact.getArtifactId().equals( "one" ), artifact );
        }

    }

    public void testExcludeArtifactId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getArtifactArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeArtifactIds = "one";
        mojo.execute();

        // test - get all direct dependencies and verify that they exist if they
        // do not have a classifier of "one"
        // then delete the file and at the end, verify the folder is empty.
        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !artifact.getArtifactId().equals( "one" ), artifact );
        }
    }

    public void testExcludeGroupId()
        throws Exception
    {
        mojo.getProject().setArtifacts( stubFactory.getGroupIdArtifacts() );
        mojo.getProject().setDependencyArtifacts( new HashSet<Artifact>() );
        mojo.excludeGroupIds = "one";
        mojo.execute();

        for ( Artifact artifact : (Iterable<Artifact>) mojo.getProject().getArtifacts() )
        {
            assertUnpacked( !artifact.getGroupId().equals( "one" ), artifact );
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

        Iterator<Artifact> iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
            // Testing with artifact id because group id is not in filename
            assertUnpacked( false, artifact );
        }

        mojo.excludeGroupIds = "";
        mojo.execute();

        iter = mojo.getProject().getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = iter.next();
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

        for ( Artifact artifact : mojo.getProject().getArtifacts() )
        {
            String type = testType != null ? testType : artifact.getType();
            this.stubFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                             VersionRange.createFromVersion( artifact.getBaseVersion() ),
                                             artifact.getScope(), type, testClassifier, false );
        }

        mojo.execute();

        for ( Artifact artifact : mojo.getProject().getArtifacts() )
        {
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
            Artifact unpacked =
                stubFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                            Artifact.SCOPE_COMPILE, useType, useClassifier );
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
        File destDir =
            DependencyUtil.getFormattedOutputDirectory( mojo.isUseSubDirectoryPerScope(),
                                                        mojo.isUseSubDirectoryPerType(),
                                                        mojo.isUseSubDirectoryPerArtifact(), mojo.useRepositoryLayout,
                                                        mojo.stripVersion, mojo.getOutputDirectory(), artifact );
        File unpacked = new File( destDir, DependencyArtifactStubFactory.getUnpackableFileName( artifact ) );
        assertTrue( unpacked.exists() );
        return unpacked;
    }

    public DefaultFileMarkerHandler getUnpackedMarkerHandler( Artifact artifact )
    {
        return new DefaultFileMarkerHandler( artifact, mojo.getMarkersDirectory() );
    }

    public void assertUnpacked( Artifact artifact, boolean overWrite )
        throws InterruptedException, MojoExecutionException, MojoFailureException
    {
        File unpackedFile = getUnpackedFile( artifact );

        Thread.sleep( 100 );
        // round down to the last second
        long time = System.currentTimeMillis();
        time = time - ( time % 1000 );
        assertTrue( unpackedFile.setLastModified( time ) );
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
