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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser that extracts <code>Issue</code>s from JIRA. This works on an XML
 * file downloaded from JIRA and creates a <code>List</code> of issues that is
 * exposed to the user of the class.
 *
 * @version $Id$
 */
public class JiraXML
    extends DefaultHandler
{
    private final List<Issue> issueList;

    private final StringBuffer currentElement = new StringBuffer( 1024 );

    private String currentParent = "";

    private final String datePattern;

    private Issue issue;

    private String jiraVersion = null;

    private final Log log;

    private SimpleDateFormat sdf = null;

    /**
     *
     * @param log not null.
     * @param datePattern may be null.
     * @since 2.4
     */
    public JiraXML( Log log, String datePattern )
    {
        this.log = log;
        this.datePattern = datePattern;

        if ( datePattern == null )
        {
            sdf = null;
        }
        else
        {
            // @todo Do we need to be able to configure the locale of the JIRA server as well?
            sdf = new SimpleDateFormat( datePattern, Locale.ENGLISH );
        }

        this.issueList = new ArrayList<Issue>( 16 );
    }

    /**
     * Parse the given xml file. The list of issues can then be retrieved with {@link #getIssueList()}.
     *
     * @param xmlPath the file to pares.
     * @throws MojoExecutionException 
     *
     * @since 2.4
     */
    public void parseXML( File xmlPath ) throws MojoExecutionException
    {
        parse( xmlPath );
    }

    private void parse( File xmlPath ) throws MojoExecutionException
    {
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse( xmlPath, this );
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException ( "Failed to parse JIRA XML.", t );
        }
    }

    public void startElement( String namespaceURI, String sName, String qName, Attributes attrs )
        throws SAXException
    {
        if ( qName.equals( "item" ) )
        {
            issue = new Issue();

            currentParent = "item";
        }
        else if ( qName.equals( "key" ) )
        {
            String id = attrs.getValue( "id" );
            if ( id != null )
            {
                issue.setId( id.trim() );
            }
        }
        else if ( qName.equals( "build-info" ) )
        {
            currentParent = "build-info";
        }
    }

    public void endElement( String namespaceURI, String sName, String qName )
        throws SAXException
    {
        if ( qName.equals( "item" ) )
        {
            issueList.add( issue );

            currentParent = "";
        }
        else if ( qName.equals( "key" ) )
        {
            issue.setKey( currentElement.toString().trim() );
        }
        else if ( qName.equals( "summary" ) )
        {
            issue.setSummary( currentElement.toString().trim() );
        }
        else if ( qName.equals( "type" ) )
        {
            issue.setType( currentElement.toString().trim() );
        }
        else if ( qName.equals( "link" ) && currentParent.equals( "item" ) )
        {
            issue.setLink( currentElement.toString().trim() );
        }
        else if ( qName.equals( "priority" ) )
        {
            issue.setPriority( currentElement.toString().trim() );
        }
        else if ( qName.equals( "status" ) )
        {
            issue.setStatus( currentElement.toString().trim() );
        }
        else if ( qName.equals( "resolution" ) )
        {
            issue.setResolution( currentElement.toString().trim() );
        }
        else if ( qName.equals( "assignee" ) )
        {
            issue.setAssignee( currentElement.toString().trim() );
        }
        else if ( qName.equals( "reporter" ) )
        {
            issue.setReporter( currentElement.toString().trim() );
        }
        else if ( qName.equals( "version" ) && currentParent.equals( "item" ) )
        {
            issue.setVersion( currentElement.toString().trim() );
        }
        else if ( qName.equals( "version" ) && currentParent.equals( "build-info" ) )
        {
            jiraVersion = currentElement.toString().trim();
        }
        else if ( qName.equals( "fixVersion" ) )
        {
            issue.addFixVersion( currentElement.toString().trim() );
        }
        else if ( qName.equals( "component" ) )
        {
            issue.addComponent( currentElement.toString().trim() );
        }
        else if ( qName.equals( "comment" ) )
        {
            issue.addComment( currentElement.toString().trim() );
        }
        else if ( qName.equals( "title" ) && currentParent.equals( "item" ) )
        {
            issue.setTitle( currentElement.toString().trim() );
        }
        else if ( qName.equals( "created" ) && currentParent.equals( "item" ) && sdf != null )
        {
            try
            {
                issue.setCreated( sdf.parse( currentElement.toString().trim() ) );
            }
            catch ( ParseException e )
            {
                log.warn( "Element \"Created\". " + e.getMessage() + ". Using the pattern \"" + datePattern + "\"" );
            }
        }
        else if ( qName.equals( "updated" ) && currentParent.equals( "item" ) && sdf != null )
        {
            try
            {
                issue.setUpdated( sdf.parse( currentElement.toString().trim() ) );
            }
            catch ( ParseException e )
            {
                log.warn( "Element \"Updated\". " + e.getMessage() + ". Using the pattern \"" + datePattern + "\"" );
            }
        }

        currentElement.setLength( 0 );
    }

    public void characters( char[] buf, int offset, int len )
        throws SAXException
    {
        currentElement.append( buf, offset, len );
    }

    public List<Issue> getIssueList()
    {
        return Collections.unmodifiableList( this.issueList );
    }

    public String getJiraVersion()
    {
        return jiraVersion;
    }
}
