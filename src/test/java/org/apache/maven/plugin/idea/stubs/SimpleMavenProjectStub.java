package org.apache.maven.plugin.idea.stubs;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class SimpleMavenProjectStub
    extends MavenProjectStub
{
    private List collectedProjects;

    private Build build;

    private List testArtifacts;

    public SimpleMavenProjectStub()
    {
        TestCounter.nextCount();

        build = new Build();
        build.setDirectory( getBasedir().getAbsolutePath() + "/target" );
        build.setOutputDirectory( getBasedir().getAbsolutePath() + "/target/classes" );
        build.setTestOutputDirectory( getBasedir().getAbsolutePath() + "/target/test-classes" );
    }

    public String getGroupId()
    {
        return "org.apache.maven.plugin.test";
    }

    public String getArtifactId()
    {
        return "plugin-test-" + TestCounter.currentCount();
    }

    public String getVersion()
    {
        return String.valueOf( TestCounter.currentCount() );
    }

    public File getBasedir()
    {
        File basedir = new File( "target/test-harness/" + TestCounter.currentCount() );

        if ( !basedir.exists() )
        {
            basedir.mkdirs();
        }

        return basedir;
    }

    public List getCollectedProjects()
    {
        if ( collectedProjects == null )
        {
            collectedProjects = new ArrayList();
        }
        return collectedProjects;
    }

    public void setCollectedProjects( List list )
    {
        collectedProjects = list;
    }

    public List getDependencies()
    {
        List dependencies = new ArrayList();

        Dependency dep = new Dependency();
        dep.setGroupId( "org.apache.maven" );
        dep.setArtifactId( "maven-model" );
        dep.setVersion( "2.0.1" );
        dep.setScope( Artifact.SCOPE_COMPILE );
        dependencies.add( dep );

        return dependencies;
    }

    public Artifact getArtifact()
    {
        Artifact artifact = new IdeaArtifactStub();

        artifact.setGroupId( getGroupId() );

        artifact.setArtifactId( getArtifactId() );

        artifact.setVersion( getVersion() );

        return artifact;
    }

    public Build getBuild()
    {
        return build;
    }

    public List getRemoteArtifactRepositories()
    {
        return Collections.EMPTY_LIST;
    }

    public List getCompileSourceRoots()
    {
        return Collections.singletonList( getBasedir().getAbsolutePath() + "/src/main/java" );
    }

    public List getTestArtifacts()
    {
        if ( testArtifacts == null )
        {
            testArtifacts = new ArrayList();

            testArtifacts.add( createArtifact( "junit", "junit", "1.0.4" ) );
        }

        return testArtifacts;
    }

    public void setTestArtifacts( List artifacts )
    {
        testArtifacts = artifacts;
    }

    public List getTestCompileSourceRoots()
    {
        return Collections.singletonList( getBasedir().getAbsolutePath() + "/src/test/java" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        Artifact artifact = new IdeaArtifactStub();

        artifact.setGroupId( groupId );
        artifact.setArtifactId( artifactId );
        artifact.setVersion( version );

        return artifact;
    }
}
