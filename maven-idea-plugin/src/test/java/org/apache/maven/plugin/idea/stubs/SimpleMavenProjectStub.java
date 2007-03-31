package org.apache.maven.plugin.idea.stubs;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;

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

    private List remoteRepositories;

    public SimpleMavenProjectStub()
    {
        TestCounter.nextCount();

        build = new Build();
        build.setDirectory( getBasedir().getAbsolutePath() + "/target" );
        build.setOutputDirectory( getBasedir().getAbsolutePath() + "/target/classes" );
        build.setTestOutputDirectory( getBasedir().getAbsolutePath() + "/target/test-classes" );

        Resource resource = new Resource();
        resource.setDirectory( "src/main/resources" );
        resource.setFiltering( false );
        build.setResources( Collections.singletonList( resource ) );

        resource = new Resource();
        resource.setFiltering( false );
        resource.setDirectory( "src/test/resources" );
        build.setTestResources( Collections.singletonList( resource ) );
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
        File basedir = new File( PlexusTestCase.getBasedir(), "target/test-harness/" + TestCounter.currentCount() );

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

    public boolean isExecutionRoot()
    {
        return true;
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

        dep = new Dependency();
        dep.setGroupId( "junit" );
        dep.setArtifactId( "junit" );
        dep.setVersion( "3.8.1" );
        dep.setScope( Artifact.SCOPE_TEST );
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
        if ( remoteRepositories == null )
        {
            File testRepo = new File( PlexusTestCase.getBasedir(), "src/test/remote-repo" );
            ArtifactRepository repository = new DefaultArtifactRepository( "test-repo",
                                                                           "file://" + testRepo.getAbsolutePath(),
                                                                           new DefaultRepositoryLayout() );
            remoteRepositories = Collections.singletonList( repository );
        }

        return remoteRepositories;
    }

    public List getCompileSourceRoots()
    {
        File src = new File( getBasedir().getAbsolutePath() + "/src/main/java" );

        src.mkdirs();

        return Collections.singletonList( src.getAbsolutePath() );
    }

    public List getTestArtifacts()
    {
        if ( testArtifacts == null )
        {
            testArtifacts = new ArrayList();

            testArtifacts.add( createArtifact( "org.apache.maven", "maven-model", "2.0.1" ) );

            testArtifacts.add( createArtifact( "junit", "junit", "3.8.1" ) );
        }

        return testArtifacts;
    }

    public void setTestArtifacts( List artifacts )
    {
        testArtifacts = artifacts;
    }

    public List getTestCompileSourceRoots()
    {
        File src = new File( getBasedir().getAbsolutePath() + "/src/test/java" );

        src.mkdirs();

        return Collections.singletonList( src.getAbsolutePath() );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version )
    {
        Artifact artifact = new IdeaArtifactStub();

        artifact.setGroupId( groupId );
        artifact.setArtifactId( artifactId );
        artifact.setVersion( version );
        artifact.setFile( new File( PlexusTestCase.getBasedir(), "target/local-repo/" +
            artifact.getGroupId().replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() +
            "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar" ) );

        return artifact;
    }

    public List getBuildPlugins()
    {
        return build.getPlugins();
    }
}
