package org.apache.maven.report.projectinfo;

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

import java.net.URL;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

/**
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
public class PluginManagementReportTest
    extends AbstractProjectInfoTestCase
{
    /**
     * WebConversation object
     */
    private static final WebConversation WEB_CONVERSATION = new WebConversation();

    /**
     * Test report
     *
     * @throws Exception if any
     */
    public void testReport()
        throws Exception
    {
        generateReport( "plugin-management", "plugin-management-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "plugin-management.html" ).exists() );

        URL reportURL = getGeneratedReport( "plugin-management.html" ).toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        assertEquals( getString( "report.pluginManagement.name" ) + " - "
                      + getString( "report.pluginManagement.title" ), response.getTitle() );

        // Test the tables
        WebTable[] webTables = response.getTables();
        assertEquals( webTables.length, 1 );

        assertEquals( webTables[0].getColumnCount(), 3 );
        assertEquals( webTables[0].getRowCount(), 1 + getTestMavenProject().getPluginManagement().getPlugins().size() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals( textBlocks[0].getText(), getString( "report.pluginManagement.title" ) );
    }
}
