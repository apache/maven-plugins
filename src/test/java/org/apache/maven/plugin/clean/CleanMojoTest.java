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

    private String basedir;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir", System.getProperty( "user.dir" ) );

        FileUtils.copyDirectoryStructure( new File( basedir, "src/test/resources/testDirectoryStructure" ),
                                          new File( basedir, TARGET_TEST_DIR ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( basedir, TARGET_TEST_DIR ) );
    }

    public void testClean()
        throws Exception
    {
        String base = TARGET_TEST_DIR;
        String directory = base + "/buildDirectory";
        String outputDirectory = base + "/buildOutputDirectory";
        String testOutputDirectory = base + "/buildTestDirectory";
        String reportDirectory = base + "/reportDirectory";

        CleanMojo mojo = new CleanMojo();

        mojo.setDirectory( new File( basedir, directory ) );
        mojo.setOutputDirectory( new File( basedir, outputDirectory ) );
        mojo.setTestOutputDirectory( new File( basedir, testOutputDirectory ) );
        mojo.setReportDirectory( new File( basedir, reportDirectory ) );

        mojo.execute();

        assertFalse( checkExists( directory ) );
        assertFalse( checkExists( outputDirectory ) );
        assertFalse( checkExists( testOutputDirectory ) );
        assertFalse( checkExists( reportDirectory ) );
    }

    public void testNestedStructure()
        throws Exception
    {
        String base = TARGET_TEST_DIR + "/target";
        String outputDirectory = base + "/classes";
        String testOutputDirectory = base + "/test-classes";

        CleanMojo mojo = new CleanMojo();

        mojo.setDirectory( new File( basedir, base ) );
        mojo.setOutputDirectory( new File( basedir, outputDirectory ) );
        mojo.setTestOutputDirectory( new File( basedir, testOutputDirectory ) );

        mojo.execute();

        assertFalse( checkExists( base ) );
        assertFalse( checkExists( outputDirectory ) );
        assertFalse( checkExists( testOutputDirectory ) );
    }

    private boolean checkExists( String testOutputDirectory )
    {
        return FileUtils.fileExists( new File( basedir, testOutputDirectory ).getAbsolutePath() );
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
        assertTrue( checkExists( base ) );
        assertTrue( checkExists( base + "/classes" ) );
        assertFalse( checkExists( base + "/classes/file.txt" ) );
        /* TODO: looks like a bug in the file-management library
         assertTrue( FileUtils.fileExists( base + "/subdir/file.txt" ) );
         */

        // fileset 2
        assertTrue( checkExists( outputDirectory ) );
        assertFalse( checkExists( outputDirectory + "/file.txt" ) );
    }

    public void testInvalidDirectory()
        throws MojoExecutionException
    {
        String path = TARGET_TEST_DIR + "/target/subdir/file.txt";

        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( basedir, path ) );

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

    /* Unix will let you get away with it, not sure how to lock the file from Java.
     public void testOpenFile()
     throws MojoExecutionException, FileNotFoundException
     {
     String path = TARGET_TEST_DIR + "/target/subdir";

     CleanMojo mojo = new CleanMojo();
     mojo.setDirectory( new File( basedir, path ) );

     FileInputStream fis = new FileInputStream( new File( basedir, path + "/file.txt" ) );

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

     FileInputStream fis = new FileInputStream( new File( basedir, path + "/file.txt" ) );

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
     */

    public void testMissingDirectory()
        throws MojoExecutionException
    {
        String path = TARGET_TEST_DIR + "/does-not-exist";

        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( basedir, path ) );
        assertFalse( checkExists( path ) );

        mojo.execute();

        assertFalse( checkExists( path ) );
    }

    private Fileset createFileset( String dir, String includes, String excludes )
    {
        Fileset fileset = new Fileset();
        fileset.setDirectory( new File( basedir, dir ).getAbsolutePath() );
        fileset.setIncludes( Arrays.asList( new String[] { includes } ) );
        fileset.setExcludes( Arrays.asList( new String[] { excludes } ) );
        return fileset;
    }

}
