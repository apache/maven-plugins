package org.apache.maven.plugins.source.stubs;

import static org.apache.maven.plugins.source.stubs.Project001Stub.readModelFromFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;

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

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class Project007Stub
    extends MavenProjectStub
{
    private Build build;

    private List<Resource> resources;

    private List<Resource> testResources;

    public Project007Stub()
    {
        Model model;
        try
        {
            model = readModelFromFile( new File( getBasedir(), "target/test-classes/unit/project-007/pom.xml" ) );
            setModel( model );

            setGroupId( model.getGroupId() );
            setArtifactId( model.getArtifactId() );
            setVersion( model.getVersion() );
            setName( model.getName() );
            setUrl( model.getUrl() );
            setPackaging( model.getPackaging() );

            Build build = new Build();
            build.setFinalName( getArtifactId() + "-" + getVersion() );
            build.setDirectory( getBasedir() + "/target/test/unit/project-007/target" );
            setBuild( build );

            String basedir = getBasedir().getAbsolutePath();
            List<String> compileSourceRoots = new ArrayList<String>();
            compileSourceRoots.add( basedir + "/target/test-classes/unit/project-007/src/main/java" );
            setCompileSourceRoots( compileSourceRoots );

            List<String> testCompileSourceRoots = new ArrayList<String>();
            testCompileSourceRoots.add( basedir + "/target/test-classes/unit/project-007/src/test/java" );
            setTestCompileSourceRoots( testCompileSourceRoots );

            setResources( model.getBuild().getResources() );
            setTestResources( model.getBuild().getTestResources() );

            SourcePluginArtifactStub artifact =
                new SourcePluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging(), null );
            artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
            artifact.setType( "jar" );
            artifact.setBaseVersion( "1.0-SNAPSHOT" );
            setArtifact( artifact );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public Build getBuild()
    {
        return build;
    }

    public void setBuild( Build build )
    {
        this.build = build;
    }

    public List<Resource> getResources()
    {
        return resources;
    }

    public void setResources( List<Resource> resources )
    {
        this.resources = resources;
    }

    public List<Resource> getTestResources()
    {
        return testResources;
    }

    public void setTestResources( List<Resource> testResources )
    {
        this.testResources = testResources;
    }
}
