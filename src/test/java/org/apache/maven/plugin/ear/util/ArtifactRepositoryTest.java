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

import org.apache.maven.plugin.ear.AbstractEarTest;

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class ArtifactRepositoryTest
    extends AbstractEarTest
{


    protected void setUp()
        throws Exception
    {
        super.setUp();
        ArtifactTypeMappingService.getInstance().configure( null );
    }


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
        ArtifactRepository repo = new ArtifactRepository(
            createArtifacts( new String[]{"myartifact", "myartifact", "myartifact"}, null, null,
                             new String[]{"class1", "class2", "class3"} ), MAIN_ARTIFACT_ID );

        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class1" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class2" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class3" ) );
        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "wrong" ) );
    }

    public void testRepositoryWithMultipleClassifiedArtifactsAndMainArtifact()
    {
        ArtifactRepository repo = new ArtifactRepository(
            createArtifacts( new String[]{"myartifact", "myartifact", "myartifact"}, null, null,
                             new String[]{"class1", "class2", null} ), MAIN_ARTIFACT_ID );

        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class1" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "class2" ) );
        assertNotNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", MAIN_ARTIFACT_ID ) );
        assertNull( repo.getUniqueArtifact( DEFAULT_GROUPID, "myartifact", "jar", "wrong" ) );
    }
}
