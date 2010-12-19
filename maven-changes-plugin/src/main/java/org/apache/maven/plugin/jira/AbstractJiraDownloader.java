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
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gets relevant issues in RSS from a given JIRA installation.
 * <p/>
 * Based on version 1.1.2 and patch by Dr. Spock (MPJIRA-8).
 *
 * @author mfranken@xebia.com
 * @author jruiz@exist.com
 * @version $Id$
 */
public abstract class AbstractJiraDownloader
{
    /** Log for debug output. */
    private Log log;
    /** Output file for xml document. */
    private File output;
    /** The maximum number of entries to show. */
    private int nbEntriesMax;
    /** The filter to apply to query to JIRA. */
    private String filter;
    /** Ids of fix versions to show, as comma separated string. */
    private String fixVersionIds;
    /** Ids of status to show, as comma separated string. */
    private String statusIds;
    /** Ids of resolution to show, as comma separated string. */
    private String resolutionIds;
    /** Ids of priority to show, as comma separated string. */
    private String priorityIds;
    /** The component to show. */
    private String component;
    /** Ids of types to show, as comma separated string. */
    private String typeIds;
    /** Column names to sort by, as comma separated string. */
    private String sortColumnNames;
    /** The username to log into JIRA. */
    private String jiraUser;
    /** The password to log into JIRA. */
    private String jiraPassword;
    /** The username to log into webserver. */
    private String webUser;
    /** The password to log into webserver. */
    private String webPassword;
    /** The maven project. */
    private MavenProject project;
    /** The maven settings. */
    private Settings settings;
    /** Mapping containing all allowed JIRA status values. */
    protected Map statusMap = new HashMap();
    /** Mapping containing all allowed JIRA resolution values. */
    protected Map resolutionMap = new HashMap();
    /** Mapping containing all allowed JIRA priority values. */
    protected Map priorityMap = new HashMap();
    /** Mapping containing all allowed JIRA type values. */
    protected Map typeMap = new HashMap();

    /**
     * Creates a filter given the parameters and some defaults.
     *
     * @return request parameters to be added to URL used for downloading the JIRA issues
     */
    private String createFilter()
    {
        // If the user has defined a filter - use that
        if ( ( this.filter != null ) && ( this.filter.length() > 0 ) )
        {
            return this.filter;
        }

        StringBuffer localFilter = new StringBuffer();

        // add fix versions
        if ( fixVersionIds != null )
        {
            String[] fixVersions = fixVersionIds.split( "," );

            for ( int i = 0; i < fixVersions.length; i++ )
            {
                if ( fixVersions[i].length() > 0 )
                {
                    localFilter.append( "&fixfor=" + fixVersions[i].trim() );
                }
            }
        }

        // get the Status Ids
        if ( statusIds != null )
        {
            String[] stats = statusIds.split( "," );

            for ( int i = 0; i < stats.length; i++ )
            {
                String statusParam = (String) statusMap.get( stats[i] );

                if ( statusParam != null )
                {
                    localFilter.append( "&statusIds=" + statusParam );
                }
            }
        }

        // get the Priority Ids
        if ( priorityIds != null )
        {
            String[] prios = priorityIds.split( "," );

            for ( int i = 0; i < prios.length; i++ )
            {
                String priorityParam = (String) priorityMap.get( prios[i] );

                if ( priorityParam != null )
                {
                    localFilter.append( "&priorityIds=" + priorityParam );
                }
            }
        }

        // get the Resolution Ids
        if ( resolutionIds != null )
        {
            String[] resos = resolutionIds.split( "," );

            for ( int i = 0; i < resos.length; i++ )
            {
                String resoParam = (String) resolutionMap.get( resos[i] );

                if ( resoParam != null )
                {
                    localFilter.append( "&resolutionIds=" + resoParam );
                }
            }
        }

        // add components
        if ( component != null )
        {
            String[] components = component.split( "," );

            for ( int i = 0; i < components.length; i++ )
            {
                if ( components[i].length() > 0 )
                {
                    localFilter.append( "&component=" + components[i] );
                }
            }
        }

        // get the Type Ids
        if ( typeIds != null )
        {
            String[] types = typeIds.split( "," );

            for ( int i = 0; i < types.length; i++ )
            {
                String typeParam = (String) typeMap.get( types[i].trim() );

                if ( typeParam != null )
                {
                    localFilter.append( "&type=" + typeParam );
                }
            }
        }

        // get the Sort order
        int validSortColumnNames = 0;
        if ( sortColumnNames != null )
        {
            String[] sortColumnNamesArray = sortColumnNames.split( "," );
            // N.B. Add in reverse order (it's the way JIRA likes it!!)
            for ( int i = sortColumnNamesArray.length - 1; i >= 0; i-- )
            {
                String lowerColumnName = sortColumnNamesArray[i].trim().toLowerCase( Locale.ENGLISH );
                boolean descending = false;
                String fieldName = null;
                if ( lowerColumnName.endsWith( "desc" ) )
                {
                    descending = true;
                    lowerColumnName = lowerColumnName.substring( 0, lowerColumnName.length() - 4 ).trim();
                }
                else if ( lowerColumnName.endsWith( "asc" ) )
                {
                    descending = false;
                    lowerColumnName = lowerColumnName.substring( 0, lowerColumnName.length() - 3 ).trim();
                }

                if ( "key".equals( lowerColumnName ) )
                {
                    fieldName = "issuekey";
                }
                else if ( "summary".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "status".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "resolution".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "assignee".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "reporter".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "type".equals( lowerColumnName ) )
                {
                    fieldName = "issuetype";
                }
                else if ( "priority".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "version".equals( lowerColumnName ) )
                {
                    fieldName = "versions";
                }
                else if ( "fix version".equals( lowerColumnName ) )
                {
                    fieldName = "fixVersions";
                }
                else if ( "component".equals( lowerColumnName ) )
                {
                    fieldName = "components";
                }
                else if ( "created".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                else if ( "updated".equals( lowerColumnName ) )
                {
                    fieldName = lowerColumnName;
                }
                if ( fieldName != null )
                {
                    localFilter.append( "&sorter/field=" );
                    localFilter.append( fieldName );
                    localFilter.append( "&sorter/order=" );
                    localFilter.append( descending ? "DESC" : "ASC" );
                    validSortColumnNames++;
                }
                else
                {
                    // Error in the configuration
                    getLog().error(
                        "maven-changes-plugin: The configured value '" + lowerColumnName
                            + "' for sortColumnNames is not correct." );
                }
            }
        }
        if ( validSortColumnNames == 0 )
        {
            // Error in the configuration
            getLog().error(
                "maven-changes-plugin: None of the configured sortColumnNames '" + sortColumnNames + "' are correct." );
        }


        return localFilter.toString();
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

            HttpState state = new HttpState();

            HostConfiguration hc = new HostConfiguration();

            client.setHostConfiguration( hc );

            client.setState( state );

            Map urlMap = getJiraUrlAndIssueId();

            String jiraUrl = (String) urlMap.get( "url" );

            String jiraId = (String) urlMap.get( "id" );

            determineProxy( jiraUrl, client );

            prepareBasicAuthentication( client );

            boolean jiraAuthenticationSuccessful = false;
            if ( isJiraAuthenticationConfigured() )
            {
                jiraAuthenticationSuccessful = doJiraAuthentication( client, jiraUrl );
            }

            if ( ( isJiraAuthenticationConfigured() && jiraAuthenticationSuccessful )
                || !isJiraAuthenticationConfigured() )
            {
                if ( jiraId == null || jiraId.length() == 0 )
                {
                    log.debug( "The JIRA URL " + project.getIssueManagement().getUrl()
                        + " doesn't include a pid, trying to extract it from JIRA." );
                    jiraId = JiraHelper.getPidFromJira( log, project.getIssueManagement().getUrl(), client );
                }

                if ( jiraId == null )
                {
                    getLog().error( "The issue management URL in the POM does not include a pid,"
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

                    String createdFilter = createFilter();
                    if ( createdFilter.charAt( 0 ) != '&' )
                    {
                        fullURL += "&";
                    }
                    fullURL += createdFilter;

                    fullURL += ( "&tempMax=" + nbEntriesMax + "&reset=true&decorator=none" );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "download jira issues from url " + fullURL );
                    }

                    // execute the GET
                    download( client, fullURL );
                }
            }
        }
        catch ( Exception e )
        {
            getLog().error( "Error accessing " + project.getIssueManagement().getUrl(), e );
        }
    }

    /**
     * Override this method if you need to get issues for a specific Fix For.
     *
     * @return A Fix For id or <code>null</code> if you don't have that need
     */
    protected String getFixFor()
    {
        return null;
    }

    /**
     * Parse out the base URL for JIRA and the JIRA project id from the issue
     * management section of the POM.
     *
     * @return A <code>Map</code> containing the URL and project id
     */
    private Map getJiraUrlAndIssueId()
    {
        HashMap urlMap = new HashMap();

        String url = project.getIssueManagement().getUrl();

        // chop off the parameter part
        int pos = url.indexOf( "?" );

        // and get the id while we're at it
        String id = "";

        if ( pos >= 0 )
        {
            // project id
            id = url.substring( url.lastIndexOf( "=" ) + 1 );
        }

        String jiraUrl = url.substring( 0, url.lastIndexOf( "/" ) );

        if ( jiraUrl.endsWith( "secure" ) || jiraUrl.endsWith( "browse" ) )
        {
            jiraUrl = jiraUrl.substring( 0, jiraUrl.lastIndexOf( "/" ) );
        }
        getLog().debug( "JIRA lives at: " + jiraUrl );

        urlMap.put( "url", jiraUrl );

        urlMap.put( "id", id );

        return urlMap;
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
     * @param client    the HttpClient
     * @param jiraUrl   the JIRA installation
     * @return <code>true</code> if the authentication was successful, otherwise <code>false</code>
     */
    private boolean doJiraAuthentication( HttpClient client, final String jiraUrl )
    {
        // log into JIRA if we have to
        String loginUrl = null;

        StringBuffer loginLink = new StringBuffer( jiraUrl );

        loginLink.append( "/login.jsp?os_destination=/secure/" );

        loginLink.append( "&os_username=" ).append( jiraUser );

        String password = null;
        if ( jiraPassword != null )
        {
            password = StringUtils.repeat( "*", jiraPassword.length() );
        }
        getLog().debug( "Login URL: " + loginLink + "&os_password=" + password );

        loginLink.append( "&os_password=" ).append( jiraPassword );

        loginUrl = loginLink.toString();

        // execute the login
        GetMethod loginGet = new GetMethod( loginUrl );

        try
        {
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
     * Check to see if we think that JIRA authentication is needed.
     *
     * @return <code>true</code> if jiraUser and jiraPassword are set, otherwise <code>false</code>
     */
    private boolean isJiraAuthenticationConfigured()
    {
        return ( jiraUser != null ) && ( jiraUser.length() > 0 ) && ( jiraPassword != null );
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

        return loginGet.getResponseBodyAsString().indexOf( loginFailureResponse ) == -1;
    }

    /**
     * Setup proxy access if we have to.
     *
     * @param client  the HttpClient
     */
    private void determineProxy( String jiraUrl, HttpClient client )
    {
        // see whether there is any proxy defined in maven
        Proxy proxy = null;

        String proxyHost = null;

        int proxyPort = 0;

        String proxyUser = null;

        String proxyPass = null;

        if ( project == null )
        {
            getLog().error( "No project set. No proxy info available." );

            return;
        }

        if ( settings != null )
        {
            proxy = settings.getActiveProxy();
        }

        if ( proxy != null )
        {

            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setNonProxyHosts(proxy.getNonProxyHosts());

            // Validation of proxy method copied from org.apache.maven.wagon.proxy.ProxyUtils.
            // @todo Can use original when maven-changes-plugin references a more recent version of maven-project

            //if ( ProxyUtils.validateNonProxyHosts( proxyInfo, jiraUrl  ) )
            if ( JiraHelper.validateNonProxyHosts( proxyInfo, jiraUrl  ) )
            {
                return;
            }

            proxyHost = settings.getActiveProxy().getHost();

            proxyPort = settings.getActiveProxy().getPort();

            proxyUser = settings.getActiveProxy().getUsername();

            proxyPass = settings.getActiveProxy().getPassword();

            getLog().debug( proxyPass );
        }

        if ( proxyHost != null )
        {
            client.getHostConfiguration().setProxy( proxyHost, proxyPort );

            getLog().debug( "Using proxy: " + proxyHost + " at port " + proxyPort );

            if ( proxyUser != null )
            {
                getLog().debug( "Using proxy user: " + proxyUser );

                client.getState().setProxyCredentials(
                                                       new AuthScope( null, AuthScope.ANY_PORT, null,
                                                                      AuthScope.ANY_SCHEME ),
                                                       new UsernamePasswordCredentials( proxyUser, proxyPass ) );
            }
        }
    }

    /**
     * Downloads the given link using the configured HttpClient, possibly following redirects.
     *
     * @param cl     the HttpClient
     * @param link   the URL to JIRA
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
                final String strGetResponseBody = gm.getResponseBodyAsString();

                if ( !output.getParentFile().exists() )
                {
                    output.getParentFile().mkdirs();
                }

                // write the reponse to file
                PrintWriter pw = new PrintWriter( new FileWriter( output ) );

                pw.print( strGetResponseBody );

                pw.close();

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

    /**
     * Set the output file for the log.
     *
     * @param thisOutput the output file
     */
    public void setOutput( File thisOutput )
    {
        this.output = thisOutput;
    }

    public File getOutput()
    {
        return this.output;
    }

    /**
     * Sets the project.
     *
     * @param thisProject  The project to set
     */
    public void setMavenProject( Object thisProject )
    {
        this.project = (MavenProject) thisProject;
    }

    /**
     * Sets the maximum number of Issues to show.
     *
     * @param nbEntries  The maximum number of Issues
     */
    public void setNbEntries( final int nbEntries )
    {
        nbEntriesMax = nbEntries;
    }

    /**
     * Sets the statusIds.
     *
     * @param thisStatusIds   The id(s) of the status to show, as comma separated string
     */
    public void setStatusIds( String thisStatusIds )
    {
        statusIds = thisStatusIds;
    }

    /**
     * Sets the priorityIds.
     *
     * @param thisPriorityIds  The id(s) of the priority to show, as comma separated string
     */
    public void setPriorityIds( String thisPriorityIds )
    {
        priorityIds = thisPriorityIds;
    }

    /**
     * Sets the resolutionIds.
     *
     * @param thisResolutionIds  The id(s) of the resolution to show, as comma separated string
     */
    public void setResolutionIds( String thisResolutionIds )
    {
        resolutionIds = thisResolutionIds;
    }

    /**
     * Sets the sort column names.
     *
     * @param thisSortColumnNames The column names to sort by
     */
    public void setSortColumnNames( String thisSortColumnNames )
    {
        sortColumnNames = thisSortColumnNames;
    }

    /**
     * Sets the password for authentication against the webserver.
     *
     * @param thisWebPassword  The password of the webserver
     */
    public void setWebPassword( String thisWebPassword )
    {
        this.webPassword = thisWebPassword;
    }

    /**
     * Sets the username for authentication against the webserver.
     *
     * @param thisWebUser   The username of the webserver
     */
    public void setWebUser( String thisWebUser )
    {
        this.webUser = thisWebUser;
    }

    /**
     * Sets the password to log into a secured JIRA.
     *
     * @param thisJiraPassword  The password for JIRA
     */
    public void setJiraPassword( final String thisJiraPassword )
    {
        this.jiraPassword = thisJiraPassword;
    }

    /**
     * Sets the username to log into a secured JIRA.
     *
     * @param thisJiraUser  The username for JIRA
     */
    public void setJiraUser( String thisJiraUser )
    {
        this.jiraUser = thisJiraUser;
    }

    /**
     * Sets the filter to apply to query to JIRA.
     *
     * @param thisFilter  The filter to query JIRA
     */
    public void setFilter( String thisFilter )
    {
        this.filter = thisFilter;
    }

    /**
     * Sets the component(s) to apply to query JIRA.
     *
     * @param theseComponents   The id(s) of components to show, as comma separated string
     */
    public void setComponent( String theseComponents )
    {
        this.component = theseComponents;
    }

    /**
     * Sets the fix version id(s) to apply to query JIRA.
     *
     * @param theseFixVersionIds The id(s) of fix versions to show, as comma separated string
     */
    public void setFixVersionIds( String theseFixVersionIds )
    {
        this.fixVersionIds = theseFixVersionIds;
    }

    /**
     * Sets the typeIds.
     *
     * @param theseTypeIds  The id(s) of the types to show, as comma separated string
     */
    public void setTypeIds( String theseTypeIds )
    {
        typeIds = theseTypeIds;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    private Log getLog()
    {
        return log;
    }

    public void setSettings( Settings settings )
    {
        this.settings = settings;
    }
}
