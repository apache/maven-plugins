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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2.8
 */
public class GitHubDownloader
{

    /**
     * The github client.
     */
    private GitHubClient client;

    /**
     * A boolean to indicate if we should include open issues as well
     */
    private boolean includeOpenIssues;

    /**
     * A boolean to indicate if we should only include issues with milestones
     */
    private boolean onlyMilestoneIssues;

    /**
     * The owner/organization of the github repo.
     */
    private String githubOwner;

    /**
     * The name of the github repo.
     */
    private String githubRepo;

    /**
     * The url to the github repo's issue management
     */
    private String githubIssueURL;

    public GitHubDownloader( MavenProject project, String githubScheme, int githubPort, boolean includeOpenIssues,
                             boolean onlyMilestoneIssues )
                                 throws MalformedURLException
    {
        this.includeOpenIssues = includeOpenIssues;
        this.onlyMilestoneIssues = onlyMilestoneIssues;

        URL githubURL = new URL( project.getIssueManagement().getUrl() );

        // The githubclient prefers to connect to 'github.com' using the api domain, unlike github enterprise
        // which can connect fine using its domain, so for github.com use empty constructor
        if ( githubURL.getHost().equalsIgnoreCase( "github.com" ) )
        {
            this.client = new GitHubClient();
        }
        else
        {
            this.client = new GitHubClient( githubURL.getHost(), githubPort, githubScheme );
        }

        this.githubIssueURL = project.getIssueManagement().getUrl();
        if ( !this.githubIssueURL.endsWith( "/" ) )
        {
            this.githubIssueURL = this.githubIssueURL + "/";
        }

        String urlPath = githubURL.getPath();
        if ( urlPath.startsWith( "/" ) )
        {
            urlPath = urlPath.substring( 1 );
        }

        if ( urlPath.endsWith( "/" ) )
        {
            urlPath = urlPath.substring( 0, urlPath.length() - 2 );
        }

        String[] urlPathParts = urlPath.split( "/" );

        if ( urlPathParts.length != 3 )
        {
            throw new MalformedURLException( "GitHub issue management URL must look like, "
                + "[GITHUB_DOMAIN]/[OWNER]/[REPO]/issues" );
        }

        this.githubOwner = urlPathParts[0];
        this.githubRepo = urlPathParts[1];
    }

    protected Issue createIssue( org.eclipse.egit.github.core.Issue githubIssue )
    {
        Issue issue = new Issue();

        issue.setKey( String.valueOf( githubIssue.getNumber() ) );
        issue.setId( String.valueOf( githubIssue.getNumber() ) );

        issue.setLink( this.githubIssueURL + githubIssue.getNumber() );

        issue.setCreated( githubIssue.getCreatedAt() );

        issue.setUpdated( githubIssue.getUpdatedAt() );

        if ( githubIssue.getAssignee() != null )
        {
            if ( githubIssue.getAssignee().getName() != null )
            {
                issue.setAssignee( githubIssue.getAssignee().getName() );
            }
            else
            {
                issue.setAssignee( githubIssue.getAssignee().getLogin() );
            }
        }

        issue.setTitle( githubIssue.getTitle() );

        issue.setSummary( githubIssue.getTitle() );

        if ( githubIssue.getMilestone() != null )
        {
            issue.addFixVersion( githubIssue.getMilestone().getTitle() );
        }

        issue.setReporter( githubIssue.getUser().getLogin() );

        if ( githubIssue.getClosedAt() != null )
        {
            issue.setStatus( "closed" );
        }
        else
        {
            issue.setStatus( "open" );
        }

        List<Label> labels = githubIssue.getLabels();
        if ( labels != null && !labels.isEmpty() )
        {
            issue.setType( labels.get( 0 ).getName() );
        }

        return issue;
    }

    public List<Issue> getIssueList()
        throws IOException
    {
        List<Issue> issueList = new ArrayList<Issue>();

        IssueService service = new IssueService( client );
        Map<String, String> issueFilter = new HashMap<String, String>();

        if ( includeOpenIssues )
        {
            // Adding open issues

            for ( org.eclipse.egit.github.core.Issue issue : service.getIssues( githubOwner, githubRepo, issueFilter ) )
            {
                if ( !onlyMilestoneIssues || issue.getMilestone() != null )
                {
                    issueList.add( createIssue( issue ) );
                }
            }
        }

        // Adding closed issues

        issueFilter.put( "state", "closed" );

        for ( org.eclipse.egit.github.core.Issue issue : service.getIssues( githubOwner, githubRepo, issueFilter ) )
        {
            if ( !onlyMilestoneIssues || issue.getMilestone() != null )
            {
                issueList.add( createIssue( issue ) );
            }
        }

        return issueList;
    }

    public void configureAuthentication( SettingsDecrypter decrypter, String githubAPIServerId, Settings settings,
                                         Log log )
    {
        boolean configured = false;

        List<Server> servers = settings.getServers();

        for ( Server server : servers )
        {
            if ( server.getId().equals( githubAPIServerId ) )
            {
                SettingsDecryptionResult result = decrypter.decrypt( new DefaultSettingsDecryptionRequest( server ) );
                for ( SettingsProblem problem : result.getProblems() )
                {
                    log.error( problem.getMessage(), problem.getException() );
                }
                server = result.getServer();
                String user = server.getUsername();
                String password = server.getPassword();
                this.client.setCredentials( user, password );

                configured = true;
                break;
            }
        }

        if ( !configured )
        {
            log.warn( "Can't find server id [" + githubAPIServerId + "] configured in githubAPIServerId." );
        }
    }

}
