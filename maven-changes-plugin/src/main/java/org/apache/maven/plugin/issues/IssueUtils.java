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
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for working with issue objects.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class IssueUtils
{
    public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * Find the issues that has a Fix Version that matches the supplied prefix.
     *
     * @param issues A list of issues
     * @param prefix The prefix of the "Fix Version" that should match
     * @return A <code>List</code> of issues fixed in versions that match the supplied prefix
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          If no issues could be found for the supplied prefix
     */
    public static List<Issue> filterIssuesWithVersionPrefix( List<Issue> issues, String prefix )
        throws MojoExecutionException
    {
        List<Issue> filteredIssues = new ArrayList<Issue>();
        boolean isFound = false;
        Issue issue = null;

        for ( int i = 0; i < issues.size(); i++ )
        {
            issue = (Issue) issues.get( i );

            if ( issue.getFixVersions() != null )
            {
                for ( String fixVersion : issue.getFixVersions() )
                {
                    if ( prefix == null || fixVersion.startsWith( prefix ) )
                    {
                        isFound = true;
                        filteredIssues.add( issue );
                        break;
                    }
                }
            }
        }

        if ( !isFound )
        {
            throw new MojoExecutionException(
                "Couldn't find any issues with a Fix Version prefix of '" + prefix + "' among the supplied issues." );
        }
        return filteredIssues;
    }

    /**
     * Find the issues for only the supplied version, by matching the "Fix for"
     * version in the supplied list of issues with the supplied version.
     * If the supplied version is a SNAPSHOT, then that part of the version
     * will be removed prior to the matching.
     *
     * @param issues A list of issues
     * @param version The version that issues should be returned for
     * @return A <code>List</code> of issues for the supplied version
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          If no issues could be found for the supplied version
     */
    public static List<Issue> getIssuesForVersion( List<Issue> issues, String version )
        throws MojoExecutionException
    {
        List<Issue> issuesForVersion = new ArrayList<Issue>();
        boolean isFound = false;
        Issue issue = null;
        String releaseVersion = version;

        // Remove "-SNAPSHOT" from the end of the version, if it's there
        if ( version != null && version.endsWith( SNAPSHOT_SUFFIX ) )
        {
            releaseVersion = version.substring( 0, version.length() - SNAPSHOT_SUFFIX.length() );
        }

        for ( int i = 0; i < issues.size(); i++ )
        {
            issue = (Issue) issues.get( i );

            if ( issue.getFixVersions() != null && issue.getFixVersions().contains( releaseVersion ) )
            {
                isFound = true;
                issuesForVersion.add( issue );
            }
        }

        if ( !isFound )
        {
            throw new MojoExecutionException(
                "Couldn't find any issues for the version '" + releaseVersion + "' among the supplied issues." );
        }
        return issuesForVersion;
    }
}
