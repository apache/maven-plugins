/*
 * Licensed to Elasticsearch under one or more contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Elasticsearch licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.maven.plugin.github;

import junit.framework.TestCase;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.User;

import java.io.IOException;

public class GitHubDownloaderTestCase
    extends TestCase
{

    public void testCreateIssue()
        throws IOException
    {

        MavenProject mavenProject = new MavenProject();
        IssueManagement issueManagement = new IssueManagement();
        issueManagement.setSystem( "GitHub" );
        issueManagement.setUrl( "https://github.com/dadoonet/spring-elasticsearch/issues/" );
        mavenProject.setIssueManagement( issueManagement );

        GitHubDownloader gitHubDownloader = new GitHubDownloader( mavenProject, "https", 80, true, false );

        org.eclipse.egit.github.core.Issue githubIssue = new org.eclipse.egit.github.core.Issue();
        githubIssue.setNumber( 1 );
        githubIssue.setBody( "Body" );
        githubIssue.setTitle( "Title" );
        User user = new User();
        githubIssue.setUser( user );

        Issue issue = gitHubDownloader.createIssue( githubIssue );

        assertEquals( Integer.toString( githubIssue.getNumber() ), issue.getId() );
        assertEquals( Integer.toString( githubIssue.getNumber() ), issue.getKey() );
        assertEquals( githubIssue.getTitle(), issue.getTitle() );
        assertEquals( githubIssue.getTitle(), issue.getSummary() );
        assertEquals( issueManagement.getUrl() + githubIssue.getNumber(), issue.getLink() );
    }
}
