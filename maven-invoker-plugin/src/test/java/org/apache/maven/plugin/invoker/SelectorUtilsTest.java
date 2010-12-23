package org.apache.maven.plugin.invoker;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests {@link SelectorUtils}.
 * 
 * @author Benjamin Bentmann
 */
public class SelectorUtilsTest
    extends TestCase
{

    private List<Integer> list( int[] numbers )
    {
        List<Integer> result = new ArrayList<Integer>();

        for ( int i = 0; i < numbers.length; i++ )
        {
            result.add( new Integer( numbers[i] ) );
        }

        return result;
    }

    public void testParseList()
    {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();

        SelectorUtils.parseList( null, includes, excludes );

        SelectorUtils.parseList( " 1.5, !1.4, 1.6+ ", includes, excludes );
        assertEquals( Arrays.asList( new String[] { "1.5", "1.6+" } ), includes );
        assertEquals( Arrays.asList( new String[] { "1.4" } ), excludes );
    }

    public void testParseVersion()
    {
        assertEquals( list( new int[] { 1, 6, 0, 12 } ), SelectorUtils.parseVersion( "1.6.0_12" ) );

        assertEquals( list( new int[] { 1, 6, 0, 12 } ), SelectorUtils.parseVersion( "1.6.0_12+" ) );
        assertEquals( list( new int[] { 1, 6, 0, 12 } ), SelectorUtils.parseVersion( "1.6.0_12-" ) );
    }

    public void testCompareVersions()
    {
        assertTrue( SelectorUtils.compareVersions( list( new int[] { 1, 6 } ), list( new int[] { 1, 6 } ) ) == 0 );

        assertTrue( SelectorUtils.compareVersions( list( new int[] { 1, 5 } ), list( new int[] { 1, 6 } ) ) < 0 );
        assertTrue( SelectorUtils.compareVersions( list( new int[] { 1, 6 } ), list( new int[] { 1, 5 } ) ) > 0 );

        assertTrue( SelectorUtils.compareVersions( list( new int[] { 1 } ), list( new int[] { 1, 6 } ) ) < 0 );
        assertTrue( SelectorUtils.compareVersions( list( new int[] { 1, 6 } ), list( new int[] { 1 } ) ) > 0 );
    }

    public void testIsMatchingJre()
    {
        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 4, 2, 8 } ), "1.5" ) );
        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 5 } ), "1.5" ) );
        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 5, 9 } ), "1.5" ) );
        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 6 } ), "1.5" ) );

        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 4, 2, 8 } ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 5 } ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 5, 9 } ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 6 } ), "1.5+" ) );

        assertTrue( SelectorUtils.isJreVersion( list( new int[] { 1, 4, 2, 8 } ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 5 } ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 5, 9 } ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( list( new int[] { 1, 6 } ), "1.5-" ) );
    }

}
