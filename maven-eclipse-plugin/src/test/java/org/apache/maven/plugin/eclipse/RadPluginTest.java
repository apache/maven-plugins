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

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
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
        assertFileEquals( this.localRepositoryDir.getCanonicalPath(), generatedManifest, expectedManifest );

    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-rad-3", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( "src/test/resources/projects/project-rad-3/ejbModule/META-INF/MANIFEST.MF" );
        File expectedManifest = getTestFile( "src/test/resources/projects/project-rad-3/ejbModule/META-INF/expected_MANIFEST.MF" );
        assertFileEquals( this.localRepositoryDir.getCanonicalPath(), generatedManifest, expectedManifest );
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

        FileUtils.deleteDirectory( getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF" ) );
        getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF" ).mkdir();

        MavenProject project = this.maven.readProjectWithDependencies( new File( basedir, "pom.xml" ) );
        MavenProject project2 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-2" ),
                                                    "pom.xml" ) );
        MavenProject project3 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-3" ),
                                                    "pom.xml" ) );

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
        this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
            .asList( new String[] {
                "install",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            new Properties(), basedir );
        MavenProject project1 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1" ),
                                                    "pom.xml" ) );
        this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
            .asList( new String[] {
                "install",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            new Properties(), basedir );

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

        FileUtils.deleteDirectory( getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF" ) );
        FileUtils.copyDirectory( getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF-2" ),
                                 getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1/META-INF" ) );

        File basedir = getTestFile( "src/test/resources/projects/project-rad-5" );
        MavenProject project = this.maven.readProjectWithDependencies( new File( basedir, "pom.xml" ) );
        MavenProject project2 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-2" ),
                                                    "pom.xml" ) );
        MavenProject project3 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-3" ),
                                                    "pom.xml" ) );

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
        this.maven.execute( Arrays.asList( new MavenProject[] { project, project2, project3 } ), Arrays
            .asList( new String[] {
                "install",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            new Properties(), basedir );
        MavenProject project1 = this.maven
            .readProjectWithDependencies( new File(
                                                    getTestFile( "src/test/resources/projects/project-rad-5/project-rad-1" ),
                                                    "pom.xml" ) );
        this.maven.execute( Arrays.asList( new MavenProject[] { project1, project2, project3 } ), Arrays
            .asList( new String[] {
                "install",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad-clean",
                "org.apache.maven.plugins:maven-eclipse-plugin:current:rad" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            new Properties(), basedir );

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
