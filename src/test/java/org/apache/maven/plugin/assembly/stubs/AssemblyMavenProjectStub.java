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

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;

import java.util.Set;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Edwin Punzalan
 */
public class AssemblyMavenProjectStub
    extends MavenProjectStub
{
    private String groupId, artifactId, version;

    private Artifact artifact;

    private Set artifacts;

    public AssemblyMavenProjectStub()
    {
        groupId = "assembly";
        artifactId = "test-project";
        version = "1.0";
    }

    public Artifact getArtifact()
    {
        if ( artifact == null )
        {
            artifact = new ArtifactStub( groupId, artifactId, version, "jar", null );
        }

        return artifact;
    }

    public Model getModel()
    {
        Model model = new Model();

        model.setProperties( new Properties() );

        model.setGroupId( getGroupId() );

        model.setArtifactId( getArtifactId() );

        model.setVersion( getVersion() );

        return model;
    }

    public Set getArtifacts()
    {
        if ( artifacts == null )
        {
            artifacts = Collections.EMPTY_SET;
        }

        return artifacts;
    }

    public void setArtifacts( Set artifacts )
    {
        this.artifacts = artifacts;
    }
}
