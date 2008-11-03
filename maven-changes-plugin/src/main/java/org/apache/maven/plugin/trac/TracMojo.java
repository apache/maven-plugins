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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.changes.AbstractChangesReport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which downloads issues from the Issue Tracking System and generates a
 * report.
 * 
 * @goal trac-report
 * @author Noriko Kinugasa
 * @version $Id$
 * @since 2.1
 */
public class TracMojo
    extends AbstractChangesReport
{
    /**
     * Defines the Trac username for authentication into a private Trac
     * installation.
     * 
     * @parameter default-value=""
     */
    private String tracUser;

    /**
     * Defines the Trac password for authentication into a private Trac
     * installation.
     * 
     * @parameter default-value=""
     */
    private String tracPassword;

    /**
     * Defines the Trac query for searching ticket.
     * 
     * @parameter default-value="order=id"
     */
    private String query;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return validateIfIssueManagementComplete();
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !canGenerateReport() )
        {
            throw new MavenReportException( "Issue Management is out of order." );
        }

        parseTracUrl();

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        try
        {
            config.setServerURL( new URL( project.getIssueManagement().getUrl() + "/login/xmlrpc" ) );
        }
        catch ( MalformedURLException e1 )
        {

            throw new MavenReportException( "The Trac URL is incorrect." );

        }
        config.setBasicUserName( tracUser );
        config.setBasicPassword( tracPassword );

        Object[] queryResult = null;
        XmlRpcClient client = new XmlRpcClient();

        client.setConfig( config );

        String qstr = "";

        if ( !StringUtils.isEmpty( query ) )
        {
            qstr = query;
        }

        Object[] params = new Object[] { new String( qstr ) };
        try
        {
            queryResult = (Object[]) client.execute( "ticket.query", params );
        }
        catch ( XmlRpcException e )
        {
            throw new MavenReportException( "XmlRpc Error.", e );
        }

        ArrayList ticketList = new ArrayList();
        TracTicket matchTicket;

        TracReportGenerator report = new TracReportGenerator();

        if ( queryResult.length == 0 )
        {

            report.doGenerateEmptyReport( getBundle( locale ), getSink() );
            getLog().warn( "No ticket has matched." );

        }
        else
        {

            for ( int i = 0; i < queryResult.length; i++ )
            {
                params = new Object[] { queryResult[i] };
                try
                {
                    Object[] Ticketresult = null;
                    matchTicket = new TracTicket();
                    Ticketresult = (Object[]) client.execute( "ticket.get", params );
                    ticketList.add( setQueryResult( Ticketresult, matchTicket ) );

                }
                catch ( XmlRpcException e )
                {
                    throw new MavenReportException( "XmlRpc Error.", e );
                }
            }
            try
            {

                report.doGenerateReport( getBundle( locale ), getSink(), ticketList );

            }
            catch ( Exception e )

            {
                e.printStackTrace();

            }

        }

    }

    public String getName( Locale locale )
    {
        return "Trac Report";
    }

    public String getDescription( Locale locale )
    {
        return "Report on Ticket from the Trac.";
    }

    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    public String getOutputName()
    {
        return "trac-report";
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "trac-report", locale, this.getClass().getClassLoader() );
    }

    private void parseTracUrl()
    {

        String tracUrl = project.getIssueManagement().getUrl();

        if ( tracUrl.endsWith( "/" ) )
        {
            project.getIssueManagement().setUrl( tracUrl.substring( 0, tracUrl.length() - 1 ) );
        }

    }

    private TracTicket setQueryResult( Object[] ticketObj, TracTicket ticket )
    {

        ticket.setId( String.valueOf( ticketObj[0] ) );

        ticket.setLink( project.getIssueManagement().getUrl() + "/ticket/" + String.valueOf( ticketObj[0] ) );

        ticket.setTimeCreated( String.valueOf( ticketObj[1] ) );

        ticket.setTimeChanged( String.valueOf( ticketObj[2] ) );

        Map attributes = (Map) ticketObj[3];

        ticket.setType( (String) attributes.get( "type" ) );

        ticket.setSummary( (String) attributes.get( "summary" ) );

        ticket.setStatus( (String) attributes.get( "status" ) );

        ticket.setResolution( (String) attributes.get( "resolution" ) );

        ticket.setOwner( (String) attributes.get( "owner" ) );

        ticket.setMilestone( (String) attributes.get( "milestone" ) );

        ticket.setPriority( (String) attributes.get( "priority" ) );

        ticket.setReporter( (String) attributes.get( "reporter" ) );

        ticket.setComponent( (String) attributes.get( "component" ) );

        return ticket;
    }

    private boolean validateIfIssueManagementComplete()
    {
        if ( project.getIssueManagement() == null )
        {
            getLog().error( "No Issue Management set. No Trac Report will be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getUrl() == null )
            || ( project.getIssueManagement().getUrl().trim().equals( "" ) ) )
        {
            getLog().error( "No URL set in Issue Management. No Trac Report will be generated." );

            return false;
        }
        else if ( ( project.getIssueManagement().getSystem() != null )
            && !( project.getIssueManagement().getSystem().equalsIgnoreCase( "trac" ) ) )
        {
            getLog().error( "The Trac Report only supports Trac.  No Trac Report will be generated." );

            return false;
        }
        return true;
    }

}
