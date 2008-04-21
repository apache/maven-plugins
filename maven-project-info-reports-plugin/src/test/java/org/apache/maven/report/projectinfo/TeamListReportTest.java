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
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class TeamListReportTest
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
        generateReport( "project-team", "project-team-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "team-list.html" ).exists() );

        URL reportURL = getGeneratedReport( "team-list.html" ).toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        assertEquals( getString( "report.team-list.name" ) + " - " + getString( "report.team-list.title" ),
                      response.getTitle() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();

        assertEquals( textBlocks.length, 7 );

        assertEquals( getString( "report.team-list.intro.title" ), textBlocks[0].getText() );
        assertEquals( getString( "report.team-list.intro.description1" ), textBlocks[1].getText() );
        assertEquals( getString( "report.team-list.intro.description2" ), textBlocks[2].getText() );
        assertEquals( getString( "report.team-list.developers.title" ), textBlocks[3].getText() );
        assertEquals( getString( "report.team-list.developers.intro" ), textBlocks[4].getText() );
        assertEquals( getString( "report.team-list.contributors.title" ), textBlocks[5].getText() );
        assertEquals( getString( "report.team-list.nocontributor" ), textBlocks[6].getText() );
    }
}
