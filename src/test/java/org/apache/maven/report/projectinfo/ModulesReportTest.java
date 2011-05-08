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

/**
 * @author ltheussl
 * @version $Id$
 */
public class ModulesReportTest
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
        generateReport( "modules", "modules-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "modules.html" ).exists() );

        URL reportURL = getGeneratedReport( "modules.html" ).toURI().toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        assertEquals( getString( "report.modules.title" ), response.getTitle() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals( 2, textBlocks.length );
        assertEquals( getString( "report.modules.title" ), textBlocks[0].getText() );
        assertEquals( getString( "report.modules.intro" ), textBlocks[1].getText() );

        String[][] cellTexts = response.getTables()[0].asText();
        assertEquals( 3, cellTexts.length );
        assertEquals( 2, cellTexts[0].length );
        assertEquals( getString( "report.modules.header.name" ), cellTexts[0][0] );
        assertEquals( getString( "report.modules.header.description" ), cellTexts[0][1] );
        assertEquals( "project1", cellTexts[1][0] );
        assertEquals( "-", cellTexts[1][1] );
        assertEquals( "project2", cellTexts[2][0] );
        assertEquals( "project2 description", cellTexts[2][1] );
    }
}
