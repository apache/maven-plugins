package org.apache.maven.plugin.dependency;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public class TestCopyDependenciesMojo
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "copy-dependencies", true );
    }

    private CopyDependenciesMojo getNewMojo()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-dependencies-test/plugin-config.xml" );
        CopyDependenciesMojo mojo = (CopyDependenciesMojo) lookupMojo( "copy-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        // mojo.silent = true;

        assertNotNull( mojo );
        assertNotNull( mojo.project );
        MavenProject project = mojo.project;

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        return mojo;

    }

    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testCopyDependenciesMojo()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoStripVersion()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.excludeTransitive = true;
        mojo.execute();
        Iterator iter = mojo.project.getDependencyArtifacts().iterator();

        // test - get all direct dependencies and verify that they exist
        // then delete the file and at the end, verify the folder is empty.
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertTrue( file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }
        // assumes you can't delete a folder that has files.
        assertTrue( mojo.outputDirectory.delete() );
    }

    public void testCopyDependenciesMojoExcludeType()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.excludeTypes = "jar";
        mojo.execute();

        // test - get all direct dependencies and verify that they exist if they
        // are not a jar
        // then delete the file and at the end, verify the folder is empty.
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), !file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }
        // assumes you can't delete a folder that has files.
        assertTrue( mojo.outputDirectory.delete() );
    }

    public void testCopyDependenciesMojoIncludeType()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );

        mojo.includeTypes = "jar";
        // if include is used, exclude should be ignored.
        mojo.excludeTypes = "jar";

        mojo.execute();

        // test - get all direct dependencies and verify that they exist only if
        // they are a jar
        // then delete the file and at the end, verify the folder is empty.
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( artifact.getType().equalsIgnoreCase( "jar" ), file.exists() );
            file.delete();
            assertFalse( file.exists() );
        }
        // assumes you can't delete a folder that has files.
        assertTrue( mojo.outputDirectory.delete() );
    }

    public void testCopyDependenciesMojoSubPerType()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.project.setArtifacts( stubFactory.getTypedArtifacts() );
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
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifact()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.useSubDirectoryPerArtifact = true;
        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File folder = DependencyUtil.getFormattedOutputDirectory( false, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoSubPerArtifactAndType()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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
            File folder = DependencyUtil.getFormattedOutputDirectory( true, true, mojo.outputDirectory, artifact );
            File file = new File( folder, fileName );
            assertTrue( file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeCompileScope()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeTestScope()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeRuntimeScope()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeprovidedScope()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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
            assertEquals( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludesystemScope()
        throws Exception
    {
        CopyDependenciesMojo mojo = getNewMojo();
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

            assertEquals( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ), file.exists() );
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
        CopyDependenciesMojo mojo = getNewMojo();
        mojo.classifier = testClassifier;
        mojo.type = testType;

        // init classifier things
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

            if ( !file.exists() )
            {
                fail( "Can't find:" + file.getAbsolutePath() );
            }
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
        CopyDependenciesMojo mojo = getNewMojo();
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
}