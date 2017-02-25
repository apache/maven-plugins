package org.apache.maven.plugins.jira;

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.issues.Issue;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Use the JIRA REST API to implement the download. This class assumes that the URL points to a copy of JIRA that
 * implements the REST API. A static function may be forthcoming in here to probe and see if a given URL supports it.
 */
public class RestJiraDownloader
    extends AbstractJiraDownloader
{
    private List<Issue> issueList;

    private JsonFactory jsonFactory;

    private SimpleDateFormat dateFormat;

    private List<String> resolvedFixVersionIds;

    private List<String> resolvedStatusIds;

    private List<String> resolvedComponentIds;

    private List<String> resolvedTypeIds;

    private List<String> resolvedResolutionIds;

    private List<String> resolvedPriorityIds;

    private String jiraProject;

    /**
     * 
     */
    public static class NoRest
        extends Exception
    {
        private static final long serialVersionUID = 6970088805270319624L;

        public NoRest()
        {
            // blank on purpose.
        }

        public NoRest( String message )
        {
            super( message );
        }
    }

    public RestJiraDownloader()
    {
        jsonFactory = new MappingJsonFactory();
        // 2012-07-17T06:26:47.723-0500
        dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
        resolvedFixVersionIds = new ArrayList<String>();
        resolvedStatusIds = new ArrayList<String>();
        resolvedComponentIds = new ArrayList<String>();
        resolvedTypeIds = new ArrayList<String>();
        resolvedResolutionIds = new ArrayList<String>();
        resolvedPriorityIds = new ArrayList<String>();
    }

    public void doExecute()
        throws Exception
    {

        Map<String, String> urlMap = JiraHelper.getJiraUrlAndProjectName( project.getIssueManagement().getUrl() );
        String jiraUrl = urlMap.get( "url" );
        jiraProject = urlMap.get( "project" );

        WebClient client = setupWebClient( jiraUrl );

        // We use version 2 of the REST API, that first appeared in JIRA 5
        // Check if version 2 of the REST API is supported
        // http://docs.atlassian.com/jira/REST/5.0/
        // Note that serverInfo can always be accessed without authentication
        client.replacePath( "/rest/api/2/serverInfo" );
        client.accept( MediaType.APPLICATION_JSON );
        Response siResponse = client.get();
        if ( siResponse.getStatus() != Response.Status.OK.getStatusCode() )
        {
            throw new NoRest( "This JIRA server does not support version 2 of the REST API, "
                + "which maven-changes-plugin requires." );
        }

        doSessionAuth( client );

        resolveIds( client, jiraProject );

        // CHECKSTYLE_OFF: LineLength
        String jqlQuery =
            new JqlQueryBuilder( log ).urlEncode( false ).project( jiraProject ).fixVersion( getFixFor() ).fixVersionIds( resolvedFixVersionIds ).statusIds( resolvedStatusIds ).priorityIds( resolvedPriorityIds ).resolutionIds( resolvedResolutionIds ).components( resolvedComponentIds ).typeIds( resolvedTypeIds ).sortColumnNames( sortColumnNames ).filter( filter ).build();
        // CHECKSTYLE_ON: LineLength

        StringWriter searchParamStringWriter = new StringWriter();
        JsonGenerator gen = jsonFactory.createGenerator( searchParamStringWriter );
        gen.writeStartObject();
        gen.writeStringField( "jql", jqlQuery );
        gen.writeNumberField( "maxResults", nbEntriesMax );
        gen.writeArrayFieldStart( "fields" );
        // Retrieve all fields. If that seems slow, we can reconsider.
        gen.writeString( "*all" );
        gen.writeEndArray();
        gen.writeEndObject();
        gen.close();
        client.replacePath( "/rest/api/2/search" );
        client.type( MediaType.APPLICATION_JSON_TYPE );
        client.accept( MediaType.APPLICATION_JSON_TYPE );
        Response searchResponse = client.post( searchParamStringWriter.toString() );
        if ( searchResponse.getStatus() != Response.Status.OK.getStatusCode() )
        {
            reportErrors( searchResponse );
        }

        JsonNode issueTree = getResponseTree( searchResponse );
        assert issueTree.isObject();
        JsonNode issuesNode = issueTree.get( "issues" );
        assert issuesNode.isArray();
        buildIssues( issuesNode, jiraUrl, jiraProject );
    }

    private JsonNode getResponseTree( Response response )
        throws IOException
    {
        JsonParser jsonParser = jsonFactory.createParser( (InputStream) response.getEntity() );
        return (JsonNode) jsonParser.readValueAsTree();
    }

    private void reportErrors( Response resp )
        throws IOException, MojoExecutionException
    {
        if ( MediaType.APPLICATION_JSON_TYPE.getType().equals( getResponseMediaType( resp ).getType() ) )
        {
            JsonNode errorTree = getResponseTree( resp );
            assert errorTree.isObject();
            JsonNode messages = errorTree.get( "errorMessages" );
            if ( messages != null )
            {
                for ( int mx = 0; mx < messages.size(); mx++ )
                {
                    getLog().error( messages.get( mx ).asText() );
                }
            }
            else
            {
                JsonNode message = errorTree.get( "message" );
                if ( message != null )
                {
                    getLog().error( message.asText() );
                }
            }
        }
        throw new MojoExecutionException( String.format( "Failed to query issues; response %d", resp.getStatus() ) );
    }

    private void resolveIds( WebClient client, String jiraProject )
        throws IOException, MojoExecutionException, MojoFailureException
    {
        resolveList( resolvedComponentIds, client, "components", component, "/rest/api/2/project/{key}/components",
                     jiraProject );
        resolveList( resolvedFixVersionIds, client, "fixVersions", fixVersionIds, "/rest/api/2/project/{key}/versions",
                     jiraProject );
        resolveList( resolvedStatusIds, client, "status", statusIds, "/rest/api/2/status" );
        resolveList( resolvedResolutionIds, client, "resolution", resolutionIds, "/rest/api/2/resolution" );
        resolveList( resolvedTypeIds, client, "type", typeIds, "/rest/api/2/issuetype" );
        resolveList( resolvedPriorityIds, client, "priority", priorityIds, "/rest/api/2/priority" );
    }

    private void resolveList( List<String> targetList, WebClient client, String what, String input,
                              String listRestUrlPattern, String... listUrlArgs )
                                  throws IOException, MojoExecutionException, MojoFailureException
    {
        if ( input == null || input.length() == 0 )
        {
            return;
        }
        if ( listUrlArgs != null && listUrlArgs.length != 0 )
        {
            client.replacePath( "/" );
            client.path( listRestUrlPattern, listUrlArgs );
        }
        else
        {
            client.replacePath( listRestUrlPattern );
        }
        client.accept( MediaType.APPLICATION_JSON );
        Response resp = client.get();
        if ( resp.getStatus() != Response.Status.OK.getStatusCode() )
        {
            getLog().error( String.format( "Could not get %s list from %s", what, listRestUrlPattern ) );
            reportErrors( resp );
        }

        JsonNode items = getResponseTree( resp );
        String[] pieces = input.split( "," );
        for ( String item : pieces )
        {
            targetList.add( resolveOneItem( items, what, item.trim() ) );
        }
    }

    private String resolveOneItem( JsonNode items, String what, String nameOrId )
        throws IOException, MojoExecutionException, MojoFailureException
    {
        for ( int cx = 0; cx < items.size(); cx++ )
        {
            JsonNode item = items.get( cx );
            if ( nameOrId.equals( item.get( "id" ).asText() ) )
            {
                return nameOrId;
            }
            else if ( nameOrId.equals( item.get( "name" ).asText() ) )
            {
                return item.get( "id" ).asText();
            }
        }
        throw new MojoFailureException( String.format( "Could not find %s %s.", what, nameOrId ) );
    }

    private MediaType getResponseMediaType( Response response )
    {
        String header = (String) response.getMetadata().getFirst( HttpHeaders.CONTENT_TYPE );
        return header == null ? null : MediaType.valueOf( header );
    }

    private void buildIssues( JsonNode issuesNode, String jiraUrl, String jiraProject )
    {
        issueList = new ArrayList<Issue>();
        for ( int ix = 0; ix < issuesNode.size(); ix++ )
        {
            JsonNode issueNode = issuesNode.get( ix );
            assert issueNode.isObject();
            Issue issue = new Issue();
            JsonNode val;

            val = issueNode.get( "id" );
            if ( val != null )
            {
                issue.setId( val.asText() );
            }

            val = issueNode.get( "key" );
            if ( val != null )
            {
                issue.setKey( val.asText() );
                issue.setLink( String.format( "%s/browse/%s", jiraUrl, val.asText() ) );
            }

            // much of what we want is in here.
            JsonNode fieldsNode = issueNode.get( "fields" );

            val = fieldsNode.get( "assignee" );
            processAssignee( issue, val );

            val = fieldsNode.get( "created" );
            processCreated( issue, val );

            val = fieldsNode.get( "comment" );
            processComments( issue, val );

            val = fieldsNode.get( "components" );
            processComponents( issue, val );

            val = fieldsNode.get( "fixVersions" );
            processFixVersions( issue, val );

            val = fieldsNode.get( "issuetype" );
            processIssueType( issue, val );

            val = fieldsNode.get( "priority" );
            processPriority( issue, val );

            val = fieldsNode.get( "reporter" );
            processReporter( issue, val );

            val = fieldsNode.get( "resolution" );
            processResolution( issue, val );

            val = fieldsNode.get( "status" );
            processStatus( issue, val );

            val = fieldsNode.get( "summary" );
            if ( val != null )
            {
                issue.setSummary( val.asText() );
            }

            val = fieldsNode.get( "title" );
            if ( val != null )
            {
                issue.setTitle( val.asText() );
            }

            val = fieldsNode.get( "updated" );
            processUpdated( issue, val );

            val = fieldsNode.get( "versions" );
            processVersions( issue, val );

            issueList.add( issue );
        }
    }

    private void processVersions( Issue issue, JsonNode val )
    {
        StringBuilder sb = new StringBuilder();
        if ( val != null )
        {
            for ( int vx = 0; vx < val.size(); vx++ )
            {
                sb.append( val.get( vx ).get( "name" ).asText() );
                sb.append( ", " );
            }
        }
        if ( sb.length() > 0 )
        {
            // remove last ", "
            issue.setVersion( sb.substring( 0, sb.length() - 2 ) );
        }
    }

    private void processStatus( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            issue.setStatus( val.get( "name" ).asText() );
        }
    }

    private void processPriority( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            issue.setPriority( val.get( "name" ).asText() );
        }
    }

    private void processResolution( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            issue.setResolution( val.get( "name" ).asText() );
        }
    }

    private String getPerson( JsonNode val )
    {
        JsonNode nameNode = val.get( "displayName" );
        if ( nameNode == null )
        {
            nameNode = val.get( "name" );
        }
        if ( nameNode != null )
        {
            return nameNode.asText();
        }
        else
        {
            return null;
        }
    }

    private void processAssignee( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            String text = getPerson( val );
            if ( text != null )
            {
                issue.setAssignee( text );
            }
        }
    }

    private void processReporter( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            String text = getPerson( val );
            if ( text != null )
            {
                issue.setReporter( text );
            }
        }
    }

    private void processCreated( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            try
            {
                issue.setCreated( parseDate( val ) );
            }
            catch ( ParseException e )
            {
                getLog().warn( "Invalid created date " + val.asText() );
            }
        }
    }

    private void processUpdated( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            try
            {
                issue.setUpdated( parseDate( val ) );
            }
            catch ( ParseException e )
            {
                getLog().warn( "Invalid updated date " + val.asText() );
            }
        }
    }

    private Date parseDate( JsonNode val )
        throws ParseException
    {
        return dateFormat.parse( val.asText() );
    }

    private void processFixVersions( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            assert val.isArray();
            for ( int vx = 0; vx < val.size(); vx++ )
            {
                JsonNode fvNode = val.get( vx );
                issue.addFixVersion( fvNode.get( "name" ).asText() );
            }
        }
    }

    private void processComments( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            JsonNode commentsArray = val.get( "comments" );
            for ( int cx = 0; cx < commentsArray.size(); cx++ )
            {
                JsonNode cnode = commentsArray.get( cx );
                issue.addComment( cnode.get( "body" ).asText() );
            }
        }
    }

    private void processComponents( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            assert val.isArray();
            for ( int cx = 0; cx < val.size(); cx++ )
            {
                JsonNode cnode = val.get( cx );
                issue.addComponent( cnode.get( "name" ).asText() );
            }
        }
    }

    private void processIssueType( Issue issue, JsonNode val )
    {
        if ( val != null )
        {
            issue.setType( val.get( "name" ).asText() );
        }
    }

    private void doSessionAuth( WebClient client )
        throws IOException, MojoExecutionException, NoRest
    {
        /* if JiraUser is specified instead of WebUser, we need to make a session. */
        if ( jiraUser != null )
        {
            client.replacePath( "/rest/auth/1/session" );
            client.type( MediaType.APPLICATION_JSON_TYPE );
            StringWriter jsWriter = new StringWriter();
            JsonGenerator gen = jsonFactory.createGenerator( jsWriter );
            gen.writeStartObject();
            gen.writeStringField( "username", jiraUser );
            gen.writeStringField( "password", jiraPassword );
            gen.writeEndObject();
            gen.close();
            Response authRes = client.post( jsWriter.toString() );
            if ( authRes.getStatus() != Response.Status.OK.getStatusCode() )
            {
                if ( authRes.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode()
                    && authRes.getStatus() != Response.Status.FORBIDDEN.getStatusCode() )
                {
                    // if not one of the documented failures, assume that there's no rest in there in the first place.
                    throw new NoRest();
                }
                throw new MojoExecutionException( String.format( "Authentication failure status %d.",
                                                                 authRes.getStatus() ) );
            }
        }
    }

    private WebClient setupWebClient( String jiraUrl )
    {
        WebClient client = WebClient.create( jiraUrl );

        ClientConfiguration clientConfiguration = WebClient.getConfig( client );
        HTTPConduit http = clientConfiguration.getHttpConduit();
        // MCHANGES-324 - Maintain the client session
        clientConfiguration.getRequestContext().put( Message.MAINTAIN_SESSION, Boolean.TRUE );

        if ( getLog().isDebugEnabled() )
        {
            clientConfiguration.getInInterceptors().add( new LoggingInInterceptor() );
            clientConfiguration.getOutInterceptors().add( new LoggingOutInterceptor() );
        }

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        // MCHANGES-341 Externalize JIRA server timeout values to the configuration section
        getLog().debug( "RestJiraDownloader: connectionTimeout: " + connectionTimeout );
        httpClientPolicy.setConnectionTimeout( connectionTimeout );
        httpClientPolicy.setAllowChunking( false );
        getLog().debug( "RestJiraDownloader: receiveTimout: " + receiveTimout );
        httpClientPolicy.setReceiveTimeout( receiveTimout );

        // MCHANGES-334 RestJiraDownloader doesn't honor proxy settings
        getProxyInfo( jiraUrl );

        if ( proxyHost != null )
        {
            getLog().debug( "Using proxy: " + proxyHost + " at port " + proxyPort );
            httpClientPolicy.setProxyServer( proxyHost );
            httpClientPolicy.setProxyServerPort( proxyPort );
            httpClientPolicy.setProxyServerType( ProxyServerType.HTTP );
            if ( proxyUser != null )
            {
                ProxyAuthorizationPolicy proxyAuthorizationPolicy = new ProxyAuthorizationPolicy();
                proxyAuthorizationPolicy.setAuthorizationType( "Basic" );
                proxyAuthorizationPolicy.setUserName( proxyUser );
                proxyAuthorizationPolicy.setPassword( proxyPass );
                http.setProxyAuthorization( proxyAuthorizationPolicy );
            }
        }

        if ( webUser != null )
        {
            AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setAuthorizationType( "Basic" );
            authPolicy.setUserName( webUser );
            authPolicy.setPassword( webPassword );
            http.setAuthorization( authPolicy );
        }

        http.setClient( httpClientPolicy );
        return client;
    }

    public List<Issue> getIssueList()
        throws MojoExecutionException
    {
        return issueList;
    }
}
