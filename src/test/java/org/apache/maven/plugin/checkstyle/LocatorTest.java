package org.apache.maven.plugin.checkstyle;

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

import junit.framework.TestCase;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.IOException;

/**
 * @author Edwin Punzalan
 */
public class LocatorTest
    extends TestCase
{
    Locator locator;

    File testDir = new File( PlexusTestCase.getBasedir(), "target/unit-test/Locator" );

    protected void setUp()
        throws Exception
    {
        locator = new Locator( null, testDir );
    }

    public void testEmptyLocation()
        throws Exception
    {
        assertNull( "Test null location", locator.resolveLocation( null, "" ) );
        assertNull( "Test empty location", locator.resolveLocation( "", "" ) );
    }

    public void testURLs()
        throws Exception
    {
        String basedir = PlexusTestCase.getBasedir();
        File resolvedFile = locator.resolveLocation( "file:///" + basedir + "/target/classes/config/maven_checks.xml",
                                                     "maven_checks.xml" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
    }

    public void testLocalFile()
        throws Exception
    {
        String basedir = PlexusTestCase.getBasedir();

        File resolvedFile = locator.resolveLocation( basedir + "/target/classes/config/avalon_checks.xml",
                                                     "avalon_checks.xml" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
    }

    public void testResource()
        throws Exception
    {
        File resolvedFile = locator.resolveLocation( "META-INF/plexus/components.xml", "components.xml" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
        
        File resolvedConfig = locator.resolveLocation( "config/sun_checks.xml", "sun_checks.xml" );
        assertNotNull( "Test resolved file", resolvedConfig );
        assertTrue( "Test resolved file exists", resolvedConfig.exists() );
    }

    public void testException()
    {
        try
        {
            locator.resolveLocation( "edwin/punzalan", "exception" );

            fail( "Expected IOException not thrown" );
        }
        catch ( IOException e )
        {
            //expected
        }
    }
}
