package org.apache.maven.plugin.ear.util;

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

import junit.framework.TestCase;
import org.apache.maven.plugin.ear.ArtifactTestStub;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class ArtifactRepositoryTest
    extends TestCase
{


    protected void setUp()
        throws Exception
    {
        super.setUp();
        ArtifactTypeMappingService.getInstance().configure( null );
    }

    public static final String DEFAULT_GROUPID = "eartest";

    public static final String DEFAULT_TYPE = "jar";

    public static final String MAIN_ARTIFACT_ID = "none";

    public void testEmptyRepository()
    {
        ArtifactRepository repo = new ArtifactRepository( createArtifacts( null ), MAIN_ARTIFACT_ID );
        assertNull( repo.getUniqueArtifact( "ear", "ar", "jar" ) );
        assertNull( repo.getUniqueArtifact( "ear", "ar", "jar", null ) );
        assertNull( repo.getUniqueArtifact( "ear", "ar", "jar", "class" ) );
    }

    public void testRepositoryWithOneUnclassifiedArtifact()
    {
        ArtifactRepository repo =
            new ArtifactRepository( createArtifacts( new String[]{"myartifact"} ), MAIN_ARTIFACT_ID );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", null ) );
    }

    public void testRepositoryWithOneClassifiedArtifact()
    {
        ArtifactRepository repo = new ArtifactRepository(
            createArtifacts( new String[]{"myartifact"}, null, null, new String[]{"classified"} ), MAIN_ARTIFACT_ID );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "classified" ) );
        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "wrong" ) );
    }

    public void testRepositoryWithMultipleClassifiedArtifacts()
    {
        ArtifactRepository repo = new ArtifactRepository( createArtifacts(
            new String[]{"myartifact", "myartifact", "myartifact"}, null, null,
            new String[]{"class1", "class2", "class3"} ), MAIN_ARTIFACT_ID );

        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class1" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class2" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class3" ) );
        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "wrong" ) );
    }

    public void testRepositoryWithMultipleClassifiedArtifactsAndMainArtifact()
    {
        ArtifactRepository repo = new ArtifactRepository( createArtifacts(
            new String[]{"myartifact", "myartifact", "myartifact"}, null, null,
            new String[]{"class1", "class2", null} ), MAIN_ARTIFACT_ID );

        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class1" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class2" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", MAIN_ARTIFACT_ID ) );
        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "wrong" ) );
    }


    private Set createArtifacts( String[] artifactsId )
    {
        return createArtifacts( artifactsId, null );
    }

    private Set createArtifacts( String[] artifactsId, String[] types )
    {
        return createArtifacts( artifactsId, types, null );
    }

    private Set createArtifacts( String[] artifactsId, String[] types, String[] groupsId )
    {
        return createArtifacts( artifactsId, types, groupsId, null );
    }

    private Set createArtifacts( String[] artifactsId, String[] types, String[] groupsId, String[] classifiers )
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

    private String getData( String[] data, int i, String defaultValue )
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


}
