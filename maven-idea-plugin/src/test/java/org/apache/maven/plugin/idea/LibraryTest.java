package org.apache.maven.plugin.idea;

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
 * @author Edwin Punzalan
 */
public class LibraryTest
    extends TestCase
{
    private Library library;

    protected void setUp()
        throws Exception
    {
        library = new Library();
    }

    public void testName()
    {
        library.setName( "library-name" );

        assertEquals( "Test name", "library-name", library.getName() );
    }

    public void testSources()
    {
        assertEquals( "Test null sources", 0, library.getSplitSources().length );

        String testString = "library-sources, sources, javasources";

        library.setSources( testString );

        assertEquals( "Test sources", testString, library.getSources() );

        String sources[] = library.getSplitSources();

        assertEquals( "Test split sources cound", 3, sources.length );

        assertEquals( "Test split source 1", "library-sources", sources[0] );

        assertEquals( "Test split source 2", "sources", sources[1] );

        assertEquals( "Test split source 3", "javasources", sources[2] );
    }

    public void testClasses()
    {
        assertEquals( "Test null classes", 0, library.getSplitClasses().length );

        String testString = "library-classes, classes, javaclasses";

        library.setClasses( testString );

        assertEquals( "Test classes", testString, library.getClasses() );

        String classes[] = library.getSplitClasses();

        assertEquals( "Test split sources cound", 3, classes.length );

        assertEquals( "Test split class 1", "library-classes", classes[0] );

        assertEquals( "Test split class 2", "classes", classes[1] );

        assertEquals( "Test split class 3", "javaclasses", classes[2] );
    }

    public void testIsExclude()
    {
        library.setExclude( true );

        assertTrue( "Test exclude", library.isExclude() );
    }
}
