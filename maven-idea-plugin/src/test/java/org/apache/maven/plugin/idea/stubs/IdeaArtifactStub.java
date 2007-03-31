package org.apache.maven.plugin.idea.stubs;

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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class IdeaArtifactStub
    extends ArtifactStub
{
    private String groupId;

    private String artifactId;

    private String version;

    private File file;

    private String scope;


    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public String getType()
    {
        return "jar";
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return new DefaultArtifactVersion( getVersion() );
    }

    public String getId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getScope()
    {
        if ( scope == null )
        {
            scope = super.getScope();
        }

        return scope;
    }
}
