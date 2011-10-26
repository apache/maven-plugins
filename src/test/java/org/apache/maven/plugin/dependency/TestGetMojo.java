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

import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;

public class TestGetMojo
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "markers", false );
    }

    /**
     * tests the proper discovery and configuration of the mojo
     * 
     * @throws Exception
     */
    public void testgetTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/get-test/plugin-config.xml" );
        assert testPom.exists();
        GetMojo mojo = (GetMojo) lookupMojo( "get", testPom );

        assertNotNull( mojo );

        setVariableValueToObject( mojo, "localRepository", new StubArtifactRepository( testDir.getAbsolutePath() ) );

        // Set properties, transitive = default value = true
        setVariableValueToObject( mojo, "transitive", Boolean.FALSE );
        setVariableValueToObject( mojo, "repositoryUrl", "http://repo1.maven.org/maven2" );
        setVariableValueToObject( mojo, "groupId", "org.apache.maven" );
        setVariableValueToObject( mojo, "artifactId", "maven-model" );
        setVariableValueToObject( mojo, "version", "2.0.9" );

        mojo.execute();

        // Set properties, transitive = false
        setVariableValueToObject( mojo, "transitive", Boolean.FALSE );
        mojo.execute();
    }
}
