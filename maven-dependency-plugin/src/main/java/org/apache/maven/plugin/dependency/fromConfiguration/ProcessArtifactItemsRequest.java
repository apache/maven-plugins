package org.apache.maven.plugin.dependency.fromConfiguration;

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

/**
 * @author Olivier Lamy
 * @since 2.7
 */
public class ProcessArtifactItemsRequest
{
    /**
     * remove the version from the filename.
     */
    private boolean removeVersion;

    /** 
     * remove the classifier from the filename.
     */
    private boolean removeClassifier;
    
    /**
     * prepend the groupId to the filename.
     */
    private boolean prependGroupId;

    /**
     * use the baseVersion of the artifact instead of version for the filename.
     */
    private boolean useBaseVersion;

    public ProcessArtifactItemsRequest()
    {
        // no op
    }

    public ProcessArtifactItemsRequest( boolean removeVersion, boolean prependGroupId, boolean useBaseVersion, boolean removeClassifier )
    {
        this.removeVersion = removeVersion;
        this.prependGroupId = prependGroupId;
        this.useBaseVersion = useBaseVersion;
        this.removeClassifier = removeClassifier;
    }

    public boolean isRemoveVersion()
    {
        return removeVersion;
    }

    public void setRemoveVersion( boolean removeVersion )
    {
        this.removeVersion = removeVersion;
    }

    public boolean isRemoveClassifier()
    {
        return removeClassifier;
    }

    public void setRemoveClassifier( boolean removeClassifier )
    {
        this.removeClassifier = removeClassifier;
    }

    
    public ProcessArtifactItemsRequest removeVersion( boolean removeVersion )
    {
        this.removeVersion = removeVersion;
        return this;
    }

    public boolean isPrependGroupId()
    {
        return prependGroupId;
    }

    public void setPrependGroupId( boolean prependGroupId )
    {
        this.prependGroupId = prependGroupId;
    }

    public ProcessArtifactItemsRequest prependGroupId( boolean prependGroupId )
    {
        this.prependGroupId = prependGroupId;
        return this;
    }

    public boolean isUseBaseVersion()
    {
        return useBaseVersion;
    }

    public void setUseBaseVersion( boolean useBaseVersion )
    {
        this.useBaseVersion = useBaseVersion;
    }

    public ProcessArtifactItemsRequest useBaseVersion( boolean useBaseVersion )
    {
        this.useBaseVersion = useBaseVersion;
        return this;
    }
}
