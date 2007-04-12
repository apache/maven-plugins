package org.apache.maven.plugin.war;

import junit.framework.TestCase;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PathsSetTest
    extends TestCase
{

    /* --------------- Normalization tests --------------*/

    /**
     * Test method for 'org.apache.maven.plugin.war.PathsSet.normalizeFilePathStatic(String)'
     */
    public void testNormalizeFilePathStatic()
    {
        assertEquals( "Normalized path error", "", PathsSet.normalizeFilePathStatic( "" ) );
        assertEquals( "Normalized path error", "", PathsSet.normalizeFilePathStatic( "/" ) );
        assertEquals( "Normalized path error", "", PathsSet.normalizeFilePathStatic( "////" ) );
        assertEquals( "Normalized path error", "", PathsSet.normalizeFilePathStatic( "\\" ) );
        assertEquals( "Normalized path error", "", PathsSet.normalizeFilePathStatic( "\\\\\\\\" ) );

        assertEquals( "Normalized path error", "abc", PathsSet.normalizeFilePathStatic( "abc" ) );
        assertEquals( "Normalized path error", "abc", PathsSet.normalizeFilePathStatic( "/abc" ) );
        assertEquals( "Normalized path error", "abc", PathsSet.normalizeFilePathStatic( "////abc" ) );
        assertEquals( "Normalized path error", "abc", PathsSet.normalizeFilePathStatic( "\\abc" ) );
        assertEquals( "Normalized path error", "abc", PathsSet.normalizeFilePathStatic( "\\\\\\\\abc" ) );

        assertEquals( "Normalized path error", "abc/def/xyz/", PathsSet.normalizeFilePathStatic( "abc/def\\xyz\\" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathsSet.normalizeFilePathStatic( "/abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathsSet.normalizeFilePathStatic( "////abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/", PathsSet.normalizeFilePathStatic( "\\abc/def/xyz/" ) );
        assertEquals( "Normalized path error", "abc/def/xyz/",
                      PathsSet.normalizeFilePathStatic( "\\\\\\\\abc/def/xyz/" ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.war.PathsSet.trimTrailingSlashes(String)'
     */
    public void testTrimTrailingSlashes()
    {
        assertEquals( "Trimed path error", "", PathsSet.trimTrailingSlashes( "" ) );
        assertEquals( "Trimed path error", "", PathsSet.trimTrailingSlashes( "/" ) );
        assertEquals( "Trimed path error", "", PathsSet.trimTrailingSlashes( "//" ) );
        assertEquals( "Trimed path error", "\\", PathsSet.trimTrailingSlashes( "\\" ) );
        assertEquals( "Trimed path error", "abc/def\\xyz\\", PathsSet.trimTrailingSlashes( "abc/def\\xyz\\" ) );
        assertEquals( "Trimed path error", "abc/def/xyz/", PathsSet.trimTrailingSlashes( "/abc/def/xyz/" ) );
        assertEquals( "Trimed path error", "abc/def/xyz/", PathsSet.trimTrailingSlashes( "////abc/def/xyz/" ) );
        assertEquals( "Trimed path error", "\\abc/def/xyz\\", PathsSet.trimTrailingSlashes( "\\abc/def/xyz\\" ) );
        assertEquals( "Trimed path error", "\\\\\\\\abc/def/xyz",
                      PathsSet.trimTrailingSlashes( "\\\\\\\\abc/def/xyz" ) );
    }

    /* -------------- Operations tests ------------------*/

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathsSet.PathsSet()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.size()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.add()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.addAll()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.iterate()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.contains()</li>
     * <li>org.apache.maven.plugin.war.PathsSet.addPrefix(String)</li>
     * </ul>
     */
    public void testPathsSetBasic()
    {
        PathsSet ps = new PathsSet();
        assertEquals( "Unexpected PathsSet size", ps.size(), 0 );
        Iterator iter = ps.iterator();
        assertNotNull( "Iterator is null", iter );
        assertFalse( "Can iterate on empty set", iter.hasNext() );

        ps.add( "abc" );
        assertEquals( "Unexpected PathsSet size", ps.size(), 1 );
        ps.add( "abc" );
        assertEquals( "Unexpected PathsSet size", ps.size(), 1 );
        ps.add( "xyz/abc" );
        assertEquals( "Unexpected PathsSet size", ps.size(), 2 );
        ps.add( "///abc" );
        assertEquals( "Unexpected PathsSet size", ps.size(), 2 );
        ps.add( "///xyz\\abc" );
        assertEquals( "Unexpected PathsSet size", ps.size(), 2 );

        ps.addAll( ps );
        assertEquals( "Unexpected PathsSet size", ps.size(), 2 );

        int i = 0;
        for ( Iterator iter2 = ps.iterator(); iter2.hasNext(); )
        {
            i++;
            String pathstr = (String) iter2.next();
            assertTrue( ps.contains( pathstr ) );
            assertTrue( ps.contains( "/" + pathstr ) );
            assertTrue( ps.contains( "/" + StringUtils.replace( pathstr, '/', '\\' ) ) );
            assertFalse( ps.contains( "/" + StringUtils.replace( pathstr, '/', '\\' ) + "/a" ) );
            assertFalse( ps.contains( "/a/" + StringUtils.replace( pathstr, '/', '\\' ) ) );
        }
        assertEquals( "Wrong count of iterations", 2, i );

        ps.addPrefix( "/ab/c/" );
        i = 0;
        for ( Iterator iter2 = ps.iterator(); iter2.hasNext(); )
        {
            i++;
            String pathstr = (String) iter2.next();
            assertTrue( pathstr.startsWith( "ab/c/" ) );
            assertFalse( pathstr.startsWith( "ab/c//" ) );
            assertTrue( ps.contains( pathstr ) );
            assertTrue( ps.contains( "/" + pathstr ) );
            assertTrue( ps.contains( "/" + StringUtils.replace( pathstr, '/', '\\' ) ) );
            assertFalse( ps.contains( "/" + StringUtils.replace( pathstr, '/', '\\' ) + "/a" ) );
            assertFalse( ps.contains( "/ab/" + StringUtils.replace( pathstr, '/', '\\' ) ) );
        }
        assertEquals( "Wrong count of iterations", 2, i );
    }

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathsSet.PathsSet(Collection)</li>
     * <li>org.apache.maven.plugin.war.PathsSet.PathsSet(String[])</li>
     * <li>org.apache.maven.plugin.war.PathsSet.Add</li>
     * <li>org.apache.maven.plugin.war.PathsSet.AddAll(String[],String)</li>
     * <li>org.apache.maven.plugin.war.PathsSet.AddAll(Collection,String)</li>
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

        PathsSet ps1 = new PathsSet( s1set );
        assertEquals( "Unexpected PathsSet size", 3, ps1.size() );

        PathsSet ps2 = new PathsSet( s2ar );
        assertEquals( "Unexpected PathsSet size", 3, ps2.size() );

        ps1.addAll( s2ar );
        assertEquals( "Unexpected PathsSet size", 5, ps1.size() );

        ps2.addAll( s1set );
        assertEquals( "Unexpected PathsSet size", 5, ps2.size() );

        for ( Iterator iter = ps1.iterator(); iter.hasNext(); )
        {
            String str = (String) iter.next();
            assertTrue( str, ps2.contains( str ) );
            assertTrue( ps2.contains( "/" + str ) );
            assertTrue( ps1.contains( str ) );
            assertTrue( ps1.contains( "/" + str ) );
        }

        for ( Iterator iter = ps2.iterator(); iter.hasNext(); )
        {
            String str = (String) iter.next();
            assertTrue( ps1.contains( str ) );
            assertTrue( ps1.contains( "/" + str ) );
            assertTrue( ps2.contains( str ) );
            assertTrue( ps2.contains( "/" + str ) );
        }

        ps1.addAll( s2ar, "/pref/" );
        assertEquals( "Unexpected PathsSet size", 8, ps1.size() );

        ps2.addAll( s2ar, "/pref/" );
        assertEquals( "Unexpected PathsSet size", 8, ps2.size() );

        for ( Iterator iter = ps1.iterator(); iter.hasNext(); )
        {
            String str = (String) iter.next();
            assertTrue( str, ps2.contains( str ) );
            assertTrue( ps2.contains( "/" + str ) );
            assertTrue( ps1.contains( str ) );
            assertTrue( ps1.contains( "/" + str ) );
        }

        for ( Iterator iter = ps2.iterator(); iter.hasNext(); )
        {
            String str = (String) iter.next();
            assertTrue( ps1.contains( str ) );
            assertTrue( ps1.contains( "/" + str ) );
            assertTrue( ps2.contains( str ) );
            assertTrue( ps2.contains( "/" + str ) );
        }

    }

    /**
     * Test method for 'org.apache.maven.plugin.war.PathsSet.addAllFilesInDirectory(File, String)'
     *
     * @throws IOException
     */
    public void testAddAllFilesInDirectory()
        throws IOException
    {
        PathsSet ps = new PathsSet();

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
        assertEquals( "Unexpected PathsSet size", 4, ps.size() );

        /*No changes after adding duplicates*/
        ps.addAllFilesInDirectory( new File( "target/testAddAllFilesInDirectory" ), "123/" );
        assertEquals( "Unexpected PathsSet size", 4, ps.size() );

        /*Cleanup*/

        f1.delete();
        f2.delete();

        /*No changes after adding a subset of files*/
        ps.addAllFilesInDirectory( new File( "target/testAddAllFilesInDirectory" ), "123/" );
        assertEquals( "Unexpected PathsSet size", 4, ps.size() );

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
        assertTrue( ps.contains("123\\d1/d2\\f2"));
		assertFalse(ps.contains("123\\f3"));
	}
}
