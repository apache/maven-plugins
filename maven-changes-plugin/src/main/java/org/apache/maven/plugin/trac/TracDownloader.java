package org.apache.maven.plugin.trac;

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

import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.codehaus.plexus.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Get issues from a Trac installation.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class TracDownloader
{
    /** The Maven project. */
    private MavenProject project;
    /** The Trac query for searching for tickets. */
    private String query;
    /** The password for authentication into a private Trac installation. */
    private String tracPassword;
    /** The username for authentication into a private Trac installation. */
    private String tracUser;

    private Issue createTicket( Object[] ticketObj )
    {
        Issue ticket = new Issue();

        ticket.setId( String.valueOf( ticketObj[0] ) );

        ticket.setLink( getUrl() + "/ticket/" + String.valueOf( ticketObj[0] ) );

        ticket.setCreated( parseDate( String.valueOf( ticketObj[1] ) ) );

        ticket.setUpdated( parseDate( String.valueOf( ticketObj[2] ) ) );

        Map attributes = (Map) ticketObj[3];

        ticket.setType( (String) attributes.get( "type" ) );

        ticket.setSummary( (String) attributes.get( "summary" ) );

        ticket.setStatus( (String) attributes.get( "status" ) );

        ticket.setResolution( (String) attributes.get( "resolution" ) );

        ticket.setAssignee( (String) attributes.get( "owner" ) );

        ticket.addFixVersion( (String) attributes.get( "milestone" ) );

        ticket.setPriority( (String) attributes.get( "priority" ) );

        ticket.setReporter( (String) attributes.get( "reporter" ) );

        ticket.addComponent( (String) attributes.get( "component" ) );

        return ticket;
    }

    public List getIssueList() throws MalformedURLException, XmlRpcException
    {
        // Create and configure an XML-RPC client
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        try
        {
            config.setServerURL( new URL( getUrl() + "/login/xmlrpc" ) );
        }
        catch ( MalformedURLException e )
        {
            throw new MalformedURLException( "The Trac URL is incorrect." );
        }
        config.setBasicUserName( tracUser );
        config.setBasicPassword( tracPassword );

        XmlRpcClient client = new XmlRpcClient();

        client.setConfig( config );

        // Fetch tickets
        String qstr = "";

        if ( !StringUtils.isEmpty( query ) )
        {
            qstr = query;
        }

        Object[] params = new Object[] { new String( qstr ) };
        Object[] queryResult = null;
        ArrayList ticketList = new ArrayList();
        try
        {
            queryResult = (Object[]) client.execute( "ticket.query", params );

            for ( int i = 0; i < queryResult.length; i++ )
            {
                params = new Object[] { queryResult[i] };
                Object[] ticketGetResult = null;
                ticketGetResult = (Object[]) client.execute( "ticket.get", params );
                ticketList.add( createTicket( ticketGetResult ) );
            }
        }
        catch ( XmlRpcException e )
        {
            throw new XmlRpcException( "XmlRpc Error.", e );
        }
        return ticketList;
    }

    private String getUrl()
    {

        String url = project.getIssueManagement().getUrl();

        if ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.length() - 1 );
        }

        return url;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public void setQuery( String query )
    {
        this.query = query;
    }

    public void setTracPassword( String tracPassword )
    {
        this.tracPassword = tracPassword;
    }

    public void setTracUser( String tracUser )
    {
        this.tracUser = tracUser;
    }

    private Date parseDate( String timeCreated )
        throws RuntimeException
    {
        try
        {
            long millis = Long.parseLong( timeCreated );
            Calendar cld = Calendar.getInstance();
            cld.setTimeInMillis( millis * 1000L );
            return cld.getTime();
        }
        catch ( NumberFormatException e )
        {
            SimpleDateFormat format = new SimpleDateFormat( "EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH );
            try
            {
                return format.parse( timeCreated );
            }
            catch ( ParseException e1 )
            {
                throw new RuntimeException( "Failed to parse date '" + timeCreated + "' as a date.", e1 );
            }
        }
    }
}
