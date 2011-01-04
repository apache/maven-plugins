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

import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdReportTest
    extends AbstractPmdReportTest
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();
        FileUtils.deleteDirectory( new File( getBasedir(), "target/test/unit" ) );
    }

    public void testDefaultConfiguration()
        throws Exception
    {
        FileUtils.copyDirectoryStructure(
            new File( getBasedir(), "src/test/resources/unit/default-configuration/jxr-files" ),
            new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        //check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/imports.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/unusedcode.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if there's a link to the JXR files
        String str =
            readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" ) );


        assertTrue( str.indexOf( "/xref/def/configuration/App.html#31" ) != -1 );

        assertTrue( str.indexOf( "/xref/def/configuration/AppSample.html#45" ) != -1 );
    }


    public void testFileURL()
        throws Exception
    {
        FileUtils.copyDirectoryStructure(
            new File( getBasedir(), "src/test/resources/unit/default-configuration/jxr-files" ),
            new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        URL url = getClass().getClassLoader().getResource( "rulesets/basic.xml" );
        URL url2 = getClass().getClassLoader().getResource( "rulesets/unusedcode.xml" );
        URL url3 = getClass().getClassLoader().getResource( "rulesets/imports.xml" );
        mojo.setRulesets( new String[] { url.toString(), url2.toString() , url3.toString() } );

        mojo.execute();

        //check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/imports.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/unusedcode.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if there's a link to the JXR files
        String str =
            readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" ) );


        assertTrue( str.indexOf( "/xref/def/configuration/App.html#31" ) != -1 );

        assertTrue( str.indexOf( "/xref/def/configuration/AppSample.html#45" ) != -1 );
    }

    /**
     * With custom rulesets
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/custom-configuration/custom-configuration-plugin-config.xml" );

        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        //check the generated files
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/pmd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/custom.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if custom ruleset was applied
        String str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" ) );
        assertTrue( str.toLowerCase().indexOf( "Avoid using if statements without curly braces".toLowerCase() ) != -1 );

        assertTrue(
            str.toLowerCase().indexOf( "Avoid using if...else statements without curly braces".toLowerCase() ) != -1 );

        assertTrue("unnecessary constructor should not be triggered because of low priority",
            str.toLowerCase().indexOf( "Avoid unnecessary constructors - the compiler will generate these for you".toLowerCase() ) == -1 );

    }

    /**
     * Verify skip parameter
     *
     * @throws Exception
     */
    public void testSkipConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/custom-configuration/skip-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        File basedir = new File( getBasedir(), "target/test/unit/skip-configuration" );

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/custom.xml" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testInvalidFormat()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/invalid-format/invalid-format-plugin-config.xml" );
            PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            setVariableValueToObject( mojo, "compileSourceRoots", mojo.project.getCompileSourceRoots() );
            mojo.executeReport( Locale.ENGLISH );

            fail( "Must throw MavenReportException." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    public void testInvalidTargetJdk()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/invalid-format/invalid-target-jdk-plugin-config.xml" );
            PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            mojo.execute();

            fail( "Must throw MavenReportException." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
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
        String strTmp;
        StringBuffer str = new StringBuffer( (int) file.length() );
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        while ( ( strTmp = in.readLine() ) != null )
        {
            str.append( ' ' );
            str.append( strTmp );
        }
        in.close();

        return str.toString();
    }


}
