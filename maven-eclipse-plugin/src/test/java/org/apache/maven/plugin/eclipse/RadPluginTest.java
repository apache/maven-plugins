/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

package org.apache.maven.plugin.eclipse;

import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class RadPluginTest
    extends AbstractEclipsePluginTestCase
{
    public void testProject1()
        throws Exception
    {
        testProject( "project-rad-1", new Properties(), "rad-clean", "rad" );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-rad-2", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( "src/test/resources/projects/project-rad-2/src/main/webapp/META-INF/MANIFEST.MF" );
        File expectedManifest = getTestFile( "src/test/resources/projects/project-rad-2/src/main/webapp/META-INF/expected_MANIFEST.MF" );
        assertFileEquals( LOCAL_REPO_DIR.getCanonicalPath(), generatedManifest, expectedManifest );

    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-rad-3", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( "src/test/resources/projects/project-rad-3/ejbModule/META-INF/MANIFEST.MF" );
        File expectedManifest = getTestFile( "src/test/resources/projects/project-rad-3/ejbModule/META-INF/expected_MANIFEST.MF" );
        assertFileEquals( LOCAL_REPO_DIR.getCanonicalPath(), generatedManifest, expectedManifest );
    }

    public void testProject4()
        throws Exception
    {
        testProject( "project-rad-4", new Properties(), "rad-clean", "rad" );
    }

    public void testProject5()
        throws Exception
    {
        File basedir = getTestFile( "src/test/resources/projects/project-rad-5" );

        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );
        new File( basedir, "project-rad-1/META-INF" ).mkdirs();

        File pom0 = new File( basedir, "pom.xml" );
        File pom1 = new File( basedir, "project-rad-1/pom.xml" );
        File pom2 = new File( basedir, "project-rad-2/pom.xml" );
        File pom3 = new File( basedir, "project-rad-3/pom.xml" );
        
        MavenProject project = readProject( pom0 );
        MavenProject project1 = readProject( pom1 );
        MavenProject project2 = readProject( pom2 );
        MavenProject project3 = readProject( pom3 );

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        String outputDirPath = IdeUtils.getPluginSetting( project, "maven-eclipse-plugin", "outputDir", null );
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
        executeMaven( pom2, props, goals );
        executeMaven( pom3, props, goals );
        
        executeMaven( pom1, props, goals );
        executeMaven( pom2, props, goals );
        executeMaven( pom3, props, goals );
        
//        this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
//            .asList( new String[] {
//                "install",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
//                            new Properties(), basedir );
//        this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
//            .asList( new String[] {
//                "install",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
//                            new Properties(), basedir );

        // jar muss reincoliert sein
        assertTrue( getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/maven-core-98.0.jar" )
            .exists() );

        Xpp3Dom applicationXml = Xpp3DomBuilder
            .build( new FileReader(
                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF/application.xml" ) ) );
        Xpp3Dom modulesmapsXml = Xpp3DomBuilder
            .build( new FileReader(
                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF/.modulemaps" ) ) );

        assertNotNull( modulesmapsXml );

        Xpp3Dom webappModule = applicationXml.getChildren( "module" )[0];
        Xpp3Dom ejbModule = applicationXml.getChildren( "module" )[1];
        if ( webappModule.getChild( "web" ) == null )
        {
            webappModule = applicationXml.getChildren( "module" )[1];
            ejbModule = applicationXml.getChildren( "module" )[0];
        }

        assertEquals( "project-rad-2.war", webappModule.getChild( "web" ).getChild( "web-uri" ).getValue() );
        assertEquals( "project-rad-2", webappModule.getChild( "web" ).getChild( "context-root" ).getValue() );
        assertEquals( "project-rad-3.jar", ejbModule.getChild( "ejb" ).getValue() );

    }

    public void testProject5_2()
        throws Exception
    {
        File basedir = getTestFile( "src/test/resources/projects/project-rad-5" );

        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );
        FileUtils.copyDirectory( new File( basedir, "project-rad-1/META-INF-2" ),
                                 new File( basedir, "project-rad-1/META-INF" ) );

        File pom0 = new File( basedir, "pom.xml" );
        File pom1 = new File( basedir, "project-rad-1/pom.xml" );
        File pom2 = new File( basedir, "project-rad-2/pom.xml" );
        File pom3 = new File( basedir, "project-rad-3/pom.xml" );
        
        MavenProject project = readProject( pom0 );
        MavenProject project1 = readProject( pom1 );
        MavenProject project2 = readProject( pom2 );
        MavenProject project3 = readProject( pom3 );

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        String outputDirPath = IdeUtils.getPluginSetting( project, "maven-eclipse-plugin", "outputDir", null );
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
        executeMaven( pom2, props, goals );
        executeMaven( pom3, props, goals );
        
        executeMaven( pom1, props, goals );
        executeMaven( pom2, props, goals );
        executeMaven( pom3, props, goals );
        
//        this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
//            .asList( new String[] {
//                "install",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
//                            new Properties(), basedir );
//        
//        this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
//            .asList( new String[] {
//                "install",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
//                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
//                            new Properties(), basedir );

        assertTrue( getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/maven-core-98.0.jar" )
            .exists() );

        File application1 = new File(
                                      "src/test/resources/projects/project-rad-5/project-rad-1/META-INF/application.xml" );
        File application2 = new File(
                                      "src/test/resources/projects/project-rad-5/project-rad-1/META-INF-2/application.xml" );
        File modulemaps1 = new File( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF/.modulemaps" );
        File modulemaps2 = new File( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF-2/.modulemaps" );

        assertNotNull( modulemaps1 );
        assertNotNull( modulemaps2 );

        Xpp3Dom applicationXml1 = Xpp3DomBuilder.build( new FileReader( application1 ) );

        Xpp3Dom webappModule1 = applicationXml1.getChildren( "module" )[0];
        Xpp3Dom ejbModule1 = applicationXml1.getChildren( "module" )[1];
        if ( webappModule1.getChild( "web" ) == null )
        {
            webappModule1 = applicationXml1.getChildren( "module" )[1];
            ejbModule1 = applicationXml1.getChildren( "module" )[0];
        }

        Xpp3Dom applicationXml2 = Xpp3DomBuilder.build( new FileReader( application2 ) );

        Xpp3Dom webappModule2 = applicationXml2.getChildren( "module" )[0];
        Xpp3Dom ejbModule2 = applicationXml2.getChildren( "module" )[1];
        if ( webappModule2.getChild( "web" ) == null )
        {
            webappModule2 = applicationXml2.getChildren( "module" )[0];
            ejbModule2 = applicationXml2.getChildren( "module" )[1];
        }

        assertEquals( webappModule1.getAttribute( "id" ), webappModule2.getAttribute( "id" ) );
        assertEquals( ejbModule1.getAttribute( "id" ), ejbModule2.getAttribute( "id" ) );

    }

}
