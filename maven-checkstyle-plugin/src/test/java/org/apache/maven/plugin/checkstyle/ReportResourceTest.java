package org.apache.maven.plugin.checkstyle;

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
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class ReportResourceTest
    extends TestCase
{
    private File outputDir = new File( PlexusTestCase.getBasedir(), "target/unit-test/ReportResource" );

    public void testConstructor()
    {
        ReportResource reportResource = new ReportResource( "reportbase", outputDir );
        assertEquals( "Test resourcePathBase", "reportbase", reportResource.getResourcePathBase() );
        assertEquals( "Test outputDirectory", outputDir, reportResource.getOutputDirectory() );
    }

    public void testSetters()
    {
        ReportResource reportResource = new ReportResource( null, null );
        reportResource.setResourcePathBase( "reportbase" );
        reportResource.setOutputDirectory( outputDir );

        assertEquals( "Test resourcePathBase", "reportbase", reportResource.getResourcePathBase() );
        assertEquals( "Test outputDirectory", outputDir, reportResource.getOutputDirectory() );
    }

    public void testCopy()
        throws Exception
    {
        ReportResource reportResource = new ReportResource( "META-INF/plexus", outputDir );
        reportResource.copy( "components.xml" );

        File copiedFile = new File( outputDir, "components.xml" );
        assertTrue( "Test copied file exists", copiedFile.exists() );
    }
}
