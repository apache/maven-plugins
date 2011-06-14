package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugin.issues.IssueManagementSystem;
import org.apache.maven.plugin.jira.JIRAIssueManagmentSystem;
import org.apache.maven.plugins.changes.model.Action;

import junit.framework.TestCase;

/**
 * @author Alan Parkinson
 * @since 2.6
 */
public class IssueAdapterTest
    extends TestCase
{

    public void testDefaultIssueTypeMapping()
    {
        IssueAdapter adapter = new IssueAdapter( new JIRAIssueManagmentSystem() );

        Issue issue = createIssue( "TST-1", "New Feature" );
        Action action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-2", "Bug" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        issue = createIssue( "TST-3", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "update", action.getType() );

        issue = createIssue( "TST-4", "Unknown Type" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );
    }

    public void testCustomIssueTypeMappingOveridesDefaultMapping()
    {
        IssueManagementSystem ims = new JIRAIssueManagmentSystem();
        
        ims.getIssueTypeMap().clear();
        IssueAdapter adapter = new IssueAdapter( ims );

        Issue issue = createIssue( "TST-1", "New Feature" );
        Action action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-2", "Bug" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-3", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );

        issue = createIssue( "TST-4", "Unknown Type" );
        action = adapter.createAction( issue );
        assertEquals( "", action.getType() );
    }

    public void testCustomIssueTypeMapping()
    {
        IssueManagementSystem ims = new JIRAIssueManagmentSystem();
        ims.getIssueTypeMap().put( "Story", IssueType.ADD);
        ims.getIssueTypeMap().put( "Epic", IssueType.ADD);
        ims.getIssueTypeMap().put( "Defect", IssueType.FIX);
        ims.getIssueTypeMap().put( "Error", IssueType.FIX);
        IssueAdapter adapter = new IssueAdapter( ims );

        Issue issue = createIssue( "TST-1", "Story" );
        Action action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-2", "Epic" );
        action = adapter.createAction( issue );
        assertEquals( "add", action.getType() );

        issue = createIssue( "TST-3", "Error" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        issue = createIssue( "TST-4", "Defect" );
        action = adapter.createAction( issue );
        assertEquals( "fix", action.getType() );

        // Test the default mapping for "update" hasn't been overridden
        issue = createIssue( "TST-5", "Improvement" );
        action = adapter.createAction( issue );
        assertEquals( "update", action.getType() );
    }

    private Issue createIssue( String key, String type )
    {
        Issue issue = new Issue();
        issue.setKey( key );
        issue.setType( type );
        issue.setAssignee( "A User" );
        issue.setSummary( "The title of this issue" );
        return issue;
    }
}
