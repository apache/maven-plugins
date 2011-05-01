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

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * A helper class with common JIRA related functionality.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class JiraHelper
{
    private static final String PID = "pid=";

    /**
     * Parse out the base URL for JIRA and the JIRA project id from the issue
     * management URL.
     *
     * @param issueManagementUrl The URL to the issue management system
     * @return A <code>Map</code> containing the URL and project id
     */
    static Map<String,String> getJiraUrlAndProjectId( String issueManagementUrl )
    {
        String url = issueManagementUrl;

        if ( url.endsWith( "/" ) )
        {
            // MCHANGES-218
            url = url.substring( 0, url.lastIndexOf( '/' ) );
        }

        // chop off the parameter part
        int pos = url.indexOf( '?' );

        // and get the id while we're at it
        String id = "";

        if ( pos >= 0 )
        {
            // project id
            id = url.substring( url.lastIndexOf( '=' ) + 1 );
        }

        String jiraUrl = url.substring( 0, url.lastIndexOf( '/' ) );

        if ( jiraUrl.endsWith( "secure" ) )
        {
            jiraUrl = jiraUrl.substring( 0, jiraUrl.lastIndexOf( '/' ) );
        }
        else
        {
            // If the issueManagement.url points to a component, then "browse"
            // will not be at the end - it might be in the middle somewhere.
            // Try to find it.
            final int index = jiraUrl.indexOf( "/browse" );
            if ( index != -1 )
            {
                jiraUrl = jiraUrl.substring( 0, index );
            }
        }

        HashMap<String,String> urlMap = new HashMap<String,String>( 4 );

        urlMap.put( "url", jiraUrl );

        urlMap.put( "id", id );

        return urlMap;
    }

    /**
     * Try to get a JIRA pid from the issue management URL.
     *
     * @param log     Used to tell the user what happened
     * @param issueManagementUrl The URL to the issue management system
     * @param client  The client used to connect to JIRA
     * @return The JIRA id for the project, or null if it can't be found
     */
    public static String getPidFromJira( Log log, String issueManagementUrl, HttpClient client )
    {
        String jiraId = null;
        GetMethod gm = new GetMethod( issueManagementUrl );

        String projectPage;
        try
        {
            client.executeMethod( gm );
            log.debug( "Successfully reached JIRA." );
            projectPage = gm.getResponseBodyAsString();
        }
        catch ( Exception e )
        {
            if ( log.isDebugEnabled() )
            {
                log.error( "Unable to reach the JIRA project page:", e );
            }
            else
            {
                log.error( "Unable to reach the JIRA project page. Cause is: " + e.getLocalizedMessage() );
            }
            return null;
        }

        int pidIndex = projectPage.indexOf( PID );

        if ( pidIndex == -1 )
        {
            log.error( "Unable to extract a JIRA pid from the page at the url " + issueManagementUrl );
        }
        else
        {
            NumberFormat nf = NumberFormat.getInstance();
            Number pidNumber = nf.parse( projectPage, new ParsePosition( pidIndex + PID.length() ) );
            jiraId = Integer.toString( pidNumber.intValue() );
            log.debug( "Found the pid " + jiraId + " at " + issueManagementUrl );
        }
        return jiraId;
    }

    /**
     * Check if the specified host is in the list of non proxy hosts.
     * <p/>
     * Method copied from org.apache.maven.wagon.proxy.ProxyUtils. Can be deleted when maven-changes-plugin
     * references a more recent version of maven-project
     *
     * @param proxy      the proxy info object contains set of properties.
     * @param targetHost the target hostname
     * @return true if the hostname is in the list of non proxy hosts, false otherwise.
     */
    public static boolean validateNonProxyHosts( ProxyInfo proxy, String targetHost )
    {
        String tHost = targetHost;
        if ( tHost == null )
        {
            tHost = new String();
        }
        if ( proxy == null )
        {
            return false;
        }
        String nonProxyHosts = proxy.getNonProxyHosts();
        if ( nonProxyHosts == null )
        {
            return false;
        }

        StringTokenizer tokenizer = new StringTokenizer( nonProxyHosts, "|" );

        while ( tokenizer.hasMoreTokens() )
        {
            String pattern = tokenizer.nextToken();
            pattern = pattern.replaceAll( "\\.", "\\\\." ).replaceAll( "\\*", ".*" );
            if ( tHost.matches( pattern ) )
            {
                return true;
            }
        }
        return false;
    }

    private JiraHelper()
    {
        // utility class
    }
}
