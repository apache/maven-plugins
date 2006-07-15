package org.apache.maven.jira;

/* ====================================================================
 *   Copyright 2001-2006 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.plugin.logging.Log;

/**
 * Gets relevant issues in RSS from a given JIRA installation.
 *
 * Based on version 1.1.2 and patch by Dr. Spock (MPJIRA-8)
 *
 * @author mfranken@xebia.com
 * @author jruiz@exist.com
 */
public final class JiraDownloader2
{

    /** Log for debug output. */
    private org.apache.maven.plugin.logging.Log log;

    /** Output file for xml document. */
    private File output;

    /** The maximum number of entries to show. */
    private int nbEntriesMax;

    /** The filter to apply to query to JIRA. */
    private String filter;

    /** Ids of status to show, as comma separated string. */
    private String statusIds;

    /** Ids of resolution to show, as comma separated string. */
    private String resolutionIds;

    /** Ids of priority to show, as comma separated string. */
    private String priorityIds;

    /** The component to show. */
    private String component;

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

    /** Mapping containing all JIRA status values. */
    private static Map statusMap = new HashMap();

    /** Mapping containing all JIRA resolution values. */
    private static Map resolutionMap = new HashMap();

    /** Mapping containing all JIRA priority values. */
    private static Map priorityMap = new HashMap();

    static
    {
        statusMap.put( "Open", "1" );
        statusMap.put( "In Progress", "3" );
        statusMap.put( "Reopened", "4" );
        statusMap.put( "Resolved", "5" );
        statusMap.put( "Closed", "6" );

        resolutionMap.put( "Unresolved", "-1" );
        resolutionMap.put( "Fixed", "1" );
        resolutionMap.put( "Won't Fix", "2" );
        resolutionMap.put( "Duplicate", "3" );
        resolutionMap.put( "Incomplete", "4" );
        resolutionMap.put( "Cannot Reproduce", "5" );

        priorityMap.put( "Blocker", "1" );
        priorityMap.put( "Critical", "2" );
        priorityMap.put( "Major", "3" );
        priorityMap.put( "Minor", "4" );
        priorityMap.put( "Trivial", "5" );
    }

    /**
     * Creates a filter given the maven.jira parameters and some defaults.
     *
     * @return request parameters to be added to URL used for downloading the JIRA issues
     */
    private String createFilter()
    {
        if ( ( this.filter != null ) && ( this.filter.length() > 0 ) )
        {
            if ( this.filter.charAt( 0 ) == '&' )
            {
                return this.filter.substring( 1 );
            }

            return this.filter;
        }

        StringBuffer localFilter = new StringBuffer();

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

        if ( resolutionIds != null )
        {
            // get the Resolution Ids
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

        // add all components
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

        // add default sorting (by priority and then creation date)
        String sort = "&sorter/field=created&sorter/order=DESC" + "&sorter/field=priority&sorter/order=DESC";

        return localFilter + sort;
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
            HttpClient cl = new HttpClient();

            HttpState state = new HttpState();

            HostConfiguration hc = new HostConfiguration();

            cl.setHostConfiguration( hc );

            cl.setState( state );

            determineProxy( cl );

            Map urlMap = getJiraUrlAndIssueId();

            String jiraUrl = (String) urlMap.get("url");

            String jiraId = (String) urlMap.get("id");

            doAuthentication( cl, jiraUrl );

            if ( jiraId == null || jiraId.length() == 0 )
            {
                jiraId = JiraHelper.getPidFromJira( log, project.getIssueManagement().getUrl(), cl );
            }

            // create the URL for getting the proper issues from JIRA
            String fullURL = jiraUrl + "/secure/IssueNavigator.jspa?view=rss&pid=" + jiraId;

            fullURL += createFilter();

            fullURL += ( "&tempMax=" + nbEntriesMax + "&reset=true&decorator=none" );

            // execute the GET
            download( cl, fullURL );
        }
        catch ( Exception e )
        {
            getLog().error( "Error accessing " + project.getIssueManagement().getUrl(), e );
        }
    }

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
            // url
            id = url.substring( url.lastIndexOf( "=" ) + 1 );
        }

        String jiraUrl = url.substring( 0, url.lastIndexOf( "/" ) );

        if ( jiraUrl.endsWith( "secure" ) || jiraUrl.endsWith( "browse" ) )
        {
            jiraUrl = jiraUrl.substring( 0, jiraUrl.lastIndexOf( "/" ) );
        }
        getLog().info( "Jira lives at: " + jiraUrl );

        urlMap.put("url", jiraUrl);

        urlMap.put("id", id);

        return urlMap;
    }

    /**
     * Authenticate against webserver and into JIRA if we have to.
     *
     * @param client    the HttpClient
     * @param jiraUrl   the JIRA installation
     */
    private void doAuthentication( HttpClient client, final String jiraUrl )
    {
        // check and prepare for basic authentication
        if ( ( webUser != null ) && ( webUser.length() > 0 ) )
        {
            client.getState().setAuthenticationPreemptive( true );

            Credentials defaultcreds = new UsernamePasswordCredentials( webUser, webPassword );
            
            getLog().info( "Using username: " + webUser + " for Basic Authentication against the webserver at " + jiraUrl );
            
            client.getState().setCredentials( null, null, defaultcreds );
        }

        // log into JIRA if we have to
        String loginUrl = null;

        if ( ( jiraUser != null ) && ( jiraUser.length() > 0 ) && ( jiraPassword != null ) )
        {
            StringBuffer loginLink = new StringBuffer( jiraUrl );
            
            loginLink.append( "/login.jsp?os_destination=/secure/" );
            
            loginLink.append( "&os_username=" ).append( jiraUser );
            
            getLog().info( "Login URL: " + loginLink + "&os_password=*******" );
            
            loginLink.append( "&os_password=" ).append( jiraPassword );
            
            loginUrl = loginLink.toString();
        }

        // execute the login
        if ( loginUrl != null )
        {
            GetMethod loginGet = new GetMethod( loginUrl );

            try
            {
                client.executeMethod( loginGet );
                
                getLog().info( "Succesfully logged in into JIRA." );
            }
            catch ( Exception e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().error( "Error trying to login into JIRA:", e );
                }
                else
                {
                    getLog().error( "Error trying to login into JIRA. Cause is: " + e.getLocalizedMessage() );
                }
                // continue any way, probably will fail later if authentication was necesaaray afterall
            }
        }
    }

    /**
     * Setup proxy access if we have to.
     *
     * @param client  the HttpClient
     */
    private void determineProxy( HttpClient client )
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
            proxyHost = settings.getActiveProxy().getHost();
            
            proxyPort = settings.getActiveProxy().getPort();
            
            proxyUser = settings.getActiveProxy().getUsername();
            
            proxyPass = settings.getActiveProxy().getPassword();
            
            getLog().info(proxyPass);
        }

        if ( proxyHost != null )
        {
            client.getHostConfiguration().setProxy( proxyHost, proxyPort );
            
            getLog().info( "Using proxy: " + proxyHost + " at port " + proxyPort );

            if ( proxyUser != null )
            {
                getLog().info( "Using proxy user: " + proxyUser );
                
                client.getState().setProxyCredentials( null, null,
                                                       new UsernamePasswordCredentials( proxyUser, proxyPass ) );
            }
        }
    }

    /**
     * Downloads the given link using the configured HttpClient, possibly following redirects.
     *
     * @param cl     the HttpClient
     * @param link   the JiraUrl
     * @return
     */
    private void download( final HttpClient cl, final String link )
    {
        try
        {
            GetMethod gm = new GetMethod( link );
            
            getLog().info( "Downloading " + link );
            
            gm.setFollowRedirects( true );
            
            cl.executeMethod( gm );

            final String strGetResponseBody = gm.getResponseBodyAsString();

            // write the reponse to file
            PrintWriter pw = new PrintWriter( new FileWriter( output ) );
            
            pw.print( strGetResponseBody );
            
            pw.close();

            StatusLine sl = gm.getStatusLine();
            
            if ( sl == null )
            {
                getLog().info( "Unknown error validating link : " + link );

                return;
            }

            // if we get a redirect, do so
            if ( gm.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY )
            {
                Header locationHeader = gm.getResponseHeader( "Location" );

                if ( locationHeader == null )
                {
                    getLog().info( "Site sent redirect, but did not set Location header" );
                }
                else
                {
                    String newLink = locationHeader.getValue();
                    
                    getLog().debug( "Following redirect to " + newLink );
                    
                    download( cl, newLink );
                }
            }

            if ( gm.getStatusCode() != HttpStatus.SC_OK )
            {
                getLog().warn( "Received: [" + gm.getStatusCode() + "]" );
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
                getLog().error( "Error downloading issues from JIRA url :  " + e.getLocalizedMessage() );
                   
            }
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Error downloading issues from JIRA :", e );
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

    public void setLog( Log log )
    {
        this.log = log;
    }

    private org.apache.maven.plugin.logging.Log getLog()
    {
        return log;
    }
    
    public void setSettings(Settings settings)
    {
        this.settings = settings;
    }
}
