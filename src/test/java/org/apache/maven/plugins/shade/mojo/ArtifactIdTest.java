package org.apache.maven.plugins.shade.mojo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

/**
 * @author Benjamin Bentmann
 */
public class ArtifactIdTest
    extends TestCase
{

    public void testIdParsing()
    {
        ArtifactId id;

        id = new ArtifactId( (String) null );
        assertEquals( "", id.getGroupId() );

        id = new ArtifactId( "" );
        assertEquals( "", id.getGroupId() );

        id = new ArtifactId( "gid" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "*", id.getArtifactId() );

        id = new ArtifactId( "gid:" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "", id.getArtifactId() );

        id = new ArtifactId( ":aid" );
        assertEquals( "", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );

        id = new ArtifactId( "gid:aid" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "*", id.getType() );
        assertEquals( "*", id.getClassifier() );

        id = new ArtifactId( "gid:aid:" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "*", id.getType() );
        assertEquals( "", id.getClassifier() );

        id = new ArtifactId( "gid:aid:cls" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "*", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:aid:type:cls" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "type", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:aid::cls" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:aid::" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "", id.getType() );
        assertEquals( "", id.getClassifier() );

        id = new ArtifactId( "*:aid:type:cls" );
        assertEquals( "*", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "type", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:*:type:cls" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "*", id.getArtifactId() );
        assertEquals( "type", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:aid:*:cls" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "*", id.getType() );
        assertEquals( "cls", id.getClassifier() );

        id = new ArtifactId( "gid:aid:type:*" );
        assertEquals( "gid", id.getGroupId() );
        assertEquals( "aid", id.getArtifactId() );
        assertEquals( "type", id.getType() );
        assertEquals( "*", id.getClassifier() );
    }

    public void testMatches()
    {
        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:type:cls" ) ) );
        assertFalse( new ArtifactId( "Gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:type:cls" ) ) );
        assertFalse( new ArtifactId( "gid", "Aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:type:cls" ) ) );
        assertFalse( new ArtifactId( "gid", "aid", "Type", "cls" ).matches( new ArtifactId( "gid:aid:type:cls" ) ) );
        assertFalse( new ArtifactId( "gid", "aid", "type", "Cls" ).matches( new ArtifactId( "gid:aid:type:cls" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "any", "cls" ).matches( new ArtifactId( "gid:aid:cls" ) ) );
        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:cls" ) ) );
        assertFalse( new ArtifactId( "id", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:cls" ) ) );
        assertFalse( new ArtifactId( "gid", "id", "type", "cls" ).matches( new ArtifactId( "gid:aid:cls" ) ) );
        assertFalse( new ArtifactId( "gid", "id", "type", "ls" ).matches( new ArtifactId( "gid:aid:cls" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "" ).matches( new ArtifactId( "gid:aid" ) ) );
        assertTrue( new ArtifactId( "gid", "aid", "any", "tests" ).matches( new ArtifactId( "gid:aid" ) ) );
        assertFalse( new ArtifactId( "id", "aid", "type", "" ).matches( new ArtifactId( "gid:aid" ) ) );
        assertFalse( new ArtifactId( "gid", "id", "type", "" ).matches( new ArtifactId( "gid:aid" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "" ).matches( new ArtifactId( "gid" ) ) );
        assertTrue( new ArtifactId( "gid", "id", "any", "any" ).matches( new ArtifactId( "gid" ) ) );
        assertFalse( new ArtifactId( "id", "aid", "type", "" ).matches( new ArtifactId( "gid" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "*:aid:type:cls" ) ) );
        assertTrue( new ArtifactId( "any", "aid", "type", "cls" ).matches( new ArtifactId( "*:aid:type:cls" ) ) );
        assertFalse( new ArtifactId( "any", "id", "type", "cls" ).matches( new ArtifactId( "*:aid:type:cls" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:*:type:cls" ) ) );
        assertTrue( new ArtifactId( "gid", "any", "type", "cls" ).matches( new ArtifactId( "gid:*:type:cls" ) ) );
        assertFalse( new ArtifactId( "id", "any", "type", "cls" ).matches( new ArtifactId( "gid:*:type:cls" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:*:cls" ) ) );
        assertTrue( new ArtifactId( "gid", "aid", "any", "cls" ).matches( new ArtifactId( "gid:aid:*:cls" ) ) );
        assertFalse( new ArtifactId( "id", "aid", "any", "cls" ).matches( new ArtifactId( "gid:aid:*:cls" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:aid:type:*" ) ) );
        assertTrue( new ArtifactId( "gid", "aid", "type", "any" ).matches( new ArtifactId( "gid:aid:type:*" ) ) );
        assertFalse( new ArtifactId( "id", "aid", "type", "any" ).matches( new ArtifactId( "gid:aid:type:*" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:a*d" ) ) );
        assertTrue( new ArtifactId( "gid", "ad", "type", "cls" ).matches( new ArtifactId( "gid:a*d" ) ) );
        assertTrue( new ArtifactId( "gid", "a---d", "type", "cls" ).matches( new ArtifactId( "gid:a*d" ) ) );

        assertTrue( new ArtifactId( "gid", "aid", "type", "cls" ).matches( new ArtifactId( "gid:a?d" ) ) );
        assertTrue( new ArtifactId( "gid", "a-d", "type", "cls" ).matches( new ArtifactId( "gid:a?d" ) ) );
        assertFalse( new ArtifactId( "gid", "ad", "type", "cls" ).matches( new ArtifactId( "gid:a?d" ) ) );
        assertFalse( new ArtifactId( "gid", "a---d", "type", "cls" ).matches( new ArtifactId( "gid:a?d" ) ) );
    }

}
