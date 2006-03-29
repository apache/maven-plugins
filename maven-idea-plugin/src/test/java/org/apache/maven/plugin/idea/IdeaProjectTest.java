package org.apache.maven.plugin.idea;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.idea.stubs.SimpleMavenProjectStub;
import org.apache.maven.plugins.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class IdeaProjectTest
    extends AbstractMojoTestCase
{
    public void testIdeaProjectTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/plugin-configs/min-plugin-config.xml" );

        IdeaProjectMojo mojo = (IdeaProjectMojo) lookupMojo( "project", testPom );

        assertNotNull( "Get project mojo instance using " + testPom.getAbsolutePath() , mojo );

        setVariableValueToObject( mojo, "artifactMetadataSource",
                                  lookup( "org.apache.maven.artifact.metadata.ArtifactMetadataSource", "maven" ) );

        setVariableValueToObject( mojo, "artifactFactory",
                                  lookup( "org.apache.maven.artifact.factory.ArtifactFactory" ) );

        setVariableValueToObject( mojo, "artifactResolver",
                                  lookup( "org.apache.maven.artifact.resolver.ArtifactResolver" ) );

        mojo.execute();

        int testCounter = SimpleMavenProjectStub.getUsageCounter();

        assertTrue( "Project file was created", new File( "target/test-harness/" + testCounter +
            "/plugin-test-" + testCounter + ".ipr" ).exists() );
    }

    public void testIdeaProjectVersion5()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/plugin-configs/plugin-config-idea5.xml" );

        IdeaProjectMojo mojo = (IdeaProjectMojo) lookupMojo( "project", testPom );

        assertNotNull( "Get project mojo instance using " + testPom.getAbsolutePath() , mojo );

        setVariableValueToObject( mojo, "artifactMetadataSource",
                                  lookup( "org.apache.maven.artifact.metadata.ArtifactMetadataSource", "maven" ) );

        setVariableValueToObject( mojo, "artifactFactory",
                                  lookup( "org.apache.maven.artifact.factory.ArtifactFactory" ) );

        setVariableValueToObject( mojo, "artifactResolver",
                                  lookup( "org.apache.maven.artifact.resolver.ArtifactResolver" ) );

        mojo.execute();

        int testCounter = SimpleMavenProjectStub.getUsageCounter();

        assertTrue( "Project file was created", new File( "target/test-harness/" + testCounter +
            "/plugin-test-" + testCounter + ".ipr" ).exists() );
    }

    public void testIdeaProjectJdkName()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/plugin-configs/plugin-config-jdkName.xml" );

        IdeaProjectMojo mojo = (IdeaProjectMojo) lookupMojo( "project", testPom );

        setVariableValueToObject( mojo, "artifactMetadataSource",
                                  lookup( "org.apache.maven.artifact.metadata.ArtifactMetadataSource", "maven" ) );

        setVariableValueToObject( mojo, "artifactFactory",
                                  lookup( "org.apache.maven.artifact.factory.ArtifactFactory" ) );

        setVariableValueToObject( mojo, "artifactResolver",
                                  lookup( "org.apache.maven.artifact.resolver.ArtifactResolver" ) );

        assertNotNull( "Get project mojo instance using " + testPom.getAbsolutePath() , mojo );

        mojo.execute();

        int testCounter = SimpleMavenProjectStub.getUsageCounter();

        assertTrue( "Project file was created", new File( "target/test-harness/" + testCounter +
            "/plugin-test-" + testCounter + ".ipr" ).exists() );
    }
}
