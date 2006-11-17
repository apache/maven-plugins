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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.resources.stub.MavenProjectResourcesStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

public class ResourcesMojoTest
    extends AbstractMojoTestCase
{
    protected final static String defaultPomFilePath = "/target/test-classes/unit/resources-test/plugin-config.xml";

    /**
     * test mojo lookup, test harness should be working fine
     *
     * @throws Exception
     */
    public void testHarnessEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );

        assertNotNull( mojo );
    }

    /**
     * @throws Exception
     */
    public void testResourceDirectoryStructure()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceDirectoryStructure" );
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
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/file4.txt" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/file3.nottest" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/notpackage/file1.include" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/test" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/notpackage/test" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceDirectoryStructure_RelativePath()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceDirectoryStructure_RelativePath" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.setOutputDirectory( "../relative_dir" );
        project.addFile( "file4.txt" );
        project.addFile( "package/file3.nottest" );
        project.addFile( "notpackage/file1.include" );
        project.addFile( "package/test/file1.txt" );
        project.addFile( "notpackage/test/file2.txt" );
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/file4.txt" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/file3.nottest" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/notpackage/file1.include" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/test" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/notpackage/test" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceEncoding()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "encoding" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file4.txt" );
        project.setResourceFiltering( 0, true );
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "encoding", "UTF-8" );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/file4.txt" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceInclude()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceInclude" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file1.include" );
        project.addFile( "file2.exclude" );
        project.addFile( "file3.nottest" );
        project.addFile( "file4.txt" );
        project.addFile( "package/file1.include" );
        project.addFile( "package/file2.exclude" );
        project.addFile( "package/file3.nottest" );
        project.addFile( "package/file4.txt" );
        project.addFile( "notpackage/file1.include" );
        project.addFile( "notpackage/file2.exclude" );
        project.addFile( "notpackage/file3.nottest" );
        project.addFile( "notpackage/file4.txt" );
        project.addFile( "package/test/file1.txt" );
        project.addFile( "package/nottest/file2.txt" );
        project.addFile( "notpackage/test/file1.txt" );
        project.addFile( "notpackage/nottest/file.txt" );
        project.setupBuildEnvironment();

        project.addInclude( "*.include" );
        project.addInclude( "**/test" );
        project.addInclude( "**/test/file*" );
        project.addInclude( "**/package/*.include" );

        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/package/test" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/file1.include" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/file1.include" ) );
        assertFalse( FileUtils.fileExists( resourcesDir + "/notpackage/file1.include" ) );
        assertFalse( FileUtils.fileExists( resourcesDir + "/notpackage/nottest/file.txt" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceExclude()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceExclude" );
        List resources = project.getBuild().getResources();
        ;

        assertNotNull( mojo );

        project.addFile( "file1.include" );
        project.addFile( "file2.exclude" );
        project.addFile( "file3.nottest" );
        project.addFile( "file4.txt" );
        project.addFile( "package/file1.include" );
        project.addFile( "package/file2.exclude" );
        project.addFile( "package/file3.nottest" );
        project.addFile( "package/file4.txt" );
        project.addFile( "notpackage/file1.include" );
        project.addFile( "notpackage/file2.exclude" );
        project.addFile( "notpackage/file3.nottest" );
        project.addFile( "notpackage/file4.txt" );
        project.addFile( "package/test/file1.txt" );
        project.addFile( "package/nottest/file2.txt" );
        project.addFile( "notpackage/test/file1.txt" );
        project.addFile( "notpackage/nottest/file.txt" );
        project.setupBuildEnvironment();

        project.addExclude( "**/*.exclude" );
        project.addExclude( "**/nottest*" );
        project.addExclude( "**/notest" );
        project.addExclude( "**/notpackage*" );
        project.addExclude( "**/notpackage*/**" );

        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/package/test" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/file1.include" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/package/file1.include" ) );
        assertFalse( FileUtils.fileExists( resourcesDir + "/notpackage/file1.include" ) );
        assertFalse( FileUtils.fileExists( resourcesDir + "/notpackage/nottest/file.txt" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceTargetPath()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceTargetPath" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.setTargetPath( "org/apache/maven/plugin/test" );

        project.addFile( "file4.txt" );
        project.addFile( "package/file3.nottest" );
        project.addFile( "notpackage/file1.include" );
        project.addFile( "package/test/file1.txt" );
        project.addFile( "notpackage/test/file2.txt" );
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();

        assertTrue( FileUtils.fileExists( resourcesDir + "/org/apache/maven/plugin/test/file4.txt" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/org/apache/maven/plugin/test/package/file3.nottest" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/org/apache/maven/plugin/test/notpackage/file1.include" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/org/apache/maven/plugin/test/package/test" ) );
        assertTrue( FileUtils.fileExists( resourcesDir + "/org/apache/maven/plugin/test/notpackage/test" ) );
    }

    /**
     * @throws Exception
     */
    public void testResourceSystemProperties_Filtering()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceSystemProperties_Filtering" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file4.txt", "current working directory = ${user.dir}" );
        project.setResourceFiltering( 0, true );
        project.setupBuildEnvironment();

        //setVariableValueToObject(mojo,"encoding","UTF-8");
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();
        String checkString = "current working directory = " + (String) System.getProperty( "user.dir" );

        assertContent( resourcesDir + "/file4.txt", checkString );
    }

    /**
     * @throws Exception
     */
    public void testResourceProjectProperties_Filtering()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourceProjectProperties_Filtering" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file4.txt", "current working directory = ${user.dir}" );
        project.setResourceFiltering( 0, true );
        project.addProperty( "user.dir", "FPJ kami!!!" );
        project.setupBuildEnvironment();

        //setVariableValueToObject(mojo,"encoding","UTF-8");
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();
        String checkString = "current working directory = FPJ kami!!!";

        assertContent( resourcesDir + "/file4.txt", checkString );
    }

    /**
     * @throws Exception
     */
    public void testProjectProperty_Filtering_PropertyDestination()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project =
            new MavenProjectResourcesStub( "resourcePojectProperty_Filtering_PropertyDestination" );
        List resources = project.getBuild().getResources();

        assertNotNull( mojo );

        project.addFile( "file4.properties", "current working directory=${description}" );
        project.setResourceFiltering( 0, true );
        project.setupBuildEnvironment();

        // setup dummy property
        project.setDescription( "c:\\\\org\\apache\\test" );

        //setVariableValueToObject(mojo,"encoding","UTF-8");
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", new LinkedList() );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();
        String checkString = "current working directory=c\\:\\\\\\\\org\\\\apache\\\\test";

        assertContent( resourcesDir + "/file4.properties", checkString );
    }

    /**
     * @throws Exception
     */
    public void testPropertyFiles_Filtering()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "resourcePropertyFiles_Filtering" );
        List resources = project.getBuild().getResources();
        LinkedList filterList = new LinkedList();

        assertNotNull( mojo );

        project.addFile( "file4.properties", "current working directory=${dir}" );
        project.addFile( "filter.properties", "dir:testdir" );
        project.setResourceFiltering( 0, true );
        project.setupBuildEnvironment();
        filterList.add( project.getResourcesDirectory() + "filter.properties" );

        //setVariableValueToObject(mojo,"encoding","UTF-8");
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "resources", resources );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "filters", filterList );
        mojo.execute();

        String resourcesDir = project.getOutputDirectory();
        String checkString = "current working directory=testdir";

        assertContent( resourcesDir + "/file4.properties", checkString );
    }

    /**
     * Ensures the file exists and its first line equals the given data.
     */
    private void assertContent( String fileName, String data )
        throws IOException
    {
        assertTrue( FileUtils.fileExists( fileName ) );

        assertEquals( data, new BufferedReader( new FileReader( fileName ) ).readLine() );
    }
}
