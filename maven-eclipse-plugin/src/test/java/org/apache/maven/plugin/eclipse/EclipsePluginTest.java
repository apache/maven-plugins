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
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipsePluginTest
    extends AbstractEclipsePluginTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
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

            // disabling this test for now. See comments in MECLIPSE-185 - Bfox
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

        File basedir = getTestFile( "target/test-classes/projects/project-35" );

        checkContextRoot( basedir, "multymodule-war", "multymodule-ear", "/somethingVeryDifferent" );

        FileReader reader =
            new FileReader( new File( basedir, "multymodule-war/.settings/org.eclipse.wst.common.component" ) );
        Xpp3Dom warComponent = Xpp3DomBuilder.build( reader );
        Xpp3Dom[] dependentModules = warComponent.getChild( "wb-module" ).getChildren( "dependent-module" );
        assertEquals( 2, dependentModules.length );
        for ( int index = 0; index < dependentModules.length; index++ )
        {
            assertEquals( "/WEB-INF/lib", dependentModules[index].getAttribute( "deploy-path" ) );
        }

        reader = new FileReader( new File( basedir, "multymodule-ear/.settings/org.eclipse.wst.common.component" ) );
        Xpp3Dom earComponent = Xpp3DomBuilder.build( reader );
        dependentModules = earComponent.getChild( "wb-module" ).getChildren( "dependent-module" );
        assertEquals( 2, dependentModules.length );
        for ( int index = 0; index < dependentModules.length; index++ )
        {
            if ( dependentModules[index].getAttribute( "archiveName" ).endsWith( "war" ) )
            {
                assertEquals( "/", dependentModules[index].getAttribute( "deploy-path" ) );
                assertTrue( !dependentModules[index].getAttribute( "archiveName" ).startsWith( ".." ) );
            }
            else
            {
                assertEquals( "lib", dependentModules[index].getAttribute( "deploy-path" ) );
                assertTrue( dependentModules[index].getAttribute( "archiveName" ).startsWith( ".." ) );
            }
        }
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

    public void testProject39()
        throws Exception
    {
        checkJRESettingsWithEclipseWorkspace( "project-39", new TempEclipseWorkspace( "eclipseWithDefault13", true ),
                                              "", null );
        checkJRESettingsWithEclipseWorkspace( "project-39", new TempEclipseWorkspace( "eclipseWithDefault15", false ),
                                              "J2SE-1.3", null );
        checkJRESettingsWithEclipseWorkspace( "project-39", new TempEclipseWorkspace( "rad7WithDefault14", false ),
                                              "J2SE-1.3", null );
    }

    public void testProject40()
        throws Exception
    {
        String jre131 = new java.io.File( "target/test-classes/eclipse/dummyJDK/1.3.1/bin/javac" ).getCanonicalPath();
        checkJRESettingsWithEclipseWorkspace( "project-40", new TempEclipseWorkspace( "eclipseWithDefault13", true ),
                                              "JVM 1.3.1", jre131 );
        checkJRESettingsWithEclipseWorkspace( "project-40", new TempEclipseWorkspace( "eclipseWithDefault15", false ),
                                              "JVM 1.3.1", jre131 );
        checkJRESettingsWithEclipseWorkspace( "project-40", new TempEclipseWorkspace( "rad7WithDefault14", false ), "",
                                              jre131 );
    }

    public void testProject41()
        throws Exception
    {
        TempEclipseWorkspace rad7 = new TempEclipseWorkspace( "rad7WithDefault14", false );
        Properties properties = new Properties();
        properties.setProperty( "eclipse.workspace", rad7.workspaceLocation.getCanonicalPath() );
        testProject( "project-41", properties, "clean", "eclipse" );

    }

    public void testProject42()
        throws Exception
    {
        TempEclipseWorkspace rad7 = new TempEclipseWorkspace( "rad7WithDefault14", false );
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
        checkContextRoot( basedir, "servlets/servlet", "ear", "servlet" );

    }

    private void checkContextRoot( File basedir, String warModule, String earModule, String expectedContextRoot )
        throws FileNotFoundException, XmlPullParserException, IOException
    {
        FileReader reader =
            new FileReader( new File( basedir, warModule + "/.settings/org.eclipse.wst.common.component" ) );
        Xpp3Dom warComponent = Xpp3DomBuilder.build( reader );
        Xpp3Dom[] properties = warComponent.getChild( "wb-module" ).getChildren( "property" );
        boolean contextRootAvaliable = false;
        for ( int index = 0; index < properties.length; index++ )
        {
            if ( properties[index].getAttribute( "name" ).equals( "context-root" ) )
            {
                assertEquals( "Context root detection in org.eclipse.wst.common.component", expectedContextRoot,
                              properties[index].getAttribute( "value" ) );
                contextRootAvaliable = true;
            }
        }
        assertTrue( "there must be a context root here", contextRootAvaliable );

        reader = new FileReader( new File( basedir, earModule + "/target/eclipseEar/META-INF/application.xml" ) );
        Xpp3Dom generatedApplicationXML = Xpp3DomBuilder.build( reader );

        Xpp3Dom[] modules = generatedApplicationXML.getChildren( "module" );
        for ( int index = 0; index < modules.length; index++ )
        {
            if ( modules[index].getChild( "web" ) != null )
            {
                assertEquals( "Context root detection in target/eclipseEar/META-INF/application.xml",
                              expectedContextRoot,
                              modules[index].getChild( "web" ).getChild( "context-root" ).getValue() );
            }
        }
    }

    private void checkJRESettingsWithEclipseWorkspace( String project, TempEclipseWorkspace workspace,
                                                       String expectedJRE, String jreExec )
        throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( "eclipse.workspace", workspace.workspaceLocation.getCanonicalPath() );
        if ( jreExec != null )
        {
            properties.setProperty( "maven.compiler.executable", jreExec );
        }
        testProject( project, properties, "clean", "eclipse" );

        Xpp3Dom classpath =
            Xpp3DomBuilder.build( new FileReader( getTestFile( "target/test-classes/projects/" + project +
                "/.classpath" ) ) );

        boolean foundDirectCompileAsProject = false;
        String foundJRE = "";

        Xpp3Dom[] classpathentries = classpath.getChildren( "classpathentry" );
        for ( int index = 0; index < classpathentries.length; index++ )
        {
            if ( "/direct-compile".equals( classpathentries[index].getAttribute( "path" ) ) )
            {
                foundDirectCompileAsProject = true;
            }
            String path = classpathentries[index].getAttribute( "path" );
            if ( "con".equals( classpathentries[index].getAttribute( "kind" ) ) && path.lastIndexOf( '/' ) > 0 )
            {
                foundJRE = path.substring( path.lastIndexOf( '/' ) + 1 );
            }
        }
        assertTrue( foundDirectCompileAsProject );
        assertEquals( expectedJRE, foundJRE );
    }

    /*
     * TODO: Add a test for downloadJavadocs. Currently, eclipse doesn't support having variables in the javadoc path.
     * This means that the expected .classpath can't match the final result as the result will have the absolute path to
     * the user's local repo.
     */
}
