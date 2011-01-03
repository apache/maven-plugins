package org.apache.maven.plugin.jira;

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

import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An adapter that can adapt JIRA specific data models to the data model used
 * by a changes.xml file.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class JiraAdapter
{
    /**
     * Adapt a <code>List</code> of <code>Issue</code>s to a
     * <code>List</code> of <code>Release</code>s.
     *
     * @param issues The JIRA issues
     * @return A list of releases
     */
    public static List getReleases( List issues )
    {
        // A Map of releases keyed by fixVersion
        Map releasesMap = new HashMap();

        // Loop through all issues looking for fixVersions
        for ( int i = 0; i < issues.size(); i++ )
        {
            Issue issue = (Issue) issues.get( i );
            // Do NOT create a release for issues that lack a fixVersion
            if ( issue.getFixVersions() != null )
            {
                for ( Iterator iterator = issue.getFixVersions().iterator(); iterator.hasNext(); )
                {
                    String fixVersion = (String) iterator.next();

                    // Try to get a matching Release from the map
                    Release release = (Release) releasesMap.get( fixVersion );
                    if ( release == null )
                    {
                        // Add a new Release to the Map if it wasn't there
                        release = new Release();
                        release.setVersion( fixVersion );
                        releasesMap.put( fixVersion, release );
                    }

                    // Add this issue as an Action to this release
                    Action action = createAction( issue );
                    release.addAction( action );
                }
            }
        }

        // Extract the releases from the Map to a List
        List releasesList = new ArrayList();
        for ( Iterator iterator = releasesMap.entrySet().iterator(); iterator.hasNext(); )
        {
            Release o = (Release) ( (Map.Entry) iterator.next() ).getValue();
            releasesList.add( o );
        }
        return releasesList;
    }

    /**
     * Create an <code>Action</code> from an issue.
     *
     * @param issue The issue to extract the information from
     * @return An <code>Action</code>
     */
    public static Action createAction( Issue issue )
    {
        Action action = new Action();

        action.setIssue( issue.getKey() );

        String type = "";
        if ( issue.getType().equals( "Bug" ) )
        {
            type = "fix";
        }
        else if ( issue.getType().equals( "New Feature" ) )
        {
            type = "add";
        }
        else if ( issue.getType().equals( "Improvement" ) )
        {
            type = "update";
        }
        action.setType( type );

        action.setDev( issue.getAssignee() );

        // Set dueTo to the empty String instead of null to make Velocity happy
        action.setDueTo( "" );
        //action.setDueTo( issue.getReporter() );

        action.setAction( issue.getSummary() );
        return action;
    }
}
