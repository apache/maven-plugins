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
import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the IssueUtils class.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class IssueUtilsTestCase
    extends TestCase
{
    public void testFilterIssuesWithVersionPrefix()
    {
        Issue issue_1;
        issue_1 = new Issue();
        issue_1.setId( "1" );
        issue_1.addFixVersion( "myPrefix-1.0" );

        Issue issue_2;
        issue_2 = new Issue();
        issue_2.setId( "2" );
        issue_2.addFixVersion( "1.0" );

        List issueList = new ArrayList();
        issueList.add( issue_1 );
        issueList.add( issue_2 );

        List filteredIssues = null;
        try
        {
            filteredIssues = IssueUtils.filterIssuesWithVersionPrefix( issueList, null );
            assertEquals( 2, filteredIssues.size() );

            filteredIssues = IssueUtils.filterIssuesWithVersionPrefix( issueList, "" );
            assertEquals( 2, filteredIssues.size() );

            filteredIssues = IssueUtils.filterIssuesWithVersionPrefix( issueList, "myPrefix-" );
            assertEquals( 1, filteredIssues.size() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getMessage() );
        }

        try
        {
            filteredIssues = IssueUtils.filterIssuesWithVersionPrefix( issueList, "yourPrefix-" );
            fail("No issues should be found.");
        }
        catch ( MojoExecutionException e )
        {
            // Expected
        }

    }

}
