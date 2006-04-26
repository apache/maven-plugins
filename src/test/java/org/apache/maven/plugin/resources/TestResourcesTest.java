package org.apache.maven.plugin.resources;

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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.resources.stub.MavenProjectResourcesStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

public class TestResourcesTest
    extends AbstractMojoTestCase
{
    protected final static String defaultPomFilePath = "/target/test-classes/unit/resources-test/plugin-config.xml";

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    protected void tearDown()
    {

    }

    /**
     * test mojo lookup, test harness should be working fine
     *
     * @throws Exception
     */
    public void testHarnessEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "testResources", testPom );

        assertNotNull( mojo );
    }

    /**
     * @throws Exception
     */
    public void testTestResourceDirectoryCreation()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        TestResourcesMojo mojo = (TestResourcesMojo) lookupMojo( "testResources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "testResourceDirectoryStructure" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file4.txt" );
        project.addFile( "package/file3.nottest" );
        project.addFile( "notpackage/file1.include" );
        project.addFile( "package/test/file1.txt" );
        project.addFile( "notpackage/test/file2.txt" );
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getTestOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resorucesDir = project.getTestOutputDirectory();

        assertTrue( FileUtils.fileExists( resorucesDir + "/file4.txt" ) );
        assertTrue( FileUtils.fileExists( resorucesDir + "/package/file3.nottest" ) );
        assertTrue( FileUtils.fileExists( resorucesDir + "/notpackage/file1.include" ) );
        assertTrue( FileUtils.fileExists( resorucesDir + "/package/test" ) );
        assertTrue( FileUtils.fileExists( resorucesDir + "/notpackage/test" ) );
    }
}
