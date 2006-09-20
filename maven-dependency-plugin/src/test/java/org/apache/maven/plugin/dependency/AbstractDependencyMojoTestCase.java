package org.apache.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.DependencyTestUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class AbstractDependencyMojoTestCase
    extends AbstractMojoTestCase
{

    protected File testDir;

    protected ArtifactStubFactory stubFactory;

    public AbstractDependencyMojoTestCase()
    {
        super();
    }

    protected void setUp( String testDirStr, boolean createFiles )
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        // pick random output location
        Random a = new Random();
        testDir = new File( getBasedir(), "target" + File.separatorChar + "unit-tests" + File.separatorChar
            + testDirStr + a.nextLong() + File.separatorChar );
        testDir.delete();
        assertFalse( testDir.exists() );

        stubFactory = new ArtifactStubFactory( this.testDir, createFiles );

    }

    protected void tearDown()
    {
        try
        {
            DependencyTestUtils.removeDirectory( testDir );
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue( false );
        }
        assertFalse( testDir.exists() );
    }

}
