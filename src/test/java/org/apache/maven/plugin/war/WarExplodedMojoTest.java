package org.apache.maven.plugin.war;

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

import org.apache.maven.plugin.war.stub.EJBArtifactStub;
import org.apache.maven.plugin.war.stub.EJBClientArtifactStub;
import org.apache.maven.plugin.war.stub.IncludeExcludeWarArtifactStub;
import org.apache.maven.plugin.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugin.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugin.war.stub.PARArtifactStub;
import org.apache.maven.plugin.war.stub.ResourceStub;
import org.apache.maven.plugin.war.stub.SimpleWarArtifactStub;
import org.apache.maven.plugin.war.stub.TLDArtifactStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class WarExplodedMojoTest
    extends AbstractWarMojoTest
{
    protected static final String pomFilePath =
        getBasedir() + "/target/test-classes/unit/warexplodedmojo/plugin-config.xml";

    private WarExplodedMojo mojo;

    protected File getTestDirectory()
        throws Exception
    {
        return new File( getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();

        mojo = (WarExplodedMojo) lookupMojo( "exploded", pomFilePath );
        assertNotNull( mojo );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setUp
    }

    /**
     * @throws Exception
     */
    public void testSimpleExplodedWar()
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
        ResourceStub[] resources = new ResourceStub[]{new ResourceStub()};

        createFile( sampleResource );

        // configure mojo
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
    }

    /**
     * @throws Exception
     */
    public void testExplodedWar_WithCustomWebXML()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithCustomWebXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
    }

    /**
     * @throws Exception
     */
    public void testExplodedWar_WithContainerConfigXML()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithContainerConfigXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File classesDir = createClassesDir( testId, true );
        File webAppSource = createWebAppSource( testId );
        File xmlSource = createXMLConfigDir( testId, new String[]{"config.xml"} );
        File webAppDirectory = new File( getTestDirectory(), testId );

        // configure mojo
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
    }

    /**
     * @throws Exception
     */
    public void testExplodedWar_WithSimpleExternalWARFile()
        throws Exception
    {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        SimpleWarArtifactStub simpleWarArtifact = new SimpleWarArtifactStub( getBasedir() );

        String testId = "ExplodedWar_WithSimpleExternalWARFile";
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
        File simpleWarFile = simpleWarArtifact.getFile();

        assertTrue( "simple war not found: " + simpleWarFile.toString(), simpleWarFile.exists() );

        createDir( workDirectory );

        // configure mojo
        project.addArtifact( simpleWarArtifact );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedManifestFile = new File( webAppDirectory, "META-INF/MANIFEST.MF" );
        File expectedWEBXMLFile = new File( webAppDirectory, "WEB-INF/web.xml" );
        File expectedWARFile = new File( webAppDirectory, "/org/sample/company/test.jsp" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        // check simple.war in the unit test dir under resources to verify the list of files  
        assertTrue( "web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists() );
        assertTrue( "manifest file not found: " + expectedManifestFile.toString(), expectedManifestFile.exists() );
        assertTrue( "war file not found: " + expectedWARFile.toString(), expectedWARFile.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File( webAppDirectory, "WEB-INF/lib/ejbclientartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedTLDArtifact = new File( webAppDirectory, "WEB-INF/tld/tldartifact-0.0-Test.tld" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "tld artifact not found: " + expectedTLDArtifact.toString(), expectedTLDArtifact.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedPARArtifact = new File( webAppDirectory, "WEB-INF/lib/parartifact-0.0-Test.jar" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "par artifact not found: " + expectedPARArtifact.toString(), expectedPARArtifact.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
        assertTrue( "ejb dup artifact not found: " + expectedEJBDupArtifact.toString(),
                    expectedEJBDupArtifact.exists() );
    }

    /**
     * @throws Exception
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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        // final name form is <artifactId>-<version>.<type>
        File expectedClass = new File( webAppDirectory, "WEB-INF/classes/sample-servlet.class" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "classes not found: " + expectedClass.toString(), expectedClass.exists() );
    }

    /**
     * @throws Exception
     */
    public void testExplodedWar_WithResourceFiltering()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithResourceFiltering";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File webAppResource = new File( getTestDirectory(), testId + "-test-data/resources" );
        File sampleResource = new File( webAppResource, "custom-setting.cfg" );
        File sampleResourceWDir = new File( webAppResource, "custom-config/custom-setting.cfg" );
        File filterFile = new File( getTestDirectory(), testId + "-test-data/filters/filter.properties" );
        LinkedList filterList = new LinkedList();
        ResourceStub[] resources = new ResourceStub[]{new ResourceStub()};

        createFile( sampleResource );
        createFile( sampleResourceWDir );
        createFile( filterFile );
        filterList.add( filterFile.getAbsolutePath() );

        // prepare web resources and filters
        String filterData = new String( "resource_key=${resource_value}\n" );
        String systemData = new String( "system_key=${user.dir}\n" );
        String projectProp = new String( "project_key=${is_this_simple}\n" );
        FileWriter writer = new FileWriter( sampleResourceWDir );
        writer.write( filterData + systemData + projectProp );
        writer.flush();
        writer.close();
        writer = new FileWriter( sampleResource );
        writer.write( filterData + systemData + projectProp );
        writer.flush();
        writer.close();

        String filterString = new String( "resource_value=this_is_filtered" );
        writer = new FileWriter( filterFile );
        writer.write( filterString );
        writer.flush();
        writer.close();

        // configure mojo
        project.addProperty( "is_this_simple", "i_think_so" );
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        resources[0].setFiltering( true );
        this.configureMojo( mojo, filterList, classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "webResources", resources );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedResourceFile = new File( webAppDirectory, "custom-setting.cfg" );
        File expectedResourceWDirFile = new File( webAppDirectory, "custom-config/custom-setting.cfg" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "resource file not found:" + expectedResourceFile.toString(), expectedResourceFile.exists() );
        assertTrue( "resource file with dir not found:" + expectedResourceWDirFile.toString(), expectedResourceWDirFile
            .exists() );

        // validate filtered file
        FileReader reader = new FileReader( expectedResourceWDirFile );
        char[] data = new char[1024];
        StringTokenizer tokenizer;

        reader.read( data );
        tokenizer = new StringTokenizer( String.valueOf( data ), "\n" );

        String token = tokenizer.nextToken();
        assertTrue( "error in filtering using filter files", token.equals( "resource_key=this_is_filtered" ) );

        token = tokenizer.nextToken();
        assertTrue( "error in filtering using System properties",
                    token.equals( "system_key=" + System.getProperty( "user.dir" ) ) );

        token = tokenizer.nextToken();
        assertTrue( "error in filtering using project properties", token.equals( "project_key=i_think_so" ) );
    }

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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
    }

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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
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
    }

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
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );

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
//        FileWriter writer = new FileWriter(expectedWebSourceFile);
//
//        // 2nd phase destination is newer than source
//        // destination should not be replaced with an blank source
//        writer.write("newdata");
//        mojo.execute();
//        reader = new FileReader(expectedWebSourceFile);
//        reader.read(data);
//        assertTrue("source file updated with old copy: " +expectedWebSourceFile.toString(),String.valueOf(data).equals("newdata") );    }
    }
}
