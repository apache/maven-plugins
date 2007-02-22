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

import java.io.File;
import java.io.StringWriter;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

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
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentNull()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, null );
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- null                                                                   -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentShort()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, "This is a short text" );
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- This is a short text                                                   -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentLong()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, "Maven is a software project management and comprehension tool. "
            + "Based on the concept of a project object model (POM), Maven can manage a project's build, reporting "
            + "and documentation from a central piece of information." );
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- Maven is a software project management and comprehension tool. Based   -->" ).append( '\n' );
        sb.append( "<!-- on the concept of a project object model (POM), Maven can manage a     -->" ).append( '\n' );
        sb.append( "<!-- project's build, reporting and documentation from a central piece of   -->" ).append( '\n' );
        sb.append( "<!-- information.                                                           -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }

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

        MavenProject project = maven.readProjectWithDependencies( testPom );

        assertEquals( AntBuildWriterUtil.getMavenCompilerPluginBasicOption( project, "debug", null ), "true" );

        assertNotNull( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "includes", null ) );
        assertEquals( AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "includes", null ).length, 2 );

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

        MavenProject project = maven.readProjectWithDependencies( testPom );

        assertEquals( AntBuildWriterUtil.getMavenWarPluginBasicOption( project, "warName", null ), "mywebapp" );
        assertTrue( AntBuildWriterUtil.getMavenWarPluginBasicOption( project, "webXml", null )
            .endsWith( "/src/main/webapp/WEB-INF/web.xml" ) );

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

        MavenProject project = maven.readProjectWithDependencies( testPom );

        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginBasicOption( project, "doclet", null ), "gr.spinellis.umlgraph.doclet.UmlGraphDoc" );

        assertNotNull( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "links", null ) );
        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "links", null ).length, 2 );

        assertNotNull( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "docletArtifacts", null ) );
        assertEquals( AntBuildWriterUtil.getMavenJavadocPluginOptions( project, "docletArtifacts", null ).length, 2 );

        maven.stop();
    }
}
