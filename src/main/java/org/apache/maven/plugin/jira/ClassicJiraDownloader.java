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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Gets relevant issues for a JIRA report via HTTP/RSS.
 *
 * @author mfranken@xebia.com
 * @author jruiz@exist.com
 * @version $Id$
 */
public final class ClassicJiraDownloader
    extends AbstractJiraDownloader
{
    public ClassicJiraDownloader()
    {
    }

    /**
     * Execute the query on the JIRA server.
     *
     * @throws Exception on error
     */
    public void doExecute()
        throws Exception
    {
        try
        {
            HttpClient client = new HttpClient();

            // MCHANGES-89 Allow circular redirects
            HttpClientParams clientParams = client.getParams();
            clientParams.setBooleanParameter( HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true );
            clientParams.setCookiePolicy( CookiePolicy.BROWSER_COMPATIBILITY ); //MCHANGES-237

            HttpState state = new HttpState();

            HostConfiguration hc = new HostConfiguration();

            client.setHostConfiguration( hc );

            client.setState( state );

            String baseUrl = JiraHelper.getBaseUrl( project.getIssueManagement().getUrl() );

            getLog().debug( "JIRA lives at: " + baseUrl );
            // Here we only need the host part of the URL
            determineProxy( baseUrl, client );

            prepareBasicAuthentication( client );

            boolean jiraAuthenticationSuccessful = false;
            if ( isJiraAuthenticationConfigured() )
            {
                // Here we only need the parts up to and including the host part of the URL
                jiraAuthenticationSuccessful = doJiraAuthentication( client, baseUrl );
            }

            if ( ( isJiraAuthenticationConfigured() && jiraAuthenticationSuccessful )
                || !isJiraAuthenticationConfigured() )
            {
                String fullUrl;

                if ( useJql )
                {
                    fullUrl = getJqlQueryURL();
                }
                else
                {
                    fullUrl = getParameterBasedQueryURL( client );
                }
                if ( log.isDebugEnabled() )
                {
                    log.debug( "download jira issues from url " + fullUrl );
                }

                // execute the GET
                download( client, fullUrl );
            }
        }
        catch ( Exception e )
        {
            if ( project.getIssueManagement() != null )
            {
                getLog().error( "Error accessing " + project.getIssueManagement().getUrl(), e );
            }
            else
            {
                getLog().error( "Error accessing mock project issues", e );
            }
        }
    }

    private String getJqlQueryURL()
    {
        // JQL is based on project names instead of project ID's
        Map<String, String> urlMap = JiraHelper.getJiraUrlAndProjectName( project.getIssueManagement().getUrl() );
        String jiraUrl = urlMap.get( "url" );
        String jiraProject = urlMap.get( "project" );

        if ( jiraProject == null )
        {
            throw new RuntimeException( "The issue management URL in the POM does not include a JIRA project name" );
        }
        else
        {
            // create the URL for getting the proper issues from JIRA
            String jqlQuery = new JqlQueryBuilder( log ).project( jiraProject ).fixVersion( getFixFor() ).fixVersionIds(
                fixVersionIds ).statusIds( statusIds ).priorityIds( priorityIds ).resolutionIds(
                resolutionIds ).components( component ).typeIds( typeIds ).sortColumnNames( sortColumnNames ).build();

            String url =
                new UrlBuilder( jiraUrl, "sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml" ).addParameter(
                    "tempMax", nbEntriesMax ).addParameter( "reset", "true" ).addParameter( "jqlQuery",
                                                                                            jqlQuery ).build();

            return url;
        }
    }

    private String getParameterBasedQueryURL( HttpClient client )
    {
        Map<String, String> urlMap = JiraHelper.getJiraUrlAndProjectId( project.getIssueManagement().getUrl() );
        String jiraUrl = urlMap.get( "url" );
        String jiraId = urlMap.get( "id" );

        if ( jiraId == null || jiraId.length() == 0 )
        {
            log.debug( "The JIRA URL " + project.getIssueManagement().getUrl()
                           + " doesn't include a pid, trying to extract it from JIRA." );
            jiraId = JiraHelper.getPidFromJira( log, project.getIssueManagement().getUrl(), client );
        }

        if ( jiraId == null )
        {
            throw new RuntimeException( "The issue management URL in the POM does not include a pid,"
                                            + " and it was not possible to extract it from the page at that URL." );
        }
        else
        {
            // create the URL for getting the proper issues from JIRA
            String fullURL = jiraUrl + "/secure/IssueNavigator.jspa?view=rss&pid=" + jiraId;

            if ( getFixFor() != null )
            {
                fullURL += "&fixfor=" + getFixFor();
            }

            String createdFilter =
                new ParameterQueryBuilder( log ).fixVersionIds( fixVersionIds ).statusIds( statusIds ).priorityIds(
                    priorityIds ).resolutionIds( resolutionIds ).components( component ).typeIds(
                    typeIds ).sortColumnNames( sortColumnNames ).filter( filter ).build();

            if ( createdFilter.charAt( 0 ) != '&' )
            {
                fullURL += "&";
            }
            fullURL += createdFilter;

            fullURL += ( "&tempMax=" + nbEntriesMax + "&reset=true&decorator=none" );

            return fullURL;
        }
    }

    /**
     * Check and prepare for basic authentication.
     *
     * @param client The client to prepare
     */
    private void prepareBasicAuthentication( HttpClient client )
    {
        if ( ( webUser != null ) && ( webUser.length() > 0 ) )
        {
            client.getParams().setAuthenticationPreemptive( true );

            Credentials defaultcreds = new UsernamePasswordCredentials( webUser, webPassword );

            getLog().debug( "Using username: " + webUser + " for Basic Authentication." );

            client.getState().setCredentials( new AuthScope( null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME ),
                                              defaultcreds );
        }
    }

    /**
     * Authenticate against JIRA. This method relies on jiraUser and
     * jiraPassword being set. You can check this by calling
     * isJiraAuthenticationConfigured().
     *
     * @param client  the HttpClient
     * @param jiraUrl the JIRA installation
     * @return <code>true</code> if the authentication was successful, otherwise <code>false</code>
     */
    private boolean doJiraAuthentication( HttpClient client, final String jiraUrl )
    {
        // log into JIRA if we have to
        String loginUrl;

        StringBuilder loginLink = new StringBuilder( jiraUrl );

        loginLink.append( "/login.jsp?os_destination=/secure/" );

        try
        {
            loginLink.append( "&os_username=" ).append( URLEncoder.encode( jiraUser, UTF_8 ) );

            String password = null;
            if ( jiraPassword != null )
            {
                password = StringUtils.repeat( "*", jiraPassword.length() );
            }
            getLog().debug( "Login URL: " + loginLink + "&os_password=" + password );

            loginLink.append( "&os_password=" ).append( URLEncoder.encode( jiraPassword, UTF_8 ) );

            loginUrl = loginLink.toString();

            // execute the login
            GetMethod loginGet = new GetMethod( loginUrl );

            client.executeMethod( loginGet );

            if ( loginSucceeded( loginGet ) )
            {
                getLog().debug( "Successfully logged in into JIRA." );
                return true;
            }
            else
            {
                getLog().warn( "Was unable to login into JIRA: wrong username and/or password." );
            }
        }
        catch ( Exception e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Error trying to login into JIRA.", e );
            }
            else
            {
                getLog().error( "Error trying to login into JIRA. Cause is: " + e.getLocalizedMessage() );
            }
        }
        return false;
    }

    /**
     * Evaluate if the login attempt to JIRA was successful or not. We can't
     * use the status code because JIRA returns 200 even if the login fails.
     *
     * @param loginGet The method that was executed
     * @return <code>false</code> if we find an error message in the response body, otherwise <code>true</code>
     * @todo There must be a nicer way to know whether we were able to login or not
     */
    private boolean loginSucceeded( GetMethod loginGet )
        throws IOException
    {
        final String loginFailureResponse = "your username and password are incorrect";

        return !loginGet.getResponseBodyAsString().contains( loginFailureResponse );
    }

    /**
     * Setup proxy access if we have to.
     *
     * @param client the HttpClient
     */
    private void determineProxy( String jiraUrl, HttpClient client )
    {
        // see whether there is any proxy defined in maven

        getProxyInfo( jiraUrl );

        if ( proxyHost != null )
        {
            client.getHostConfiguration().setProxy( proxyHost, proxyPort );

            getLog().debug( "Using proxy: " + proxyHost + " at port " + proxyPort );

            if ( proxyUser != null )
            {
                getLog().debug( "Using proxy user: " + proxyUser );

                client.getState().setProxyCredentials(
                    new AuthScope( null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME ),
                    new UsernamePasswordCredentials( proxyUser, proxyPass ) );
            }
        }
    }

    /**
     * Downloads the given link using the configured HttpClient, possibly following redirects.
     *
     * @param cl   the HttpClient
     * @param link the URL to JIRA
     */
    private void download( final HttpClient cl, final String link )
    {
        try
        {
            GetMethod gm = new GetMethod( link );

            getLog().info( "Downloading from JIRA at: " + link );

            gm.setFollowRedirects( true );

            cl.executeMethod( gm );

            StatusLine sl = gm.getStatusLine();

            if ( sl == null )
            {
                getLog().error( "Unknown error validating link: " + link );

                return;
            }

            // if we get a redirect, do so
            if ( gm.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY )
            {
                Header locationHeader = gm.getResponseHeader( "Location" );

                if ( locationHeader == null )
                {
                    getLog().warn( "Site sent redirect, but did not set Location header" );
                }
                else
                {
                    String newLink = locationHeader.getValue();

                    getLog().debug( "Following redirect to " + newLink );

                    download( cl, newLink );
                }
            }

            if ( gm.getStatusCode() == HttpStatus.SC_OK )
            {
                final InputStream responseBodyStream = gm.getResponseBodyAsStream();

                if ( !output.getParentFile().exists() )
                {
                    output.getParentFile().mkdirs();
                }

                // write the response to file
                OutputStream out = null;
                try
                {
                    out = new FileOutputStream( output );
                    IOUtil.copy( responseBodyStream, out );
                }
                finally
                {
                    IOUtil.close( out );
                    IOUtil.close( responseBodyStream );
                }

                getLog().debug( "Downloading from JIRA was successful" );
            }
            else
            {
                getLog().warn( "Downloading from JIRA failed. Received: [" + gm.getStatusCode() + "]" );
            }
        }
        catch ( HttpException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Error downloading issues from JIRA:", e );
            }
            else
            {
                getLog().error( "Error downloading issues from JIRA url: " + e.getLocalizedMessage() );

            }
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Error downloading issues from JIRA:", e );
            }
            else
            {
                getLog().error( "Error downloading issues from JIRA. Cause is " + e.getLocalizedMessage() );
            }
        }
    }

    public List<Issue> getIssueList()
        throws MojoExecutionException
    {
        if ( output.isFile() )
        {
            JiraXML jira = new JiraXML( log, jiraDatePattern );
            jira.parseXML( output );
            getLog().info( "The JIRA version is '" + jira.getJiraVersion() + "'" );
            return jira.getIssueList();
        }
        else
        {
            getLog().warn( "JIRA file " + output.getPath() + " doesn't exist." );
            return Collections.emptyList();
        }
    }

}
