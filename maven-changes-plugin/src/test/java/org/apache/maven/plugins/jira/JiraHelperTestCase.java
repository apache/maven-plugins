package org.apache.maven.plugins.jira;

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

import junit.framework.TestCase;

import java.util.Map;

import org.apache.maven.plugins.jira.JiraHelper;

/**
 * Tests for the JiraHelper class.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class JiraHelperTestCase
    extends TestCase
{
    public void testGetJiraUrlAndProjectId()
    {
        Map<String, String> map;

        map = JiraHelper.getJiraUrlAndProjectId( "https://issues.apache.org/jira/browse/DOXIA" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );

        // MCHANGES-218
        map = JiraHelper.getJiraUrlAndProjectId( "https://issues.apache.org/jira/browse/DOXIA/" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );

        // MCHANGES-222
        map =
            JiraHelper.getJiraUrlAndProjectId( "https://issues.apache.org/jira/secure/IssueNavigator.jspa?pid=11761&reset=true" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );
        map = JiraHelper.getJiraUrlAndProjectId( "https://issues.apache.org/jira/browse/MSHARED/component/13380" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );
    }

    public void testGetJiraUrlAndProjectName()
    {
        Map<String, String> map;
        map = JiraHelper.getJiraUrlAndProjectName( "https://issues.apache.org/jira/browse/DOXIA/" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );
        assertEquals( "DOXIA", map.get( "project" ) );

        map = JiraHelper.getJiraUrlAndProjectName( "https://issues.apache.org/jira/browse/DOXIA" );
        assertEquals( "https://issues.apache.org/jira", map.get( "url" ) );
        assertEquals( "DOXIA", map.get( "project" ) );
    }

    public void testGetBaseUrl()
    {
        String expected = "http://www.jira.com";
        String actual = JiraHelper.getBaseUrl( "http://www.jira.com/context/test?werewrew" );
        assertEquals( expected, actual );

        expected = "https://www.jira.com";
        actual = JiraHelper.getBaseUrl( "https://www.jira.com/context/test?werewrew" );
        assertEquals( expected, actual );
    }
}
