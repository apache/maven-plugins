package org.apache.maven.plugin.war.stub;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MavenProjectArtifactsStub
    extends MavenProjectBasicStub
{
    TreeSet artifacts;

    public MavenProjectArtifactsStub()
        throws Exception
    {
        artifacts = new TreeSet();
    }

    public void addArtifact( ArtifactStub stub )
    {
        artifacts.add( stub );
    }

    public Set getArtifacts()
    {
        return artifacts;
    }

    public List getDependencies()
    {
        if ( getArtifacts() == null )
        {
            return new ArrayList();
        }
        final List dependencies = new ArrayList();
        for (Object o : getArtifacts()) {
            Artifact a = (Artifact) o;
            Dependency dependency = new Dependency();
            dependency.setArtifactId(a.getArtifactId());
            dependency.setGroupId(a.getGroupId());
            dependency.setVersion(a.getVersion());
            dependency.setScope(a.getScope());
            dependency.setType(a.getType());
            dependency.setClassifier(a.getClassifier());
            dependencies.add(dependency);

        }
        return dependencies;

    }

    public List getRuntimeClasspathElements()
    {
        List artifacts = new ArrayList();

        artifacts.add(
            "src/test/resources/unit/manifest/manifest-with-classpath/sample-artifacts/maven-artifact1-1.0-SNAPSHOT.jar" );
        artifacts.add(
            "src/test/resources/unit/manifest/manifest-with-classpath/sample-artifacts/maven-artifact2-1.0-SNAPSHOT.jar" );

        return artifacts;
    }
}
