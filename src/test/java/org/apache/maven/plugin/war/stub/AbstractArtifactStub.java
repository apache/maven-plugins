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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import java.util.ArrayList;

public abstract class AbstractArtifactStub
    extends ArtifactStub
{
    protected String basedir;

    public AbstractArtifactStub( String _basedir )
    {
        basedir = _basedir;
    }

    public String getVersion()
    {
        return "0.0-Test";
    }

    public String getScope()
    {
        return Artifact.SCOPE_RUNTIME;
    }

    public VersionRange getVersionRange()
    {
        return VersionRange.createFromVersion( getVersion());
    }

    public boolean isOptional()
    {
        return false;
    }

    public ArtifactHandler getArtifactHandler()
    {
        return new DefaultArtifactHandler( getType() );
    }

    /*
     * TODO: Coppied from org/apache/maven/artifact/DefaultArtifact.java; Consider merging...
     */
    public int compareTo( Object o )
    {
        Artifact a = (Artifact) o;

        /* -- We need to support groupId=null (it is missing in DefaultArtifact.java) */
        int result;
        if ( a.getGroupId() != null )
        {
            result = getGroupId().compareTo( a.getGroupId() );
        }
        else
        {
            result = ( getGroupId() == null ? 0 : -1 );
        }
        /* -- */

        if ( result == 0 )
        {
            result = getArtifactId().compareTo( a.getArtifactId() );
            if ( result == 0 )
            {
                result = getType().compareTo( a.getType() );
                if ( result == 0 )
                {
                    if ( getClassifier() == null )
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = 1;
                        }
                    }
                    else
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = getClassifier().compareTo( a.getClassifier() );
                        }
                        else
                        {
                            result = -1;
                        }
                    }
                    if ( result == 0 )
                    {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = getVersion().compareTo( a.getVersion() );
                    }
                }
            }
        }
        return result;
    }

    /*
     * TODO: Coppied from org/apache/maven/artifact/DefaultArtifact.java; Consider merging...
     */
    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Artifact ) )
        {
            return false;
        }

        Artifact a = (Artifact) o;

        /* -- We need to support groupId=null (it is missing in DefaultArtifact.java) */
        if ( a.getGroupId() == null ? ( getGroupId() != null ) : a.getGroupId().equals( getGroupId() ) )
        {
            return false;
        }
        else if ( !a.getArtifactId().equals( getArtifactId() ) )
        {
            return false;
        }
        else if ( !a.getVersion().equals( getVersion() ) )
        {
            return false;
        }
        else if ( !a.getType().equals( getType() ) )
        {
            return false;
        }
        else if ( a.getClassifier() == null ? getClassifier() != null : !a.getClassifier().equals( getClassifier() ) )
        {
            return false;
        }

        // We don't consider the version range in the comparison, just the resolved version

        return true;
    }
}
