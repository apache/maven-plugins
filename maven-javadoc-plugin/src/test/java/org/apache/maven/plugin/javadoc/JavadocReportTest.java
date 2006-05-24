package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocReportTest
    extends AbstractMojoTestCase
{
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        createTestRepo();
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

        //package level generated javadoc files
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

        //class level generated javadoc files
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/class-use/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/def/configuration/class-use/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //project level generated javadoc files
        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/allclasses-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/default-configuration/target/site/apidocs/allclasses-noframe.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/constant-values.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/deprecated-list.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/help-doc.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/index-all.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/overview-tree.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/package-list" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs/stylesheet.css" );
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
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/subpackages-test/subpackages-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        //check the excluded packages
        File generatedFile =
            new File( getBasedir(), "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/excluded" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/included/exclude" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if the classes in the specified subpackages were included
        generatedFile =
            new File( getBasedir(), "target/test/unit/subpackages-test/target/site/apidocs/subpackages/test/App.html" );
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
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/docfiles-test/docfiles-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        //check if the doc-files subdirectories were copied
        File generatedFile = new File( getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/doc-files" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/included-dir1/sample-included1.gif" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/docfiles-test/target/site/apidocs/doc-files/included-dir2/sample-included2.gif" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/doc-files/excluded-dir1" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/doc-files/excluded-dir2" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }


    /**
     * Test javadoc plugin using custom configuration.
     * noindex, notree and nodeprecated parameters were set to true
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

        //check if there is a tree page generated (notree == true)
        File generatedFile =
            new File( getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs/overview-tree.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/package-tree.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if the main index page was generated (noindex == true)
        generatedFile =
            new File( getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs/index-all.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if the deprecated list and the deprecated api were generated (nodeprecated == true)
        //@todo Fix: the class-use of the deprecated api is still created eventhough the deprecated api of that class is no longer generated
        generatedFile =
            new File( getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs/deprecated-list.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/App.html" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //read the contents of the html files based on some of the parameter values
        //author == false
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/custom-configuration/target/site/apidocs/custom/configuration/AppSample.html" ) );
        assertTrue( str.toLowerCase().indexOf( "author" ) == -1 );

        //bottom
        assertTrue( str.toUpperCase().indexOf( "SAMPLE BOTTOM CONTENT" ) != -1 );

        //offlineLinks
        assertTrue( str.toLowerCase().indexOf(
            "HREF=\"http://java.sun.com/j2se/1.4.2/docs/api/java/lang/String.html\"".toLowerCase() ) != -1 );

        //header
        assertTrue( str.toUpperCase().indexOf( "MAVEN JAVADOC PLUGIN TEST" ) != -1 );

        //footer
        assertTrue( str.toUpperCase().indexOf( "MAVEN JAVADOC PLUGIN TEST FOOTER" ) != -1 );

        //nohelp == true
        assertTrue( str.toUpperCase().indexOf( "/help-doc.html".toUpperCase() ) == -1 );

        //check the wildcard (*) package exclusions -- excludePackageNames parameter
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

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/doclet-test/doclet-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        //check if the generated file exists
        File generatedFile = new File( getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot" );
        FileUtils.fileExists( generatedFile.getAbsolutePath() );

    }

    /**
     * Method to test the aggregate parameter
     *
     * @throws Exception
     */
    public void testAggregate()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/aggregate-test/aggregate-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        //check if project1 api files exist
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

        //check if project2 api files exist
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
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/quotedpath'test/quotedpath-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
        mojo.execute();

        //package level generated javadoc files
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/quotedpath'test/target/site/apidocs/quotedpath/test/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/quotedpath'test/target/site/apidocs/quotedpath/test/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //project level generated javadoc files
        generatedFile =
            new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/index-all.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/overview-tree.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/package-list" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }

    public void testExceptions()
        throws Exception
    {

        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/default-configuration/exception-test-plugin-config.xml" );
            JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );
            mojo.execute();

            fail( "Must throw exception." );

        }
        catch ( Exception e )
        {
            assertTrue( true );

            try
            {
                FileUtils.deleteDirectory( new File( getBasedir(), "exception") );
            }
            catch( IOException ie )
            {
                
            }
        }

    }


    protected void tearDown()
        throws Exception
    {

    }

    /**
     * Create test repository in target directory.
     */
    private void createTestRepo()
        throws IOException
    {
        File f = new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1" );
        f.mkdirs();

        FileUtils.copyFile( new File( getBasedir(),
                                      "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/maven-metadata-local.xml" ),
                            new File( getBasedir(),
                                      "target/local-repo/umlgraph/UMLGraph/maven-metadata-local.xml" ) );

        FileUtils.copyFile( new File( getBasedir(),
                                      "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ),
                            new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) );

        FileUtils.copyFile( new File( getBasedir(),
                                      "src/test/resources/unit/doclet-test/artifact-doclet/umlgraph/UMLGraph/2.1/UMLGraph-2.1.pom" ),
                            new File( getBasedir(), "target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.pom" ) );

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
