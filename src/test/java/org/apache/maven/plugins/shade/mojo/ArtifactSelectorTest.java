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

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author Benjamin Bentmann
 */
public class ArtifactSelectorTest
    extends TestCase
{

    private ArtifactSelector newSelector( Collection includes, Collection excludes, String groupPrefix )
    {
        return new ArtifactSelector( includes, excludes, groupPrefix );
    }

    public void testIsSelected()
    {
        ArtifactSelector selector;

        selector = newSelector( null, null, null );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );

        selector = newSelector( null, null, "" );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );

        selector = newSelector( null, null, "gid" );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );
        assertTrue( selector.isSelected( new ArtifactId( "gid.test", "aid", "type", "cls" ) ) );
        assertFalse( selector.isSelected( new ArtifactId( "id", "aid", "type", "cls" ) ) );

        selector = newSelector( Collections.EMPTY_SET, Collections.EMPTY_SET, null );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );

        selector = newSelector( Collections.singleton( "gid:aid" ), Collections.EMPTY_SET, null );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );
        assertFalse( selector.isSelected( new ArtifactId( "gid", "id", "type", "cls" ) ) );

        selector = newSelector( Collections.EMPTY_SET, Collections.singleton( "gid:aid" ), null );
        assertFalse( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "id", "type", "cls" ) ) );

        selector = newSelector( Collections.singleton( "gid:*" ), Collections.singleton( "*:aid" ), null );
        assertTrue( selector.isSelected( new ArtifactId( "gid", "id", "type", "cls" ) ) );
        assertFalse( selector.isSelected( new ArtifactId( "gid", "aid", "type", "cls" ) ) );
        assertFalse( selector.isSelected( new ArtifactId( "gid.test", "id", "type", "cls" ) ) );
    }

}
