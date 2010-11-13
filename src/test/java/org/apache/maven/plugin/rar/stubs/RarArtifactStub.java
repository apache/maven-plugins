package org.apache.maven.plugin.rar.stubs;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class RarArtifactStub
    extends ArtifactStub
{
    private String groupId;
    
    private String artifactId;
    
    private String version;
    
    private String scope;
    
    private boolean optional;
    
    private File file;

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }
    
    public String getBaseVersion()
    {
        return getVersion();
    }

    public VersionRange getVersionRange()
    {
        return VersionRange.createFromVersion( this.version );
    }

    public ArtifactHandler getArtifactHandler()
    {
        return new ArtifactHandler()
        {
            
            public boolean isIncludesDependencies()
            {
                return false;
            }
            
            public boolean isAddedToClasspath()
            {
                return false;
            }
            
            public String getPackaging()
            {
                return null;
            }
            
            public String getLanguage()
            {
                return null;
            }
            
            public String getExtension()
            {
                return null;
            }
            
            public String getDirectory()
            {
                return null;
            }
            
            public String getClassifier()
            {
                return null;
            }
        };
    }

    public String getType()
    {
        return "rar";
    }
    
    
}
