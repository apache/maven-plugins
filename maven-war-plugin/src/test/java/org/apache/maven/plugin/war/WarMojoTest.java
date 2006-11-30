package org.apache.maven.plugin.war;

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

import org.apache.maven.plugin.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugin.war.stub.ProjectHelperStub;
import org.apache.maven.plugin.war.stub.SimpleWarArtifact4CCStub;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * comprehensive test on buildExplodedWebApp is done on WarExplodedMojoTest
 */
public class WarMojoTest
    extends AbstractWarMojoTest
{
    WarMojo mojo;

    private static File pomFile =
        new File( getBasedir(), "target/test-classes/unit/warmojotest/plugin-config-primary-artifact.xml" );

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/warmojotest" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        mojo = (WarMojo) lookupMojo( "war", pomFile );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setup
    }

    public void testSimpleWar()
        throws Exception
    {
        String testId = "SimpleWar";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        SimpleWarArtifact4CCStub warArtifact = new SimpleWarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        //validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        Map jarContent = new HashMap();

        assertTrue( "war file not created: " + expectedJarFile.toString(), expectedJarFile.exists() );

        JarFile jarFile = new JarFile( expectedJarFile );
        JarEntry entry;
        Enumeration enumeration = jarFile.entries();

        while ( enumeration.hasMoreElements() )
        {
            entry = (JarEntry) enumeration.nextElement();
            jarContent.put( entry.getName(), entry );
        }

        assertTrue( "manifest file not found", jarContent.containsKey( "META-INF/MANIFEST.MF" ) );
        assertTrue( "web xml not found", jarContent.containsKey( "WEB-INF/web.xml" ) );
        assertEquals( "web xml file incorrect", mojo.getWebXml().toString(),
                      IOUtil.toString( jarFile.getInputStream( (ZipEntry) jarContent.get( "WEB-INF/web.xml" ) ) ) );
        assertTrue( "web source not found: pansit.jsp", jarContent.containsKey( "pansit.jsp" ) );
        assertTrue( "web source not found: org/web/app/last-exile.jsp",
                    jarContent.containsKey( "org/web/app/last-exile.jsp" ) );
        assertTrue( "pom file not found",
                    jarContent.containsKey( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        assertTrue( "pom properties not found",
                    jarContent.containsKey( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
    }

    public void testClassifier()
        throws Exception
    {
        String testId = "Classifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        SimpleWarArtifact4CCStub warArtifact = new SimpleWarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "classifier", "test-classifier" );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        //validate jar file
        File expectedJarFile = new File( outputDir, "simple-test-classifier.war" );
        HashSet jarContent = new HashSet();

        assertTrue( "war file not created: " + expectedJarFile.toString(), expectedJarFile.exists() );
        assertNotNull( "artifact not attached to project", projectHelper.getArtifactFile() );

        JarFile jarFile = new JarFile( expectedJarFile );
        JarEntry entry;
        Enumeration enumeration = jarFile.entries();

        while ( enumeration.hasMoreElements() )
        {
            entry = (JarEntry) enumeration.nextElement();
            jarContent.add( entry.getName() );
        }

        assertTrue( "manifest file not found", jarContent.contains( "META-INF/MANIFEST.MF" ) );
        assertTrue( "web xml not found", jarContent.contains( "WEB-INF/web.xml" ) );
        assertTrue( "web source not found: pansit.jsp", jarContent.contains( "pansit.jsp" ) );
        assertTrue( "web source not found: org/web/app/last-exile.jsp",
                    jarContent.contains( "org/web/app/last-exile.jsp" ) );
        assertTrue( "pom file not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        assertTrue( "pom properties not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
    }

    public void testPrimaryArtifact()
        throws Exception
    {
        String testId = "PrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        SimpleWarArtifact4CCStub warArtifact = new SimpleWarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        warArtifact.setFile( new File( "error.war" ) );
        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        //validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        HashSet jarContent = new HashSet();

        assertTrue( "war file not created: " + expectedJarFile.toString(), expectedJarFile.exists() );
        assertTrue( "war file was not set as primary artifact",
                    project.getArtifact().getFile().getName().equals( "simple.war" ) );

        JarFile jarFile = new JarFile( expectedJarFile );
        JarEntry entry;
        Enumeration enumeration = jarFile.entries();

        while ( enumeration.hasMoreElements() )
        {
            entry = (JarEntry) enumeration.nextElement();
            jarContent.add( entry.getName() );
        }

        assertTrue( "manifest file not found", jarContent.contains( "META-INF/MANIFEST.MF" ) );
        assertTrue( "web xml not found", jarContent.contains( "WEB-INF/web.xml" ) );
        assertTrue( "web source not found: pansit.jsp", jarContent.contains( "pansit.jsp" ) );
        assertTrue( "web source not found: org/web/app/last-exile.jsp",
                    jarContent.contains( "org/web/app/last-exile.jsp" ) );
        assertTrue( "pom file not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        assertTrue( "pom properties not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
    }

    public void testNotPrimaryArtifact()
        throws Exception
    {
        // use a different pom
        File pom = new File( getBasedir(), "target/test-classes/unit/warmojotest/not-primary-artifact.xml" );
        mojo = (WarMojo) lookupMojo( "war", pom );

        String testId = "NotPrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        SimpleWarArtifact4CCStub warArtifact = new SimpleWarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        warArtifact.setFile( new File( "error.war" ) );
        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        //validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        HashSet jarContent = new HashSet();

        assertTrue( "war file not created: " + expectedJarFile.toString(), expectedJarFile.exists() );
        assertTrue( "war file was set", project.getArtifact().getFile().getName().equals( "error.war" ) );

        JarFile jarFile = new JarFile( expectedJarFile );
        JarEntry entry;
        Enumeration enumeration = jarFile.entries();

        while ( enumeration.hasMoreElements() )
        {
            entry = (JarEntry) enumeration.nextElement();
            jarContent.add( entry.getName() );
        }

        assertTrue( "manifest file not found", jarContent.contains( "META-INF/MANIFEST.MF" ) );
        assertTrue( "web xml not found", jarContent.contains( "WEB-INF/web.xml" ) );
        assertTrue( "web source not found: pansit.jsp", jarContent.contains( "pansit.jsp" ) );
        assertTrue( "web source not found: org/web/app/last-exile.jsp",
                    jarContent.contains( "org/web/app/last-exile.jsp" ) );
        assertTrue( "pom file not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        assertTrue( "pom properties not found",
                    jarContent.contains( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
    }
}
