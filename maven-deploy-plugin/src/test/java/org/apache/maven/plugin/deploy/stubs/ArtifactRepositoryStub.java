package org.apache.maven.plugin.deploy.stubs;

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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

public class ArtifactRepositoryStub
    implements ArtifactRepository
{
    private boolean blacklisted;
    
    private ArtifactRepositoryLayout layout;
    
    private String url;
    
    public String pathOf( Artifact artifact )
    {
        return getLayout().pathOf( artifact );
    }
    
    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return getLayout().pathOfRemoteRepositoryMetadata( artifactMetadata );
    }
    
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return getLayout().pathOfLocalRepositoryMetadata( metadata, repository );
    }
    
    public String getUrl()
    {
        return url;
    }
    
    public void setAppendToUrl( String dir )
    {
        this.url = "file://" + System.getProperty( "basedir" ) + "/target/remote-repo/" + dir;
    }
    
    public String getBasedir()
    {
        return System.getProperty( "basedir" );
    }
    
    public String getProtocol()
    {
        return "file";
    }
    
    public String getId()
    {
        return "deploy-test";
    }
    
    public ArtifactRepositoryPolicy getSnapshots()
    {
        return new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                             ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }
    
    public ArtifactRepositoryPolicy getReleases()
    {
        return new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                             ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }
    
    public ArtifactRepositoryLayout getLayout()
    {
        if( layout != null )
        {
            return layout;
        }
        else
        {
            return new DefaultRepositoryLayout();
        }
    }
    
    public String getKey()
    {
        return getId();
    }

    public boolean isUniqueVersion()
    {
        return false;
    }
   
    public void setBlacklisted( boolean blackListed )
    {
        this.blacklisted = blackListed;
    }

    public boolean isBlacklisted()
    {
        return blacklisted;
    }
}
