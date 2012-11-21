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

package org.apache.maven.plugin.jira;

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
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.issues.Issue;

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
 * Use the JIRA REST API to implement the download.
 *
 * This class assumes that the URL points to a copy of JIRA that implements the REST API.
 * A static function may be forthcoming in here to probe and see if a given URL supports it.
 *
 */
public class RestJiraDownloader extends AbstractJiraDownloader
{
    private List<Issue> issueList;
    private JsonFactory jsonFactory;
    private SimpleDateFormat dateFormat;

    public static class NoRest extends Exception {
        public NoRest( )
        {
            // blank on purpose.
        }
    }

    public RestJiraDownloader() {
        jsonFactory = new MappingJsonFactory(  );
        //2012-07-17T06:26:47.723-0500
        dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
    }

    public void doExecute() throws Exception
    {

        Map<String, String> urlMap = JiraHelper.getJiraUrlAndProjectName( project.getIssueManagement().getUrl() );
        String jiraUrl = urlMap.get( "url" );
        String jiraProject = urlMap.get( "project" ); // assumed to be a 'project key'
        WebClient client = setupWebClient( jiraUrl );
        /*
         If there is no session auth, explicitly probe to see if there is any REST.
         */
        if ( jiraUser == null)
        {
            client.replacePath( "/rest/api/2/serverInfo" );
            client.accept( MediaType.APPLICATION_JSON );
            Response siResponse = client.get();
            if ( siResponse.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new NoRest();
            }
        }
        doSessionAuth( client );

        String jqlQuery = new JqlQueryBuilder( log )
            .urlEncode( false )
            .project( jiraProject )
            .fixVersion( getFixFor() )
            .fixVersionIds( fixVersionIds )
            .statusIds( statusIds )
            .priorityIds( priorityIds )
            .resolutionIds( resolutionIds )
            .components( component )
            .typeIds( typeIds )
            .sortColumnNames( sortColumnNames )
            .build();

        StringWriter searchParamStringWriter = new StringWriter( );
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
            if ( MediaType.APPLICATION_JSON_TYPE.getType().equals( getResponseMediaType( searchResponse ).getType() ) )
            {
                JsonParser jsonParser = jsonFactory.createJsonParser( ( InputStream ) searchResponse.getEntity() );
                JsonNode errorTree = jsonParser.readValueAsTree();
                assert errorTree.isObject();
                JsonNode messages = errorTree.get( "errorMessages" );
                if ( messages != null )
                {
                    for ( int mx = 0; mx < messages.size(); mx ++ )
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
            throw new MojoExecutionException( String.format( "Failed to query issues; response %d", searchResponse.getStatus() ) );
        }

        JsonParser jsonParser = jsonFactory.createJsonParser( ( InputStream ) searchResponse.getEntity() );
        JsonNode issueTree = jsonParser.readValueAsTree();
        assert issueTree.isObject();
        JsonNode issuesNode = issueTree.get( "issues" );
        assert issuesNode.isArray();
        buildIssues( issuesNode, jiraUrl, jiraProject );
    }

    private MediaType getResponseMediaType( Response response )
    {
        String header = (String) response.getMetadata().getFirst( HttpHeaders.CONTENT_TYPE ) ;
        return header == null ? null : MediaType.valueOf( header );
    }

    private void buildIssues( JsonNode issuesNode, String jiraUrl, String jiraProject )
    {
        issueList = new ArrayList<Issue>(  );
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
                issue.setLink( String.format( "%s/browse/%s", jiraUrl, val.asText()) );
            }

            // much of what we want is in here.
            JsonNode fieldsNode = issueNode.get( "fields" );

            val = fieldsNode.get( "assignee" );
            processAssignee( issue, val );

            val = fieldsNode.get( "created" );
            processCreated( issue, val );

            val = fieldsNode.get( "comment" );
            processComments( issue, val );

            val = fieldsNode.get( "fixVersions" );
            processFixVersions( issue, val );


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

            val = issueNode.get( "title" );
            if ( val != null )
            {
                issue.setTitle( val.asText() );
            }

            val = issueNode.get( "updated" );
            processUpdated( issue, val );

            val = issueNode.get( "versions" );
            processVersions( issue, val );


            issueList.add( issue );
        }
    }

    private void processVersions( Issue issue, JsonNode val )
    {
        StringBuilder sb = new StringBuilder( );
        if ( val != null )
        {
            for ( int vx = 0; vx < val.size(); vx ++ )
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
        if (val != null )
        {
            issue.setStatus( val.get( "name" ).asText() );
        }
    }

    private void processPriority( Issue issue, JsonNode val )
    {
        if (val != null )
        {
            issue.setPriority( val.get( "name" ).asText() );
        }
    }

    private void processResolution( Issue issue, JsonNode val )
    {
        if (val != null )
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
                getLog().warn( "Invalid created date " + val.asText() );
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
        if (val != null)
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

    private void doSessionAuth( WebClient client )
        throws IOException, MojoExecutionException, NoRest
    {
        /* if JiraUser is specified instead of WebUser, we need to make a session. */
        if ( jiraUser != null )
        {
            client.replacePath( "/rest/auth/1/session" );
            client.type( MediaType.APPLICATION_JSON_TYPE );
            StringWriter jsWriter = new StringWriter( );
            JsonGenerator gen = jsonFactory.createGenerator( jsWriter );
            gen.writeStartObject();
            gen.writeStringField( "username", jiraUser );
            gen.writeStringField( "password", jiraPassword );
            gen.writeEndObject();
            gen.close();
            Response authRes = client.post( jsWriter.toString() );
            if ( authRes.getStatus() != Response.Status.OK.getStatusCode() )
            {
                if ( authRes.getStatus() != 401 && authRes.getStatus() != 403 )
                {
                    // if not one of the documented failures, assume that there's no rest in there in the first place.
                    throw new NoRest();
                }
                throw new MojoExecutionException( String.format( "Authentication failure status %d.", authRes.getStatus() ) );
            }
        }
    }

    private WebClient setupWebClient( String jiraUrl )
    {
        WebClient client = WebClient.create( jiraUrl );

        ClientConfiguration clientConfiguration = WebClient.getConfig( client );
        HTTPConduit http = clientConfiguration.getHttpConduit();

        if ( getLog().isDebugEnabled() )
        {
            clientConfiguration.getInInterceptors().add( new LoggingInInterceptor(  ) );
            clientConfiguration.getOutInterceptors().add( new LoggingOutInterceptor(  ) );
        }

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setConnectionTimeout(36000);
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setReceiveTimeout(32000);

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

        http.setClient(httpClientPolicy);
        return client;
    }

    public List<Issue> getIssueList() throws MojoExecutionException
    {
        return issueList;
    }
}
