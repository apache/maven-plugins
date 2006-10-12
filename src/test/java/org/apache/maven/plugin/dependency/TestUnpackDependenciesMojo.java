package org.apache.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.DependencyTestUtils;
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

        // i'm using one file repeatedly to unpack.
        stubFactory.setSrcFile( new File( getBasedir() + File.separatorChar
            + "target/test-classes/unit/unpack-dependencies-test/test.zip" ) );

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

    public void assertMarkerFile( Artifact artifact )
    {
        DefaultFileMarkerHandler handle = new DefaultFileMarkerHandler( artifact, mojo.markersDirectory );
        try
        {
            assertTrue( handle.isMarkerSet() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getLongMessage() );
        }
    }
    
    public void assertMarkerFile(boolean val, Artifact artifact)
    {
        if (val)
        {
            assertMarkerFile(artifact);
        }
        else
        {
            assertNoMarkerFile(artifact);
        }
    }

    public void testNull()
    {

    }

    public void testUnpackDependenciesMojo()
        throws Exception
    {
        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertMarkerFile( artifact );
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
            assertMarkerFile( artifact );
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

            assertMarkerFile(!artifact.getType().equalsIgnoreCase( "jar" ),artifact );
            
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

            assertMarkerFile(artifact.getType().equalsIgnoreCase( "jar" ),artifact );        }
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( true, false, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertMarkerFile(artifact);
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( false, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertMarkerFile(artifact);
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( true, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertMarkerFile(artifact);
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertMarkerFile( saf.include( artifact ), artifact );
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertMarkerFile( saf.include( artifact ), artifact );
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertMarkerFile( saf.include( artifact ), artifact );
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertMarkerFile( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ), artifact );
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
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertMarkerFile( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ), artifact );
        }
    }

/*    public void testCDMClassifier()
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
    }*/

    public void dotestUnpackDependenciesMojoClassifierType( String testClassifier, String testType )
        throws Exception
    {
        mojo.classifier = testClassifier;
        mojo.type = testType; // init classifier things
        mojo.factory = DependencyTestUtils.getArtifactFactory();
        mojo.resolver = new StubArtifactResolver( false, false );
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
                useClassifier = "-" + testClassifier;
                // type is only used if classifier is used.
                if ( StringUtils.isNotEmpty( testType ) )
                {
                    useType = testType;
                }
            }
            String fileName = artifact.getArtifactId() + useClassifier + "-" + artifact.getVersion() + "." + useType;
            File file = new File( mojo.outputDirectory, fileName );

            assertMarkerFile(artifact);
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
        mojo.resolver = new StubArtifactResolver( are, anfe );
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
        long time = System.currentTimeMillis();
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
        long time = System.currentTimeMillis();
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

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
        long time = System.currentTimeMillis();
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
        long time = System.currentTimeMillis();
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
    }

    public void testGetDependencies()
        throws MojoExecutionException
    {
        assertSame( mojo.getDependencies( true ), mojo.getDependencySets( true ).getResolvedDependencies() );
    }
*/
}