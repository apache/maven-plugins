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

import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MavenProjectArtifactsStub
    extends MavenProjectBasicStub
{
    HashSet artifacts;

    public MavenProjectArtifactsStub()
        throws Exception
    {
        artifacts = new HashSet();
    }

    public void addArtifact( ArtifactStub stub )
    {
        artifacts.add( stub );
    }

    public Set getArtifacts()
    {
        return artifacts;
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
