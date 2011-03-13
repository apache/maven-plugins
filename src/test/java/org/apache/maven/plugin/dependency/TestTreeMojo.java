package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * Tests <code>TreeMojo</code>.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 2.0
 */
public class TestTreeMojo
    extends AbstractDependencyMojoTestCase
{
    // TestCase methods -------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.testing.AbstractMojoTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "tree", false );
    }

    // tests ------------------------------------------------------------------

    /**
     * Tests the proper discovery and configuration of the mojo.
     *
     * @throws Exception
     */
    public void testTreeTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/tree-test/plugin-config.xml" );
        TreeMojo mojo = (TreeMojo) lookupMojo( "tree", testPom );
        setVariableValueToObject( mojo, "localRepository", new StubArtifactRepository( testDir.getAbsolutePath() ) );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();
        project.setArtifact( this.stubFactory.createArtifact( "testGroupId", "project", "1.0" ) );

        Set<Artifact> artifacts = this.stubFactory.getScopedArtifacts();
        Set<Artifact> directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.execute();

        DependencyNode rootNode = mojo.getDependencyTree();
        assertNodeEquals( "testGroupId:project:jar:1.0:compile", rootNode );
        assertEquals( 2, rootNode.getChildren().size() );
        assertChildNodeEquals( "testGroupId:snapshot:jar:2.0-SNAPSHOT:compile", rootNode, 0 );
        assertChildNodeEquals( "testGroupId:release:jar:1.0:compile", rootNode, 1 );
    }

    /**
     * Test the DOT format serialization
     *
     * @throws Exception
     */
    public void testTreeDotSerializing()
        throws Exception
    {
        List<String> contents = runTreeMojo( "tree1.dot", "dot" );
        assertTrue( findString( contents, "digraph \"testGroupId:project:jar:1.0:compile\" {" ) );
        assertTrue( findString( contents,
                                "\"testGroupId:project:jar:1.0:compile\" -> \"testGroupId:snapshot:jar:2.0-SNAPSHOT:compile\"" ) );
        assertTrue( findString( contents,
                                "\"testGroupId:project:jar:1.0:compile\" -> \"testGroupId:release:jar:1.0:compile\"" ) );
    }

    /**
     * Test the GraphML format serialization
     *
     * @throws Exception
     */
    public void testTreeGraphMLSerializing()
        throws Exception
    {
        List<String> contents = runTreeMojo( "tree1.graphml", "graphml" );

        assertTrue( findString( contents, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ) );
        assertTrue( findString( contents, "<y:NodeLabel>testGroupId:project:jar:1.0:compile</y:NodeLabel>" ) );
        assertTrue( findString( contents, "<y:NodeLabel>testGroupId:snapshot:jar:2.0-SNAPSHOT:compile</y:NodeLabel>" ) );
        assertTrue( findString( contents, "<y:NodeLabel>testGroupId:release:jar:1.0:compile</y:NodeLabel>" ) );
        assertTrue( findString( contents, "<key for=\"node\" id=\"d0\" yfiles.type=\"nodegraphics\"/>" ) );
        assertTrue( findString( contents, "<key for=\"edge\" id=\"d1\" yfiles.type=\"edgegraphics\"/>" ) );
    }

    /**
     * Test the TGF format serialization
     *
     * @throws Exception
     */
    public void testTreeTGFSerializing()
        throws Exception
    {
        List<String> contents = runTreeMojo( "tree1.tgf", "tgf" );
        assertTrue( findString( contents, "testGroupId:project:jar:1.0:compile" ) );
        assertTrue( findString( contents, "testGroupId:snapshot:jar:2.0-SNAPSHOT:compile" ) );
        assertTrue( findString( contents, "testGroupId:release:jar:1.0:compile" ) );
    }

    /**
     * Help finding content in the given list of string
     * @param outputFile
     * @param format
     * @return list of strings in the output file
     */
    private List<String> runTreeMojo( String outputFile, String format )
             throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/tree-test/plugin-config.xml" );
        String outputFileName = testDir.getAbsolutePath() + outputFile;
        TreeMojo mojo = (TreeMojo) lookupMojo( "tree", testPom );
        setVariableValueToObject( mojo, "localRepository", new StubArtifactRepository( testDir.getAbsolutePath() ) );
        setVariableValueToObject( mojo, "outputType", format );
        setVariableValueToObject( mojo, "outputFile", new File( outputFileName ) );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();
        project.setArtifact( this.stubFactory.createArtifact( "testGroupId", "project", "1.0" ) );

        Set<Artifact> artifacts = this.stubFactory.getScopedArtifacts();
        Set<Artifact> directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.execute();

        BufferedReader fp1 = new BufferedReader( new FileReader( outputFileName ) );
        List<String> contents = new ArrayList<String>();

        String line = null;
        while ( ( line = fp1.readLine() ) != null )
        {
            contents.add( line );
        }
        fp1.close();

        return contents ;
    }

    /**
     * Help finding content in the given list of string
     * @param contents
     * @param str
     */
    private boolean findString( List<String> contents, String str )
    {
        for ( String line : contents )
        {
            if ( line.indexOf( str ) != -1 )
            {
                // if match then return here
                return true;
            }
        }

        // in case no match for the whole list
        return false;
    }

    // private methods --------------------------------------------------------

    private void assertChildNodeEquals( String expectedNode, DependencyNode actualParentNode, int actualChildIndex )
    {
        DependencyNode actualNode = (DependencyNode) actualParentNode.getChildren().get( actualChildIndex );

        assertNodeEquals( expectedNode, actualNode );
    }

    private void assertNodeEquals( String expectedNode, DependencyNode actualNode )
    {
        String[] tokens = expectedNode.split( ":" );

        assertNodeEquals( tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], actualNode );
    }

    private void assertNodeEquals( String expectedGroupId, String expectedArtifactId, String expectedType,
                                   String expectedVersion, String expectedScope, DependencyNode actualNode )
    {
        Artifact actualArtifact = actualNode.getArtifact();

        assertEquals( "group id", expectedGroupId, actualArtifact.getGroupId() );
        assertEquals( "artifact id", expectedArtifactId, actualArtifact.getArtifactId() );
        assertEquals( "type", expectedType, actualArtifact.getType() );
        assertEquals( "version", expectedVersion, actualArtifact.getVersion() );
        assertEquals( "scope", expectedScope, actualArtifact.getScope() );
    }
}
