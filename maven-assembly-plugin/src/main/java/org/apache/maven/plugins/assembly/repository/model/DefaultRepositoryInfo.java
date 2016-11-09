package org.apache.maven.plugins.assembly.repository.model;

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

import org.apache.maven.artifact.Artifact;

import java.util.List;

/**
 * 
 */
public class DefaultRepositoryInfo
    implements RepositoryInfo
{

    private boolean includeMetadata;

    private String scope = Artifact.SCOPE_COMPILE;

    private List<String> includes;

    private List<GroupVersionAlignment> groupVersionAlignments;

    private List<String> excludes;

    public List<String> getExcludes()
    {
        return excludes;
    }

    public void setGroupVersionAlignments( List<GroupVersionAlignment> groupVersionAlignments )
    {
        this.groupVersionAlignments = groupVersionAlignments;
    }

    public void setIncludeMetadata( boolean includeMetadata )
    {
        this.includeMetadata = includeMetadata;
    }

    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public List<GroupVersionAlignment> getGroupVersionAlignments()
    {
        return groupVersionAlignments;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public String getScope()
    {
        return scope;
    }

    public boolean isIncludeMetadata()
    {
        return includeMetadata;
    }

    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

}
