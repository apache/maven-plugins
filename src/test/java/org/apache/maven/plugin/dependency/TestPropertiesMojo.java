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
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public class TestPropertiesMojo
    extends AbstractDependencyMojoTestCase
{
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "markers", true );
    }

    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testSetProperties()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/properties-test/plugin-config.xml" );
        PropertiesMojo mojo = (PropertiesMojo) lookupMojo( "properties", testPom );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        // this.assertNull( project.getProperties().getProperty( "org.apacha ) )
        mojo.execute();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            
            Artifact artifact = (Artifact) i.next();
            File artifactFile = artifact.getFile();
            assertNotNull( artifact.getDependencyConflictId() );
            assertTrue( artifactFile.isFile() );

        }

    }

}