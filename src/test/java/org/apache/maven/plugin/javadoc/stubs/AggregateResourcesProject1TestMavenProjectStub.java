package org.apache.maven.plugin.javadoc.stubs;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AggregateResourcesProject1TestMavenProjectStub
    extends MavenProjectStub
{
    public AggregateResourcesProject1TestMavenProjectStub()
    {
        readModel( new File( getBasedir(), "pom.xml" ) );

        setGroupId( getModel().getGroupId() );
        setArtifactId( getModel().getArtifactId() );
        setVersion( getModel().getVersion() );
        setName( getModel().getName() );
        setUrl( getModel().getUrl() );
        setPackaging( getModel().getPackaging() );

        setExecutionRoot( true );

        Artifact artifact = new JavadocPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

        Build build = new Build();
        build.setFinalName( getModel().getArtifactId() );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/aggregate-resources-test/project1/target" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<String>();
        compileSourceRoots.add( getBasedir().getAbsolutePath() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/aggregate-resources-test/project1" );
    }

    /** {@inheritDoc} */
    public MavenProject getExecutionProject()
    {
        return this;
    }
}
