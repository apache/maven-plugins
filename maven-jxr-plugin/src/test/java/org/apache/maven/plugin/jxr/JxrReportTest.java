package org.apache.maven.plugin.jxr;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:dennisl@apache.org">Dennis Lundberg</a>
 */
public class JxrReportTest
    extends AbstractMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Test the plugin with default configuration
     *
     * @throws Exception
     */
    public void testDefaultConfiguration()
        throws Exception
    {
        copyFilesFromDirectory( new File( getBasedir(), "src/test/resources/unit/default-configuration/javadoc-files" ),
                                new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        //check if xref files were generated
        File generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/xref/allclasses-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/xref/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/xref/overview-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/xref/overview-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/xref/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/xref/def/configuration/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/xref/def/configuration/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/xref/def/configuration/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/xref/def/configuration/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if there's a link to the javadoc files
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/default-configuration/target/site/xref/def/configuration/AppSample.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/def/configuration/AppSample.html\"".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/xref/def/configuration/App.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/def/configuration/app.html\"".toLowerCase() ) != -1 );

    }

    /**
     * Test when javadocLink is disabled in the configuration
     *
     * @throws Exception
     */
    public void testNoJavadocLink()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/nojavadoclink-configuration/nojavadoclink-configuration-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        //check if xref files were generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/nojavadoclink-configuration/target/site/xref/allclasses-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/nojavadoclink-configuration/target/site/xref/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/overview-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/overview-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/nojavadoclink-configuration/target/site/xref/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/sample/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/sample/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/sample/Sample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if there's a link to the javadoc files
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/AppSample.html" ) );
        assertTrue(
            str.toLowerCase().indexOf( "/apidocs/nojavadoclink/configuration/AppSample.html\"".toLowerCase() ) == -1 );

        str = readFile( new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/App.html" ) );
        assertTrue(
            str.toLowerCase().indexOf( "/apidocs/nojavadoclink/configuration/app.html\"".toLowerCase() ) == -1 );

        str = readFile( new File( getBasedir(),
                                  "target/test/unit/nojavadoclink-configuration/target/site/xref/nojavadoclink/configuration/sample/Sample.html" ) );
        assertTrue( str.toLowerCase().indexOf(
            "/apidocs/nojavadoclink/configuration/sample/sample.html\"".toLowerCase() ) == -1 );

    }

    /**
     * Method for testing plugin when aggregate parameter is set to true
     *
     * @throws Exception
     */
    public void testAggregate()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/aggregate-test/aggregate-test-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        //check if xref files were generated for submodule1
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule1/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule1/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule1/Submodule1App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule1/Submodule1AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if xref files were generated for submodule2
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule2/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule2/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule2/Submodule2App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/xref/aggregate/test/submodule2/Submodule2AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }

    /**
     * Method for testing plugin when the specified javadocDir does not exist
     *
     * @throws Exception
     */
    public void testNoJavadocDir()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/nojavadocdir-test/nojavadocdir-test-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        //check if there's a link to the javadoc files
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/nojavadocdir-test/target/site/xref/nojavadocdir/test/AppSample.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/nojavadocdir/test/AppSample.html".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(),
                                  "target/test/unit/nojavadocdir-test/target/site/xref/nojavadocdir/test/App.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/nojavadocdir/test/app.html".toLowerCase() ) != -1 );

    }

    /**
     * Test the plugin with an exclude configuration.
     *
     * @throws Exception
     */
    public void testExclude()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/exclude-configuration/exclude-configuration-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        // check that the non-excluded xref files were generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/exclude-configuration/target/site/xref/exclude/configuration/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check that the excluded xref files were not generated
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/exclude-configuration/target/site/xref/exclude/configuration/AppSample.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * Test the plugin with an include configuration.
     *
     * @throws Exception
     */
    public void testInclude()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/include-configuration/include-configuration-plugin-config.xml" );
        JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
        mojo.execute();

        // check that the included xref files were generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/include-configuration/target/site/xref/include/configuration/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check that the non-included xref files were not generated
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/include-configuration/target/site/xref/include/configuration/AppSample.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testExceptions()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/default-configuration/exception-test-plugin-config.xml" );
            JxrReport mojo = (JxrReport) lookupMojo( "jxr", testPom );
            mojo.execute();

            fail( "Must throw exception");
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    protected void tearDown()
        throws Exception
    {

    }

    /**
     * Copy files from the specified source directory to the specified destination directory
     *
     * @param srcDir
     * @param destDir
     * @throws IOException
     */
    private void copyFilesFromDirectory( File srcDir, File destDir )
        throws IOException
    {
        FileUtils.copyDirectoryStructure( srcDir, destDir );
    }

    /**
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException
     */
    private String readFile( File file )
        throws IOException
    {
        String str = "", strTmp = "";
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        while ( ( strTmp = in.readLine() ) != null )
        {
            str = str + " " + strTmp;
        }
        in.close();

        return str;
    }

}
