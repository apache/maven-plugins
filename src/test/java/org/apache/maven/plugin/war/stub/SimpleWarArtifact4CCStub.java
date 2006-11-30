package org.apache.maven.plugin.war.stub;

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
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * 
 *  stub for copy constructor
 *  to preven the copy constructor frow blowing up
 *
 */
public class SimpleWarArtifact4CCStub
    extends SimpleWarArtifactStub
{
    public SimpleWarArtifact4CCStub( String basedir )
    {
        super( basedir );
    }

    public VersionRange getVersionRange()
    {
        return VersionRange.createFromVersion( getVersion() );
    }

    public String getGroupId()
    {
        return "org.maven.plugin.test";
    }

    public String getClassifier()
    {
        return "testclassifier";
    }

    public ArtifactHandler getArtifactHandler()
    {
        return new DefaultArtifactHandler();
    }
}
