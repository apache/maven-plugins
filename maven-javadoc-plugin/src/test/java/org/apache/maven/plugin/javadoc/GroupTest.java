package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.javadoc.options.Group;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class GroupTest
    extends PlexusTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Test when the passed object is not a Group object
     *
     * @throws Exception
     */
    public void testNotEquals()
        throws Exception
    {
        Group group = new Group();
        group.setTitle( "GROUP" );
        group.setPackages( "PACKAGES" );

        assertFalse( group.equals( new String() ) );
    }

    /**
     * Test if the passed object is a Group object
     *
     * @throws Exception
     */
    public void testEquals()
        throws Exception
    {
        Group group = new Group();
        group.setTitle( "GROUP" );
        group.setPackages( "PACKAGES" );

        Group equalGroup = new Group();
        equalGroup.setTitle( "GROUP" );
        equalGroup.setPackages( "PACKAGES" );

        assertTrue( group.equals( equalGroup ) );
    }

    /**
     * Test the hashCode method
     *
     * @throws Exception
     */
    public void testHashCode()
        throws Exception
    {
        Group group = new Group();
        group.setTitle( "GROUP" );
        group.setPackages( "PACKAGES" );

        assertEquals( group.hashCode(), -242064495 );
    }

    /**
     * Test the toString() method
     *
     * @throws Exception
     */
    public void testToString()
        throws Exception
    {
        Group group = new Group();
        group.setTitle( "GROUP" );
        group.setPackages( "PACKAGES" );

        assertTrue( group.toString().indexOf( "GROUP" ) != -1 );
        assertTrue( group.toString().indexOf( "PACKAGES" ) != -1 );
    }

    protected void tearDown()
        throws Exception
    {

    }

}
