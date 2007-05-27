package org.apache.maven.report.projectinfo.stubs;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public abstract class ProjectInfoProjectStub
    extends MavenProjectStub
{
    private Model model;

    private Build build;

    /**
     * Default constructor
     */
    public ProjectInfoProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir(), getPOM() ) ) );
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

        Artifact artifact = new ProjectInfoPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(),
                                                               getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( super.getBasedir() + "/target/test/unit/" + model.getArtifactId() + "/target" );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build
            .setOutputDirectory( super.getBasedir() + "/target/test/unit/" + model.getArtifactId() + "/target/classes" );
        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        build.setTestOutputDirectory( super.getBasedir() + "/target/test/unit/" + model.getArtifactId()
            + "/target/test-classes" );
        setBuild( build );

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List testCompileSourceRoots = new ArrayList();
        testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
        setTestCompileSourceRoots( testCompileSourceRoots );
    }

    /**
     * @return the POM file name
     */
    protected abstract String getPOM();

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getModel()
     */
    public Model getModel()
    {
        return model;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getBuild()
     */
    public Build getBuild()
    {
        return build;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#setBuild(org.apache.maven.model.Build)
     */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getBasedir()
     */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/plugin-configs/" );
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getArtifacts()
     */
    public Set getArtifacts()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getRemoteArtifactRepositories()
     */
    public List getRemoteArtifactRepositories()
    {
        ArtifactRepository repository = new DefaultArtifactRepository( "central", "http://repo1.maven.org/maven2",
                                                                       new DefaultRepositoryLayout() );

        return Collections.singletonList( repository );
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getDependencyArtifacts()
     */
    public Set getDependencyArtifacts()
    {
        return Collections.singleton( new DefaultArtifact( "junit", "junit", VersionRange.createFromVersion( "3.8.1" ),
                                                           Artifact.SCOPE_TEST, "jar", null,
                                                           new DefaultArtifactHandler( "jar" ), false ) );
    }
}
