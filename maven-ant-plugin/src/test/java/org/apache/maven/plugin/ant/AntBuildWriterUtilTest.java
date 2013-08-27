package org.apache.maven.plugin.ant;

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

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Map;

/**
 * Test cases for 'org.apache.maven.plugin.ant.AntBuildWriterUtil'
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntBuildWriterUtilTest
    extends PlexusTestCase
{
    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.getMavenCompilerPluginConfiguration(MavenProject, String, String)'
     *
     * @throws Exception
     */
    public void testGetMavenCompilerPluginConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/ant-compiler-config-test/pom.xml" );

        MavenEmbedder maven = new MavenEmbedder();
        maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        maven.setLogger( new MavenEmbedderConsoleLogger() );
        maven.setLocalRepositoryDirectory( getTestFile( "target/local-repo" ) );
        maven.setOffline( true );
        maven.start();

        MavenProject project = maven.readProject( testPom );

        assertEquals( AntBuildWriterUtil.getMavenCompilerPluginBasicOption( project, "debug", null ), "true" );

        assertNotNull( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "includes", null ) );
        assertEquals( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "includes", null ).length, 2 );
        assertNotNull( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "excludes", null ) );
        assertEquals( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "excludes", null ).length, 1 );

        maven.stop();
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.getMavenWarPluginConfiguration(MavenProject, String, String)'
     *
     * @throws Exception
     */
    public void testGetMavenWarPluginConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/ant-war-config-test/pom.xml" );

        MavenEmbedder maven = new MavenEmbedder();
        maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        maven.setLogger( new MavenEmbedderConsoleLogger() );
        maven.setLocalRepositoryDirectory( getTestFile( "target/local-repo" ) );
        maven.setOffline( true );
        maven.start();

        MavenProject project = maven.readProject( testPom );

        assertEquals( AntBuildWriterUtil.getMavenWarPluginBasicOption( project, "warName", null ), "mywebapp" );
        assertTrue( AntBuildWriterUtil.getMavenWarPluginBasicOption( project, "webXml", null ).endsWith(
            "/src/main/webapp/WEB-INF/web.xml" ) );

        maven.stop();
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.getMavenJavadocPluginConfiguration(MavenProject, String, String)'
     *
     * @throws Exception
     */
    public void testGetMavenJavadocPluginConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/ant-javadoc-test/pom.xml" );

        MavenEmbedder maven = new MavenEmbedder();
        maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        maven.setLogger( new MavenEmbedderConsoleLogger() );
        maven.setLocalRepositoryDirectory( getTestFile( "target/local-repo" ) );
        maven.setOffline( true );
        maven.start();

        MavenProject project = maven.readProject( testPom );

        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginBasicOption( project, "doclet", null ),
                      "gr.spinellis.umlgraph.doclet.UmlGraphDoc" );

        assertNotNull( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "links", null ) );
        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "links", null ).length, 2 );

        assertNotNull( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "docletArtifacts", null ) );
        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "docletArtifacts", null ).length, 2 );

        Map[] options = AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "tags", null );
        assertNotNull( options );
        assertEquals( options.length, 1 );
        assertEquals( 1, options[0].size() );
        Map properties = (Map) options[0].get( "tag" );
        assertNotNull( properties );
        assertEquals( "requirement", properties.get( "name" ) );
        assertEquals( "a", properties.get( "placement" ) );
        assertEquals( "Software Requirement:", properties.get( "head" ) );

        maven.stop();
    }

    /**
     * Test method for <code>AntBuildWriterUtil.getSingularForm(String)}</code>.
     *
     * @throws Exception
     */
    public static void testGetSingularForm()
        throws Exception
    {
        assertEquals( "property", AntBuildWriterUtil.getSingularForm( "properties" ) );
        assertEquals( "branch", AntBuildWriterUtil.getSingularForm( "branches" ) );
        assertEquals( "report", AntBuildWriterUtil.getSingularForm( "reports" ) );
        assertEquals( "", AntBuildWriterUtil.getSingularForm( "singular" ) );
        assertEquals( "", AntBuildWriterUtil.getSingularForm( null ) );
    }

    /**
     * Test method for <code>AntBuildWriterUtil.toRelative(File, String)</code>.
     *
     * @throws Exception
     */
    public static void testToRelative()
        throws Exception
    {
        assertEquals( "relative", AntBuildWriterUtil.toRelative( new File( "/home" ), "relative" ) );
        assertEquals( "dir",
                      AntBuildWriterUtil.toRelative( new File( "home" ), new File( "home/dir" ).getAbsolutePath() ) );
        assertEquals( "dir",
                      AntBuildWriterUtil.toRelative( new File( "/home" ), new File( "/home/dir" ).getAbsolutePath() ) );
        assertEquals( "dir/", AntBuildWriterUtil.toRelative( new File( "/home" ),
                                                             new File( "/home/dir" ).getAbsolutePath() + "/" ) );
        assertEquals( "dir/sub", AntBuildWriterUtil.toRelative( new File( "/home" ),
                                                                new File( "/home/dir/sub" ).getAbsolutePath() ) );
        assertEquals( ".",
                      AntBuildWriterUtil.toRelative( new File( "/home" ), new File( "/home" ).getAbsolutePath() ) );
        assertEquals( "./", AntBuildWriterUtil.toRelative( new File( "/home" ),
                                                           new File( "/home" ).getAbsolutePath() + "/" ) );
    }

}
