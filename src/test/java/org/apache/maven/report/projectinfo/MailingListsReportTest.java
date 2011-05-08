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
import java.util.Locale;

import org.apache.maven.report.projectinfo.MailingListsReport.MailingListsRenderer;

import junitx.util.PrivateAccessor;

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
public class MailingListsReportTest
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
        generateReport( "mailing-list", "mailing-list-plugin-config.xml" );
        assertTrue( "Test html generated", getGeneratedReport( "mail-lists.html" ).exists() );

        URL reportURL = getGeneratedReport( "mail-lists.html" ).toURI().toURL();
        assertNotNull( reportURL );

        // HTTPUnit
        WebRequest request = new GetMethodWebRequest( reportURL.toString() );
        WebResponse response = WEB_CONVERSATION.getResponse( request );

        // Basic HTML tests
        assertTrue( response.isHTML() );
        assertTrue( response.getContentLength() > 0 );

        // Test the Page title
        assertEquals( getString( "report.mailing-lists.title" ), response.getTitle() );

        // Test the texts
        TextBlock[] textBlocks = response.getTextBlocks();
        assertEquals( textBlocks[0].getText(), getString( "report.mailing-lists.title" ) );
        assertEquals( textBlocks[1].getText(), getString( "report.mailing-lists.intro" ) );
    }

    /**
     * Test report in French (MPIR-59)
     *
     * @throws Exception if any
     */
    public void testFrenchReport()
        throws Exception
    {
        Locale oldLocale = Locale.getDefault();

        try
        {
            Locale.setDefault( Locale.FRENCH );

            generateReport( "mailing-list", "mailing-list-plugin-config.xml" );
            assertTrue( "Test html generated", getGeneratedReport( "mail-lists.html" ).exists() );
        }
        finally
        {
            Locale.setDefault( oldLocale );
        }
    }

    /**
     * @throws Throwable if any
     */
    public void testGetArchiveServer()
        throws Throwable
    {
        String server = "http://mail-archives.apache.org/mod_mbox/maven-announce/";
        assertEquals( "mail-archives.apache.org", invokeGetArchiveServer( server ) );

        server = "http://mail-archives.apache.org/mod_mbox/maven-announce";
        assertEquals( "mail-archives.apache.org", invokeGetArchiveServer( server ) );

        server = "http://www.mail-archive.com/announce@maven.apache.org";
        assertEquals( "www.mail-archive.com", invokeGetArchiveServer( server ) );

        server = "http://www.nabble.com/Maven-Announcements-f15617.html";
        assertEquals( "www.nabble.com", invokeGetArchiveServer( server ) );

        server = "http://maven.announce.markmail.org/";
        assertEquals( "maven.announce.markmail.org", invokeGetArchiveServer( server ) );

        server = "http://maven.announce.markmail.org";
        assertEquals( "maven.announce.markmail.org", invokeGetArchiveServer( server ) );
    }

    /**
     * @throws Throwable if any
     */
    private static String invokeGetArchiveServer( String s )
        throws Throwable
    {
        return (String) PrivateAccessor.invoke( MailingListsRenderer.class, "getArchiveServer",
                                                new Class[] { String.class }, new Object[] { s } );
    }
}
