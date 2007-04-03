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

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class SourcePluginArtifactStub
    extends ArtifactStub
{

    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    private String classifier;

    public SourcePluginArtifactStub( String groupId, String artifactId, String version, String type, String classifier )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
    }

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

    public void setType( String packaging )
    {
        this.type = packaging;
    }

    public String getType()
    {
        return type;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return classifier != null;
    }

    public String getId()
    {
        String id = groupId + ":" + artifactId + ":" + type + ":";

        if ( hasClassifier() )
        {
            id = id + getClassifier() + ":";
        }

        id = id + version;

        return id;
    }

}
