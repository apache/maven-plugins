package org.apache.maven.plugin.dependency;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;

import org.apache.maven.plugin.dependency.resolvers.ResolveDependenciesMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

public class TestResolveMojo
    extends AbstractMojoTestCase
{
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
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
        project.setArtifacts(new HashSet());
        project.setDependencyArtifacts(new HashSet());
   
   //     mojo.execute();
    }
}