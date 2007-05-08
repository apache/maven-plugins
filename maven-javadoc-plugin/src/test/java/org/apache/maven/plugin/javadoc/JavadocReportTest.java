package org.apache.maven.plugin.javadoc;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class JavadocReportTest
    extends AbstractMojoTestCase
{
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        createTestRepo();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        // nop
    }

    /**
     * Create test repository in target directory.
     *
     * @throws IOException
     *             if any
     */
    private void createTestRepo()
        throws IOException
    {
        // ----------------------------------------------------------------------
        // UMLGraph
        // ----------------------------------------------------------------------

        File f = new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1" );
        f.mkdirs();

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/maven-metadata-local.xml" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/maven-metadata-local.xml" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/2.1/UMLGraph-2.1.pom" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.pom" ) );

        // ----------------------------------------------------------------------
        // UMLGraph-bis
        // ----------------------------------------------------------------------

        f = new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph-bis/2.1" );
        f.mkdirs();

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-path-test/artifact-doclet/umlgraph/UMLGraph-bis/maven-metadata-local.xml" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph-bis/maven-metadata-local.xml" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-path-test/artifact-doclet/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/doclet-path-test/artifact-doclet/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.pom" ),
                       new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.pom" ) );

        // ----------------------------------------------------------------------
        // commons-attributes-compiler
        // http://www.tullmann.org/pat/taglets/
        // ----------------------------------------------------------------------

        f = new File( getBasedir(), "target/local-repo/org/tullmann/taglets/1.0" );
        f.mkdirs();

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/taglet-test/artifact-taglet/org/tullmann/taglets/maven-metadata-local.xml" ),
                       new File( getBasedir(), "target/local-repo/org/tullmann/taglets/maven-metadata-local.xml" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/taglet-test/artifact-taglet/org/tullmann/taglets/1.0/taglets-1.0.jar" ),
                       new File( getBasedir(), "target/local-repo/org/tullmann/taglets/1.0/taglets-1.0.jar" ) );

        FileUtils
            .copyFile(
                       new File( getBasedir(),
                                 "src/test/resources/unit/taglet-test/artifact-taglet/org/tullmann/taglets/1.0/taglets-1.0.pom" ),
                       new File( getBasedir(), "target/local-repo/org/tullmann/taglets/1.0/taglets-1.0.pom" ) );
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>space</code> as line separator.
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile( File file )
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

    /**
     * Test when default configuration is provided for the plugin
     *
     * @throws Exception
     */
    public void testDefaultConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // package level generated javadoc files
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/default-configuration/target/site/apidocs/def/configuration/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/package-tree.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/package-use.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // class level generated javadoc files
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/class-use/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/class-use/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // project level generated javadoc files
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/allclasses-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/allclasses-noframe.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/constant-values.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/deprecated-list.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/help-doc.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/index-all.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/overview-tree.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/package-list" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * Method for testing the subpackages and excludePackageNames parameter
     *
     * @throws Exception
     */
    public void testSubpackages()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/subpackages-test/subpackages-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // check the excluded packages
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/excluded" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/included/exclude" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if the classes in the specified subpackages were included
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/included/IncludedApp.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/included/IncludedAppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * Test the recursion and exclusion of the doc-files subdirectories.
     *
     * @throws Exception
     */
    public void testDocfiles()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/docfiles-test/docfiles-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // check if the doc-files subdirectories were copied
        File generatedFile = new File( getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/doc-files" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/included-dir1/sample-included1.gif" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/included-dir2/sample-included2.gif" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/excluded-dir1" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/excluded-dir2" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * Test javadoc plugin using custom configuration. noindex, notree and nodeprecated parameters were set to true
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/custom-configuration/custom-configuration-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // check if there is a tree page generated (notree == true)
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/custom-configuration/target/site/apidocs/overview-tree.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/package-tree.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if the main index page was generated (noindex == true)
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/index-all.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if the deprecated list and the deprecated api were generated (nodeprecated == true)
        // @todo Fix: the class-use of the deprecated api is still created eventhough the deprecated api of that class
        // is no longer generated
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/deprecated-list.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/App.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // read the contents of the html files based on some of the parameter values
        // author == false
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/AppSample.html" ) );
        assertTrue( str.toLowerCase().indexOf( "author" ) == -1 );

        // bottom
        assertTrue( str.toUpperCase().indexOf( "SAMPLE BOTTOM CONTENT" ) != -1 );

        // offlineLinks
        assertTrue( str.toLowerCase().indexOf(
                                               "HREF=\"http://java.sun.com/j2se/1.4.2/docs/api/java/lang/String.html"
                                                   .toLowerCase() ) != -1 );

        // header
        assertTrue( str.toUpperCase().indexOf( "MAVEN JAVADOC PLUGIN TEST" ) != -1 );

        // footer
        assertTrue( str.toUpperCase().indexOf( "MAVEN JAVADOC PLUGIN TEST FOOTER" ) != -1 );

        // nohelp == true
        assertTrue( str.toUpperCase().indexOf( "/help-doc.html".toUpperCase() ) == -1 );

        // check the wildcard (*) package exclusions -- excludePackageNames parameter
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/exclude1/Exclude1App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/exclude1/subexclude/SubexcludeApp.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/exclude2/Exclude2App.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * Method to test the doclet artifact configuration
     *
     * @throws Exception
     */
    public void testDoclets()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // doclet-test: check if the file generated by UmlGraph exists and if
        // doclet path contains the UmlGraph artifact
        // ----------------------------------------------------------------------

        File testPom = new File( getBasedir(), "src/test/resources/unit/doclet-test/doclet-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        File optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        String options = readFile( optionsFile );
        assertTrue( options.indexOf( "/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) != -1 );

        // ----------------------------------------------------------------------
        // doclet-path: check if the file generated by UmlGraph exists and if
        // doclet path contains the twice UmlGraph artifacts
        // ----------------------------------------------------------------------

        testPom = new File( getBasedir(), "src/test/resources/unit/doclet-path-test/doclet-path-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        generatedFile = new File( getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        options = readFile( optionsFile );
        assertTrue( options.indexOf( "/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) != -1 );
        assertTrue( options.indexOf( "/target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar" ) != -1 );
    }

    /**
     * Method to test the aggregate parameter
     *
     * @throws Exception
     */
    public void testAggregate()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/aggregate-test/aggregate-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // check if project1 api files exist
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project1/Project1App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project1/Project1AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project1/Project1Sample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project1/Project1Test.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if project2 api files exist
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project2/Project2App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project2/Project2AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project2/Project2Sample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/aggregate-test/target/site/apidocs/aggregate/test/project2/Project2Test.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }

    /**
     * Method to test when the path to the project sources has an apostrophe (')
     *
     * @throws Exception
     */
    public void testQuotedPath()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/quotedpath'test/quotedpath-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // package level generated javadoc files
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/quotedpath'test/target/site/apidocs/quotedpath/test/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/quotedpath'test/target/site/apidocs/quotedpath/test/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // project level generated javadoc files
        generatedFile = new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/index-all.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/quotedpath'test/target/site/apidocs/overview-tree.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/package-list" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    /**
     * @throws Exception if any
     */
    public void testExceptions()
        throws Exception
    {
        try
        {
            File testPom = new File( getBasedir(),
                                     "src/test/resources/unit/default-configuration/exception-test-plugin-config.xml" );
            JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
            mojo.execute();

            fail( "Must throw exception." );
        }
        catch ( Exception e )
        {
            assertTrue( true );

            try
            {
                FileUtils.deleteDirectory( new File( getBasedir(), "exception" ) );
            }
            catch ( IOException ie )
            {
                // nop
            }
        }
    }

    /**
     * Method to test the taglet artifact configuration
     *
     * @throws Exception
     */
    public void testTaglets()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // taglet-test: check if a taglet is used
        // ----------------------------------------------------------------------

        File testPom = new File( getBasedir(), "src/test/resources/unit/taglet-test/taglet-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File index = new File( getBasedir(), "target/test/unit/taglet-test/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( index.getAbsolutePath() ) );

        File appFile = new File( getBasedir(), "target/test/unit/taglet-test/target/site/apidocs/taglet/test/App.html" );
        assertTrue( appFile.exists() );
        String appString = readFile( appFile );
        assertTrue( appString.indexOf( "<b>To Do:</b>" ) != -1 );
    }

    /**
     * Method to test the jdk5 javadoc
     *
     * @throws Exception
     */
    public void testJdk5()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.5f ) )
        {
            getContainer().getLogger().warn(
                                             "JdkDK 5.0 or more is required to run javadoc for "
                                                 + "'org.apache.maven.plugin.javadoc.JavadocReportTest#testJdk5()'." );
            return;
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/jdk5-test/jdk5-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File index = new File( getBasedir(), "target/test/unit/jdk5-test/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( index.getAbsolutePath() ) );

        File overviewSummary = new File( getBasedir(),
                                         "target/test/unit/jdk5-test/target/site/apidocs/overview-summary.html" );
        assertTrue( FileUtils.fileExists( overviewSummary.getAbsolutePath() ) );
        String readed = readFile( overviewSummary );
        assertTrue( readed.indexOf( "<b>Test the package-info</b>" ) != -1 );

        File packageSummary = new File( getBasedir(),
                                        "target/test/unit/jdk5-test/target/site/apidocs/jdk5/test/package-summary.html" );
        assertTrue( FileUtils.fileExists( packageSummary.getAbsolutePath() ) );
        readed = readFile( packageSummary );
        assertTrue( readed.indexOf( "<b>Test the package-info</b>" ) != -1 );
    }

    /**
     * Test to find the javadoc executable when <code>java.home</code> is not in the JDK_HOME. In this case, try to
     * use the <code>JAVA_HOME</code> environment variable.
     *
     * @throws Exception
     */
    public void testToFindJavadoc()
        throws Exception
    {
        String oldJreHome = System.getProperty( "java.home" );
        System.setProperty( "java.home", "foo/bar" );

        File testPom = new File( getBasedir(), "src/test/resources/unit/javaHome-test/javaHome-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        System.setProperty( "java.home", oldJreHome );
    }

    /**
     * Test the javadoc resources.
     *
     * @throws Exception
     */
    public void testJavadocResources()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/resources-test/resources-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File app = new File( getBasedir(),
                             "target/test/unit/resources-test/target/site/apidocs/resources/test/App.html" );
        assertTrue( FileUtils.fileExists( app.getAbsolutePath() ) );
        String readed = readFile( app );
        assertTrue( readed.indexOf( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\"/>" ) != -1 );
        File feather = new File( getBasedir(),
                                 "target/test/unit/resources-test/target/site/apidocs/resources/test/doc-files/maven-feather.png" );
        assertTrue( FileUtils.fileExists( feather.getAbsolutePath() ) );

        File app2 = new File( getBasedir(),
                              "target/test/unit/resources-test/target/site/apidocs/resources/test2/App2.html" );
        assertTrue( FileUtils.fileExists( app2.getAbsolutePath() ) );
        readed = readFile( app2 );
        assertTrue( readed.indexOf( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\"/>" ) != -1 );
        File feather2 = new File( getBasedir(),
                                  "target/test/unit/resources-test/target/site/apidocs/resources/test2/doc-files/maven-feather.png" );
        assertFalse( FileUtils.fileExists( feather2.getAbsolutePath() ) );
    }

    /**
     * Test the javadoc resources in the aggregation case.
     *
     * @throws Exception
     */
    public void testAggregateJavadocResources()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/aggregate-resources-test/aggregate-resources-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        // Test overview
        String lineSeparator = " ";
        File overviewSummary = new File( getBasedir(),
                                         "target/test/unit/aggregate-resources-test/target/site/apidocs/overview-summary.html" );
        assertTrue( FileUtils.fileExists( overviewSummary.getAbsolutePath() ) );
        String readed = readFile( overviewSummary );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + lineSeparator
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources/test/package-summary.html\">resources.test</A></B></TD>"
            + lineSeparator + "<TD>blabla</TD>" + lineSeparator + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + lineSeparator
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources/test2/package-summary.html\">resources.test2</A></B></TD>"
            + lineSeparator + "<TD>&nbsp;</TD>" + lineSeparator + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + lineSeparator
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources2/test/package-summary.html\">resources2.test</A></B></TD>"
            + lineSeparator + "<TD>blabla</TD>" + lineSeparator + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + lineSeparator
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources2/test2/package-summary.html\">resources2.test2</A></B></TD>"
            + lineSeparator + "<TD>&nbsp;</TD>" + lineSeparator + "</TR>" ) != -1 );

        // Test doc-files
        File app = new File( getBasedir(),
                             "target/test/unit/aggregate-resources-test/target/site/apidocs/resources/test/App.html" );
        assertTrue( FileUtils.fileExists( app.getAbsolutePath() ) );
        readed = readFile( app );
        assertTrue( readed.indexOf( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\"/>" ) != -1 );
        File feather = new File( getBasedir(),
                                 "target/test/unit/aggregate-resources-test/target/site/apidocs/resources/test/doc-files/maven-feather.png" );
        assertTrue( FileUtils.fileExists( feather.getAbsolutePath() ) );
    }
}
