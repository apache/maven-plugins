package org.apache.maven.plugin.javadoc;

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

import org.apache.maven.plugin.javadoc.options.Tag;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class TagTest
    extends PlexusTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Test when the specified object is not a Tag object
     *
     * @throws Exception
     */
    public void testNotEquals()
        throws Exception
    {
        Tag tag = new Tag();
        tag.setHead( "HEAD" );
        tag.setName( "NAME" );
        tag.setPlacement( "aop" );

        assertFalse( tag.equals( new String() ) );
    }

    /**
     * Test when the passed object is a Tag object
     *
     * @throws Exception
     */
    public void testEquals()
        throws Exception
    {
        Tag tag = new Tag();
        tag.setHead( "HEAD" );
        tag.setName( "NAME" );
        tag.setPlacement( "aop" );

        Tag equalTag = new Tag();
        equalTag.setHead( "HEAD" );
        equalTag.setName( "NAME" );
        equalTag.setPlacement( "aop" );

        assertTrue( tag.equals( equalTag ) );
    }

    /**
     * Test hashCode method
     *
     * @throws Exception
     */
    public void testHashCode()
        throws Exception
    {
        Tag tag = new Tag();
        tag.setHead( "HEAD" );
        tag.setName( "NAME" );

        assertEquals( tag.hashCode(), 90615520 );
    }

    /**
     * Test the toString method
     *
     * @throws Exception
     */
    public void testToString()
        throws Exception
    {
        Tag tag = new Tag();
        tag.setHead( "HEAD" );
        tag.setName( "NAME" );

        assertTrue( tag.toString().indexOf( "HEAD" ) != -1 );
        assertTrue( tag.toString().indexOf( "NAME" ) != -1 );
    }

    protected void tearDown()
        throws Exception
    {

    }
}
