package org.apache.maven.plugin.issues;

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

/**
 * Tests for the Issue class.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class IssueTestCase
    extends TestCase
{
    Issue issue;

    protected void setUp()
        throws Exception
    {
        issue = new Issue();
    }

    protected void tearDown()
        throws Exception
    {
    }

    public void testGetSetKey()
    {
        issue.setKey( "key" );

        assertEquals( "key", issue.getKey() );
    }

    public void testGetSetSummary()
    {
        issue.setSummary( "summary" );

        assertEquals( "summary", issue.getSummary() );
    }

    public void testGetSetStatus()
    {
        issue.setStatus( "status" );

        assertEquals( "status", issue.getStatus() );
    }

    public void testGetSetResolution()
    {
        issue.setResolution( "resolution" );

        assertEquals( "resolution", issue.getResolution() );
    }

    public void testGetSetAssignee()
    {
        issue.setAssignee( "assignee" );

        assertEquals( "assignee", issue.getAssignee() );
    }

}
