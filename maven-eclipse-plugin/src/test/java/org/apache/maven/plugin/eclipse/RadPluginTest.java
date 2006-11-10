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
    
    private static final String PROJECTS_BASEDIR = "target/test-classes/projects";
    
    public void testProject1()
        throws Exception
    {
        testProject( "project-rad-1", new Properties(), "rad-clean", "rad" );
    }

    public void testProject2()
        throws Exception
    {
        testProject( "project-rad-2", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( PROJECTS_BASEDIR + "/project-rad-2/src/main/webapp/META-INF/MANIFEST.MF" );
        File expectedManifest = getTestFile( PROJECTS_BASEDIR + "/project-rad-2/src/main/webapp/META-INF/expected_MANIFEST.MF" );
        assertFileEquals( localRepositoryDirectory.getCanonicalPath(), expectedManifest, generatedManifest );

    }

    public void testProject3()
        throws Exception
    {
        testProject( "project-rad-3", new Properties(), "rad-clean", "rad" );
        File generatedManifest = getTestFile( PROJECTS_BASEDIR + "/project-rad-3/ejbModule/META-INF/MANIFEST.MF" );
        File expectedManifest = getTestFile( PROJECTS_BASEDIR + "/project-rad-3/ejbModule/META-INF/expected_MANIFEST.MF" );
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
        goals.add( "install" );
        
        Properties props = new Properties();
        
        executeMaven( pom0, props, goals );
        
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
        assertTrue( new File( basedir, "project-rad-1/maven-core-98.0.jar" ).exists() );

        Xpp3Dom applicationXml = Xpp3DomBuilder
            .build( new FileReader( new File( basedir, "project-rad-1/META-INF/application.xml" ) ) );
        
        Xpp3Dom modulesmapsXml = Xpp3DomBuilder
            .build( new FileReader( new File( basedir, "project-rad-1/META-INF/.modulemaps" ) ) );

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
        assertEquals( "project-rad-5_3.jar", ejbModule.getChild( "ejb" ).getValue() );

    }

    public void testProject5_2()
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/project-rad-5" );

        FileUtils.deleteDirectory( new File( basedir, "project-rad-1/META-INF" ) );

        File pom0 = new File( basedir, "pom.xml" );
        
        MavenProject project = readProject( pom0 );

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

        assertTrue( new File( basedir, "project-rad-1/maven-core-98.0.jar" ).exists() );

        File modulemaps = new File( basedir, "project-rad-1/META-INF/.modulemaps" );

        assertNotNull( modulemaps );
        
        File application = new File( basedir, "project-rad-1/META-INF/application.xml" );
        
        Xpp3Dom applicationXml = Xpp3DomBuilder.build( new FileReader( application ) );
        
        Xpp3Dom[] children = applicationXml.getChildren( "module" );
        
        assertEquals( 2, children.length );
        
        boolean ejbVerified = false;
        boolean warVerified = false;
        
        for ( int i = 0; i < children.length; i++ )
        {
            Xpp3Dom child = children[i];
            
            if ( child.getAttribute( "id" ).startsWith( "WebModule_") )
            {
                assertEquals( "project-rad-5_2.war", child.getChild( "web" ).getChild( "web-uri" ).getValue() );
                warVerified = true;
            }
            else if ( child.getAttribute( "id" ).startsWith( "EjbModule_" ) )
            {
                assertEquals( "project-rad-5_3.jar", child.getChild( "ejb" ).getValue() );
                ejbVerified = true;
            }
        }
        
        assertTrue( warVerified );
        assertTrue( ejbVerified );
    }

}
