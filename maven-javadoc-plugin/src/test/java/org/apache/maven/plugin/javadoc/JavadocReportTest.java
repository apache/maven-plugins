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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.javadoc.ProxyServer.AuthAsyncProxyServlet;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Test {@link org.apache.maven.plugin.javadoc.JavadocReport} class.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class JavadocReportTest
    extends AbstractMojoTestCase
{
    private static final char LINE_SEPARATOR = ' ';

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        createTestRepo();
    }

    /**
     * Create test repository in target directory.
     *
     * @throws IOException if any
     */
    private void createTestRepo()
        throws IOException
    {
        if ( TEST_REPO_CREATED )
        {
            return;
        }

        File localRepo = new File( getBasedir(), "target/local-repo/" );
        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // UMLGraph
        // ----------------------------------------------------------------------

        File sourceDir = new File( getBasedir(), "src/test/resources/unit/doclet-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // UMLGraph-bis
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/doclet-path-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // commons-attributes-compiler
        // http://www.tullmann.org/pat/taglets/
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/taglet-test/artifact-taglet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // stylesheetfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/stylesheetfile-test/artifact-stylesheetfile" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // helpfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/helpfile-test/artifact-helpfile" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // Remove SCM files
        List<String> files =
            FileUtils.getFileAndDirectoryNames( localRepo, FileUtils.getDefaultExcludesAsString(), null, true,
                                                true, true, true );
        for ( String filename : files )
        {
            File file = new File( filename );

            if ( file.isDirectory() )
            {
                FileUtils.deleteDirectory( file );
            }
            else
            {
                file.delete();
            }
        }

        TEST_REPO_CREATED = true;
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>space</code> as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile( File file )
        throws IOException
    {
        String strTmp;
        StringBuffer str = new StringBuffer( (int) file.length() );
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        try
        {
            while ( ( strTmp = in.readLine() ) != null )
            {
                str.append( LINE_SEPARATOR );
                str.append( strTmp );
            }
        }
        finally
        {
            in.close();
        }

        return str.toString();
    }

    /**
     * Test when default configuration is provided for the plugin
     *
     * @throws Exception if any
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
     * @throws Exception if any
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
     * @throws Exception if any
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

        testPom = new File( getBasedir(), "src/test/resources/unit/docfiles-with-java-test/docfiles-with-java-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();
    }

    /**
     * Test javadoc plugin using custom configuration. noindex, notree and nodeprecated parameters
     * were set to true.
     *
     * @throws Exception if any
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

        File options =
            new File( getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs/options" );
        assertTrue( options.isFile() );
        String contentOptions = null;
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newPlatformReader( options );
            contentOptions = IOUtil.toString( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTrue( contentOptions != null );
        assertTrue( contentOptions.indexOf( "-link" ) != -1 );
        assertTrue( contentOptions.indexOf( "http://java.sun.com/j2se/" ) != -1 );
    }

    /**
     * Method to test the doclet artifact configuration
     *
     * @throws Exception if any
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
     * @throws Exception if any
     */
    public void testAggregate()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/aggregate-test/aggregate-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "aggregate", testPom );
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
     * @throws Exception if any
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
     * @throws Exception if any
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
     * @throws Exception if any
     */
    public void testJdk5()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.5f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 5.0 or more is required to run javadoc for '"
                                                 + getClass().getName() + "#" + getName() + "()'." );
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
     * @throws Exception if any
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
     * @throws Exception if any
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

        // with excludes
        testPom = new File( getBasedir(),
                                 "src/test/resources/unit/resources-with-excludes-test/resources-with-excludes-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        app = new File( getBasedir(),
                             "target/test/unit/resources-with-excludes-test/target/site/apidocs/resources/test/App.html" );
        assertTrue( FileUtils.fileExists( app.getAbsolutePath() ) );
        readed = readFile( app );
        assertTrue( readed.indexOf( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\"/>" ) != -1 );
        feather = new File( getBasedir(),
                                 "target/test/unit/resources-with-excludes-test/target/site/apidocs/resources/test/doc-files/maven-feather.png" );
        assertFalse( FileUtils.fileExists( feather.getAbsolutePath() ) );

        app2 = new File( getBasedir(),
                              "target/test/unit/resources-with-excludes-test/target/site/apidocs/resources/test2/App2.html" );
        assertTrue( FileUtils.fileExists( app2.getAbsolutePath() ) );
        readed = readFile( app2 );
        assertTrue( readed.indexOf( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\"/>" ) != -1 );
        feather2 = new File( getBasedir(),
                                  "target/test/unit/resources-with-excludes-test/target/site/apidocs/resources/test2/doc-files/maven-feather.png" );
        assertTrue( FileUtils.fileExists( feather2.getAbsolutePath() ) );
    }

    /**
     * Test the javadoc resources in the aggregation case.
     *
     * @throws Exception if any
     */
    public void testAggregateJavadocResources()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/aggregate-resources-test/aggregate-resources-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "aggregate", testPom );
        mojo.execute();

        // Test overview
        File overviewSummary = new File( getBasedir(),
                                         "target/test/unit/aggregate-resources-test/target/site/apidocs/overview-summary.html" );
        assertTrue( FileUtils.fileExists( overviewSummary.getAbsolutePath() ) );
        String readed = readFile( overviewSummary );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + LINE_SEPARATOR
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources/test/package-summary.html\">resources.test</A></B></TD>"
            + LINE_SEPARATOR + "<TD>blabla</TD>" + LINE_SEPARATOR + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + LINE_SEPARATOR
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources/test2/package-summary.html\">resources.test2</A></B></TD>"
            + LINE_SEPARATOR + "<TD>&nbsp;</TD>" + LINE_SEPARATOR + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + LINE_SEPARATOR
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources2/test/package-summary.html\">resources2.test</A></B></TD>"
            + LINE_SEPARATOR + "<TD>blabla</TD>" + LINE_SEPARATOR + "</TR>" ) != -1 );
        assertTrue( readed.indexOf( "<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">" + LINE_SEPARATOR
            + "<TD WIDTH=\"20%\"><B><A HREF=\"resources2/test2/package-summary.html\">resources2.test2</A></B></TD>"
            + LINE_SEPARATOR + "<TD>&nbsp;</TD>" + LINE_SEPARATOR + "</TR>" ) != -1 );

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

    /**
     * Test the javadoc for a POM project.
     *
     * @throws Exception if any
     */
    public void testPom()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/pom-test/pom-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        assertFalse( new File( getBasedir(), "target/test/unit/pom-test" ).exists() );
    }

    /**
     * Test the javadoc with tag.
     *
     * @throws Exception if any
     */
    public void testTag()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/tag-test/tag-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File app = new File( getBasedir(), "target/test/unit/tag-test/target/site/apidocs/tag/test/App.html" );
        assertTrue( FileUtils.fileExists( app.getAbsolutePath() ) );
        String readed = readFile( app );
        assertTrue( readed.indexOf( "<B>To do something:</B>" ) != -1 );
        assertTrue( readed.indexOf( "<B>Generator Class:</B>" ) != -1 );
        assertTrue( readed.indexOf( "<B>Version:</B>" ) != -1 );
        assertTrue( readed.indexOf( "<DT><B>Version:</B></DT>" + LINE_SEPARATOR + "  <DD>1.0</DD>" + LINE_SEPARATOR
            + "</DL>" ) != -1 );
    }

    /**
     * Test newline in the header/footer parameter
     *
     * @throws Exception if any
     */
    public void testHeaderFooter()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/header-footer-test/header-footer-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Doesnt handle correctly newline for header or footer parameter", false );
        }

        assertTrue( true );
    }

    /**
     * Test newline in various string parameters
     *
     * @throws Exception if any
     */
    public void testNewline()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/newline-test/newline-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            fail( "Doesn't handle correctly newline for string parameters. See options and packages files." );
        }

        assertTrue( true );
    }

    /**
     * Method to test the jdk6 javadoc
     *
     * @throws Exception if any
     */
    public void testJdk6()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.6f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 6.0 or more is required to run javadoc for '"
                                                 + getClass().getName() + "#" + getName() + "()'." );
            return;
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/jdk6-test/jdk6-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        File index = new File( getBasedir(), "target/test/unit/jdk6-test/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( index.getAbsolutePath() ) );

        File overviewSummary = new File( getBasedir(),
                                         "target/test/unit/jdk6-test/target/site/apidocs/overview-summary.html" );
        assertTrue( FileUtils.fileExists( overviewSummary.getAbsolutePath() ) );
        String readed = readFile( overviewSummary );
        assertTrue( readed.indexOf( "Top - Copyright &#169; All rights reserved." ) != -1 );
        assertTrue( readed.indexOf( "Header - Copyright &#169; All rights reserved." ) != -1 );
        assertTrue( readed.indexOf( "Footer - Copyright &#169; All rights reserved." ) != -1 );

        File packageSummary = new File( getBasedir(),
                                        "target/test/unit/jdk6-test/target/site/apidocs/jdk6/test/package-summary.html" );
        assertTrue( FileUtils.fileExists( packageSummary.getAbsolutePath() ) );
        readed = readFile( packageSummary );
        assertTrue( readed.indexOf( "Top - Copyright &#169; All rights reserved." ) != -1 );
        assertTrue( readed.indexOf( "Header - Copyright &#169; All rights reserved." ) != -1 );
        assertTrue( readed.indexOf( "Footer - Copyright &#169; All rights reserved." ) != -1 );
    }

    /**
     * Method to test proxy support in the javadoc
     *
     * @throws Exception if any
     */
    public void testProxy()
        throws Exception
    {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();

        //dummy proxy
        proxy.setActive( true );
        proxy.setHost( "127.0.0.1" );
        proxy.setPort( 80 );
        proxy.setProtocol( "http" );
        proxy.setUsername( "toto" );
        proxy.setPassword( "toto" );
        proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
        settings.addProxy( proxy );

        File testPom = new File( getBasedir(), "src/test/resources/unit/proxy-test/proxy-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        assertNotNull( mojo );
        setVariableValueToObject( mojo, "settings", settings );
        mojo.execute();

        File commandLine = new File( getBasedir(), "target/test/unit/proxy-test/target/site/apidocs/javadoc." + ( SystemUtils.IS_OS_WINDOWS ? "bat" : "sh" ) );
        assertTrue( FileUtils.fileExists( commandLine.getAbsolutePath() ) );
        String readed = readFile( commandLine );
        assertTrue( readed.indexOf( "-J-Dhttp.proxySet=true" ) != -1 );
        assertTrue( readed.indexOf( "-J-Dhttp.proxyHost=127.0.0.1" ) != -1 );
        assertTrue( readed.indexOf( "-J-Dhttp.proxyPort=80" ) != -1 );
        assertTrue( readed.indexOf( "-J-Dhttp.proxyUser=\\\"toto\\\"" ) != -1 );
        assertTrue( readed.indexOf( "-J-Dhttp.proxyPassword=\\\"toto\\\"" ) != -1 );
        assertTrue( readed.indexOf( "-J-Dhttp.nonProxyHosts=\\\"www.google.com|*.somewhere.com\\\"" ) != -1 );

        File options = new File( getBasedir(), "target/test/unit/proxy-test/target/site/apidocs/options" );
        assertTrue( FileUtils.fileExists( options.getAbsolutePath() ) );
        String optionsContent = readFile( options );
        // NO -link http://download.oracle.com/javase/1.5.0/docs/api/docs/api/package-list
        if ( JavadocUtilTest.isWebSiteOnline( null, getContainer().getLogger(), "http://download.oracle.com/" ) )
        {
            assertTrue( optionsContent.indexOf( "-link" ) == -1 );
        }

        // real proxy
        ProxyServer proxyServer = null;
        AuthAsyncProxyServlet proxyServlet = null;
        try
        {
            proxyServlet = new AuthAsyncProxyServlet();
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            settings.addProxy( proxy );

            mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
            setVariableValueToObject( mojo, "settings", settings );
            mojo.execute();
            readed = readFile( commandLine );
            assertTrue( readed.indexOf( "-J-Dhttp.proxySet=true" ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyHost=" + proxyServer.getHostName() ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyPort=" + proxyServer.getPort() ) != -1 );

            optionsContent = readFile( options );
            // -link http://download.oracle.com/javase/1.5.0/docs/api/package-list
            if ( JavadocUtilTest.isWebSiteOnline( null, getContainer().getLogger(), "http://download.oracle.com/" ) )
            {
                assertTrue( optionsContent.indexOf( "-link" ) != -1 );
            }
            assertTrue( true );
        }
        catch ( Exception e )
        {
            assertTrue( false );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        // auth proxy
        Map<String, String> authentications = new HashMap<String, String>();
        authentications.put( "foo", "bar" );
        try
        {
            proxyServlet = new AuthAsyncProxyServlet();
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "bar" );
            settings.addProxy( proxy );

            mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
            setVariableValueToObject( mojo, "settings", settings );
            mojo.execute();
            readed = readFile( commandLine );
            assertTrue( readed.indexOf( "-J-Dhttp.proxySet=true" ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyHost=" + proxyServer.getHostName() ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyPort=" + proxyServer.getPort() ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyUser=\\\"foo\\\"" ) != -1 );
            assertTrue( readed.indexOf( "-J-Dhttp.proxyPassword=\\\"bar\\\"" ) != -1 );

            optionsContent = readFile( options );
            // -link http://download.oracle.com/javase/1.5.0/docs/api/docs/api/package-list
            if ( JavadocUtilTest.isWebSiteOnline( null, getContainer().getLogger(), "http://download.oracle.com" ) )
            {
                assertTrue( optionsContent.indexOf( "-link" ) != -1 );
            }
            assertTrue( true );
        }
        catch ( Exception e )
        {
            assertTrue( false );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    public void testValidateOptions()
        throws Exception
    {
        // encoding
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/wrong-encoding-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
            assertTrue( "Not wrong encoding catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Not wrong encoding catch",
                        e.getMessage().indexOf( "Unsupported option <encoding/>" ) != -1 );
        }
        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/wrong-docencoding-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
            assertTrue( "Not wrong docencoding catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Not wrong docencoding catch", e.getMessage()
                                                        .indexOf( "Unsupported option <docencoding/>" ) != -1 );
        }
        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/wrong-charset-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
            assertTrue( "Not wrong charset catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Not wrong charset catch", e.getMessage().indexOf( "Unsupported option <charset/>" ) != -1 );
        }

        // locale
        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/wrong-locale-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
            assertTrue( "Not wrong locale catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Not wrong locale catch", e.getMessage().indexOf( "Unsupported option <locale/>" ) != -1 );
        }
        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/wrong-locale-with-variant-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();
        assertTrue( "Not wrong locale catch", true );

        // conflict options
        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/validate-options-test/conflict-options-test-plugin-config.xml" );
        mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        try
        {
            mojo.execute();
            assertTrue( "Not conflict catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Not conflict catch", e.getMessage()
                                               .indexOf( "Option <nohelp/> conflicts with <helpfile/>" ) != -1 );
        }
    }

    /**
     * Method to test the <code>&lt;tagletArtifacts/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testTagletArtifacts()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/tagletArtifacts-test/tagletArtifacts-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );

        setVariableValueToObject( mojo, "remoteRepositories", mojo.project.getRemoteArtifactRepositories() );

        mojo.execute();

        File optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        String options = readFile( optionsFile );
        // count -taglet
        assertEquals( 20, StringUtils.countMatches( options, LINE_SEPARATOR + "-taglet" + LINE_SEPARATOR ) );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoAggregatorTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoComponentFieldTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoConfiguratorTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoExecuteTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoExecutionStrategyTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoGoalTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoInheritByDefaultTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoInstantiationStrategyTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoParameterFieldTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoPhaseTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoReadOnlyFieldTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiredFieldTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiresDependencyResolutionTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiresDirectInvocationTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiresOnLineTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiresProjectTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.apache.maven.tools.plugin.javadoc.MojoRequiresReportsTypeTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.codehaus.plexus.javadoc.PlexusConfigurationTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.codehaus.plexus.javadoc.PlexusRequirementTaglet" ) != -1 );
        assertTrue( options.indexOf( "org.codehaus.plexus.javadoc.PlexusComponentTaglet" ) != -1 );
    }

    /**
     * Method to test the <code>&lt;stylesheetfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testStylesheetfile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/stylesheetfile-test/pom.xml" );

        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        assertNotNull( mojo );

        setVariableValueToObject( mojo, "remoteRepositories", mojo.project.getRemoteArtifactRepositories() );

        File stylesheetfile =
            new File( getBasedir(), "target/test/unit/stylesheetfile-test/target/site/apidocs/stylesheet.css" );

        File options = new File( getBasedir(), "target/test/unit/stylesheetfile-test/target/site/apidocs/options" );

        // stylesheet == maven OR java
        setVariableValueToObject( mojo, "stylesheet", "javamaven" );

        try
        {
            mojo.execute();
            assertTrue( false );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

        // stylesheet == java
        setVariableValueToObject( mojo, "stylesheet", "java" );
        mojo.execute();

        String content = readFile( stylesheetfile );
        assertTrue( content.indexOf( "/* Javadoc style sheet */" ) != -1 );

        String optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-stylesheetfile" ) == -1 );

        // stylesheet == maven
        setVariableValueToObject( mojo, "stylesheet", "maven" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.indexOf( "/* Javadoc style sheet */" ) != -1
            && content.indexOf( "Licensed to the Apache Software Foundation (ASF) under one" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-stylesheetfile" ) != -1 );
        assertTrue( optionsContent
                                  .indexOf( "'" + stylesheetfile.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) != -1 );

        // stylesheetfile defined as a project resource
        setVariableValueToObject( mojo, "stylesheet", null );
        setVariableValueToObject( mojo, "stylesheetfile", "com/mycompany/app/javadoc/css/stylesheet.css" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.indexOf( "/* Custom Javadoc style sheet in project */" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-stylesheetfile" ) != -1 );
        File stylesheetResource =
            new File( getBasedir(),
                      "src/test/resources/unit/stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css/stylesheet.css" );
        assertTrue( optionsContent.indexOf( "'" + stylesheetResource.getAbsolutePath().replaceAll( "\\\\", "/" )
            + "'" ) != -1 );

        // stylesheetfile defined in a javadoc plugin dependency
        setVariableValueToObject( mojo, "stylesheetfile", "com/mycompany/app/javadoc/css2/stylesheet.css" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.indexOf( "/* Custom Javadoc style sheet in artefact */" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-stylesheetfile" ) != -1 );
        assertTrue( optionsContent
                                  .indexOf( "'" + stylesheetfile.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) != -1 );

        // stylesheetfile defined as file
        File css =
            new File( getBasedir(),
                      "src/test/resources/unit/stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css" );
        setVariableValueToObject( mojo, "stylesheetfile", css.getAbsolutePath() );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.indexOf( "/* Custom Javadoc style sheet as file */" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-stylesheetfile" ) != -1 );
        stylesheetResource =
            new File( getBasedir(),
                      "src/test/resources/unit/stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css" );
        assertTrue( optionsContent.indexOf( "'" + stylesheetResource.getAbsolutePath().replaceAll( "\\\\", "/" )
            + "'" ) != -1 );
    }

    /**
     * Method to test the <code>&lt;helpfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testHelpfile()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/helpfile-test/pom.xml" );

        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        assertNotNull( mojo );

        setVariableValueToObject( mojo, "remoteRepositories", mojo.project.getRemoteArtifactRepositories() );

        File helpfile =
            new File( getBasedir(), "target/test/unit/helpfile-test/target/site/apidocs/help-doc.html" );

        File options = new File( getBasedir(), "target/test/unit/helpfile-test/target/site/apidocs/options" );

        // helpfile by default
        mojo.execute();

        String content = readFile( helpfile );
        assertTrue( content.indexOf( "<!-- Generated by javadoc" ) != -1 );

        String optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-helpfile" ) == -1 );

        // helpfile defined in a javadoc plugin dependency
        setVariableValueToObject( mojo, "helpfile", "com/mycompany/app/javadoc/helpfile/help-doc.html" );
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.indexOf( "<!--  Help file from artefact -->" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-helpfile" ) != -1 );
        File help = new File( getBasedir(), "target/test/unit/helpfile-test/target/site/apidocs/help-doc.html" );
        assertTrue( optionsContent.indexOf( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) != -1 );

        // helpfile defined as a project resource
        setVariableValueToObject( mojo, "helpfile", "com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.indexOf( "<!--  Help file from file -->" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-helpfile" ) != -1 );
        help =
            new File( getBasedir(),
                      "src/test/resources/unit/helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        assertTrue( optionsContent.indexOf( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) != -1 );

        // helpfile defined as file
        help =
            new File( getBasedir(),
                      "src/test/resources/unit/helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        setVariableValueToObject( mojo, "helpfile", help.getAbsolutePath() );
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.indexOf( "<!--  Help file from file -->" ) != -1 );

        optionsContent = readFile( options );
        assertTrue( optionsContent.indexOf( "-helpfile" ) != -1 );
        assertTrue( optionsContent.indexOf( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) != -1 );
    }
}
