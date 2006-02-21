package org.apache.maven.plugin.clean;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Test the clean mojo.
 */
public class CleanMojoTest
    extends TestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.copyDirectoryStructure( new File( "src/test/resources/testDirectoryStructure" ),
                                          new File( "target/test/testDirectoryStructure" ) );
    }


    public void testClean()
        throws Exception
    {
        CleanMojo mojo = new CleanMojo();
        mojo.setDirectory( new File( "target/test/testDirectoryStructure/buildDirectory" ) );
        mojo.setOutputDirectory( new File( "target/test/testDirectoryStructure/buildOutputDirectory" ) );
        mojo.setTestOutputDirectory( new File( "target/test/testDirectoryStructure/buildTestDirectory" ) );

        mojo.execute();

        assertFalse( FileUtils.fileExists( "target/test/testDirectoryStructure/buildDirectory" ) );
        assertFalse( FileUtils.fileExists( "target/test/testDirectoryStructure/buildOutputDirectory" ) );
        assertFalse( FileUtils.fileExists( "target/test/testDirectoryStructure/buildTestDirectory" ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( "target/test/testDirectoryStructure" ) );
    }
}
