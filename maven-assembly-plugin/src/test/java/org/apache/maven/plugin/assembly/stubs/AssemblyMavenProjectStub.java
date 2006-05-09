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
import org.apache.maven.model.Build;

import java.util.Set;
import java.util.Collections;
import java.util.Properties;
import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class AssemblyMavenProjectStub
    extends MavenProjectStub
{
    private String groupId, artifactId, version;

    private Artifact artifact;

    private Set artifacts;

    private Model model;

    private File basedir;

    public Build getBuild()
    {
        return model.getBuild();
    }

    public AssemblyMavenProjectStub()
    {
        groupId = "assembly";
        artifactId = "test-project";
        version = "1.0";
    }

    public File getBasedir()
    {
        return basedir;
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
        if ( model == null )
        {
            model = new Model();

            model.setProperties( new Properties() );

            model.setGroupId( getGroupId() );

            model.setArtifactId( getArtifactId() );

            model.setVersion( getVersion() );

            Build build = new Build();
            build.setFinalName( getArtifactId() + "-" + getVersion() );
            model.setBuild( build );
        }

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

    public Properties getProperties()
    {
        return new Properties();
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }
}
