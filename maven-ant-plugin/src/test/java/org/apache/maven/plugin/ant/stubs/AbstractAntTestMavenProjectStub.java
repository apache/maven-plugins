package org.apache.maven.plugin.ant.stubs;

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
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractAntTestMavenProjectStub
    extends MavenProjectStub
{
    private Build build;

    /**
     * Default
     */
    public AbstractAntTestMavenProjectStub()
    {
        File antTestDir = new File( PlexusTestCase.getBasedir() + "/src/test/resources/unit/" + getProjetPath() + "/" );

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( antTestDir, "pom.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );

        build = new Build();
        Resource resource = new Resource();

        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir().getAbsolutePath() + "/target" );

        build.setSourceDirectory( antTestDir + "/src/main/java" );
        resource.setDirectory( antTestDir + "/src/main/resources" );
        build.setResources( Collections.singletonList( resource ) );
        build.setOutputDirectory( getBasedir().getAbsolutePath() + "/target/classes" );

        build.setTestSourceDirectory( antTestDir + "/src/test/java" );
        resource = new Resource();
        resource.setDirectory( antTestDir + "/src/test/resources" );
        build.setTestResources( Collections.singletonList( resource ) );
        build.setTestOutputDirectory( getBasedir().getAbsolutePath() + "/target/test-classes" );

        setBuild( build );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getBuild()
     */
    public Build getBuild()
    {
        return build;
    }

    /**
     * @see org.apache.maven.project.MavenProject#getBasedir()
     */
    public File getBasedir()
    {
        File basedir = new File( PlexusTestCase.getBasedir(), "/target/test/unit/" + getProjetPath() + "/" );

        if ( !basedir.exists() )
        {
            basedir.mkdirs();
        }

        return basedir;
    }

    /**
     * @see org.apache.maven.project.MavenProject#getCompileSourceRoots()
     */
    public List getCompileSourceRoots()
    {
        File src = new File( PlexusTestCase.getBasedir() + "/src/test/resources/unit/" + getProjetPath() + "src/main/java" );
        return Collections.singletonList( src.getAbsolutePath() );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getTestCompileSourceRoots()
     */
    public List getTestCompileSourceRoots()
    {
        File test = new File( PlexusTestCase.getBasedir() + "/src/test/resources/unit/" + getProjetPath() + "src/test/java" );
        return Collections.singletonList( test.getAbsolutePath() );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getCompileArtifacts()
     */
    public List getCompileArtifacts()
    {
        Artifact junit = new DefaultArtifact( "junit", "junit", VersionRange.createFromVersion( "3.8.1" ),
                                              Artifact.SCOPE_TEST, "jar", null, new DefaultArtifactHandler( "jar" ),
                                              false );
        junit.setFile( new File( "junit/junit/3.8.1/junit-3.8.1.jar" ) );

        return Collections.singletonList( junit );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getTestArtifacts()
     */
    public List getTestArtifacts()
    {
        Artifact junit = new DefaultArtifact( "junit", "junit", VersionRange.createFromVersion( "3.8.1" ),
                                              Artifact.SCOPE_TEST, "jar", null, new DefaultArtifactHandler( "jar" ),
                                              false );
        junit.setFile( new File( "junit/junit/3.8.1/junit-3.8.1.jar" ) );

        return Collections.singletonList( junit );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getRepositories()
     */
    public List getRepositories()
    {
        Repository repo = new Repository();
        repo.setId( "central" );
        repo.setName( "central" );
        repo.setUrl( "http://repo1.maven.org/maven2" );

        return Collections.singletonList( repo );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getProperties()
     */
    public Properties getProperties()
    {
        return getModel().getProperties();
    }

    /**
     * @return the project path from <code>src/test/resources/unit</code> directory
     */
    public abstract String getProjetPath();
}
