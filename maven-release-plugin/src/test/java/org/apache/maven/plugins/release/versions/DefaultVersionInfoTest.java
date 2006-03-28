package org.apache.maven.plugins.release.versions;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
        testParse( "1.0", null, "1.0", null, null, null );
    }

    public void testParseWithBadVersion()
        throws Exception
    {
        try
        {
            testParse( "SNAPSHOT", null, null, null, null, "SNAPSHOT" );
            fail( "version is incorrect, must fail." );
        }
        catch( VersionParseException e )
        {
        }
    }
        
    public void testParseMultiDigit() 
        throws Exception
    {        
        testParse( "99.99", null, "99.99", null, null, null );
        testParse( "990.990.990", null, "990.990.990", null, null, null );
    }
    
    public void testParseComponent() 
        throws Exception
    {        
        testParse( "my-component-99.99",     "my-component", "99.99",    null, null, null );
        testParse( "my-component_99.99",     "my-component", "99.99",    null, null, null );
        testParse( "my-component1.2.3",      "my-component", "1.2.3",    null, null, null );
        testParse( "my-component11.22.33",   "my-component", "11.22.33", null, null, null );
    }
    
    public void testParseSnapshotVersion() 
        throws Exception
    {
        testParse( "1.0-beta-4-SNAPSHOT", null, "1.0", "beta", "4", "SNAPSHOT" );        
        testParse( "1.0-beta-4_SNAPSHOT", null, "1.0", "beta", "4", "SNAPSHOT" );
    }
    
    public void testParseAnnotationVersion() 
        throws Exception
    {
        testParse( "1.0-beta-4-SNAPSHOT",   null, "1.0",    "beta", "4",    "SNAPSHOT" );
        testParse( "1.0-beta-4",            null, "1.0",    "beta", "4",    null );
        testParse( "1.2.3-beta-99",         null, "1.2.3",  "beta", "99",   null );
        testParse( "1.2.3-beta99",          null, "1.2.3",  "beta", "99",   null );
        testParse( "1.2.3-beta99-SNAPSHOT", null, "1.2.3",  "beta", "99",   "SNAPSHOT" );
        testParse( "1.2.3-RC4",             null, "1.2.3",  "RC",   "4",    null );
    }
    
    public void testParseSeparators() 
        throws Exception
    {
        testParse("log4j-1.2.9-beta-9-SNAPSHOT", "log4j", "1.2.9", "beta", "9", "SNAPSHOT");
        testParse("log4j1.2.9beta9SNAPSHOT", "log4j", "1.2.9", "beta", "9", "SNAPSHOT");
        testParse("log4j1.2.9beta-9SNAPSHOT", "log4j", "1.2.9", "beta", "9", "SNAPSHOT");
        testParse("log4j_1.2.9_beta_9_SNAPSHOT", "log4j", "1.2.9", "beta", "9", "SNAPSHOT");
    }
    
    public void testParseAnnotationNoVersionButSnapshot() 
        throws Exception
    {
        testParse( "1.0-beta-SNAPSHOT",   null, "1.0",   "beta", null,   "SNAPSHOT" );        
        testParse( "1.2.3-beta99",        null, "1.2.3", "beta", "99",   null );
        testParse( "1.2.3-RC4-SNAPSHOT",  null, "1.2.3", "RC",   "4",    "SNAPSHOT" );
    }
    
    public void testParseAnnotationVersionWithRevision() 
        throws Exception
    {
        testParse( "1.0-beta-4-SNAPSHOT",   null, "1.0",   "beta", "4",  "SNAPSHOT" );
        testParse( "1.0-beta-4",            null, "1.0",   "beta", "4",  null );
        testParse( "1.2.3-beta-99",         null, "1.2.3", "beta", "99", null );
        testParse( "1.2.3-beta99",          null, "1.2.3", "beta", "99", null );
        testParse( "1.2.3-RC4",             null, "1.2.3", "RC",   "4",  null );

        testParse( "mycomponent-1.2.3-RC4", "mycomponent", "1.2.3", "RC", "4",  null );
        testParse( "mycomponent-1.2.3-RC4", "mycomponent", "1.2.3", "RC", "4",  null );
        testParse( "log4j-1.2.9",           "log4j",       "1.2.9", null, null, null );
    }
    
    public void testParseLeadingZeros() 
        throws Exception
    {
        testParse( "1.01-beta-04-SNAPSHOT",   null, "1.01",   "beta", "04",  "SNAPSHOT" );        
        testParse( "01.01.001-beta-04-SNAPSHOT",   null, "01.01.001",   "beta", "04",  "SNAPSHOT" );
    }
    
    public void testParseBuildNumber() 
        throws Exception
    {        
        testParse( "plexus-logging-provider-test-1.0-alpha-2-20051013.095555-2", 
                   "plexus-logging-provider-test", "1.0", "alpha", "2", "20051013.095555-2" );
    }
    
    public void testNextVersion() 
        throws Exception
    {
        testNextVersion( "1.01",  "1.02" );
        testNextVersion( "1.9",   "1.10" );
        testNextVersion( "1.09",  "1.10" );
        testNextVersion( "1.009", "1.010" );
        
        testNextVersion( "1.99", "1.100" );
    }
    
    public void testNextAnnotationRevision() 
        throws Exception
    {
        testNextVersion( "1.01-beta-04", "1.01-beta-05" );
        testNextVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-05-SNAPSHOT" );
        testNextVersion( "9.99.999-beta-9-SNAPSHOT", "9.99.999-beta-10-SNAPSHOT" );
        testNextVersion( "9.99.999-beta-09-SNAPSHOT", "9.99.999-beta-10-SNAPSHOT" );
        testNextVersion( "9.99.999-beta-009-SNAPSHOT", "9.99.999-beta-010-SNAPSHOT" );
        testNextVersion( "9.99.999-beta9-SNAPSHOT", "9.99.999-beta10-SNAPSHOT" );        
    }
    
    public void testCompareToDigitsOnly() 
        throws Exception
    {
        testVersionLessThanVersion( "1.01", "1.02" );
        testVersionLessThanVersion( "1.01", "1.00009" );
        testVersionLessThanVersion( "1.01.99", "1.0002" );
        testVersionLessThanVersion( "1.01", "1.01.01" );
        
        testVersionEqualVersion( "1.01", "1.1" );
        testVersionEqualVersion( "1.01", "1.01" );
        testVersionEqualVersion( "1.01", "1.001" );
        
    }
    
    public void testCompareToAnnotation() 
        throws Exception
    {
        testVersionLessThanVersion( "1.01-alpha",   "1.01" );
        testVersionLessThanVersion( "1.01-alpha",   "1.01-beta" );
        testVersionLessThanVersion( "1.01-beta",    "1.01-RC1");
        testVersionLessThanVersion( "1.01-beta",    "1.01-RC" );
        testVersionLessThanVersion( "1.01-alpha-4", "1.01.1-beta-1" );
        testVersionLessThanVersion( "1.01-alpha-4-SNAPSHOT", "1.01-beta");
        
        testVersionEqualVersion( "1.01-alpha-4-SNAPSHOT", "1.01-alpha-004-SNAPSHOT");        
    }
    
    public void testCompareToAnnotationRevision() 
        throws Exception
    {
        testVersionLessThanVersion( "1.01-beta-04-SNAPSHOT",    "1.01-beta-05-SNAPSHOT" );
        testVersionLessThanVersion( "1.01-beta-0004-SNAPSHOT",  "1.01-beta-5-SNAPSHOT" );
        testVersionLessThanVersion( "1.01-beta-4-SNAPSHOT",     "1.01.1-beta-4-SNAPSHOT" );
        
        testVersionEqualVersion( "1.01-beta-4-SNAPSHOT", "1.01-beta-0004-SNAPSHOT");
        testVersionEqualVersion( "1.01-beta4",           "1.01-beta-0004");        
        
        testVersionLessThanVersion( "1.01-beta9", "1.01-RC1");
        testVersionLessThanVersion( "1.01-beta9", "1.01-RC-1");
    }
    
    public void testCompareToBuildSpecifier() 
        throws Exception
    {
        testVersionLessThanVersion( "1.01-SNAPSHOT",         "1.01" );        
        testVersionLessThanVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04" );
        
        testVersionEqualVersion( "1.01-beta-04-SNAPSHOT", "1.01-beta-04-SNAPSHOT" );
        
        testVersionLessThanVersion( "1.01-beta-04-20051112.134500-2", "1.01-beta-04-SNAPSHOT");
        testVersionLessThanVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-20051112.134500-2" );
        testVersionLessThanVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-20051113.134500-1" );
    }
    
    public void testGetReleaseVersion()
        throws Exception
    {
        testGetReleaseVersion( "1.01",          "1.01" );
        testGetReleaseVersion( "1.01-beta",     "1.01-beta" );
        testGetReleaseVersion( "1.01-beta-04",  "1.01-beta-04" );
        
        testGetReleaseVersion( "1.01-beta-04-SNAPSHOT",          "1.01-beta-04" );
        testGetReleaseVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04" );        
    }
    
    public void testGetSnapshotVersion()
        throws Exception
    {
        testGetSnapshotVersion( "1.01",          "1.01-SNAPSHOT" );
        testGetSnapshotVersion( "1.01-beta",     "1.01-beta-SNAPSHOT" );
        testGetSnapshotVersion( "1.01-beta-04",  "1.01-beta-04-SNAPSHOT" );
        
        testGetSnapshotVersion( "1.01-beta-04-SNAPSHOT",          "1.01-beta-04-SNAPSHOT" );
        testGetSnapshotVersion( "1.01-beta-04-20051112.134500-1", "1.01-beta-04-SNAPSHOT" );        
        testGetSnapshotVersion( "1.01-beta-04_20051112.134500-1", "1.01-beta-04_SNAPSHOT" );        
    }
    
    private void testGetReleaseVersion(String strVersion, String expected)
        throws Exception
    {
        DefaultVersionInfo v = new DefaultVersionInfo( strVersion );
        assertEquals(expected, v.getReleaseVersionString());
    }
    
    private void testGetSnapshotVersion(String strVersion, String expected)
        throws Exception
    {
        DefaultVersionInfo v = new DefaultVersionInfo( strVersion );
        assertEquals(expected, v.getSnapshotVersionString());
    }
    
    private void testParse( String strVersion, String component, String digits, String annotation,
                           String annotationRevision, String buildSpecifier )
        throws Exception
    {
        DefaultVersionInfo v = new DefaultVersionInfo( strVersion );

        assertEquals( strVersion, v.getVersionString() );
        assertEquals( component, v.getComponent() );
        assertEquals( digits, DefaultVersionInfo.joinDigitString( v.getDigits() ) );
        assertEquals( annotation, v.getAnnotation() );
        assertEquals( annotationRevision, v.getAnnotationRevision() );
        assertEquals( buildSpecifier, v.getBuildSpecifier() );
    }

    private void testNextVersion( String strVersion, String nextVersion )
        throws Exception
    {
        DefaultVersionInfo v = new DefaultVersionInfo( strVersion );
        VersionInfo nextV = v.getNextVersion();

        assertNotNull( nextV );
        assertEquals( nextVersion, nextV.getVersionString() );
    }

    private void testVersionLessThanVersion( String lesserVersion, String greaterVersion )
        throws Exception
    {
        testCompareTo( lesserVersion, greaterVersion, false );

    }
    
    private void testVersionEqualVersion( String version1, String version2 )
        throws Exception
    {
        testCompareTo( version1, version2, true );
        
    }

    private void testCompareTo( String lesserVersion, String greaterVersion, boolean equal )
        throws Exception
    {
        DefaultVersionInfo lesserV = new DefaultVersionInfo( lesserVersion );
        DefaultVersionInfo greaterV = new DefaultVersionInfo( greaterVersion );

        if ( equal )
        {
            assertEquals( lesserV.compareTo( greaterV ), 0 );
        }
        else
        {
            assertTrue( lesserV.compareTo( greaterV ) < 0 );
        }
    }
}
