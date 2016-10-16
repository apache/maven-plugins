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

import org.apache.maven.project.MavenProject;

/**
 * A utility class for working with Project objects.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public final class ProjectUtils
{

    private ProjectUtils()
    {
    }

    /**
     * Check if the issue management system has been properly configured in the Maven project.
     *
     * @param project The Maven project
     * @param issueManagementSystem The name of the issue management system that is required
     * @param mojoResult What the calling mojo produces, used in the error messages
     * @return <code>null</code> if the &lt;issueManagement&gt; element of the POM is complete, otherwise a String
     *         containing the reason of the failed validation.
     */
    public static String validateIssueManagement( MavenProject project, String issueManagementSystem,
                                                  String mojoResult )
    {
        if ( project.getIssueManagement() == null )
        {
            return "No Issue Management set. No " + mojoResult + " will be generated.";
        }
        else if ( ( project.getIssueManagement().getUrl() == null )
            || ( project.getIssueManagement().getUrl().trim().equals( "" ) ) )
        {
            return "No URL set in Issue Management. No " + mojoResult + " will be generated.";
        }
        else if ( ( project.getIssueManagement().getSystem() != null )
            && !( project.getIssueManagement().getSystem().equalsIgnoreCase( issueManagementSystem ) ) )
        {
            return "The " + mojoResult + " only supports " + issueManagementSystem + ".  No " + mojoResult
                + " will be generated.";
        }
        return null;
    }
}
