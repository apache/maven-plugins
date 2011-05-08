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
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * @author Edwin Punzalan
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class IssueTrackingReportTest
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
        generateReport( "issue-tracking", "issue-tracking-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "issue-tracking.html" ).exists() );

        URL reportURL = getGeneratedReport( "issue-tracking.html" ).toURI().toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        assertEquals( getString( "report.issuetracking.title" ), response.getTitle() );

        // Test the links
        WebLink[] weblinks = response.getLinks();
        assertEquals( 3, weblinks.length );

        assertEquals( "JIRA", weblinks[1].getText() );

        assertEquals( "http://localhost/jira", weblinks[2].getText() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals( getString( "report.issuetracking.overview.title" ), textBlocks[0].getText() );
        assertEquals( "This project uses JIRA a J2EE-based, issue tracking and project management application.", textBlocks[1].getText() ); // due to link pattern
        assertEquals( getString( "report.issuetracking.name" ), textBlocks[2].getText() );
    }
}
