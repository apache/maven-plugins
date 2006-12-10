package org.apache.maven.plugin.ejb;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.ejb.stub.MavenProjectResourcesStub;
import org.apache.maven.plugin.ejb.utils.JarContentChecker;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.jar.JarFile;


/**
 * EJB plugin Test Case
 */

public class EjbMojoTest
    extends AbstractMojoTestCase
{
    private static final String defaultPOMPath = "target/test-classes/unit/ejbmojotest/plugin-config.xml";

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {

    }

    /**
     * check test environment
     *
     * @throws Exception
     */
    public void testTestEnvironment()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );
    }

    /**
     * basic jar creation test
     *
     * @throws Exception
     */
    public void testJarCreation_WithoutClientJar()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "jarCreation_WithoutClientJar" );

        // create the necessary test files

        // put this on the target dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

        // configure mojo
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + ".jar";
        String checkedClientJarFile = project.getBuild().getDirectory() + "/" + jarName + "-client.jar";

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertFalse( FileUtils.fileExists( checkedClientJarFile ) );
    }

    /**
     * basic jar creation test with client jar
     *
     * @throws Exception
     */
    public void testJarCreation_WithClientJar()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "jarCreation_WithClientJar" );

        // set up test files

        // put this on the target dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

        // configure mojo
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + ".jar";
        String checkedClientJarFile = project.getBuild().getDirectory() + "/" + jarName + "-client.jar";

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertTrue( FileUtils.fileExists( checkedClientJarFile ) );
    }

    /**
     * default ejb jar inclusion and exclusion
     *
     * @throws Exception
     */
    public void testDefaultInclusionsExclusions()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "defaultInclusionsExclusions" );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppBean.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppCMP.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppSession.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        // set up test data
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        JarContentChecker inclusionChecker = new JarContentChecker();
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + ".jar";

        // set expected jar contents
        inclusionChecker.addFile( new File( "META-INF/MANIFEST.MF" ) );
        inclusionChecker.addFile( new File( "META-INF/ejb-jar.xml" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppBean.class" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppCMP.class" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppSession.class" ) );

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertTrue( inclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
    }

    /**
     * client jar default inclusion and exclusion test
     *
     * @throws Exception
     */
    public void testClientJarDefaultInclusionsExclusions()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "clientJarDefaultInclusionsExclusions" );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppBean.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppCMP.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppSession.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppStub.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        // set up test data
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        JarContentChecker inclusionChecker = new JarContentChecker();
        JarContentChecker exclusionChecker = new JarContentChecker();
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + "-client.jar";

        // set expected jar contents
        inclusionChecker.addFile( new File( "META-INF/MANIFEST.MF" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppStub.class" ) );

        // files not included
        exclusionChecker.addFile( new File( "META-INF/ejb-jar.xml" ) );
        exclusionChecker.addFile( new File( "org/sample/ejb/AppBean.class" ) );
        exclusionChecker.addFile( new File( "org/sample/ejb/AppCMP.class" ) );
        exclusionChecker.addFile( new File( "org/sample/ejb/AppSession.class" ) );

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertTrue( inclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
        assertFalse( exclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
    }

    /**
     * client jar inclusion test
     *
     * @throws Exception
     */
    public void testClientJarInclusions()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "clientJarInclusions" );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppInclude.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppExclude.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        // set up test data
        String jarName = "testJar";
        LinkedList inclusions = new LinkedList();

        inclusions.add( "**/*Include.class" );

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", inclusions );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        JarContentChecker inclusionChecker = new JarContentChecker();
        JarContentChecker exclusionChecker = new JarContentChecker();
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + "-client.jar";

        // set expected jar contents
        inclusionChecker.addFile( new File( "META-INF/MANIFEST.MF" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppInclude.class" ) );

        // read the packaging conventions first for this one
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );

        // files not included
        exclusionChecker.addFile( new File( "META-INF/ejb-jar.xml" ) );
        exclusionChecker.addFile( new File( "org/sample/ejb/AppExclude.class" ) );

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertTrue( inclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
        assertFalse( exclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
    }

    /**
     * client jar exclusions test
     *
     * @throws Exception
     */
    public void testClientJarExclusions()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "clientJarExclusions" );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppInclude.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppExclude.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        // set up test data
        String jarName = "testJar";
        LinkedList exclusions = new LinkedList();

        exclusions.add( "**/*Exclude.class" );

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "clientExcludes", exclusions );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        // validate jar creation
        JarContentChecker inclusionChecker = new JarContentChecker();
        JarContentChecker exclusionChecker = new JarContentChecker();
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + "-client.jar";

        // set expected jar contents
        inclusionChecker.addFile( new File( "META-INF/MANIFEST.MF" ) );
        inclusionChecker.addFile( new File( "org/sample/ejb/AppInclude.class" ) );

        // read the packaging conventions first for this one
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml" ) );
        inclusionChecker.addFile( new File( "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties" ) );

        // files not included
        exclusionChecker.addFile( new File( "META-INF/ejb-jar.xml" ) );
        exclusionChecker.addFile( new File( "org/sample/ejb/AppExclude.class" ) );

        assertTrue( FileUtils.fileExists( checkedJarFile ) );
        assertTrue( inclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
        assertFalse( exclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
    }

    /**
     * tests if the mojo throws an exception when the EJB version is < 3.0
     * and no deployment descriptor is present. The case with deployment descriptor
     * present is covered by the testJarCreation* tests.
     *
     * @throws Exception
     */
    public void testEjbCompliance_2_1_WithoutDescriptor()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "testEjbCompliance_2_1_WithoutDescriptor" );

        // create the necessary test files

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

        // configure mojo
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        try
        {
            mojo.execute();
            fail( "Exception should be thrown: No deployment descriptor present." );
        }
        catch ( MojoExecutionException e )
        {
        }
    }

    /**
     * Tests if the jar is created under EJB version 3.0 with
     * deployment descriptor present.
     *
     * @throws Exception
     */
    public void testEjbCompliance_3_0_WithDescriptor()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "testEjbCompliance_3_0_WithDescriptor" );

        // create the necessary test files

        // put this on the target dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

        // configure mojo
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "3.0" );

        mojo.execute();

        // validate jar creation
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + ".jar";
        assertTrue( FileUtils.fileExists( checkedJarFile ) );
    }

    /**
     * Tests if the jar is created under EJB version 3.0 without
     * deployment descriptor present.
     *
     * @throws Exception
     */
    public void testEjbCompliance_3_0_WithoutDescriptor()
        throws Exception
    {
        File pomFile = new File( getBasedir(), defaultPOMPath );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        // this will automatically create the isolated
        // test environment
        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "testEjbCompliance_3_0_WithoutDescriptor" );

        // create the necessary test files

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

        // configure mojo
        String jarName = "testJar";

        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", jarName );
        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "clientExcludes", new LinkedList() );
        setVariableValueToObject( mojo, "clientIncludes", new LinkedList() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "ejbVersion", "3.0" );

        mojo.execute();

        // validate jar creation
        String checkedJarFile = project.getBuild().getDirectory() + "/" + jarName + ".jar";
        assertTrue( FileUtils.fileExists( checkedJarFile ) );
    }
}
