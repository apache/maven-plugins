package org.apache.maven.report.projectinfo.stubs;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public class ProjectInfoPluginArtifactStub
    extends ArtifactStub
{
    private String groupId;

    private String artifactId;

    private String version;

    private String packaging;

    private VersionRange versionRange;

    private ArtifactHandler handler;

    /**
     * @param groupId
     * @param artifactId
     * @param version
     * @param packaging
     */
    public ProjectInfoPluginArtifactStub( String groupId, String artifactId, String version, String packaging )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        versionRange = VersionRange.createFromVersion( version );
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#setGroupId(java.lang.String)
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getGroupId()
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#setArtifactId(java.lang.String)
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getArtifactId()
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#setVersion(java.lang.String)
     */
    public void setVersion( String version )
    {
        this.version = version;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getVersion()
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @param packaging
     */
    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    /**
     * @return the packaging
     */
    public String getPackaging()
    {
        return packaging;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getVersionRange()
     */
    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#setVersionRange(org.apache.maven.artifact.versioning.VersionRange)
     */
    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getArtifactHandler()
     */
    public ArtifactHandler getArtifactHandler()
    {
        return handler;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#setArtifactHandler(org.apache.maven.artifact.handler.ArtifactHandler)
     */
    public void setArtifactHandler( ArtifactHandler handler )
    {
        this.handler = handler;
    }
}
