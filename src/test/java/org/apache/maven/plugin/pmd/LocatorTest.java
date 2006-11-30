package org.apache.maven.plugin.pmd;

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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class LocatorTest
    extends PlexusTestCase
{
    private Locator locator;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        locator = new Locator( null );
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
        copyFile( new File( getBasedir(), "src/main/resources/cpd-report.properties" ),
                  new File( getBasedir(), "target/test/unit/locator-test" ), "cpd-report.properties" );

        File resolvedFile = locator.resolveLocation(
            "file:///" + getBasedir() + "/target/test/unit/locator-test/cpd-report.properties",
            "cpd-report.properties" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
    }

    public void testLocalFile()
        throws Exception
    {
        copyFile( new File( getBasedir(), "src/main/resources/cpd-report.properties" ),
                  new File( getBasedir(), "target/test/unit/locator-test" ), "cpd-report.properties" );

        File resolvedFile = locator.resolveLocation(
            getBasedir() + "/target/test/unit/locator-test/cpd-report.properties", "cpd-report.properties" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
    }

    public void testResource()
        throws Exception
    {
        File resolvedFile = locator.resolveLocation( "META-INF/plexus/components.xml", "components.xml" );

        assertNotNull( "Test resolved file", resolvedFile );
        assertTrue( "Test resolved file exists", resolvedFile.exists() );
    }

    public void testException()
    {
        try
        {
            locator.resolveLocation( "sample/test", "exception" );

            fail( "Expected IOException not thrown" );
        }
        catch ( IOException e )
        {
            assertTrue( true );
        }
    }

    protected void tearDown()
        throws Exception
    {

    }

    /**
     * Copy the source file to new directory
     *
     * @param src
     * @param dest
     * @throws IOException
     */
    private void copyFile( File src, File dest, String filename )
        throws IOException
    {
        FileUtils.fileDelete( dest.getAbsolutePath() + "/" + filename );
        FileUtils.copyFileToDirectory( src, dest );
    }
}
