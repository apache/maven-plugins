package org.apache.maven.plugin.jar;

import org.apache.maven.plugins.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * 
 */
public class JarMojoTest extends AbstractMojoTestCase
{
    private File testPom = new File ( getBasedir(), "src/test/resources/unit/jar-basic-test/pom.xml" );

    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();

    }

    /**
     * tests the proper discovery and configuration of the mojo
     *
     * @throws Exception
     */
    public void testJarTestEnvironment() throws Exception {

        //File pom = new File( getBasedir(), "src/test/resources/unit/clean/pom.xml" );

        JarMojo mojo = (JarMojo) lookupMojo ("jar", testPom );

        assertNotNull( mojo );

        assertEquals( "foo", mojo.getProject().getGroupId() );
    }

}
