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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.pmd.cpd.CPD;
import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.JavaLanguage;
import net.sourceforge.pmd.cpd.Mark;
import net.sourceforge.pmd.cpd.Match;
import net.sourceforge.pmd.cpd.TokenEntry;

import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class CpdReportTest
    extends AbstractPmdReportTest
{
    /**
     * {@inheritDoc}
     */
    @Override
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
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check the contents of cpd.html
        String str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "AppSample.java" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "App.java" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "public String dup( String str )" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "tmp = tmp + str.substring( i, i + 1);" ) );
    }

    /**
     * Test CPDReport using custom configuration
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/custom-configuration/cpd-custom-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/cpd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // Contents that should NOT be in the report
        String str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertFalse( lowerCaseContains( str, "/Sample.java" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertFalse( lowerCaseContains( str, "public void duplicateMethod( int i )" ) );

        // Contents that should be in the report
        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "AnotherSample.java" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "public static void main( String[] args )" ) );

        str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/cpd.html" ) );
        assertTrue( lowerCaseContains( str, "private String unusedMethod(" ) );
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
            setVariableValueToObject( mojo, "compileSourceRoots", mojo.project.getCompileSourceRoots() );
            mojo.execute();

            fail( "MavenReportException must be thrown" );
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
        StringBuilder str = new StringBuilder( (int) file.length() );
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        while ( ( strTmp = in.readLine() ) != null )
        {
            str.append( ' ' );
            str.append( strTmp );
        }
        in.close();

        return str.toString();
    }

    public void testWriteNonHtml()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        assertNotNull( mojo );

        TokenEntry tFirstEntry = new TokenEntry( "public java", "MyClass.java", 34 );
        TokenEntry tSecondEntry = new TokenEntry( "public java", "MyClass3.java", 55 );
        List<Match> tList = new ArrayList<>();
        Mark tFirstMark = new Mark( tFirstEntry );
        Mark tSecondMark = new Mark( tSecondEntry );
        tFirstMark.setSoureCodeSlice( "// ----- ACCESSEURS  avec �l�ments -----" );
        Match tMatch = new Match( 2, tFirstMark, tSecondMark );
        tList.add( tMatch );

        CPDConfiguration cpdConfiguration = new CPDConfiguration();
        cpdConfiguration.setMinimumTileSize( 100 );
        cpdConfiguration.setLanguage( new JavaLanguage() );
        cpdConfiguration.setEncoding( "UTF-8" );
        CPD tCpd = new MockCpd( cpdConfiguration, tList.iterator() );

        tCpd.go();
        mojo.writeNonHtml( tCpd );

        File tReport = new File( "target/test/unit/default-configuration/target/cpd.xml" );
        // parseDocument( new BufferedInputStream( new FileInputStream( report ) ) );

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document pmdCpdDocument = builder.parse( tReport );
        assertNotNull( pmdCpdDocument );
    }

    public void testSkipEmptyReportConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/empty-report/cpd-skip-empty-report-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/cpd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testEmptyReportConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/empty-report/cpd-empty-report-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        // verify the generated files do exist, even if there are no violations
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/cpd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( new File( getBasedir(), "target/test/unit/empty-report/target/site/cpd.html" ) );
        assertFalse( lowerCaseContains( str, "Hello.java" ) );
    }

    public void testCpdEncodingConfiguration()
        throws Exception
    {
        String originalEncoding = System.getProperty( "file.encoding" );
        try
        {
            System.setProperty( "file.encoding", "UTF-16" );

            File testPom =
                new File( getBasedir(),
                          "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml" );
            CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
            mojo.execute();

            // check if the CPD files were generated
            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( lowerCaseContains( str, "AppSample.java" ) );
        }
        finally
        {
            System.setProperty( "file.encoding", originalEncoding );
        }
    }

    public void testCpdJavascriptConfiguration()
        throws Exception
    {
        File testPom =
                new File( getBasedir(), "src/test/resources/unit/default-configuration/cpd-javascript-plugin-config.xml" );
            CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
            mojo.execute();

            // verify  the generated file to exist and violations are reported
            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( lowerCaseContains( str, "Sample.js" ) );
            assertTrue( lowerCaseContains( str, "SampleDup.js" ) );
    }

    public void testCpdJspConfiguration()
            throws Exception
    {
        File testPom =
                new File( getBasedir(), "src/test/resources/unit/default-configuration/cpd-jsp-plugin-config.xml" );
            CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
            mojo.execute();

            // verify  the generated file to exist and violations are reported
            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( lowerCaseContains( str, "sample.jsp" ) );
            assertTrue( lowerCaseContains( str, "sampleDup.jsp" ) );
    }

    public static class MockCpd
        extends CPD
    {

        private Iterator<Match> matches;

        public MockCpd( CPDConfiguration configuration, Iterator<Match> tMatch )
        {
            super( configuration );
            matches = tMatch;
        }

        @Override
        public Iterator<Match> getMatches()
        {
            return matches;
        }

    }

}
