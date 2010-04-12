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
package org.apache.maven.plugin.eclipse.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.TempEclipseWorkspace;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipsePluginIT
    extends AbstractEclipsePluginIT
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Assumes that unit tests (ReadWorkspaceLocationsTest) have been run so that .location files have been created
     * correctly.
     */
    public void testDynamicWorkspaceLookup()
        throws Exception
    {
        File basedir =
            new File( TempEclipseWorkspace.getFixtureEclipseDynamicWorkspace().workspaceLocation, "project-Z" );
        testProject( basedir );
    }

    public void testProject01()
        throws Exception
    {
        testProject( "project-01" );
    }

    public void testProject02()
        throws Exception
    {
        testProject( "project-02" );
    }

    public void testProject03()
        throws Exception
    {
        testProject( "project-03" );
    }

    public void testProject04()
        throws Exception
    {
        testProject( "project-04" );
    }

    public void testProject05()
        throws Exception
    {
        testProject( "project-05" );
    }

    public void testProject06()
        throws Exception
    {
        testProject( "project-06" );
    }

    /**
     * @throws Exception
     */
    public void testProject07()
        throws Exception
    {
        // Fails because of MECLIPSE-367
        testProject( "project-07" );
    }

    public void testProject08()
        throws Exception
    {
        testProject( "project-08" );
    }

    /**
     * Tests with <code>outputDirectory</code> and <code>outputDir</code>
     * 
     * @throws Exception
     */
    public void testProject09()
        throws Exception
    {
        testProject( "project-09" );
    }

    public void testProject10()
        throws Exception
    {
        testProject( "project-10" );
    }

    public void testProject11()
        throws Exception
    {
        testProject( "project-11" );
    }

    /**
     * Ear packaging
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject12()
        throws Exception
    {
        testProject( "project-12" );
    }

    /**
     * Dependency range - MECLIPSE-96
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject13()
        throws Exception
    {
        testProject( "project-13" );
    }

    /**
     * Additional natures and builders - MECLIPSE-64
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject14()
        throws Exception
    {
        testProject( "project-14" );
    }

    /**
     * <code>outputDirectory</code> parameter - MECLIPSE-11
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject15()
        throws Exception
    {
        Properties props = new Properties();
        props.put( "outputDirectory", "bin" );
        testProject( "project-15", props, "clean", "eclipse" );
    }

    /**
     * UTF8 encoding - MECLIPSE-56
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject16()
        throws Exception
    {
        // failing with maven < 2.0.8 due to MNG-2025
        testProject( "project-16" );
    }

    /**
     * ISO-8859-15 encoding - MECLIPSE-56
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject17()
        throws Exception
    {
        // failing with maven < 2.0.8 due to MNG-2025
        testProject( "project-17" );
    }

    /**
     * relative location of system dependencies - MECLIPSE-89
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject18()
        throws Exception
    {
        testProject( "project-18" );
    }

    /**
     * Resource targetPath is relative to the project's output directory - MECLIPSE-77
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject19()
        throws Exception
    {
        testProject( "project-19" );
    }

    /**
     * WTP 1.5 changes in wtpmodules.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject20()
        throws Exception
    {
        testProject( "project-20" );
    }

    /**
     * PDE support.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject21()
        throws Exception
    {
        testProject( "project-21" );
    }

    /**
     * PDE support using eclipse-plugin packaging.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject22()
        throws Exception
    {
        testProject( "project-22" );
    }

    /**
     * Additional config files using "additionalConfig" property.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject23()
        throws Exception
    {
        testProject( "project-23" );
    }

    /**
     * Test rewriting of OSGI manifest files.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject24()
        throws Exception
    {
        testProject( "project-24" );
    }

    /**
     * Test source exclude/include.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject25()
        throws Exception
    {
        testProject( "project-25" );
    }

    /**
     * Test different compiler settings for test sources.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject26()
        throws Exception
    {
        testProject( "project-26" );
    }

    /**
     * Test additional project facets specified.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject27()
        throws Exception
    {
        testProject( "project-27" );
    }

    /**
     * MECLIPSE-241 : Compiler settings from parent project aren't used in wtp facet.
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject28()
        throws Exception
    {
        testProject( "project-28/module-1" );
    }

    /**
     * MECLIPSE-198 : EJB version is not resloved
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject29()
        throws Exception
    {
        testProject( "project-29" );
    }

    /**
     * MECLIPSE-108 : .wtpmodules with version 2.4 for javax.servlet:servlet-api:2.3
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject30()
        throws Exception
    {
        testProject( "project-30" );
    }

    /**
     * MECLIPSE-185 : plugin doesn't fail when dependencies are missing
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject31()
        throws Exception
    {
        try
        {
            testProject( "project-31" );

            // TODO: disabling this test for now. See comments in MECLIPSE-185 - Bfox
            // fail("Expected to receive a MojoExecutionException");
        }
        catch ( MojoExecutionException e )
        {
            // expected exception here
        }
    }

    /**
     * MECLIPSE-109 : .component wb-resource source path incorrect for ear packaging
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject32()
        throws Exception
    {
        testProject( "project-32" );
    }

    /**
     * MECLIPSE-287 : dependencies with and without classifiers. MECLIPSE-151 : test jar source attachments.
     * MECLIPSE-367 : Dependency to artifact with classifier tests not distinguished from the regular artifact
     * 
     * @throws Exception any exception thrown during test
     */
    public void testProject33()
        throws Exception
    {
        testProject( "project-33" );
    }

    public void testProject34()
        throws Exception
    {
        testProject( "project-34" );
    }

    public void testProject35()
        throws Exception
    {
        testProject( "project-35" );
    }

    public void testProject36()
        throws Exception
    {
        // Install artefacts
        File basedir = getTestFile( "target/test-classes/projects/project-36" );
        File pom = new File( basedir, "pom.xml" );
        List goals = new ArrayList();
        goals.add( "install" );
        executeMaven( pom, new Properties(), goals );
        // Test
        testProject( "project-36" );
    }

    public void testProject37()
        throws Exception
    {
        testProject( "project-37" );
    }

    /**
     * MECLIPSE-56 : problem with encoding of non-ascii characters in pom.xml
     */
    public void testMECLIPSE_56_encoding()
        throws Exception
    {
        testProject( "MECLIPSE-56_encoding" );
    }

    public void testProject38()
        throws Exception
    {
        testProject( "project-38" );
    }

    public void testProject39_a()
        throws Exception
    {
        checkJRESettingsWithEclipseWorkspace( "project-39-a", TempEclipseWorkspace.getFixtureEclipseWithDefault13(),
                                              null );
    }

    public void testProject39_b()
        throws Exception
    {
        checkJRESettingsWithEclipseWorkspace( "project-39-b", TempEclipseWorkspace.getFixtureEclipseWithDefault15(),
                                              null );
    }

    public void testProject39_c()
        throws Exception
    {
        checkJRESettingsWithEclipseWorkspace( "project-39-c",
                                              TempEclipseWorkspace.getFixtureEclipseWorkspaceWithRad7Default14(), null );
    }

    public void testProject40_a()
        throws Exception
    {
        String jre131 = new java.io.File( "target/test-classes/eclipse/dummyJDK/1.3.1/bin/javac" ).getCanonicalPath();
        checkJRESettingsWithEclipseWorkspace( "project-40-a", TempEclipseWorkspace.getFixtureEclipseWithDefault13(),
                                              jre131 );
    }

    public void testProject40_b()
        throws Exception
    {
        String jre131 = new java.io.File( "target/test-classes/eclipse/dummyJDK/1.3.1/bin/javac" ).getCanonicalPath();
        checkJRESettingsWithEclipseWorkspace( "project-40-b", TempEclipseWorkspace.getFixtureEclipseWithDefault15(),
                                              jre131 );
    }

    public void testProject40_c()
        throws Exception
    {
        String jre131 = new java.io.File( "target/test-classes/eclipse/dummyJDK/1.3.1/bin/javac" ).getCanonicalPath();
        checkJRESettingsWithEclipseWorkspace( "project-40-c",
                                              TempEclipseWorkspace.getFixtureEclipseWorkspaceWithRad7Default14(),
                                              jre131 );
    }

    public void testProject41()
        throws Exception
    {
        TempEclipseWorkspace rad7 = TempEclipseWorkspace.getFixtureEclipseWorkspaceWithRad7Default14();
        Properties properties = new Properties();
        properties.setProperty( "eclipse.workspace", rad7.workspaceLocation.getCanonicalPath() );
        testProject( "project-41", properties, "clean", "eclipse" );

    }

    public void testProject42()
        throws Exception
    {
        TempEclipseWorkspace rad7 = TempEclipseWorkspace.getFixtureEclipseWorkspaceWithRad7Default14();
        Properties properties = new Properties();
        properties.setProperty( "eclipse.workspace", rad7.workspaceLocation.getCanonicalPath() );
        testProject( "project-42", properties, "clean", "eclipse" );

    }

    /**
     * [MECLIPSE-79]
     * 
     * @since 2.5
     * @throws Exception
     */
    public void testProject43()
        throws Exception
    {
        testProject( "project-43" );
    }

    /**
     * [MECLIPSE-219]
     * 
     * @since 2.5
     * @throws Exception
     */
    public void testProject44()
        throws Exception
    {
        testProject( "project-44" );
    }

    /**
     * [MECLIPSE-379] When downloading sources and javadocs dependency classifier is not respected.
     * 
     * @since 2.5
     * @throws Exception
     */
    public void testProject45()
        throws Exception
    {
        testProject( "project-45" );
    }

    /**
     * Test not available marker file is created for sources/javadocs. biz.aQute:bndlib:0.0.145 does not have sources or
     * javadocs.
     * 
     * @throws Exception
     */
    public void testProject46()
        throws Exception
    {
        testProject( "project-46" );
        assertNotAvailableMarkerFileExists( "biz.aQute", "bndlib", "0.0.145", null, "sources" );
        assertNotAvailableMarkerFileExists( "biz.aQute", "bndlib", "0.0.145", null, "javadoc" );
    }

    /**
     * Test not available marker file is created for sources/javadocs. commons-lang:commons-lang:1.0 does not have
     * sources but does have javadocs.
     * 
     * @throws Exception
     */
    public void testProject47()
        throws Exception
    {
        testProject( "project-47" );
        assertNotAvailableMarkerFileExists( "commons-lang", "commons-lang", "1.0", null, "sources" );
        assertNotAvailableMarkerFileDoesNotExist( "commons-lang", "commons-lang", "1.0", null, "javadoc" );
    }

    /**
     * Test not available marker file is created for sources/javadocs. does-not-exist:does-not-exist:666 doesn't exist
     * so no markers should be created.
     * 
     * @throws Exception
     */
    public void testProject48()
        throws Exception
    {
        testProject( "project-48" );
        assertNotAvailableMarkerFileDoesNotExist( "does-not-exist", "does-not-exist", "666", null, "sources" );
        assertNotAvailableMarkerFileDoesNotExist( "does-not-exist", "does-not-exist", "666", null, "javadoc" );
    }

    /**
     * Test forceRecheck
     * 
     * @throws Exception
     */
    public void testProject49()
        throws Exception
    {
        File notAvailableMarkerFile =
            getNotAvailableMarkerFile( "commons-lang", "commons-lang", "2.4", null, "sources" );
        notAvailableMarkerFile.getParentFile().mkdirs();
        notAvailableMarkerFile.createNewFile();
        getNotAvailableMarkerFile( "commons-lang", "commons-lang", "2.4", null, "javadoc" ).createNewFile();

        testProject( "project-49" );
        assertNotAvailableMarkerFileDoesNotExist( "commons-lang", "commons-lang", "2.4", null, "sources" );
        assertNotAvailableMarkerFileDoesNotExist( "commons-lang", "commons-lang", "2.4", null, "javadoc" );
    }

    /**
     * [MECLIPSE-415] settings are stored in wrong directory if project is not in the workspace.
     * 
     * @throws Exception
     */
    public void testProject50MECLIPSE415()
        throws Exception
    {
        testProject( "project-50-MECLIPSE-415" );
    }

    /**
     * [MECLIPSE-415] settings are stored in wrong directory if project is not in the workspace.
     * 
     * @throws Exception
     */
    public void testProject51MECLIPSE415()
        throws Exception
    {
        testProject( "project-51-MECLIPSE-415" );
    }

    /**
     * [MECLIPSE-104] Add the ability to specify source exclusions
     * 
     * @throws Exception
     */
    public void testProject52MECLIPSE104()
        throws Exception
    {
        testProject( "project-52-MECLIPSE-104" );
    }

    /**
     * [MECLIPSE-551] Source directory and resource directory are the same
     * 
     * @throws Exception
     */
    public void testProject53MECLIPSE551()
        throws Exception
    {
        testProject( "project-53-MECLIPSE-551" );
    }

    /**
     * [MECLIPSE-178] symbolic links need to able to be specified in the pom
     * 
     * @throws Exception
     */
    public void testProject54MECLIPSE178()
        throws Exception
    {
        testProject( "project-54-MECLIPSE-178" );
    }

    /**
     * [MECLIPSE-178] symbolic links need to able to be specified in the pom Test the case where a link is already
     * existing in the .project
     * 
     * @throws Exception
     */
    public void testProject55MECLIPSE178()
        throws Exception
    {
        testProject( "project-55-MECLIPSE-178" );
    }

    /**
     * [MECLIPSE-603] checks exclusions on direct and transitive dependencies
     * 
     * @throws Exception
     */
    public void testProject56()
        throws Exception
    {
        testProject( "project-56-MECLIPSE-603" );
    }

    /**
     * [MECLIPSE-368] Dependency configuration in DependencyManagement with exclusions is ignored
     * 
     * @throws Exception
     */
    public void testProject57()
        throws Exception
    {
        testProject( "project-57-MECLIPSE-368" );
    }

    /**
     * [MECLIPSE-621] mvn eclipse:eclipse fails or doesn't generate proper .classpath when specifying the same resource directory with different filtering rules
     * 
     * @throws Exception
     */
    public void testProject58()
        throws Exception
    {
        testProject( "project-58-MECLIPSE-621" );
    }

    /**
     * [MECLIPSE-576] Merge resource dirs shall pass gracefully
     * 
     * @throws Exception
     */
    public void testProject59()
        throws Exception
    {
        testProject( "project-59-MECLIPSE-576" );
    }

    /**
     * [MECLIPSE-652] Ability to map a webapp to the root context
     * 
     * @throws Exception
     */
    public void testProject60()
        throws Exception
    {
        testProject( "project-60-MECLIPSE-652" );
    }

    public void testJeeSimple()
        throws Exception
    {
        // Install artefacts
        File basedir = getTestFile( "target/test-classes/projects/j2ee-simple" );
        File pom = new File( basedir, "pom.xml" );
        List goals = new ArrayList();
        goals.add( "install" );
        executeMaven( pom, new Properties(), goals );
        // Test project
        testProject( "j2ee-simple" );
    }

    private void checkJRESettingsWithEclipseWorkspace( String project, TempEclipseWorkspace workspace, String jreExec )
        throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( "eclipse.workspace", workspace.workspaceLocation.getCanonicalPath() );
        if ( jreExec != null )
        {
            properties.setProperty( "maven.compiler.executable", jreExec );
        }
        testProject( project, properties, "clean", "eclipse" );
    }

}
