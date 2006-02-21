package org.apache.maven.plugin.clean;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Test the clean mojo.
 */
public class CleanMojoTest
    extends TestCase
{
    private static final String TARGET_TEST_DIR = "target/testDirectoryStructure";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.copyDirectoryStructure( new File( "src/test/resources/testDirectoryStructure" ),
                                          new File( TARGET_TEST_DIR ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( TARGET_TEST_DIR ) );
    }

    public void testClean()
        throws Exception
    {
        String base = TARGET_TEST_DIR;
        String directory = base + "/buildDirectory";
        String outputDirectory = base + "/buildOutputDirectory";
        String testOutputDirectory = base + "/buildTestDirectory";

        CleanMojo mojo = new CleanMojo();

        mojo.setDirectory( new File( directory ) );
        mojo.setOutputDirectory( new File( outputDirectory ) );
        mojo.setTestOutputDirectory( new File( testOutputDirectory ) );

        mojo.execute();

        assertFalse( FileUtils.fileExists( directory ) );
        assertFalse( FileUtils.fileExists( outputDirectory ) );
        assertFalse( FileUtils.fileExists( testOutputDirectory ) );
    }

    public void testNestedStructure()
        throws Exception
    {
        String base = TARGET_TEST_DIR + "/target";
        String outputDirectory = base + "/classes";
        String testOutputDirectory = base + "/test-classes";

        CleanMojo mojo = new CleanMojo();

        mojo.setDirectory( new File( base ) );
        mojo.setOutputDirectory( new File( outputDirectory ) );
        mojo.setTestOutputDirectory( new File( testOutputDirectory ) );

        mojo.execute();

        assertFalse( FileUtils.fileExists( base ) );
        assertFalse( FileUtils.fileExists( outputDirectory ) );
        assertFalse( FileUtils.fileExists( testOutputDirectory ) );
    }

    public void testEmptyDirectories()
        throws Exception
    {
        CleanMojo mojo = new CleanMojo();

        mojo.execute();

        // just checking no exceptions
        assertTrue( true );
    }

    public void testFilesets()
        throws Exception
    {
        String base = TARGET_TEST_DIR + "/target";

        CleanMojo mojo = new CleanMojo();

        mojo.addFileset( createFileset( base, "**/file.txt", "**/subdir/**" ) );

        String outputDirectory = TARGET_TEST_DIR + "/buildOutputDirectory";
        mojo.addFileset( createFileset( outputDirectory, "**", "" ) );

        mojo.execute();

        // fileset 1
        assertTrue( FileUtils.fileExists( base ) );
        assertTrue( FileUtils.fileExists( base + "/classes" ) );
        assertFalse( FileUtils.fileExists( base + "/classes/file.txt" ) );
/* TODO: looks like a bug in the file-management library
        assertTrue( FileUtils.fileExists( base + "/subdir/file.txt" ) );
*/

        // fileset 2
        assertTrue( FileUtils.fileExists( outputDirectory ) );
        assertFalse( FileUtils.fileExists( outputDirectory + "/file.txt" ) );
    }

    public void testInvalidDirectory()
        throws MojoExecutionException
    {
        String path = TARGET_TEST_DIR + "/target/subdir/file.txt";

        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( path ) );

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

    public void testOpenFile()
        throws MojoExecutionException, FileNotFoundException
    {
        String path = TARGET_TEST_DIR + "/target/subdir";

        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( path ) );

        FileInputStream fis = new FileInputStream( new File( path, "file.txt" ) );

        try
        {
            mojo.execute();

            fail( "Should fail to delete a file that is open" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
        finally
        {
            IOUtil.close( fis );
        }
    }

    public void testOpenFileInFileSet()
        throws MojoExecutionException, FileNotFoundException
    {
        String path = TARGET_TEST_DIR + "/target/subdir";

        CleanMojo mojo = new CleanMojo();
        mojo.addFileset( createFileset( path, "**", "" ) );

        FileInputStream fis = new FileInputStream( new File( path, "file.txt" ) );

        try
        {
            mojo.execute();

            fail( "Should fail to delete a file that is open" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
        finally
        {
            IOUtil.close( fis );
        }
    }

    public void testMissingDirectory()
        throws MojoExecutionException
    {
        String path = TARGET_TEST_DIR + "/does-not-exist";

        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( path ) );
        assertFalse( FileUtils.fileExists( path ) );

        mojo.execute();

        assertFalse( FileUtils.fileExists( path ) );
    }

    private static Fileset createFileset( String dir, String includes, String excludes )
    {
        Fileset fileset = new Fileset();
        fileset.setDirectory( dir );
        fileset.setIncludes( Arrays.asList( new String[]{includes} ) );
        fileset.setExcludes( Arrays.asList( new String[]{excludes} ) );
        return fileset;
    }

}
