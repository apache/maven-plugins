package org.apache.maven.plugin.dependency.utils.filters;

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

import org.apache.maven.artifact.Artifact;

/**
 * Filter on GroupId Name.
 * 
 * @author clove
 * @see org.apache.maven.plugin.dependency.utils.filters.AbstractArtifactFeatureFilter
 * @since 2.0
 * 
 */
public class GroupIdFilter
    extends AbstractArtifactFeatureFilter
{

    private String includeScope;

    private String excludeScope;

    /**
     * Construction will setup the super call with a filtertype of 'GroupId'
     * 
     * @param include
     * @param exclude
     */
    public GroupIdFilter( String include, String exclude )
    {
        super( include, exclude, "GroupId" );
        this.includeScope = include;
        this.excludeScope = exclude;
    }

    /**
     * @return Returns the excludeScope.
     */
    public String getExcludeScope()
    {
        return excludeScope;
    }

    /**
     * @return Returns the includeScope.
     */
    public String getIncludeScope()
    {
        return includeScope;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.dependency.utils.filters.AbstractArtifactFeatureFilter#getArtifactFeature(org.apache.maven.artifact.Artifact)
     */
    protected String getArtifactFeature( Artifact artifact )
    {
        return artifact.getGroupId();
    }
}
