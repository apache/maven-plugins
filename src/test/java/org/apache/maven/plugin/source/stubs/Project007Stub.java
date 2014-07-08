package org.apache.maven.plugin.source.stubs;

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
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.ReaderFactory;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class Project007Stub
    extends MavenProjectStub
{
    private Build build;

    private List resources;

    private List testResources;

    public Project007Stub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;

        try
        {
            model = pomReader.read(
                ReaderFactory.newXmlReader( new File( getBasedir(), "target/test-classes/unit/project-007/pom.xml" ) ) );
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
            List compileSourceRoots = new ArrayList();
            compileSourceRoots.add( basedir + "/target/test-classes/unit/project-007/src/main/java" );
            setCompileSourceRoots( compileSourceRoots );

            List testCompileSourceRoots = new ArrayList();
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

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public List getTestResources()
    {
        return testResources;
    }

    public void setTestResources( List testResources )
    {
        this.testResources = testResources;
    }
}
