package org.apache.maven.plugin.trac;

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

import org.apache.maven.plugin.changes.IssueType;
import org.apache.maven.plugin.issues.AbstractIssueManagementSystem;

/**
 * The Trac issue management system.
 */
public class TracIssueManagmentSystem
    extends AbstractIssueManagementSystem
{
    private static final String DEFAULT_ADD_TYPE = "New Feature";
    private static final String DEFAULT_FIX_TYPE = "Bug";
    private static final String DEFAULT_UPDATE_TYPE = "Improvement";

    public TracIssueManagmentSystem()
    {
        super();
        // The standard issue types for Trac (probably wrong)
        issueTypeMap.put( DEFAULT_ADD_TYPE, IssueType.ADD );
        issueTypeMap.put( DEFAULT_FIX_TYPE, IssueType.FIX );
        issueTypeMap.put( DEFAULT_UPDATE_TYPE, IssueType.UPDATE );
    }

    @Override
    public String getName()
    {
        return "Trac";
    }

}
