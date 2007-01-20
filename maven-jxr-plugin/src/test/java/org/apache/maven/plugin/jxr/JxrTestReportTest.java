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
 */
public class JxrTestReportTest
    extends AbstractMojoTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Method to test when the source dir is the test source dir
     *
     * @throws Exception
     */
    public void testSourceDir()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/testsourcedir-test/testsourcedir-test-plugin-config.xml" );
        JxrTestReport mojo = (JxrTestReport) lookupMojo( "test-jxr", testPom );
        mojo.execute();

        //check if the jxr docs were generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/AppSampleTest.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/AppTest.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/package-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/package-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/testsourcedir-test/target/site/xref-test/allclasses-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/testsourcedir-test/target/site/xref-test/index.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/testsourcedir-test/target/site/xref-test/overview-frame.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/testsourcedir-test/target/site/xref-test/overview-summary.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(), "target/test/unit/testsourcedir-test/target/site/xref-test/stylesheet.css" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //check if there's a link to the javadoc files
        String str = readFile( new File( getBasedir(),
                                         "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/AppSampleTest.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/testsourcedir/test/AppSample.html\"".toLowerCase() ) == -1 );

        str = readFile( new File( getBasedir(),
                                  "target/test/unit/testsourcedir-test/target/site/xref-test/testsourcedir/test/AppTest.html" ) );
        assertTrue( str.toLowerCase().indexOf( "/apidocs/testsourcedir/test/App.html\"".toLowerCase() ) == -1 );

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
