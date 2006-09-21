package org.apache.maven.plugin.ant.stubs;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntTestMavenProjectStub
    extends MavenProjectStub
{
    private Build build;

    /**
     * Default
     */
    public AntTestMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( PlexusTestCase.getBasedir()
                + "/src/test/resources/unit/ant-test/ant-test-plugin-config.xml" ) ) );
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
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir().getAbsolutePath() + "/target" );
        build.setOutputDirectory( getBasedir().getAbsolutePath() + "/target/classes" );
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
        File basedir = new File( PlexusTestCase.getBasedir(), "/target/test/unit/ant-test/" );

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
        File src = new File( PlexusTestCase.getBasedir() + "/src/test/resources/unit/ant-test/src/main/java" );
        return Collections.singletonList( src.getAbsolutePath() );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getTestCompileSourceRoots()
     */
    public List getTestCompileSourceRoots()
    {
        File test = new File( PlexusTestCase.getBasedir() + "/src/test/resources/unit/ant-test/src/test/java" );
        return Collections.singletonList( test.getAbsolutePath() );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getArtifacts()
     */
    public Set getArtifacts()
    {
        Artifact junit = new DefaultArtifact( "junit", "junit", VersionRange.createFromVersion( "3.8.1" ),
                                              Artifact.SCOPE_TEST, "jar", null, new DefaultArtifactHandler( "jar" ),
                                              false );
        junit.setFile( new File( "junit/junit/3.8.1/junit-3.8.1.jar" ) );

        return Collections.singleton( junit );
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
}
