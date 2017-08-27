package org.apache.maven.plugins.ear;

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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.maven.plugins.ear.EnvEntry;

/**
 * @author Stephane Nicoll
 */
public class EnvEntryTest
{

    public static final String DESCRIPTION = "description";

    public static final String NAME = "name";

    public static final String TYPE = Integer.class.getName();

    public static final String VALUE = "34";

    public static final String LOOKUP_NAME = "lookupName";

    @Test
    public void createComplete()
    {
        final EnvEntry envEntry = new EnvEntry( DESCRIPTION, NAME, TYPE, VALUE, LOOKUP_NAME );
        assertEnvEntry( envEntry, DESCRIPTION, NAME, TYPE, VALUE, LOOKUP_NAME );
    }

    @Test
    public void createWithoutTypeButValue()
    {
        final EnvEntry envEntry = new EnvEntry( null, NAME, null, VALUE, LOOKUP_NAME );
        assertEnvEntry( envEntry, null, NAME, null, VALUE, LOOKUP_NAME );
    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithoutName()
    {
        new EnvEntry( DESCRIPTION, null, TYPE, VALUE, LOOKUP_NAME );

    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithEmptyName()
    {
        new EnvEntry( DESCRIPTION, "", TYPE, VALUE, LOOKUP_NAME );
    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithNullTypeAndNoValue()
    {
        new EnvEntry( DESCRIPTION, NAME, null, null, LOOKUP_NAME );
    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithEmptyTypeAndNoValue()
    {
        new EnvEntry( DESCRIPTION, NAME, "", null, LOOKUP_NAME );
    }

    @Test
    public void createWithEmptyLookupName()
    {
        new EnvEntry( DESCRIPTION, NAME, TYPE, VALUE, null );
    }

    private void assertEnvEntry( EnvEntry actual, String description, String name, String type, String value,
                                 String lookupName )
    {
        assertNotNull( "Env entry could not be null", actual );
        assertNotNull( "ToString could not be null", actual.toString() );
        assertEquals( "Wrong env entry description for [" + actual + "]", description, actual.getDescription() );
        assertEquals( "Wrong env entry name for [" + actual + "]", name, actual.getName() );
        assertEquals( "Wrong env entry type for [" + actual + "]", type, actual.getType() );
        assertEquals( "Wrong env entry value for [" + actual + "]", value, actual.getValue() );
        assertEquals( "Wrong env entry value for [" + actual + "]", lookupName, actual.getLookupName() );

    }
}
