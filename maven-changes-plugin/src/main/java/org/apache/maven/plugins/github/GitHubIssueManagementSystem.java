package org.apache.maven.plugins.github;

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

import org.apache.maven.plugins.changes.IssueType;
import org.apache.maven.plugins.issues.AbstractIssueManagementSystem;

/**
 * @since 2.8
 */
public class GitHubIssueManagementSystem
    extends AbstractIssueManagementSystem
{

    private static final String DEFAULT_ADD_TYPE = "enhancement";

    private static final String DEFAULT_FIX_TYPE = "bug";

    public GitHubIssueManagementSystem()
    {
        super();
        // The standard issue types for GitHub
        issueTypeMap.put( DEFAULT_ADD_TYPE, IssueType.ADD );
        issueTypeMap.put( DEFAULT_FIX_TYPE, IssueType.FIX );
    }

    @Override
    public String getName()
    {
        return "Github";
    }

}
