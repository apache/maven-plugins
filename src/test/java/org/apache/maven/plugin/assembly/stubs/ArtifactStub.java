package org.apache.maven.plugin.assembly.stubs;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class ArtifactStub
    extends DefaultArtifact
{
    public ArtifactStub( String groupId, String artifactId, String version, String packaging, String scope )
    {
        super( groupId, artifactId, VersionRange.createFromVersion( version ), scope, packaging,
               null, new DefaultArtifactHandler(), false );
    }

    public File getFile()
    {
        return new File( getArtifactId() + "-" + getVersion() + "." + getType() );
    }
}
