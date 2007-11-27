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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class RadPluginTest
    extends AbstractEclipsePluginTestCase
{

    private static final String PROJECTS_BASEDIR = "target/test-classes/projects";

    public void testProject1()
        throws Exception
    {
        testProject( "project-rad-1", new Properties(), "rad-clean", "rad" );
    }

    /*
     * TODO: fix failing test public void testProject2() throws Exception { testProject( "project-rad-2", new
     * Properties(), "rad-clean", "rad" ); File generatedManifest = getTestFile( PROJECTS_BASEDIR +
     * "/project-rad-2/src/main/webapp/META-INF/MANIFEST.MF" ); File expectedManifest = getTestFile( PROJECTS_BASEDIR +
     * "/project-rad-2/src/main/webapp/META-INF/expected_MANIFEST.MF" ); assertFileEquals(
     * localRepositoryDirectory.getCanonicalPath(), expectedManifest, generatedManifest ); }
     */

    public void testProject3()
        throws Exception
    {
        testProject( "project-rad-3", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( PROJECTS_BASEDIR + "/project-rad-3/ejbModule/META-INF/MANIFEST.MF" );
        File expectedManifest =
            getTestFile( PROJECTS_BASEDIR + "/project-rad-3/ejbModule/META-INF/expected_MANIFEST.MF" );
        assertFileEquals( localRepositoryDirectory.getCanonicalPath(), expectedManifest, generatedManifest );
    }

    public void testProject4()
        throws Exception
    {
        testProject( "project-rad-4", new Properties(), "rad-clean", "rad" );
    }

    public void testProject5()
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/project-rad-5" );

        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );
        new File( basedir, "project-rad-1/META-INF" ).mkdirs();

        File pom0 = new File( basedir, "pom.xml" );

        MavenProject project = readProject( pom0 );

        String outputDirPath =
            IdeUtils.getPluginSetting( project, "org.apache.maven.plugins:maven-eclipse-plugin", "outputDir", null );
        File outputDir;

        if ( outputDirPath == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            new File( outputDir, project.getArtifactId() );
        }

        List goals = new ArrayList();

        String pluginSpec = getPluginCLISpecification();

        goals.add( pluginSpec + "rad-clean" );
        goals.add( pluginSpec + "rad" );
        goals.add( "install" );

        Properties props = new Properties();

        executeMaven( pom0, props, goals );

        // this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
        // .asList( new String[] {
        // "install",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
        // new Properties(), basedir );
        // this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
        // .asList( new String[] {
        // "install",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
        // new Properties(), basedir );

        // jar muss reincoliert sein
        assertTrue( "Expected file not found: project-rad-1/maven-core-98.0.jar",
                    new File( basedir, "project-rad-1/maven-core-98.0.jar" ).exists() );

        Xpp3Dom applicationXml =
            Xpp3DomBuilder.build( new InputStreamReader(
                                                         new FileInputStream(
                                                                              new File( basedir,
                                                                                        "project-rad-1/META-INF/application.xml" ) ),
                                                         "UTF-8" ) );

        Xpp3Dom modulesmapsXml =
            Xpp3DomBuilder.build( new InputStreamReader(
                                                         new FileInputStream(
                                                                              new File( basedir,
                                                                                        "project-rad-1/META-INF/.modulemaps" ) ),
                                                         "UTF-8" ) );

        assertNotNull( modulesmapsXml );

        Xpp3Dom webappModule = applicationXml.getChildren( "module" )[0];
        Xpp3Dom ejbModule = applicationXml.getChildren( "module" )[1];
        if ( webappModule.getChild( "web" ) == null )
        {
            webappModule = applicationXml.getChildren( "module" )[1];
            ejbModule = applicationXml.getChildren( "module" )[0];
        }

        assertEquals( "project-rad-5_2.war", webappModule.getChild( "web" ).getChild( "web-uri" ).getValue() );
        assertEquals( "project-rad-5_2", webappModule.getChild( "web" ).getChild( "context-root" ).getValue() );
        assertEquals( "project-rad-5_3.jar", ejbModule.getChild( Constants.PROJECT_PACKAGING_EJB ).getValue() );

        Xpp3Dom websettings =
            Xpp3DomBuilder.build( new InputStreamReader(
                                                         new FileInputStream( new File( basedir,
                                                                                        "project-rad-2/.websettings" ) ),
                                                         "UTF-8" ) );

        assertEquals( "project-rad-5_4.jar",
                      websettings.getChild( "lib-modules" ).getChild( "lib-module" ).getChild( "jar" ).getValue() );
        assertEquals( "project-rad-5_4",
                      websettings.getChild( "lib-modules" ).getChild( "lib-module" ).getChild( "project" ).getValue() );
    }

    public void testProject5_2()
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/project-rad-5" );

        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );

        File pom0 = new File( basedir, "pom.xml" );

        MavenProject project = readProject( pom0 );

        String outputDirPath =
            IdeUtils.getPluginSetting( project, "org.apache.maven.plugins:maven-eclipse-plugin", "outputDir", null );
        File outputDir;

        if ( outputDirPath == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            new File( outputDir, project.getArtifactId() );
        }

        List goals = new ArrayList();

        String pluginSpec = getPluginCLISpecification();

        goals.add( pluginSpec + "rad-clean" );
        goals.add( pluginSpec + "rad" );

        Properties props = new Properties();

        executeMaven( pom0, props, goals );

        // this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
        // .asList( new String[] {
        // "install",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
        // new Properties(), basedir );
        //        
        // this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
        // .asList( new String[] {
        // "install",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
        // "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
        // new Properties(), basedir );

        assertTrue( "Expected file not found: project-rad-1/maven-core-98.0.jar",
                    new File( basedir, "project-rad-1/maven-core-98.0.jar" ).exists() );

        File modulemaps = new File( basedir, "project-rad-1/META-INF/.modulemaps" );

        assertNotNull( modulemaps );

        File application = new File( basedir, "project-rad-1/META-INF/application.xml" );

        Xpp3Dom applicationXml =
            Xpp3DomBuilder.build( new InputStreamReader( new FileInputStream( application ), "UTF-8" ) );

        Xpp3Dom[] children = applicationXml.getChildren( "module" );

        assertEquals( 2, children.length );

        boolean ejbVerified = false;
        boolean warVerified = false;

        for ( int i = 0; i < children.length; i++ )
        {
            Xpp3Dom child = children[i];

            if ( child.getAttribute( "id" ).startsWith( "WebModule_" ) )
            {
                assertEquals( "project-rad-5_2.war", child.getChild( "web" ).getChild( "web-uri" ).getValue() );
                warVerified = true;
            }
            else if ( child.getAttribute( "id" ).startsWith( "EjbModule_" ) )
            {
                assertEquals( "project-rad-5_3.jar", child.getChild( Constants.PROJECT_PACKAGING_EJB ).getValue() );
                ejbVerified = true;
            }
        }

        assertTrue( warVerified );
        assertTrue( ejbVerified );
    }

    public void testProject6()
        throws Exception
    {
        testProject( "project-rad-6", new Properties(), "rad-clean", "rad" );
    }

    /**
     * Tests warSourceDirectory setting to be reflected in generated .websettings, location of jars in WEB-INF/lib and
     * generation of MANIFEST.MF at the right place
     * 
     * @throws Exception
     */
    public void testProject7()
        throws Exception
    {
        testProject( "project-rad-7", new Properties(), "rad-clean", "rad" );

        /*
         * testing libs in web content directory
         */
        File basedir = getTestFile( "target/test-classes/projects/project-rad-7" );
        File pom = new File( basedir, "pom.xml" );
        MavenProject project = readProject( pom );
        File outputDir;
        File projectOutputDir = basedir;

        String outputDirPath =
            IdeUtils.getPluginSetting( project, "org.apache.maven.plugins:maven-eclipse-plugin", "outputDir", null );
        if ( outputDirPath == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            projectOutputDir = new File( outputDir, project.getArtifactId() );
        }

        compareDirectoryContent( basedir, projectOutputDir, "WebContent/WEB-INF/lib/" );
        compareDirectoryContent( basedir, projectOutputDir, "WebContent/META-INF/" );

    }

}
