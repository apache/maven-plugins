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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser for <code>JiraIssue</code>s. This works on an XML file downloaded
 * from JIRA and creates a List of issues that is exposed to the user of the
 * class.
 *
 * @version $Id$
 */
public class JiraXML
    extends DefaultHandler
{
    private List issueList;

    private StringBuffer currentElement = new StringBuffer( 1024 );

    private String currentParent = "";

    private JiraIssue issue;

    public JiraXML( File xmlPath )
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        issueList = new ArrayList();

        try
        {
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse( xmlPath, this );
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
    }

    public void startElement( String namespaceURI, String sName, String qName, Attributes attrs )
        throws SAXException
    {
        if ( qName.equals( "item" ) )
        {
            issue = new JiraIssue();

            currentParent = "item";
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

        currentElement.setLength( 0 );
    }

    public void characters( char[] buf, int offset, int len )
        throws SAXException
    {
        currentElement.append( buf, offset, len );
    }

    public List getIssueList()
    {
        return this.issueList;
    }

    public static List getReleases( List issues )
    {
        // A Map of releases keyed by fixVersion
        Map releasesMap = new HashMap();

        // Loop through all issues looking for fixVersions
        for ( int i = 0; i < issues.size(); i++ )
        {
            JiraIssue issue = (JiraIssue) issues.get( i );
            // Do NOT create a release for issues that lack a fixVersion
            if ( issue.getFixVersions() != null )
            {
                for ( Iterator iterator = issue.getFixVersions().iterator(); iterator.hasNext(); )
                {
                    String fixVersion = (String) iterator.next();

                    // Try to get a matching Release from the map
                    Release release = (Release) releasesMap.get( fixVersion );
                    if ( release == null )
                    {
                        // Add a new Release to the Map if it wasn't there
                        release = new Release();
                        release.setVersion( fixVersion );
                        releasesMap.put( fixVersion, release );
                    }

                    // Add this issue as an Action to this release
                    Action action = createAction( issue );
                    release.addAction( action );
                }
            }
        }

        // Extract the releases from the Map to a List
        List releasesList = new ArrayList();
        for ( Iterator iterator = releasesMap.entrySet().iterator(); iterator.hasNext(); )
        {
            Release o = (Release) ( (Map.Entry) iterator.next() ).getValue();
            releasesList.add( o );
        }
        return releasesList;
    }

    /**
     * Create an <code>Action</code> from a JIRA issue.
     *
     * @param issue The issue to extract the information from
     * @return An <code>Action</code>
     */
    private static Action createAction( JiraIssue issue )
    {
        Action action = new Action();

        action.setIssue( issue.getKey() );

        String type = "";
        if ( issue.getType().equals( "Bug" ) )
        {
            type = "fix";
        }
        else if ( issue.getType().equals( "New Feature" ) )
        {
            type = "add";
        }
        else if ( issue.getType().equals( "Improvement" ) )
        {
            type = "update";
        }
        action.setType( type );

        action.setDev( issue.getAssignee() );

        // Set dueTo to the empty String instead of null to make Velocity happy
        action.setDueTo( "" );
        //action.setDueTo( issue.getReporter() );

        action.setAction( issue.getSummary() );
        return action;
    }
}
