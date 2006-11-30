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

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

public class ProjectHelperStub
    implements MavenProjectHelper
{
    File artifactFile;

    String artifactType;

    String artifactClassifier;

    public File getArtifactFile()
    {
        return artifactFile;
    }

    public String getArtifactType()
    {
        return artifactType;
    }

    public String getArtifactClassifier()
    {
        return artifactClassifier;
    }

    public void attachArtifact( MavenProject project, File artifactFile, String artifactClassifier )
    {

    }

    public void attachArtifact( MavenProject project, String artifactType, File artifactFile )
    {

    }

    public void attachArtifact( MavenProject project, String _artifactType, String _artifactClassifier,
                               File _artifactFile )
    {
        artifactType = _artifactType;
        artifactClassifier = _artifactClassifier;
        artifactFile = _artifactFile;
    }

    public void addResource( MavenProject project, String resourceDirectory, List includes, List excludes )
    {

    }

    public void addTestResource( MavenProject project, String resourceDirectory, List includes, List excludes )
    {

    }
}
