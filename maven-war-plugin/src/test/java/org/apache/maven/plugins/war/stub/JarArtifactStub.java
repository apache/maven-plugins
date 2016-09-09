package org.apache.maven.plugins.war.stub;

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

import java.io.File;

public class JarArtifactStub
    extends AbstractArtifactStub
{

    protected String groupId;

    protected String artifactId;

    protected String version;

    protected boolean optional = false;

    protected String scope;

    private File file;

    private ArtifactHandler artifactHandler;

    public JarArtifactStub( String basedir, ArtifactHandler artifactHandler )
    {
        super( basedir );
        this.artifactHandler = artifactHandler;
    }

    public void setGroupId( String id )
    {
        groupId = id;
    }

    public String getGroupId()
    {
        if ( groupId != null )
        {
            return groupId;
        }
        else
        {
            return "org.sample.jar";
        }
    }

    public String getType()
    {
        return "jar";
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getArtifactId()
    {
        if ( artifactId != null )
        {
            return artifactId;
        }
        else
        {
            return "jarartifact";
        }
    }

    public String getVersion()
    {
        if ( version != null )
        {
            return version;
        }
        else
        {
            return super.getVersion();
        }
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public String getScope()
    {
        if ( scope != null )
        {
            return scope;
        }
        else
        {
            return super.getScope();
        }
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public File getFile()
    {
        if ( file == null )
        {
            return new File( basedir, "/target/test-classes/unit/sample_wars/simple.jar" );
        }
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public ArtifactHandler getArtifactHandler()
    {
        return artifactHandler;
    }
}
