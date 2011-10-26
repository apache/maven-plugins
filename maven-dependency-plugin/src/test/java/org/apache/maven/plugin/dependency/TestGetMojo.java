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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;

public class TestGetMojo
    extends AbstractDependencyMojoTestCase
{
    GetMojo mojo;

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "markers", false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/get-test/plugin-config.xml" );
        assert testPom.exists();
        mojo = (GetMojo) lookupMojo( "get", testPom );

        assertNotNull( mojo );
        setVariableValueToObject( mojo, "localRepository", new StubArtifactRepository( testDir.getAbsolutePath() ) );
    }

    /**
     * Test transitive parameter
     * 
     * @throws Exception
     */
    public void testTransitive()
        throws Exception
    {
        // Set properties, transitive = default value = true
        setVariableValueToObject( mojo, "transitive", Boolean.FALSE );
        setVariableValueToObject( mojo, "repositoryUrl", "http://repo1.maven.apache.org/maven2" );
        setVariableValueToObject( mojo, "groupId", "org.apache.maven" );
        setVariableValueToObject( mojo, "artifactId", "maven-model" );
        setVariableValueToObject( mojo, "version", "2.0.9" );

        mojo.execute();

        // Set properties, transitive = false
        setVariableValueToObject( mojo, "transitive", Boolean.FALSE );
        mojo.execute();
    }

    /**
     * Test remote repositories parameter
     * 
     * @throws Exception
     */
    public void testRemoteRepositories()
        throws Exception
    {
        setVariableValueToObject( mojo, "remoteRepositories", "central::default::http://repo1.maven.apache.org/maven2,"
            + "central::::http://repo1.maven.apache.org/maven2," + "http://repo1.maven.apache.org/maven2" );
        setVariableValueToObject( mojo, "groupId", "org.apache.maven" );
        setVariableValueToObject( mojo, "artifactId", "maven-model" );
        setVariableValueToObject( mojo, "version", "2.0.9" );

        mojo.execute();
    }

    /**
     * Test parsing of the remote repositories parameter
     * 
     * @throws Exception
     */
    public void testParseRepository()
        throws Exception
    {
        ArtifactRepository repo;
        ArtifactRepositoryPolicy policy = null;
        repo = mojo.parseRepository( "central::default::http://repo1.maven.apache.org/maven2", policy );
        assertEquals( "central", repo.getId() );
        assertEquals( DefaultRepositoryLayout.class, repo.getLayout().getClass() );
        assertEquals( "http://repo1.maven.apache.org/maven2", repo.getUrl() );

        repo = mojo.parseRepository( "central::legacy::http://repo1.maven.apache.org/maven2", policy );
        assertEquals( "central", repo.getId() );
        assertEquals( LegacyRepositoryLayout.class, repo.getLayout().getClass() );
        assertEquals( "http://repo1.maven.apache.org/maven2", repo.getUrl() );

        repo = mojo.parseRepository( "central::::http://repo1.maven.apache.org/maven2", policy );
        assertEquals( "central", repo.getId() );
        assertEquals( DefaultRepositoryLayout.class, repo.getLayout().getClass() );
        assertEquals( "http://repo1.maven.apache.org/maven2", repo.getUrl() );

        repo = mojo.parseRepository( "http://repo1.maven.apache.org/maven2", policy );
        assertEquals( "temp", repo.getId() );
        assertEquals( DefaultRepositoryLayout.class, repo.getLayout().getClass() );
        assertEquals( "http://repo1.maven.apache.org/maven2", repo.getUrl() );

        try
        {
            repo = mojo.parseRepository( "::::http://repo1.maven.apache.org/maven2", policy );
            fail( "Exception expected" );
        }
        catch ( MojoFailureException e )
        {
            // expected
        }

        try
        {
            repo = mojo.parseRepository( "central::http://repo1.maven.apache.org/maven2", policy );
            fail( "Exception expected" );
        }
        catch ( MojoFailureException e )
        {
            // expected
        }
    }
}
