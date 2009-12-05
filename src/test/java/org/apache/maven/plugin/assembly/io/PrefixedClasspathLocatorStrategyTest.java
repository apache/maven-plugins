package org.apache.maven.plugin.assembly.io;

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

import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.io.logging.DefaultMessageHolder;
import org.apache.maven.shared.io.logging.MessageHolder;

import junit.framework.TestCase;

/**
 * @author Benjamin Bentmann
 */
public class PrefixedClasspathLocatorStrategyTest
    extends TestCase
{

    public void testResolvePrefixWithLeadingSlashAndWithTrailingSlash()
    {
        MessageHolder mh = new DefaultMessageHolder();

        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy( "/assemblies/" );
        Location location = ls.resolve( "empty.xml", mh );

        assertNotNull( location );
        assertEquals( 0, mh.size() );
    }

    public void testResolvePrefixWithLeadingSlashAndWithoutTrailingSlash()
    {
        MessageHolder mh = new DefaultMessageHolder();

        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy( "/assemblies" );
        Location location = ls.resolve( "empty.xml", mh );

        assertNotNull( location );
        assertEquals( 0, mh.size() );
    }

    public void testResolvePrefixWithoutLeadingSlashAndWithTrailingSlash()
    {
        MessageHolder mh = new DefaultMessageHolder();

        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy( "assemblies/" );
        Location location = ls.resolve( "empty.xml", mh );

        assertNotNull( location );
        assertEquals( 0, mh.size() );
    }

    public void testResolvePrefixWithoutLeadingSlashAndWithoutTrailingSlash()
    {
        MessageHolder mh = new DefaultMessageHolder();

        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy( "assemblies" );
        Location location = ls.resolve( "empty.xml", mh );

        assertNotNull( location );
        assertEquals( 0, mh.size() );
    }

}
