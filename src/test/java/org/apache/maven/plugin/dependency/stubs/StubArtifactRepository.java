/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.maven.plugin.dependency.stubs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

public class StubArtifactRepository
    implements ArtifactRepository
{
    String baseDir = null;

    public StubArtifactRepository( String dir )
    {
        baseDir = dir;
    }

    public String pathOf( Artifact artifact )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUrl()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getBasedir()
    {
        return baseDir;
    }

    public String getProtocol()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getKey()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isUniqueVersion()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setBlacklisted( boolean blackListed )
    {
        // TODO Auto-generated method stub

    }

    public boolean isBlacklisted()
    {
        // TODO Auto-generated method stub
        return false;
    }

}
