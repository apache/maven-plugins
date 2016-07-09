package org.apache.maven.plugin.javadoc;

import java.io.BufferedReader;

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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

public class AggregatorJavadocReportTest
    extends AbstractMojoTestCase
{
    private static final char LINE_SEPARATOR = ' ';

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    private File unit;

    private File localRepo;

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        unit = new File( getBasedir(), "src/test/resources/unit" );

        localRepo = new File( getBasedir(), "target/local-repo/" );

        createTestRepo();
    }

    private JavadocReport lookupMojo( File testPom )
        throws Exception
    {
        JavadocReport mojo = (JavadocReport) lookupMojo( "aggregate", testPom );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setPlugin( new Plugin() );

        setVariableValueToObject( mojo, "plugin", pluginDescriptor );
        return mojo;
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

        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // UMLGraph
        // ----------------------------------------------------------------------

        File sourceDir = new File( unit, "doclet-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // UMLGraph-bis
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "doclet-path-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // commons-attributes-compiler
        // http://www.tullmann.org/pat/taglets/
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "taglet-test/artifact-taglet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // stylesheetfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "stylesheetfile-test/artifact-stylesheetfile" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // helpfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "helpfile-test/artifact-helpfile" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // Remove SCM files
        List<String> files = FileUtils.getFileAndDirectoryNames( localRepo, FileUtils.getDefaultExcludesAsString(),
                                                                 null, true, true, true, true );
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
     * Convenience method that reads the contents of the specified file object into a string with a <code>space</code>
     * as line separator.
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
        StringBuilder str = new StringBuilder( (int) file.length() );
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
     * Method to test the aggregate parameter
     *
     * @throws Exception if any
     */
    public void testAggregate()
        throws Exception
    {
        File testPom = new File( unit, "aggregate-test/aggregate-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/aggregate-test/target/site/apidocs/" );

        // check if project1 api files exist
        assertTrue( new File( apidocs, "aggregate/test/project1/Project1App.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project1/Project1AppSample.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project1/Project1Sample.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project1/Project1Test.html" ).exists() );

        // check if project2 api files exist
        assertTrue( new File( apidocs, "aggregate/test/project2/Project2App.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project2/Project2AppSample.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project2/Project2Sample.html" ).exists() );
        assertTrue( new File( apidocs, "aggregate/test/project2/Project2Test.html" ).exists() );
    }

    /**
     * Test the javadoc resources in the aggregation case.
     *
     * @throws Exception if any
     */
    public void testAggregateJavadocResources()
        throws Exception
    {
        File testPom = new File( unit, "aggregate-resources-test/aggregate-resources-test-plugin-config.xml" );
        JavadocReport mojo = (JavadocReport) lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/aggregate-resources-test/target/site/apidocs" );

        // Test overview
        File overviewSummary = new File( apidocs, "overview-summary.html" );
        assertTrue( overviewSummary.exists() );
        String overview = readFile( overviewSummary ).toLowerCase();
        assertTrue( overview.contains( "<a href=\"resources/test/package-summary.html\">resources.test</a>" ) );
        assertTrue( overview.contains( ">blabla</" ) );
        assertTrue( overview.contains( "<a href=\"resources/test2/package-summary.html\">resources.test2</a>" ) );
        assertTrue( overview.contains( "<a href=\"resources2/test/package-summary.html\">resources2.test</a>" ) );
        assertTrue( overview.contains( "<a href=\"resources2/test2/package-summary.html\">resources2.test2</a>" ) );

        // Test doc-files
        File app = new File( apidocs, "resources/test/App.html" );
        assertTrue( app.exists() );
        overview = readFile( app );
        assertTrue( overview.contains( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">" ) );
        assertTrue( new File( apidocs, "resources/test/doc-files/maven-feather.png" ).exists() );
    }
}
