package org.apache.maven.plugin.dependency;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.dependency.resolvers.ResolveDependenciesMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.project.MavenProject;

public class TestResolveMojo
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "markers", false );
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
        mojo.silent = false;
        assertNotNull( mojo );
        assertNotNull( mojo.project );
        MavenProject project = mojo.project;

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.execute();
        DependencyStatusSets results = mojo.getResults();
        assertNotNull( results );
        assertEquals( artifacts.size(), results.getResolvedDependencies().size() );

        setVariableValueToObject( mojo, "excludeTransitive", Boolean.TRUE );

        mojo.execute();
        results = mojo.getResults();
        assertNotNull( results );
        assertEquals( directArtifacts.size(), results.getResolvedDependencies().size() );
    }
    
    //TODO: Test skipping artifacts.
}