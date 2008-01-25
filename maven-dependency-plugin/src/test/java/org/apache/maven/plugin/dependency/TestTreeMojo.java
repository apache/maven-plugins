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
import java.util.Set;

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

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.execute();
        
        DependencyNode rootNode = mojo.getDependencyTree();
        assertNodeEquals( "testGroupId:project:jar:1.0:compile", rootNode);
        assertEquals( 2, rootNode.getChildren().size() );
        assertChildNodeEquals( "testGroupId:snapshot:jar:2.0-SNAPSHOT:compile", rootNode, 0 );
        assertChildNodeEquals( "testGroupId:release:jar:1.0:compile", rootNode, 1 );
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
