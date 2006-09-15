package org.apache.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.maven.plugin.dependency.resolvers.ResolveDependenciesMojo;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.DependencyTestUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

public class TestResolveMojo
    extends AbstractMojoTestCase
{
    File outputFolder;
    
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        //pick random output location
        Random a = new Random();
        outputFolder = new File("target/markers"+a.nextLong()+"/");
        outputFolder.delete();
        assertFalse(outputFolder.exists());
    }

    protected void tearDown()
    {
        try
        {
            DependencyTestUtils.removeDirectory(outputFolder);
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }
        assertFalse(outputFolder.exists());
    }
    
    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testresolveTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/resolve-test/plugin-config.xml" );
        ResolveDependenciesMojo mojo = (ResolveDependenciesMojo) lookupMojo( "resolve", testPom );

        assertNotNull( mojo );
        assertNotNull(mojo.project);
        MavenProject project = mojo.project;
              
        ArtifactStubFactory factory = new ArtifactStubFactory(outputFolder,true);
        
        Set artifacts = factory.getScopedArtifacts();
        Set directArtifacts = factory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll(directArtifacts);
        
        project.setArtifacts(artifacts);
        project.setDependencyArtifacts(directArtifacts);
   
        mojo.execute();
        DependencyStatusSets results = mojo.getResults();
        assertNotNull(results);
        assertEquals(artifacts.size(),results.getResolvedDependencies().size());
        
        
        setVariableValueToObject(mojo,"excludeTransitive",Boolean.TRUE);
        
        mojo.execute();
        results = mojo.getResults();
        assertNotNull(results);
        assertEquals(directArtifacts.size(),results.getResolvedDependencies().size());
    }
}