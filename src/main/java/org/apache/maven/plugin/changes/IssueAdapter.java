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
import org.apache.maven.plugin.issues.IssueManagementSystem;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter that can adapt data models from other issue management system to the data models used in the changes.xml
 * file.
 * 
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class IssueAdapter
{
    private static final String UNKNOWN_ISSUE_TYPE = "";
    private IssueManagementSystem ims;

    /**
     * Create a new adapter.
     *
     * @param ims The issue management system that has the data that should be adapted
     */
    public IssueAdapter( IssueManagementSystem ims )
    {
        this.ims = ims;
    }

    private Map<String, IssueType> getIssueTypeMap()
    {
        return ims.getIssueTypeMap();
    }

    /**
     * Adapt a <code>List</code> of <code>Issue</code>s to a <code>List</code> of <code>Release</code>s.
     * 
     * @param issues The issues
     * @return A list of releases
     */
    public List<Release> getReleases( List<Issue> issues )
    {
        // A Map of releases keyed by fixVersion
        Map<String, Release> releasesMap = new HashMap<String, Release>();

        // Loop through all issues looking for fixVersions
        for ( Issue issue : issues )
        {
            // Do NOT create a release for issues that lack a fixVersion
            if ( issue.getFixVersions() != null )
            {
                for ( String fixVersion : issue.getFixVersions() )
                {
                    // Try to get a matching Release from the map
                    Release release = releasesMap.get( fixVersion );
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
        List<Release> releasesList = new ArrayList<Release>();
        for ( Release release : releasesMap.values() )
        {
            releasesList.add( release );
        }
        return releasesList;
    }

    /**
     * Create an <code>Action</code> from an issue.
     * 
     * @param issue The issue to extract the information from
     * @return An <code>Action</code>
     */
    public Action createAction( Issue issue )
    {
        Action action = new Action();

        // @todo We need to add something like issue.getPresentationIdentifier() to be able to support other IMSes
        // beside JIRA
        action.setIssue( issue.getKey() );

        // Try to map the IMS-specific issue type to one that is used in a changes.xml file
        IssueType type = null;
        if ( getIssueTypeMap().containsKey( issue.getType() ) )
        {
            type = getIssueTypeMap().get( issue.getType() );
            action.setType( type.modelRepresentation() );
        }
        else
        {
            action.setType( UNKNOWN_ISSUE_TYPE );
        }

        action.setDev( issue.getAssignee() );

        // Set dueTo to the empty String instead of null to make Velocity happy
        action.setDueTo( "" );
        // action.setDueTo( issue.getReporter() );

        action.setAction( issue.getSummary() );
        return action;
    }
}
