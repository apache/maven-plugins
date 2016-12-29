package org.apache.maven.plugins.war;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.stub.AarArtifactStub;
import org.apache.maven.plugins.war.stub.EJBArtifactStub;
import org.apache.maven.plugins.war.stub.EJBArtifactStubWithClassifier;
import org.apache.maven.plugins.war.stub.EJBClientArtifactStub;
import org.apache.maven.plugins.war.stub.IncludeExcludeWarArtifactStub;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.PARArtifactStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.apache.maven.plugins.war.stub.TLDArtifactStub;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.apache.maven.plugins.war.stub.XarArtifactStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;

public class WarExplodedMojoTest
    extends AbstractWarExplodedMojoTest
{

    protected File getPomFile()
    {
        return new File( getBasedir(), "/target/test-classes/unit/warexplodedmojo/plugin-config.xml" );
    }

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir" );
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testSimpleExplodedWar()
        throws Exception
    {
        // setup test data
        String testId = "SimpleExplodedWar";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File webAppResource = new File( getTestDirectory(), testId + "-resources" );
        File webAppDirectory = new File( getTestDirectory(), testId );
        File sampleResource = new File( webAppResource, "pix/panis_na.jpg" );
        ResourceStub[] resources = new ResourceStub[] { new ResourceStub() };

        createFile( sampleResource );

        assertTrue( "sampeResource not found", sampleResource.exists() );

        // configure mojo
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "webResources", resources );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedWebResourceFile = new File( webAppDirectory, "pix/panis_na.jpg" );
        File expectedWEBINFDir = new File( webAppDirectory, "WEB-INF" );
        File expectedMETAINFDir = new File( webAppDirectory, "META-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "resources doesn't exist: " + expectedWebResourceFile, expectedWebResourceFile.exists() );
        assertTrue( "WEB-INF not found", expectedWEBINFDir.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testSimpleExplodedWarWTargetPath()
        throws Exception
    {
        // setup test data
        String testId = "SimpleExplodedWar";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File webAppResource = new File( getTestDirectory(), "resources" );
        File webAppDirectory = new File( getTestDirectory(), testId );
        File sampleResource = new File( webAppResource, "pix/panis_na.jpg" );
        ResourceStub[] resources = new ResourceStub[] { new ResourceStub() };

        createFile( sampleResource );

        // configure mojo
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        resources[0].setTargetPath( "targetPath" );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "webResources", resources );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedWebResourceFile = new File( webAppDirectory, "targetPath/pix/panis_na.jpg" );
        File expectedWEBINFDir = new File( webAppDirectory, "WEB-INF" );
        File expectedMETAINFDir = new File( webAppDirectory, "META-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "resources doesn't exist: " + expectedWebResourceFile, expectedWebResourceFile.exists() );
        assertTrue( "WEB-INF not found", expectedWEBINFDir.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithCustomWebXML()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithCustomWebXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedWEBXMLFile = new File( webAppDirectory, "WEB-INF/web.xml" );
        File expectedMETAINFDir = new File( webAppDirectory, "META-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "WEB XML not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );
        assertEquals( "WEB XML not correct", mojo.getWebXml().toString(), FileUtils.fileRead( expectedWEBXMLFile ) );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithContainerConfigXML()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithContainerConfigXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File classesDir = createClassesDir( testId, true );
        File webAppSource = createWebAppSource( testId );
        File xmlSource = createXMLConfigDir( testId, new String[] { "config.xml" } );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.setContainerConfigXML( new File( xmlSource, "config.xml" ) );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedContainerConfigXMLFile = new File( webAppDirectory, "META-INF/config.xml" );
        File expectedWEBINFDir = new File( webAppDirectory, "WEB-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "WEB-INF not found", expectedWEBINFDir.exists() );
        assertTrue( "Container Config XML not found:" + expectedContainerConfigXMLFile.toString(),
                    expectedContainerConfigXMLFile.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedContainerConfigXMLFile.delete();
        expectedWEBINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithSimpleExternalWARFile()
        throws Exception
    {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        WarArtifactStub warArtifact = new WarArtifactStub( getBasedir() );

        String testId = "ExplodedWar_WithSimpleExternalWARFile";
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
        File simpleWarFile = warArtifact.getFile();

        assertTrue( "simple war not found: " + simpleWarFile.toString(), simpleWarFile.exists() );

        createDir( workDirectory );

        // configure mojo
        project.addArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );
        mojo.execute();

        // validate operation - META-INF is automatically excluded so remove the file from the list
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedWEBXMLFile = new File( webAppDirectory, "WEB-INF/web.xml" );
        File expectedWARFile = new File( webAppDirectory, "/org/sample/company/test.jsp" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        // check simple.war in the unit test dir under resources to verify the list of files
        assertTrue( "web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists() );
        assertTrue( "war file not found: " + expectedWARFile.toString(), expectedWARFile.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedWARFile.delete();
    }

    /**
     * Merge a dependent WAR when a file in the war source directory overrides one found in the WAR.
     * @throws Exception in case of an error.
     */
    public void testExplodedWarMergeWarLocalFileOverride()
        throws Exception
    {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        WarArtifactStub warArtifact = new WarArtifactStub( getBasedir() );

        String testId = "testExplodedWarMergeWarLocalFileOverride";
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = getWebAppSource( testId );
        File simpleJSP = new File( webAppSource, "org/sample/company/test.jsp" );
        createFile( simpleJSP );

        File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
        createDir( workDirectory );

        File classesDir = createClassesDir( testId, true );

        // configure mojo
        project.addArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );
        mojo.execute();

        // validate operation
        File expectedFile = new File( webAppDirectory, "/org/sample/company/test.jsp" );

        assertTrue( "file not found: " + expectedFile.toString(), expectedFile.exists() );
        assertEquals( "file incorrect", simpleJSP.toString(), FileUtils.fileRead( expectedFile ) );

        // check when the merged war file is newer - so set an old time on the local file
        long time = new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2005-1-1" ).getTime();
        simpleJSP.setLastModified( time );
        expectedFile.setLastModified( time );

        project.addArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );
        mojo.execute();

        assertTrue( "file not found: " + expectedFile.toString(), expectedFile.exists() );
        assertEquals( "file incorrect", simpleJSP.toString(), FileUtils.fileRead( expectedFile ) );

        // house keeping
        expectedFile.delete();
    }

    // The last modified thingy behavior is not applicable anymore. This is the only test that
    // has been removed.
    // /**
    // * Merge a dependent WAR that gets updated since the last run.
    // */
    // public void testExplodedWarMergeWarUpdated()
    // throws Exception
    // {
    // // setup test data
    // MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
    // WarArtifactStub warArtifact = new WarArtifactStub( getBasedir() );
    //
    // String testId = "testExplodedWarMergeWarUpdated";
    // File webAppDirectory = new File( getTestDirectory(), testId );
    // FileUtils.deleteDirectory( webAppDirectory );
    //
    // File webAppSource = getWebAppSource( testId );
    //
    // File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
    // createDir( workDirectory );
    //
    // File classesDir = createClassesDir( testId, true );
    //
    // // configure mojo
    // project.addArtifact( warArtifact );
    // this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
    // setVariableValueToObject( mojo, "workDirectory", workDirectory );
    // mojo.execute();
    //
    // // validate operation
    // File expectedFile = new File( webAppDirectory, "/org/sample/company/test.jsp" );
    //
    // assertTrue( "file not found: " + expectedFile.toString(), expectedFile.exists() );
    // assertEquals( "file incorrect", "", FileUtils.fileRead( expectedFile ) );
    //
    // // update file, so the local one is older
    // warArtifact.setFile( new File( warArtifact.getFile().getParentFile(), "simple-updated.war" ) );
    //
    // mojo.execute();
    //
    // assertTrue( "file not found: " + expectedFile.toString(), expectedFile.exists() );
    // assertEquals( "file incorrect", "updated\n", FileUtils.fileRead( expectedFile ) );
    //
    // // update file, so the local one is newer
    // warArtifact.setFile( new File( warArtifact.getFile().getParentFile(), "simple.war" ) );
    //
    // mojo.execute();
    //
    // assertTrue( "file not found: " + expectedFile.toString(), expectedFile.exists() );
    // assertEquals( "file incorrect", "updated\n", FileUtils.fileRead( expectedFile ) );
    //
    // // house keeping
    // expectedFile.delete();
    // }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithEJB()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        EJBArtifactStub ejbArtifact = new EJBArtifactStub( getBasedir() );
        File ejbFile = ejbArtifact.getFile();

        assertTrue( "ejb jar not found: " + ejbFile.toString(), ejbFile.exists() );

        // configure mojo
        project.addArtifact( ejbArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar" );
        // File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    public void testExplodedWarWithJar()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithJar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        File jarFile = jarArtifact.getFile();

        assertTrue( "jar not found: " + jarFile.toString(), jarFile.exists() );

        // configure mojo
        project.addArtifact( jarArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File( webAppDirectory, "WEB-INF/lib/jarartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithEJBClient()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        EJBClientArtifactStub ejbArtifact = new EJBClientArtifactStub( getBasedir() );
        File ejbFile = ejbArtifact.getFile();

        assertTrue( "ejb jar not found: " + ejbFile.toString(), ejbFile.exists() );

        // configure mojo
        project.addArtifact( ejbArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbclientartifact-0.0-Test-client.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithTLD()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithTLD";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        TLDArtifactStub tldArtifact = new TLDArtifactStub( getBasedir() );
        File tldFile = tldArtifact.getFile();

        assertTrue( "tld jar not found: " + tldFile.getAbsolutePath(), tldFile.exists() );

        // configure mojo
        project.addArtifact( tldArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedTLDArtifact = new File( webAppDirectory, "WEB-INF/tld/tldartifact-0.0-Test.tld" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "tld artifact not found: " + expectedTLDArtifact.toString(), expectedTLDArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedTLDArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithPAR()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithPAR";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        PARArtifactStub parartifact = new PARArtifactStub( getBasedir() );
        File parFile = parartifact.getFile();

        assertTrue( "par not found: " + parFile.getAbsolutePath(), parFile.exists() );

        // configure mojo
        project.addArtifact( parartifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedPARArtifact = new File( webAppDirectory, "WEB-INF/lib/parartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "par artifact not found: " + expectedPARArtifact.toString(), expectedPARArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedPARArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithAar()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithAar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        // Fake here since the aar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub aarArtifact = new AarArtifactStub( getBasedir(), artifactHandler );
        File aarFile = aarArtifact.getFile();

        assertTrue( "jar not found: " + aarFile.toString(), aarFile.exists() );

        // configure mojo
        project.addArtifact( aarArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File( webAppDirectory, "WEB-INF/services/aarartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithMar()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithMar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        // Fake here since the mar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub marArtifact = new MarArtifactStub( getBasedir(), artifactHandler );
        File marFile = marArtifact.getFile();

        assertTrue( "jar not found: " + marFile.toString(), marFile.exists() );

        // configure mojo
        project.addArtifact( marArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File( webAppDirectory, "WEB-INF/modules/marartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithXar()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithXar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        // Fake here since the xar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub xarArtifact = new XarArtifactStub( getBasedir(), artifactHandler );
        File xarFile = xarArtifact.getFile();

        assertTrue( "jar not found: " + xarFile.toString(), xarFile.exists() );

        // configure mojo
        project.addArtifact( xarArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File( webAppDirectory, "WEB-INF/extensions/xarartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithDuplicateDependencies()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithDuplicateDependencies";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        EJBArtifactStub ejbArtifact = new EJBArtifactStub( getBasedir() );
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub( getBasedir() );
        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue( "ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists() );

        // configure mojo
        ejbArtifact.setGroupId( "org.sample.ejb" );
        ejbArtifactDup.setGroupId( "org.dup.ejb" );
        project.addArtifact( ejbArtifact );
        project.addArtifact( ejbArtifactDup );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact-0.0-Test.jar" );
        File expectedEJBDupArtifact = new File( webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );
        assertTrue( "ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_DuplicateWithClassifier()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_DuplicateWithClassifier";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        EJBArtifactStub ejbArtifact = new EJBArtifactStub( getBasedir() );
        EJBArtifactStubWithClassifier ejbArtifactDup = new EJBArtifactStubWithClassifier( getBasedir() );

        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue( "ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists() );

        // configure mojo

        ejbArtifact.setGroupId( "org.sample.ejb" );
        ejbArtifactDup.setGroupId( "org.sample.ejb" );

        ejbArtifactDup.setClassifier( "classifier" );

        project.addArtifact( ejbArtifact );
        project.addArtifact( ejbArtifactDup );

        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar" );
        File expectedEJBDupArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test-classifier.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );
        assertTrue( "ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithClasses()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithClasses";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );

        // configure mojo
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedClass = new File( webAppDirectory, "WEB-INF/classes/sample-servlet.class" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "classes not found: " + expectedClass.toString(), expectedClass.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedClass.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithSourceIncludeExclude()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithSourceIncludeExclude";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "warSourceIncludes", "**/*sit.jsp" );
        setVariableValueToObject( mojo, "warSourceExcludes", "**/last*.*" );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedWEBXMLDir = new File( webAppDirectory, "WEB-INF" );
        File expectedMETAINFDir = new File( webAppDirectory, "META-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertFalse( "source files found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "WEB XML not found: " + expectedWEBXMLDir.toString(), expectedWEBXMLDir.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLDir.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWar_WithWarDependencyIncludeExclude()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithWarDependencyIncludeExclude";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        IncludeExcludeWarArtifactStub includeexcludeWarArtifact = new IncludeExcludeWarArtifactStub( getBasedir() );
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
        File includeExcludeWarFile = includeexcludeWarArtifact.getFile();

        assertTrue( "war not found: " + includeExcludeWarFile.toString(), includeExcludeWarFile.exists() );

        createDir( workDirectory );

        // configure mojo
        project.addArtifact( includeexcludeWarArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "dependentWarIncludes", "**/*Include.jsp,**/*.xml" );
        setVariableValueToObject( mojo, "dependentWarExcludes", "**/*Exclude*,**/MANIFEST.MF" );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedManifestFile = new File( webAppDirectory, "META-INF/MANIFEST.MF" );
        File expectedWEBXMLFile = new File( webAppDirectory, "WEB-INF/web.xml" );
        File expectedIncludedWARFile = new File( webAppDirectory, "/org/sample/company/testInclude.jsp" );
        File expectedExcludedWarfile = new File( webAppDirectory, "/org/sample/companyExclude/test.jsp" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        // check include-exclude.war in the unit test dir under resources to verify the list of files
        assertTrue( "web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists() );
        assertFalse( "manifest file found: " + expectedManifestFile.toString(), expectedManifestFile.exists() );
        assertTrue( "war file not found: " + expectedIncludedWARFile.toString(), expectedIncludedWARFile.exists() );
        assertFalse( "war file not found: " + expectedExcludedWarfile.toString(), expectedExcludedWarfile.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedManifestFile.delete();
        expectedWEBXMLFile.delete();
        expectedIncludedWARFile.delete();
        expectedExcludedWarfile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithSourceModificationCheck()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithSourceModificationCheck";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );

        // destination file is already created manually containing an "error" string
        // source is newer than the destination file
        mojo.execute();

        // validate operation

        File expectedWEBINFDir = new File( webAppDirectory, "WEB-INF" );
        File expectedMETAINFDir = new File( webAppDirectory, "META-INF" );
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "WEB-INF not found", expectedWEBINFDir.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );

        // 1st phase destination is older than source
        // destination starts with a value of error replaced with a blank source
        assertFalse( "source files not updated with new copy: " + expectedWebSourceFile.toString(),
                     "error".equals( FileUtils.fileRead( expectedWebSourceFile ) ) );

        // TODO: uncomment when lastModified problem is resolved
        // FileWriter writer = new FileWriter(expectedWebSourceFile);
        //
        // // 2nd phase destination is newer than source
        // // destination should not be replaced with an blank source
        // writer.write("newdata");
        // mojo.execute();
        // reader = new FileReader(expectedWebSourceFile);
        // reader.read(data);
        // assertTrue("source file updated with old copy: "
        // +expectedWebSourceFile.toString(),String.valueOf(data).equals("newdata") ); }

        // house keeping
        expectedWEBINFDir.delete();
        expectedMETAINFDir.delete();
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithOutputFileNameMapping()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithFileNameMapping";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        File jarFile = jarArtifact.getFile();

        assertTrue( "jar not found: " + jarFile.toString(), jarFile.exists() );

        // configure mojo
        project.addArtifact( jarArtifact );
        mojo.setOutputFileNameMapping( "@{artifactId}@.@{extension}@" );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File( webAppDirectory, "WEB-INF/lib/jarartifact.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithOutputFileNameMappingAndDuplicateDependencies()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWarWithFileNameMappingAndDuplicateDependencies";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        EJBArtifactStub ejbArtifact = new EJBArtifactStub( getBasedir() );
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub( getBasedir() );
        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue( "ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists() );

        // configure mojo
        ejbArtifact.setGroupId( "org.sample.ejb" );
        ejbArtifactDup.setGroupId( "org.dup.ejb" );
        project.addArtifact( ejbArtifact );
        project.addArtifact( ejbArtifactDup );
        mojo.setOutputFileNameMapping( "@{artifactId}@.@{extension}@" );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact.jar" );
        File expectedEJBDupArtifact = new File( webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );
        assertTrue( "ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /* --------------------- 2.1 Overlay tests ----------------------------------- */

    /*---------------------------*/

}
