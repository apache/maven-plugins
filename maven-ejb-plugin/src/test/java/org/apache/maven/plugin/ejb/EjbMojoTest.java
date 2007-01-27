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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.jar.JarFile;


/**
 * EJB plugin Test Case
 */

public class EjbMojoTest
    extends AbstractMojoTestCase
{
    static final String DEFAULT_POM_PATH = "target/test-classes/unit/ejbmojotest/plugin-config.xml";

    static final String DEFAULT_JAR_NAME = "testJar";

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
     * @throws Exception if any exception occurs
     */
    public void testTestEnvironment()
        throws Exception
    {
        // Perform lookup on the Mojo to make sure everything is ok
        lookupMojo();
    }

    /**
     * Basic jar creation test.
     *
     * @throws Exception if any exception occurs
     */
    public void testDefaultWithoutClientJar()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "default-noclient" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        setupDefaultProject( project );

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, false );
    }

    /**
     * Classified jar creation test.
     *
     * @throws Exception if any exception occurs
     */
    public void testClassifiedJarWithoutClientJar()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "classified-noclient" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        setupDefaultProject( project );

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );
        setVariableValueToObject( mojo, "classifier", "classified" );

        mojo.execute();

        assertJarCreation( project, true, false, "classified" );
    }

    /**
     * Basic jar creation test with client jar.
     *
     * @throws Exception if any exception occurs
     */
    public void testDefaultWithClientJar()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "default-client" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        setupDefaultProject( project );

        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, true );
    }

    /**
     * Classified jar creation test with client jar.
     *
     * @throws Exception if any exception occurs
     */
    public void testClassifiedJarWithClientJar()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "classified-client" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        setupDefaultProject( project );

        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );
        setVariableValueToObject( mojo, "classifier", "classified" );

        mojo.execute();

        assertJarCreation( project, true, true, "classified" );
    }

    /**
     * Default ejb jar inclusion and exclusion test.
     *
     * @throws Exception if any exception occurs
     */
    public void testDefaultInclusionsExclusions()
        throws Exception
    {

        final MavenProjectResourcesStub project = createTestProject( "includes-excludes-default" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppBean.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppCMP.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppSession.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, false );
        assertJarContent( project, new String[]{"META-INF/MANIFEST.MF", "META-INF/ejb-jar.xml",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties", "org/sample/ejb/AppBean.class",
            "org/sample/ejb/AppCMP.class", "org/sample/ejb/AppSession.class"}, null );
    }

    /**
     * Client jar default inclusion and exclusion test.
     *
     * @throws Exception if any exception occurs
     */
    public void testClientJarDefaultInclusionsExclusions()
        throws Exception
    {

        final MavenProjectResourcesStub project = createTestProject( "includes-excludes-client" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

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

        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, true );
        assertClientJarContent( project, new String[]{"META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties", "org/sample/ejb/AppStub.class"},
                                         new String[]{"META-INF/ejb-jar.xml", "org/sample/ejb/AppBean.class",
                                             "org/sample/ejb/AppCMP.class", "org/sample/ejb/AppSession.class"} );
    }

    /**
     * Client jar inclusion test.
     *
     * @throws Exception if any exception occurs
     */
    public void testClientJarInclusions()
        throws Exception
    {
        final LinkedList inclusions = new LinkedList();
        inclusions.add( "**/*Include.class" );

        final MavenProjectResourcesStub project = createTestProject( "client-includes" );
        final EjbMojo mojo = lookupMojoWithSettings( project, inclusions, new LinkedList() );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppInclude.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppExclude.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, true );
        assertClientJarContent( project, new String[]{"META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties", "org/sample/ejb/AppInclude.class"},
                                         new String[]{"META-INF/ejb-jar.xml", "org/sample/ejb/AppExclude.class"} );
    }

    /**
     * Client jar exclusions test.
     *
     * @throws Exception if any exception occurs
     */
    public void testClientJarExclusions()
        throws Exception
    {

        final LinkedList exclusions = new LinkedList();
        exclusions.add( "**/*Exclude.class" );

        final MavenProjectResourcesStub project = createTestProject( "client-excludes" );
        final EjbMojo mojo = lookupMojoWithSettings( project, new LinkedList(), exclusions );

        // put this on the target output dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppInclude.class", MavenProjectResourcesStub.OUTPUT_FILE );
        project.addFile( "org/sample/ejb/AppExclude.class", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "true" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        mojo.execute();

        assertJarCreation( project, true, true );
        assertClientJarContent( project, new String[]{"META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.xml",
            "META-INF/maven/org.apache.maven.test/maven-test-plugin/pom.properties", "org/sample/ejb/AppInclude.class"},
                                         new String[]{"META-INF/ejb-jar.xml", "org/sample/ejb/AppExclude.class"} );

    }

    /**
     * Tests if the mojo throws an exception when the EJB version is < 3.0
     * and no deployment descriptor is present. The case with deployment descriptor
     * present is covered by previous tests.
     *
     * @throws Exception if any exception occurs
     */
    public void testEjbComplianceVersionTwoDotOneWithoutDescriptor()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "compliance-nodescriptor-2.1" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "2.1" );

        try
        {
            mojo.execute();
            fail( "Exception should be thrown: No deployment descriptor present." );
        }
        catch ( MojoExecutionException e )
        {
            // OK
        }
    }

    /**
     * Tests if the jar is created under EJB version 3.0 with
     * deployment descriptor present.
     *
     * @throws Exception if any exception occurs
     */
    public void testEjbComplianceVersionThreeWithDescriptor()
        throws Exception
    {

        final MavenProjectResourcesStub project = createTestProject( "compliance-descriptor-3" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        // put this on the target dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "3.0" );

        mojo.execute();

        assertJarCreation( project, true, false );

    }

    /**
     * Tests if the jar is created under EJB version 3.0 without
     * deployment descriptor present.
     *
     * @throws Exception if any exception occurs
     */
    public void testEjbCompliance_3_0_WithoutDescriptor()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "compliance-nodescriptor-3" );
        final EjbMojo mojo = lookupMojoWithDefaultSettings( project );

        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );

        // start creating the environment
        project.setupBuildEnvironment();

        setVariableValueToObject( mojo, "generateClient", "false" );
        setVariableValueToObject( mojo, "ejbVersion", "3.0" );

        mojo.execute();

        assertJarCreation( project, true, false );
    }


    protected EjbMojo lookupMojo()
        throws Exception
    {
        File pomFile = new File( getBasedir(), DEFAULT_POM_PATH );
        EjbMojo mojo = (EjbMojo) lookupMojo( "ejb", pomFile );

        assertNotNull( mojo );

        return mojo;
    }

    protected MavenProjectResourcesStub createTestProject( final String testName )
        throws Exception
    {
        // this will automatically create the isolated
        // test environment
        return new MavenProjectResourcesStub( testName );
    }

    protected void setupDefaultProject( final MavenProjectResourcesStub project )
        throws Exception
    {
        // put this on the target dir
        project.addFile( "META-INF/ejb-jar.xml", MavenProjectResourcesStub.OUTPUT_FILE );
        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        // start creating the environment
        project.setupBuildEnvironment();

    }

    protected EjbMojo lookupMojoWithSettings( final MavenProject project, LinkedList clientIncludes,
                                              LinkedList clientExcludes )
        throws Exception
    {
        final EjbMojo mojo = lookupMojo();
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "basedir", project.getBuild().getDirectory() );
        setVariableValueToObject( mojo, "outputDirectory", project.getBuild().getOutputDirectory() );
        setVariableValueToObject( mojo, "jarName", DEFAULT_JAR_NAME );
        setVariableValueToObject( mojo, "clientExcludes", clientExcludes );
        setVariableValueToObject( mojo, "clientIncludes", clientIncludes );

        return mojo;
    }

    protected EjbMojo lookupMojoWithDefaultSettings( final MavenProject project )
        throws Exception
    {
        return lookupMojoWithSettings( project, new LinkedList(), new LinkedList() );
    }


    protected void assertJarCreation( final MavenProject project, boolean ejbJarCreated, boolean ejbClientJarCreated,
                                      String classifer )
    {
        String checkedJarFile;
        if ( classifer == null )
        {
            checkedJarFile = project.getBuild().getDirectory() + "/" + DEFAULT_JAR_NAME + ".jar";
        }
        else
        {
            checkedJarFile = project.getBuild().getDirectory() + "/" + DEFAULT_JAR_NAME + "-" + classifer + ".jar";
        }

        final String checkedClientJarFile = project.getBuild().getDirectory() + "/" + DEFAULT_JAR_NAME + "-client.jar";

        assertEquals( "Invalid value for ejb-jar creation", ejbJarCreated, FileUtils.fileExists( checkedJarFile ) );
        assertEquals( "Invalid value for ejb-jar client creation", ejbClientJarCreated,
                      FileUtils.fileExists( checkedClientJarFile ) );

    }

    protected void assertJarCreation( final MavenProject project, boolean ejbJarCreated, boolean ejbClientJarCreated )
    {
        assertJarCreation( project, ejbJarCreated, ejbClientJarCreated, null );

    }

    private void doAssertJarContent( final MavenProject project, final String fileName, final String[] expectedFiles,
                                     final String[] unexpectedFiles )
        throws IOException
    {
        String checkedJarFile = project.getBuild().getDirectory() + "/" + fileName;
        if ( expectedFiles != null )
        {
            final JarContentChecker inclusionChecker = new JarContentChecker();

            // set expected jar contents
            for ( int i = 0; i < expectedFiles.length; i++ )
            {
                String expectedFile = expectedFiles[i];
                inclusionChecker.addFile( new File( expectedFile ) );
            }
            assertTrue( inclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
        }
        if ( unexpectedFiles != null )
        {
            final JarContentChecker exclusionChecker = new JarContentChecker();
            for ( int i = 0; i < unexpectedFiles.length; i++ )
            {
                String unexpectedFile = unexpectedFiles[i];
                exclusionChecker.addFile( new File( unexpectedFile ) );
            }
            assertFalse( exclusionChecker.isOK( new JarFile( checkedJarFile ) ) );
        }


    }

    protected void assertJarContent( final MavenProject project, final String[] expectedFiles,
                                     final String[] unexpectedFiles )
        throws IOException
    {

        doAssertJarContent( project, DEFAULT_JAR_NAME + ".jar", expectedFiles, unexpectedFiles );
    }

    protected void assertClientJarContent( final MavenProject project, final String[] expectedFiles,
                                           final String[] unexpectedFiles )
        throws IOException
    {

        doAssertJarContent( project, DEFAULT_JAR_NAME + "-client.jar", expectedFiles, unexpectedFiles );

    }
}
