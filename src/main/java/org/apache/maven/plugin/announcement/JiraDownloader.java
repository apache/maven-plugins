package org.apache.maven.plugin.announcement;

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

import org.apache.maven.plugin.jira.AbstractJiraDownloader;

/**
 * Gets relevant issues for an announcement.
 *
 * @author mfranken@xebia.com
 * @author jruiz@exist.com
 * @version $Id$
 */
public final class JiraDownloader extends AbstractJiraDownloader
{
    public JiraDownloader()
    {
        statusMap.put( "Closed", "6" );

        resolutionMap.put( "Fixed", "1" );

        priorityMap.put( "Blocker", "1" );
        priorityMap.put( "Critical", "2" );
        priorityMap.put( "Major", "3" );
        priorityMap.put( "Minor", "4" );
        priorityMap.put( "Trivial", "5" );
    }
}
