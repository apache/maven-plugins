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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.egit.github.core.User;
import org.mockito.ArgumentCaptor;

import junit.framework.TestCase;

public class GitHubDownloaderTestCase
    extends TestCase
{

    public void testCreateIssue()
        throws IOException
    {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader( issueManagement );

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

    public void testConfigureAuthenticationWithProblems()
        throws Exception
    {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader( issueManagement );
        Settings settings = new Settings();
        Server server = newServer( "github-server" );
        settings.addServer( server );
        SettingsDecrypter decrypter = mock( SettingsDecrypter.class );
        SettingsDecryptionResult result = mock( SettingsDecryptionResult.class );
        Log log = mock( Log.class );
        when( result.getProblems() ).thenReturn( Arrays.<SettingsProblem>asList( new DefaultSettingsProblem( "Ups "
            + server.getId(), Severity.ERROR, null, -1, -1, null ) ) );
        when( result.getServer() ).thenReturn( server );
        when( decrypter.decrypt( any( SettingsDecryptionRequest.class ) ) ).thenReturn( result );

        gitHubDownloader.configureAuthentication( decrypter, "github-server", settings, log );

        verify( log ).error( "Ups github-server", null );
        ArgumentCaptor<SettingsDecryptionRequest> argument = ArgumentCaptor.forClass( SettingsDecryptionRequest.class );
        verify( decrypter ).decrypt( argument.capture() );
        List<Server> servers = ( (DefaultSettingsDecryptionRequest) argument.getValue() ).getServers();
        assertEquals( 1, servers.size() );
        assertSame( server, servers.get( 0 ) );
    }

    public void testConfigureAuthenticationWithNoServer()
        throws Exception
    {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader( issueManagement );
        Settings settings = new Settings();
        Server server = newServer( "not-the-right-one" );
        settings.addServer( server );
        SettingsDecrypter decrypter = mock( SettingsDecrypter.class );
        SettingsDecryptionResult result = mock( SettingsDecryptionResult.class );
        Log log = mock( Log.class );
        when( result.getProblems() ).thenReturn( Collections.<SettingsProblem>emptyList() );
        when( result.getServer() ).thenReturn( server );
        when( decrypter.decrypt( new DefaultSettingsDecryptionRequest( server ) ) ).thenReturn( result );

        gitHubDownloader.configureAuthentication( decrypter, "github-server", settings, log );

        verify( log ).warn( "Can't find server id [github-server] configured in githubAPIServerId." );
    }

    private Server newServer( String id )
    {
        Server server = new Server();
        server.setId( id );
        server.setUsername( "some-user" );
        server.setPassword( "Sup3rSecret" );
        return server;
    }

    private GitHubDownloader newGitHubDownloader( IssueManagement issueManagement )
        throws MalformedURLException
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setIssueManagement( issueManagement );
        return new GitHubDownloader( mavenProject, "https", 80, true, false );
    }

    private IssueManagement newGitHubIssueManagement()
    {
        IssueManagement issueManagement = new IssueManagement();
        issueManagement.setSystem( "GitHub" );
        issueManagement.setUrl( "https://github.com/dadoonet/spring-elasticsearch/issues/" );
        return issueManagement;
    }

}
