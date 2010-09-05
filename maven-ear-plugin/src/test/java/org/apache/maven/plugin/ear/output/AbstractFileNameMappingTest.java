package org.apache.maven.plugin.ear.output;

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
import org.apache.maven.plugin.ear.AbstractEarTest;
import org.apache.maven.plugin.ear.ArtifactTestStub;

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractFileNameMappingTest
    extends AbstractEarTest
{


    protected Artifact createArtifactWithGroupId( String groupId, String artifactId, String version, String type,
                                                  String classifier )
    {
        return new ArtifactTestStub( groupId, artifactId, type, classifier, version );
    }


    protected Artifact createArtifactWithGroupId( String groupId, String artifactId, String version, String type )
    {
        return createArtifactWithGroupId( groupId, artifactId, version, type, null );
    }


    protected Artifact createArtifact( String artifactId, String version, String type, String classifier )
    {
        return new ArtifactTestStub( DEFAULT_GROUPID, artifactId, type, classifier, version );
    }

    protected Artifact createArtifact( String artifactId, String version, String type )
    {
        return createArtifact( artifactId, version, type, null );
    }
}
