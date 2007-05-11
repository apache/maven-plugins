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

import org.apache.maven.project.MavenProject;

public class TestBuildClasspathMojo
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "build-classpath", true );
    }

    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/build-classpath-test/plugin-config.xml" );
        BuildClasspathMojo mojo = (BuildClasspathMojo) lookupMojo( "build-classpath", testPom );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();

        // mojo.silent = true;
        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.execute();
        String file = null;
        try
        {
            file = mojo.readClasspathFile();
            
            fail( "Expected an illegal Argument Exception" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected to catch this.
        }

        mojo.setCpFile( new File(testDir,"buildClasspath.txt") );
        mojo.execute();
        
        file = mojo.readClasspathFile();
        assertNotNull( file );
        assertTrue( file.length() > 0 );

        assertTrue(file.contains( File.pathSeparator ));
        assertTrue(file.contains( File.separator ));
        
        String fileSep = "#####";
        String pathSep = "%%%%%";
        
        mojo.setFileSeparator( fileSep );
        mojo.setPathSeparator( pathSep );
        mojo.execute();
        
        file = mojo.readClasspathFile();
        assertNotNull( file );
        assertTrue( file.length() > 0 );

        assertFalse(file.contains( File.pathSeparator ));
        assertFalse(file.contains( File.separator ));
        assertTrue(file.contains( fileSep ));
        assertTrue(file.contains( pathSep ));
    }

}