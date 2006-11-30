package org.apache.maven.plugin.pmd;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 */
public class CpdReportTest
    extends AbstractMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();
        FileUtils.deleteDirectory( new File( getBasedir(), "target/test/unit" ) );
    }

    /**
     * Test CPDReport given the default configuration
     *
     * @throws Exception
     */
    public void testDefaultConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        //check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check the contents of cpd.html
        String str =
            readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "AppSample.java".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "App.java".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "public String dup( String str )".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "tmp = tmp + str.substring( i, i + 1);".toLowerCase() ) != -1 );

    }

    /**
     * Test CPDReport using custom configuration
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/custom-configuration/cpd-custom-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        //check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/cpd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // Contents that should NOT be in the report
        String str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/Sample.java".toLowerCase() ) == -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "public void duplicateMethod( int i )".toLowerCase() ) == -1 );

        // Contents that should be in the report
        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "AnotherSample.java".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "public static void main( String[] args )".toLowerCase() ) != -1 );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( str.toLowerCase().indexOf(
            "private String unusedMethod( String unusedParam )".toLowerCase() ) != -1 );

    }

    /**
     * Test CPDReport with invalid format
     *
     * @throws Exception
     */
    public void testInvalidFormat()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/invalid-format/cpd-invalid-format-plugin-config.xml" );
            CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
            mojo.execute();

            fail( "MavenReportException must be thrown" );
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
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws java.io.IOException
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
