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
    /**
     * Find the issues that has a Fix Version that matches the supplied prefix.
     *
     * @param issues A list of issues
     * @param prefix The prefix of the "Fix Version" that should match
     * @return A <code>List</code> of issues fixed in versions that match the supplied prefix
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          If no issues could be found for the supplied prefix
     */
    public static List filterIssuesWithVersionPrefix( List issues, String prefix )
        throws MojoExecutionException
    {
        List filteredIssues = new ArrayList();
        boolean isFound = false;
        Issue issue = null;

        for ( int i = 0; i < issues.size(); i++ )
        {
            issue = (Issue) issues.get( i );

            if ( issue.getFixVersions() != null )
            {
                for ( int j = 0; j < issue.getFixVersions().size(); j++ )
                {
                    String fixVersion = (String) issue.getFixVersions().get( j );
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
}
