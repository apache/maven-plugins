package org.apache.maven.plugin.clean;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test the clean mojo.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class CleanMojoTest
    extends AbstractMojoTestCase
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests the simple removal of directories
     *
     * @throws Exception
     */
    public void testBasicClean()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/basic-clean-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/basic-clean-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/basic-clean-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        mojo.execute();

        assertFalse( "Directory exists", checkExists( getBasedir() + "/target/test-classes/unit/"
            + "basic-clean-test/buildDirectory" ) );
        assertFalse( "Directory exists", checkExists( getBasedir() + "/target/test-classes/unit/basic-clean-test/"
            + "buildOutputDirectory" ) );
        assertFalse( "Directory exists", checkExists( getBasedir() + "/target/test-classes/unit/basic-clean-test/"
            + "buildTestDirectory" ) );
    }

    /**
     * Tests the removal of files and nested directories
     *
     * @throws Exception
     */
    public void testCleanNestedStructure()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/nested-clean-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/nested-clean-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/nested-clean-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        mojo.execute();

        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target/classes" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target/test-classes" ) );
    }

    /**
     * Tests that no exception is thrown when all internal variables are empty and that it doesn't
     * just remove whats there
     *
     * @throws Exception
     */
    public void testCleanEmptyDirectories()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/empty-clean-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/empty-clean-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/empty-clean-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupEmptyMojo( "clean", pluginPom );
        assertNotNull( mojo );

        mojo.execute();

        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/testDirectoryStructure" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/"
            + "testDirectoryStructure/file.txt" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/"
            + "testDirectoryStructure/outputDirectory" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/"
            + "testDirectoryStructure/outputDirectory/file.txt" ) );
    }

    /**
     * Tests the removal of files using fileset
     *
     * @throws Exception
     */
    public void testFilesetsClean()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/fileset-clean-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/fileset-clean-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/fileset-clean-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        mojo.execute();

        // fileset 1
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/test-classes" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes/file.txt" ) );
        assertTrue( checkEmpty( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes" ) );
        assertTrue( checkEmpty( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir" ) );

        // fileset 2
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/"
            + "buildOutputDirectory" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/"
            + "buildOutputDirectory/file.txt" ) );
    }

    /**
     * Tests the removal of a directory as file
     *
     * @throws Exception
     */
    public void testCleanInvalidDirectory()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/invalid-directory-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/invalid-directory-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/invalid-directory-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        try
        {
            mojo.execute();

            fail( "Should fail to delete a file treated as a directory" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
    }

    /**
     * Tests the removal of a missing directory
     *
     * @throws Exception
     */
    public void testMissingDirectory()
        throws Exception
    {
        String pluginPom = getBasedir() + "/src/test/resources/unit/missing-directory-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/missing-directory-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/missing-directory-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        mojo.execute();

        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/missing-directory-test/does-not-exist" ) );
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <br/>
     * Note: Unix systems doesn't lock any files.
     *
     * @throws Exception
     */
    public void testCleanLockedFile()
        throws Exception
    {
        if ( System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) == -1 )
        {
            assertTrue( "Ignored this test on none Windows based systems", true );
            return;
        }

        String pluginPom = getBasedir() + "/src/test/resources/unit/locked-file-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/locked-file-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/locked-file-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        assertNotNull( mojo );

        File f = new File( getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt" );
        FileChannel channel = null;
        FileLock lock = null;
        try
        {
            channel = new RandomAccessFile( f, "rw" ).getChannel();
            lock = channel.lock();

            mojo.execute();

            fail( "Should fail to delete a file that is locked" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
        finally
        {
            if ( lock != null )
            {
                lock.release();
            }

            if ( channel != null )
            {
                channel.close();
            }
        }
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <br/>
     * Note: Unix systems doesn't lock any files.
     *
     * @throws Exception
     */
    public void testCleanLockedFileWithNoError()
        throws Exception
    {
        if ( System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) == -1 )
        {
            assertTrue( "Ignored this test on none Windows based systems", true );
            return;
        }

        String pluginPom = getBasedir() + "/src/test/resources/unit/locked-file-test/plugin-pom.xml";

        // safety
        FileUtils.copyDirectory( new File( getBasedir(), "src/test/resources/unit/locked-file-test" ),
                                 new File( getBasedir(), "target/test-classes/unit/locked-file-test" ), null, "**/.svn,**/.svn/**" );

        CleanMojo mojo = (CleanMojo) lookupMojo( "clean", pluginPom );
        setVariableValueToObject( mojo, "failOnError", Boolean.FALSE );
        assertNotNull( mojo );

        File f = new File( getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt" );
        FileChannel channel = null;
        FileLock lock = null;
        try
        {
            channel = new RandomAccessFile( f, "rw" ).getChannel();
            lock = channel.lock();

            mojo.execute();

            assertTrue( true );
        }
        catch ( MojoExecutionException expected )
        {
            fail( "Should display a warning when deleting a file that is locked" );
        }
        finally
        {
            if ( lock != null )
            {
                lock.release();
            }

            if ( channel != null )
            {
                channel.close();
            }
        }
    }

    /**
     * @param dir a dir or a file
     * @return true if a file/dir exists, false otherwise
     */
    private boolean checkExists( String dir )
    {
        return FileUtils.fileExists( new File( dir ).getAbsolutePath() );
    }

    /**
     * @param dir a directory
     * @return true if a dir is empty, false otherwise
     */
    private boolean checkEmpty( String dir )
    {
        return FileUtils.sizeOfDirectory( new File( dir ).getAbsolutePath() ) == 0;
    }
}
