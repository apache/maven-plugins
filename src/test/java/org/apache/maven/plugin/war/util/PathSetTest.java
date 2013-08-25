package org.apache.maven.plugin.war.util;

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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PathSetTest
    extends TestCase
{

    /* --------------- Normalization tests --------------*/

    /**
     * Test method for 'org.apache.maven.plugin.war.PathSet.normalizeFilePathStatic(String)'
     */
    public void testNormalizeFilePathStatic()
    {
        assertEquals( "Normalized path error", "", PathSet.normalizeFilePathStatic( "" ) );
        assertEquals( "Normalized path error", "", PathSet.normalizeFilePathStatic( "/" ) );
        assertEquals( "Normalized path error", "", PathSet.normalizeFilePathStatic( "////" ) );
        assertEquals( "Normalized path error", "", PathSet.normalizeFilePathStatic( "\\" ) );
        assertEquals( "Normalized path error", "", PathSet.normalizeFilePathStatic( "\\\\\\\\" ) );

        assertEquals( "Normalized path error", "abc", PathSet.normalizeFilePathStatic( "abc" ) );
        assertEquals( "Normalized path error", "abc", PathSet.normalizeFilePathStatic( "/abc" ) );
        assertEquals( "Normalized path error", "abc", PathSet.normalizeFilePathStatic( "////abc" ) );
        assertEquals( "Normalized path error", "abc", PathSet.normalizeFilePathStatic( "\\abc" ) );
        assertEquals( "Normalized path error", "abc", PathSet.normalizeFilePathStatic( "\\\\\\\\abc" ) );

        assertEquals( "Normalized path error", "abc/def/xyz/", PathSet.normalizeFilePathStatic( "abc/def\\xyz\\" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathSet.normalizeFilePathStatic( "/abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathSet.normalizeFilePathStatic( "////abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathSet.normalizeFilePathStatic( "\\abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/",
                      PathSet.normalizeFilePathStatic( "\\\\\\\\abc/def/xyz/" ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.war.PathSet.trimTrailingSlashes(String)'
     */
    public void testTrimTrailingSlashes()
    {
        assertEquals( "Trimed path error", "", PathSet.trimTrailingSlashes( "" ) );
        assertEquals( "Trimed path error", "", PathSet.trimTrailingSlashes( "/" ) );
        assertEquals( "Trimed path error", "", PathSet.trimTrailingSlashes( "//" ) );
        assertEquals( "Trimed path error", "\\", PathSet.trimTrailingSlashes( "\\" ) );
        assertEquals( "Trimed path error", "abc/def\\xyz\\", PathSet.trimTrailingSlashes( "abc/def\\xyz\\" ) );
        assertEquals( "Trimed path error", "abc/def/xyz/", PathSet.trimTrailingSlashes( "/abc/def/xyz/" ) );
        assertEquals( "Trimed path error", "abc/def/xyz/", PathSet.trimTrailingSlashes( "////abc/def/xyz/" ) );
        assertEquals( "Trimed path error", "\\abc/def/xyz\\", PathSet.trimTrailingSlashes( "\\abc/def/xyz\\" ) );
        assertEquals( "Trimed path error", "\\\\\\\\abc/def/xyz",
                      PathSet.trimTrailingSlashes( "\\\\\\\\abc/def/xyz" ) );
    }

    /* -------------- Operations tests ------------------*/

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet()</li>
     * <li>org.apache.maven.plugin.war.PathSet.size()</li>
     * <li>org.apache.maven.plugin.war.PathSet.add()</li>
     * <li>org.apache.maven.plugin.war.PathSet.addAll()</li>
     * <li>org.apache.maven.plugin.war.PathSet.iterate()</li>
     * <li>org.apache.maven.plugin.war.PathSet.contains()</li>
     * <li>org.apache.maven.plugin.war.PathSet.addPrefix(String)</li>
     * </ul>
     */
    public void testPathsSetBasic()
    {
        PathSet ps = new PathSet();
        assertEquals( "Unexpected PathSet size", ps.size(), 0 );
        Iterator iter = ps.iterator();
        assertNotNull( "Iterator is null", iter );
        assertFalse( "Can iterate on empty set", iter.hasNext() );

        ps.add( "abc" );
        assertEquals( "Unexpected PathSet size", ps.size(), 1 );
        ps.add( "abc" );
        assertEquals( "Unexpected PathSet size", ps.size(), 1 );
        ps.add( "xyz/abc" );
        assertEquals( "Unexpected PathSet size", ps.size(), 2 );
        ps.add( "///abc" );
        assertEquals( "Unexpected PathSet size", ps.size(), 2 );
        ps.add( "///xyz\\abc" );
        assertEquals( "Unexpected PathSet size", ps.size(), 2 );

        ps.addAll( ps );
        assertEquals( "Unexpected PathSet size", ps.size(), 2 );

        int i = 0;
        for (String p1 : ps) {
            i++;
            String pathstr = p1;
            assertTrue(ps.contains(pathstr));
            assertTrue(ps.contains("/" + pathstr));
            assertTrue(ps.contains("/" + StringUtils.replace(pathstr, '/', '\\')));
            assertFalse(ps.contains("/" + StringUtils.replace(pathstr, '/', '\\') + "/a"));
            assertFalse(ps.contains("/a/" + StringUtils.replace(pathstr, '/', '\\')));
        }
        assertEquals( "Wrong count of iterations", 2, i );

        ps.addPrefix( "/ab/c/" );
        i = 0;
        for (String p : ps) {
            i++;
            String pathstr = p;
            assertTrue(pathstr.startsWith("ab/c/"));
            assertFalse(pathstr.startsWith("ab/c//"));
            assertTrue(ps.contains(pathstr));
            assertTrue(ps.contains("/" + pathstr));
            assertTrue(ps.contains("/" + StringUtils.replace(pathstr, '/', '\\')));
            assertFalse(ps.contains("/" + StringUtils.replace(pathstr, '/', '\\') + "/a"));
            assertFalse(ps.contains("/ab/" + StringUtils.replace(pathstr, '/', '\\')));
        }
        assertEquals( "Wrong count of iterations", 2, i );
    }

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet(Collection)</li>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet(String[])</li>
     * <li>org.apache.maven.plugin.war.PathSet.Add</li>
     * <li>org.apache.maven.plugin.war.PathSet.AddAll(String[],String)</li>
     * <li>org.apache.maven.plugin.war.PathSet.AddAll(Collection,String)</li>
     * </ul>
     */
    public void testPathsSetAddAlls()
    {
        Set s1set = new HashSet();
        s1set.add( "/a/b" );
        s1set.add( "a/b/c" );
        s1set.add( "a\\b/c" );
        s1set.add( "//1//2\3a" );

        String[] s2ar = new String[]{"/a/b", "a2/b2/c2", "a2\\b2/c2", "//21//22\23a"};

        PathSet ps1 = new PathSet( s1set );
        assertEquals( "Unexpected PathSet size", 3, ps1.size() );

        PathSet ps2 = new PathSet( s2ar );
        assertEquals( "Unexpected PathSet size", 3, ps2.size() );

        ps1.addAll( s2ar );
        assertEquals( "Unexpected PathSet size", 5, ps1.size() );

        ps2.addAll( s1set );
        assertEquals( "Unexpected PathSet size", 5, ps2.size() );

        for (String str : ps1) {
            assertTrue(str, ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
        }

        for (String str : ps2) {
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
            assertTrue(ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
        }

        ps1.addAll( s2ar, "/pref/" );
        assertEquals( "Unexpected PathSet size", 8, ps1.size() );

        ps2.addAll( s2ar, "/pref/" );
        assertEquals( "Unexpected PathSet size", 8, ps2.size() );

        for (String str : ps1) {
            assertTrue(str, ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
        }

        for (String str : ps2) {
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
            assertTrue(ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
        }

    }

    /**
     * Test method for 'org.apache.maven.plugin.war.PathSet.addAllFilesInDirectory(File, String)'
     *
     * @throws IOException if an io error occurred
     */
    public void testAddAllFilesInDirectory()
        throws IOException
    {
        PathSet ps = new PathSet();

        /* Preparing directory structure*/
        File testDir = new File( "target/testAddAllFilesInDirectory" );
        testDir.mkdirs();

        File f1 = new File( testDir, "f1" );
        f1.createNewFile();
        File f2 = new File( testDir, "f2" );
        f2.createNewFile();

        File d1 = new File( testDir, "d1" );
        File d1d2 = new File( testDir, "d1/d2" );
        d1d2.mkdirs();
        File d1d2f1 = new File( d1d2, "f1" );
        d1d2f1.createNewFile();
        File d1d2f2 = new File( d1d2, "f2" );
        d1d2f2.createNewFile();

        ps.addAllFilesInDirectory( new File( "target/testAddAllFilesInDirectory" ), "123/" );
        assertEquals( "Unexpected PathSet size", 4, ps.size() );

        /*No changes after adding duplicates*/
        ps.addAllFilesInDirectory( new File( "target/testAddAllFilesInDirectory" ), "123/" );
        assertEquals( "Unexpected PathSet size", 4, ps.size() );

        /*Cleanup*/

        f1.delete();
        f2.delete();

        /*No changes after adding a subset of files*/
        ps.addAllFilesInDirectory( new File( "target/testAddAllFilesInDirectory" ), "123/" );
        assertEquals( "Unexpected PathSet size", 4, ps.size() );

        d1d2f1.delete();
        d1d2f2.delete();
        d1d2.delete();
        d1.delete();
        testDir.delete();

        assertTrue( ps.contains( "123/f1" ) );
        assertTrue( ps.contains( "/123/f1" ) );
        assertTrue( ps.contains( "123\\f1" ) );
        assertTrue( ps.contains( "123\\f2" ) );
        assertTrue( ps.contains( "\\123/d1\\d2/f1" ) );
        assertTrue( ps.contains( "123\\d1/d2\\f2" ) );
        assertFalse( ps.contains( "123\\f3" ) );
    }
}
