package org.apache.maven.plugins.release.versions;

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

import org.codehaus.plexus.PlexusTestCase;

public class DefaultVersionInfoTest
    extends PlexusTestCase
{
    public void testParse()
        throws Exception
    {
        checkParsing( "1.0", "1.0", null, null, null );
    }

    public void testParseWithBadVersion()
        throws Exception
    {
        try
        {
            checkParsing( "foo", null, null, null, "foo" );
            fail( "version is incorrect, must fail." );
        }
        catch ( VersionParseException e )
        {
        }
    }

    public void testParseMultiDigit()
        throws Exception
    {
        checkParsing( "99.99", "99.99", null, null, null );
        checkParsing( "990.990.990", "990.990.990", null, null, null );
    }

    public void testParseSnapshotVersion()
        throws Exception
    {
        checkParsing( "SNAPSHOT", null, null, null, "SNAPSHOT" );
        checkParsing( "1.0-beta-4-SNAPSHOT", "1.0", "beta", "4", "SNAPSHOT" );
        checkParsing( "1.0-beta-4_SNAPSHOT", "1.0", "beta", "4", "SNAPSHOT" );
    }

    public void testParseAnnotationVersion()
        throws Exception
    {
        checkParsing( "1.0-beta-4-SNAPSHOT", "1.0", "beta", "4", "SNAPSHOT" );
        checkParsing( "1.0-beta-4", "1.0", "beta", "4", null );
        checkParsing( "1.2.3-beta-99", "1.2.3", "beta", "99", null );
        checkParsing( "1.2.3-beta99", "1.2.3", "beta", "99", null );
        checkParsing( "1.2.3-beta99-SNAPSHOT", "1.2.3", "beta", "99", "SNAPSHOT" );
        checkParsing( "1.2.3-RC4", "1.2.3", "RC", "4", null );
    }

    public void testParseSeparators()
        throws Exception
    {
        checkParsing( "1.2.9-beta-9-SNAPSHOT", "1.2.9", "beta", "9", "SNAPSHOT" );
        checkParsing( "1.2.9beta9SNAPSHOT", "1.2.9", "beta", "9", "SNAPSHOT" );
        checkParsing( "1.2.9beta-9SNAPSHOT", "1.2.9", "beta", "9", "SNAPSHOT" );
        checkParsing( "1.2.9_beta_9_SNAPSHOT", "1.2.9", "beta", "9", "SNAPSHOT" );
    }

    public void testParseAnnotationNoVersionButSnapshot()
        throws Exception
    {
        checkParsing( "1.0-beta-SNAPSHOT", "1.0", "beta", null, "SNAPSHOT" );
        checkParsing( "1.2.3-beta99", "1.2.3", "beta", "99", null );
        checkParsing( "1.2.3-RC4-SNAPSHOT", "1.2.3", "RC", "4", "SNAPSHOT" );
    }

    public void testParseAnnotationVersionWithRevision()
        throws Exception
    {
        checkParsing( "1.0-beta-4-SNAPSHOT", "1.0", "beta", "4", "SNAPSHOT" );
        checkParsing( "1.0-beta-4", "1.0", "beta", "4", null );
        checkParsing( "1.2.3-beta-99", "1.2.3", "beta", "99", null );
        checkParsing( "1.2.3-beta99", "1.2.3", "beta", "99", null );
        checkParsing( "1.2.3-RC4", "1.2.3", "RC", "4", null );

        checkParsing( "1.2.9", "1.2.9", null, null, null );
    }

    public void testParseAnnotationVersionWithoutRevision()
        throws Exception
    {
        checkParsing( "1.0-beta", "1.0", "beta", null, null );
        checkParsing( "1.0-beta-SNAPSHOT", "1.0", "beta", null, "SNAPSHOT" );
    }

    public void testParseAnnotationRevisionOnly()
        throws Exception
    {
        checkParsing( "1.0-4", "1.0", null, "4", null );
    }

    public void testParseLeadingZeros()
        throws Exception
    {
        checkParsing( "1.01-beta-04-SNAPSHOT", "1.01", "beta", "04", "SNAPSHOT" );
        checkParsing( "01.01.001-beta-04-SNAPSHOT", "01.01.001", "beta", "04", "SNAPSHOT" );
    }

    public void testParseBuildNumber()
        throws Exception
    {
        checkParsing( "1.0-alpha-2-20051013.095555-2", "1.0", "alpha", "2", "20051013.095555-2" );
    }

    public void testNextVersion()
        throws Exception
    {
        VersionInfo v = new DefaultVersionInfo( "SNAPSHOT" );
        assertNull( v.getNextVersion() );

        checkNextVersion( "1", "2" );
        checkNextVersion( "1.01", "1.02" );
        checkNextVersion( "1.9", "1.10" );
        checkNextVersion( "1.09", "1.10" );
        checkNextVersion( "1.009", "1.010" );

        checkNextVersion( "1.99", "1.100" );
    }

    public void testNextAnnotationRevision()
        throws Exception
    {
        checkNextVersion( "1.01-beta-04", "1.01-beta-05" );
        checkNextVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-05-SNAPSHOT" );
        checkNextVersion( "9.99.999-beta-9-SNAPSHOT", "9.99.999-beta-10-SNAPSHOT" );
        checkNextVersion( "9.99.999-beta-09-SNAPSHOT", "9.99.999-beta-10-SNAPSHOT" );
        checkNextVersion( "9.99.999-beta-009-SNAPSHOT", "9.99.999-beta-010-SNAPSHOT" );
        checkNextVersion( "9.99.999-beta9-SNAPSHOT", "9.99.999-beta10-SNAPSHOT" );
    }

    public void testCompareToDigitsOnly()
        throws Exception
    {
        checkVersionLessThanVersion( "1.01", "1.02" );
        checkVersionLessThanVersion( "1.00009", "1.01" );
        checkVersionLessThanVersion( "1.01", "1.01.01" );

        checkVersionLessThanVersion( "1.01", "1.1" );
        checkVersionEqualVersion( "1.01", "1.01" );
        checkVersionLessThanVersion( "1.001", "1.01" );
    }

    public void testCompareToAnnotation()
        throws Exception
    {
        checkVersionLessThanVersion( "1.01-alpha", "1.01" );
        checkVersionLessThanVersion( "1.01-alpha", "1.01-beta" );
        checkVersionLessThanVersion( "1.01-beta", "1.01-RC1" );
        checkVersionLessThanVersion( "1.01-beta", "1.01-RC" );
        checkVersionLessThanVersion( "1.01-alpha-4", "1.01.1-beta-1" );
        checkVersionLessThanVersion( "1.01-alpha-4-SNAPSHOT", "1.01-beta" );
        checkVersionLessThanVersion( "1.01-alpha-4-SNAPSHOT", "1.01-alpha-4" );
        checkVersionLessThanVersion( "1.01-alpha-4", "1.01-alpha-5-SNAPSHOT" );

        checkVersionLessThanVersion( "1.01-alpha-004-SNAPSHOT", "1.01-alpha-4-SNAPSHOT" );
    }

    public void testCompareToAnnotationRevision()
        throws Exception
    {
        checkVersionLessThanVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-05-SNAPSHOT" );
        checkVersionLessThanVersion( "1.01-beta-0004-SNAPSHOT", "1.01-beta-5-SNAPSHOT" );
        checkVersionLessThanVersion( "1.01-beta-4-SNAPSHOT", "1.01.1-beta-4-SNAPSHOT" );

        checkVersionLessThanVersion( "1.01-beta-0004-SNAPSHOT", "1.01-beta-4-SNAPSHOT" );
    }

    public void testCompareToBuildSpecifier()
        throws Exception
    {
        checkVersionLessThanVersion( "1.01-SNAPSHOT", "1.01" );
        checkVersionLessThanVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04" );

        checkVersionEqualVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04-SNAPSHOT" );

        checkVersionLessThanVersion( "1.01-beta-04-20051112.134500-2", "1.01-beta-04-SNAPSHOT" );
        checkVersionLessThanVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-20051112.134500-2" );
        checkVersionLessThanVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-20051113.134500-1" );
    }

    public void testGetReleaseVersion()
        throws Exception
    {
        checkGetReleaseVersion( "1-SNAPSHOT", "1" );
        checkGetReleaseVersion( "1", "1" );

        checkGetReleaseVersion( "1.01", "1.01" );
        checkGetReleaseVersion( "1.01-beta", "1.01-beta" );
        checkGetReleaseVersion( "1.01-beta-04", "1.01-beta-04" );

        checkGetReleaseVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04" );
        checkGetReleaseVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04" );
    }

    public void testGetSnapshotVersion()
        throws Exception
    {
        checkGetSnapshotVersion( "1", "1-SNAPSHOT" );
        checkGetSnapshotVersion( "1.01", "1.01-SNAPSHOT" );
        checkGetSnapshotVersion( "1.01-beta", "1.01-beta-SNAPSHOT" );
        checkGetSnapshotVersion( "1.01-beta-04", "1.01-beta-04-SNAPSHOT" );

        checkGetSnapshotVersion( "SNAPSHOT", "SNAPSHOT" );
        // TODO: bug in Artifact pattern
//        checkGetSnapshotVersion( "20051112.134500-1", "SNAPSHOT" );
        checkGetSnapshotVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04-SNAPSHOT" );
        checkGetSnapshotVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-SNAPSHOT" );
        checkGetSnapshotVersion( "1.01-beta-04_20051112.134500-1", "1.01-beta-04_20051112.134500-1-SNAPSHOT" );
    }

    public void testSnapshot()
        throws VersionParseException
    {
        assertFalse( new DefaultVersionInfo( "1.01" ).isSnapshot() );
        assertFalse( new DefaultVersionInfo( "1.01-beta" ).isSnapshot() );
        assertFalse( new DefaultVersionInfo( "1.01-beta-04" ).isSnapshot() );

        assertTrue( new DefaultVersionInfo( "1.01-beta-04-SNAPSHOT" ).isSnapshot() );
        assertTrue( new DefaultVersionInfo( "1.01-beta-04-20051112.134500-1" ).isSnapshot() );
        assertFalse( new DefaultVersionInfo( "1.01-beta-04_20051112.134500-1" ).isSnapshot() );
    }

    private static void checkGetReleaseVersion( String strVersion, String expected )
        throws Exception
    {
        VersionInfo v = new DefaultVersionInfo( strVersion );
        assertEquals( expected, v.getReleaseVersionString() );
    }

    private static void checkGetSnapshotVersion( String strVersion, String expected )
        throws Exception
    {
        VersionInfo v = new DefaultVersionInfo( strVersion );
        assertEquals( expected, v.getSnapshotVersionString() );
    }

    private static void checkParsing( String strVersion, String digits, String annotation, String annotationRevision,
                                      String buildSpecifier )
        throws Exception
    {
        DefaultVersionInfo v = new DefaultVersionInfo( strVersion );

        assertEquals( strVersion, v.toString() );
        assertEquals( digits, DefaultVersionInfo.joinDigitString( v.getDigits() ) );
        assertEquals( annotation, v.getAnnotation() );
        assertEquals( annotationRevision, v.getAnnotationRevision() );
        assertEquals( buildSpecifier, v.getBuildSpecifier() );
    }

    private static void checkNextVersion( String strVersion, String nextVersion )
        throws Exception
    {
        VersionInfo v = new DefaultVersionInfo( strVersion );
        VersionInfo nextV = v.getNextVersion();

        assertNotNull( nextV );
        assertEquals( nextVersion, nextV.toString() );
    }

    private static void checkVersionLessThanVersion( String lesserVersion, String greaterVersion )
        throws VersionParseException
    {
        checkCompareTo( lesserVersion, greaterVersion, -1 );
        checkCompareTo( greaterVersion, lesserVersion, +1 );
    }

    private static void checkVersionEqualVersion( String version1, String version2 )
        throws Exception
    {
        checkCompareTo( version1, version2, 0 );
    }

    private static void checkCompareTo( String lesserVersion, String greaterVersion, int comparison )
        throws VersionParseException
    {
        VersionInfo lesserV = new DefaultVersionInfo( lesserVersion );
        VersionInfo greaterV = new DefaultVersionInfo( greaterVersion );

        if ( comparison == 0 )
        {
            assertEquals( 0, lesserV.compareTo( greaterV ) );
            assertEquals( lesserV, greaterV );
        }
        else if ( comparison < 0 )
        {
            assertTrue( lesserV.compareTo( greaterV ) < 0 );
        }
        else if ( comparison > 0 )
        {
            assertTrue( lesserV.compareTo( greaterV ) > 0 );
        }
    }
}
