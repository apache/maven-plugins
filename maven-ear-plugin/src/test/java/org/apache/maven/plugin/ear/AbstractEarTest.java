package org.apache.maven.plugin.ear;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;

import java.util.Set;
import java.util.TreeSet;

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

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractEarTest
    extends TestCase
{

    public static final String DEFAULT_GROUPID = "eartest";

    public static final String DEFAULT_TYPE = "jar";


    protected void setUri( EarModule module, String uri )
    {
        ( (AbstractEarModule) module ).setUri( uri );
    }


    protected Set createArtifacts( String[] artifactsId )
    {
        return createArtifacts( artifactsId, null );
    }

    protected Set createArtifacts( String[] artifactsId, String[] types )
    {
        return createArtifacts( artifactsId, types, null );
    }

    protected Set createArtifacts( String[] artifactsId, String[] types, String[] groupsId )
    {
        return createArtifacts( artifactsId, types, groupsId, null );
    }

    protected Set createArtifacts( String[] artifactsId, String[] types, String[] groupsId, String[] classifiers )
    {
        Set result = new TreeSet();
        if ( artifactsId == null || artifactsId.length == 0 )
        {
            return result;
        }
        for ( int i = 0; i < artifactsId.length; i++ )
        {
            String artifactId = artifactsId[i];
            String type = getData( types, i, DEFAULT_TYPE );
            String groupId = getData( groupsId, i, DEFAULT_GROUPID );
            String classifier = getData( classifiers, i, null );
            result.add( new ArtifactTestStub( groupId, artifactId, type, classifier ) );

        }
        return result;
    }

    protected String getData( String[] data, int i, String defaultValue )
    {
        if ( data == null || data[i] == null )
        {
            return defaultValue;
        }
        else
        {
            return data[i];

        }
    }

    protected String getDefaultValue( String t, String defaultValue )
    {
        if ( t == null )
        {
            return defaultValue;
        }
        else
        {
            return t;
        }
    }

    protected Artifact createArtifact( String artifactId, String type, String groupId, String classifier )
    {
        return new ArtifactTestStub( getDefaultValue( groupId, DEFAULT_GROUPID ), artifactId,
                                     getDefaultValue( type, DEFAULT_TYPE ), classifier );
    }


    protected Artifact createArtifact( String artifactId, String type, String groupId )
    {
        return createArtifact( artifactId, type, groupId, null );

    }

    protected Artifact createArtifact( String artifactId, String type )
    {
        return createArtifact( artifactId, type, null );

    }


}
