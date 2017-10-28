package org.apache.maven.plugins.pmd.stubs;

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
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdPluginArtifactStub
    extends ArtifactStub
{
    private String groupId;

    private String artifactId;

    private String version;

    private String packaging;

    private VersionRange versionRange;

    private ArtifactHandler handler;

    public PmdPluginArtifactStub( String groupId, String artifactId, String version, String packaging )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        versionRange = VersionRange.createFromVersion( version );
    }

    /** {@inheritDoc} */
    @Override
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupId()
    {
        return groupId;
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /** {@inheritDoc} */
    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    /** {@inheritDoc} */
    @Override
    public void setVersion( String version )
    {
        this.version = version;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion()
    {
        return version;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getPackaging()
    {
        return packaging;
    }

    /** {@inheritDoc} */
    @Override
    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    /** {@inheritDoc} */
    @Override
    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactHandler getArtifactHandler()
    {
        return handler;
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactHandler( ArtifactHandler handler )
    {
        this.handler = handler;
    }
}
