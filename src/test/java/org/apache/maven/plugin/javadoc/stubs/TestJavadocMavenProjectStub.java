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
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class TestJavadocMavenProjectStub
    extends MavenProjectStub
{
    public TestJavadocMavenProjectStub()
    {
        readModel( new File( getBasedir(), "test-javadoc-test-plugin-config.xml" ) );

        setGroupId( getModel().getGroupId() );
        setArtifactId( getModel().getArtifactId() );
        setVersion( getModel().getVersion() );
        setName( getModel().getName() );
        setUrl( getModel().getUrl() );
        setPackaging( getModel().getPackaging() );

        Artifact junit = new DefaultArtifact( "junit", "junit", VersionRange.createFromVersion( "3.8.1" ),
                                              Artifact.SCOPE_TEST, "jar", null, new DefaultArtifactHandler( "jar" ),
                                              false );
        junit.setFile( new File( getBasedir() + "/junit/junit/3.8.1/junit-3.8.1.jar" ) );
        setTestArtifacts( Collections.singletonList( junit ) );

        Build build = new Build();
        build.setFinalName( getModel().getArtifactId() );
        build.setDirectory( super.getBasedir() + "/target/test/unit/test-javadoc-test/target" );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setOutputDirectory( super.getBasedir() + "/target/test/unit/test-javadoc-test/target/classes" );
        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        build.setTestOutputDirectory( super.getBasedir() + "/target/test/unit/test-javadoc-test/target/test-classes" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<String>();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List<String> testCompileSourceRoots = new ArrayList<String>();
        testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
        setTestCompileSourceRoots( testCompileSourceRoots );
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/test-javadoc-test" );
    }

    /** {@inheritDoc} */
    public MavenProject getExecutionProject()
    {
        return this;
    }
}
