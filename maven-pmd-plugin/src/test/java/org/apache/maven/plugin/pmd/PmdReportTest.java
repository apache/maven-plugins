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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdReportTest
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

    public void testDefaultConfiguration()
        throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/java-basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/java-imports.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/java-unusedcode.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if there's a link to the JXR files
        String str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" ) );

        assertTrue( str.contains( "/xref/def/configuration/App.html#L31" ) );

        assertTrue( str.contains( "/xref/def/configuration/AppSample.html#L45" ) );
    }

    public void testJavascriptConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/javascript-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // these are the rulesets, that have been applied...
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/ecmascript-basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/ecmascript-braces.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/ecmascript-unnecessary.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );
        assertTrue( str.contains( "Avoid using global variables" ) );
    }

    public void testFileURL()
        throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        // Additional test case for MPMD-174 (http://jira.codehaus.org/browse/MPMD-174).
        WireMockServer mockServer = new WireMockServer( 3456 );
        mockServer.start();

        String sonarRuleset =
            IOUtils.toString( getClass().getClassLoader().getResourceAsStream( "unit/default-configuration/rulesets/sonar-way-ruleset.xml" ),
                    StandardCharsets.UTF_8 );

        String sonarMainPageHtml =
            IOUtils.toString( getClass().getClassLoader().getResourceAsStream( "unit/default-configuration/rulesets/sonar-main-page.html" ),
                    StandardCharsets.UTF_8 );

        final String sonarBaseUrl = "/profiles";
        final String sonarProfileUrl = sonarBaseUrl + "/export?format=pmd&language=java&name=Sonar%2520way";
        final String sonarExportRulesetUrl = "http://localhost:" + mockServer.port() + sonarProfileUrl;

        mockServer.stubFor( WireMock.get( WireMock.urlEqualTo( sonarBaseUrl ) ).willReturn( WireMock.aResponse().withStatus( 200 ).withHeader( "Content-Type",
                                                                                                                                               "text/html" ).withBody( sonarMainPageHtml ) ) );

        mockServer.stubFor( WireMock.get( WireMock.urlEqualTo( sonarProfileUrl ) ).willReturn( WireMock.aResponse().withStatus( 200 ).withHeader( "Content-Type",
                                                                                                                                                  "text/xml" ).withBody( sonarRuleset ) ) );

        URL url = getClass().getClassLoader().getResource( "rulesets/java/basic.xml" );
        URL url2 = getClass().getClassLoader().getResource( "rulesets/java/unusedcode.xml" );
        URL url3 = getClass().getClassLoader().getResource( "rulesets/java/imports.xml" );
        mojo.setRulesets( new String[] { url.toString(), url2.toString(), url3.toString(), sonarExportRulesetUrl } );

        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/imports.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/unusedcode.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(),
                      "target/test/unit/default-configuration/target/export_format_pmd_language_java_name_Sonar_2520way.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if there's a link to the JXR files
        String str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" ) );

        assertTrue( str.contains( "/xref/def/configuration/App.html#L31" ) );

        assertTrue( str.contains( "/xref/def/configuration/AppSample.html#L45" ) );

        mockServer.stop();
    }

    /**
     * With custom rulesets
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/custom-configuration/custom-configuration-plugin-config.xml" );

        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check the generated files
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/pmd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/custom.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if custom ruleset was applied
        String str = readFile( new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" ) );
        assertTrue( lowerCaseContains( str, "Avoid using if statements without curly braces" ) );

        // Must be false as IfElseStmtsMustUseBraces is excluded!
        assertFalse( lowerCaseContains( str, "Avoid using if...else statements without curly braces" ) );

        assertFalse( "unnecessary constructor should not be triggered because of low priority",
                    lowerCaseContains( str, "Avoid unnecessary constructors - the compiler will generate these for you" ) );

        // veryLongVariableNameWithViolation is really too long
        assertTrue( lowerCaseContains( str, "veryLongVariableNameWithViolation" ) );
        // notSoLongVariableName should not be reported
        assertFalse( lowerCaseContains( str, "notSoLongVariableName" ) );
    }

    /**
     * Verify skip parameter
     *
     * @throws Exception
     */
    public void testSkipConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/custom-configuration/skip-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/custom.xml" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testSkipEmptyReportConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/empty-report/skip-empty-report-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/pmd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testEmptyReportConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/empty-report/empty-report-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do exist, even if there are no violations
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/pmd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( new File( getBasedir(), "target/test/unit/empty-report/target/site/pmd.html" ) );
        assertFalse( lowerCaseContains( str, "Hello.java" ) );
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
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new FileReader( file ) );

            final StringBuilder str = new StringBuilder( (int) file.length() );

            for ( String line = reader.readLine(); line != null; line = reader.readLine() )
            {
                str.append( ' ' );
                str.append( line );
            }

            reader.close();
            reader = null;

            return str.toString();
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Verify the correct working of the localtionTemp method
     *
     * @throws Exception
     */
    public void testLocationTemp()
        throws Exception
    {

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        assertEquals( "locationTemp is not correctly encoding filename",
                      "export_format_pmd_language_java_name_some_2520name.xml",
                      mojo.getLocationTemp( "http://nemo.sonarsource.org/sonar/profiles/export?format=pmd&language=java&name=some%2520name" ) );

    }

    /**
     * Verify that suppressMarker works
     *
     * @throws Exception
     */
    public void testSuppressMarkerConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-with-suppressMarker-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" ) );

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse( str.contains( "Avoid unused private fields such as 'unusedVar2'." ) );
    }

    public void testJspConfiguration()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/default-configuration/jsp-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // these are the rulesets, that have been applied...
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/jsp-basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );
        assertTrue(str.contains("JSP file should use UTF-8 encoding"));
        assertTrue(str.contains("Using unsanitized JSP expression can lead to Cross Site Scripting (XSS) attacks"));
        assertTrue(str.contains("Avoid having style information in JSP files."));
    }

    public void testPMDProcessingError()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/processing-error/pmd-processing-error-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        try {
            mojo.execute();
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertTrue( e.getMessage().endsWith( "Found 1 PMD processing errors" ) );
        }
    }

    public void testPMDExcludeRootsShouldExcludeSubdirectories() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/exclude-roots/pmd-exclude-roots-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo ("pmd", testPom);
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/exclude-roots/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );

        assertTrue( "Seems like all directories are excluded now", str.contains("ForLoopShouldBeWhileLoop") );
        assertFalse( "Exclusion of an exact source directory not working", str.contains( "OverrideBothEqualsAndHashcode" ) );
        assertFalse( "Exclusion of basedirectory with subdirectories not working (MPMD-178)", str.contains( "JumbledIncrementer") );
    }
}
