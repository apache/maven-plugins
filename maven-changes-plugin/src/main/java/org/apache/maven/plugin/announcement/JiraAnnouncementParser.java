package org.apache.maven.plugin.announcement;

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

import org.apache.maven.plugin.changes.Action;
import org.apache.maven.plugin.changes.Release;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XML Parser for <code>JiraAnnouncement</code>s. This works on an XML file
 * downloaded from JIRA and creates a List of issues that is exposed to the
 * user of the class. It can also extract a List of releases from such issues.
 *
 * @author aramirez@exist.com
 * @version $Id$
 */
public class JiraAnnouncementParser
    extends DefaultHandler
{
    private String elementValue;

    private String parentElement = "";

    private JiraAnnouncement issue;

    private List issues = new ArrayList();

    public JiraAnnouncementParser( String xmlPath )
    {
        File xml = new File( xmlPath );

        parseJira( xml );
    }

    public JiraAnnouncementParser( File xmlPath )
    {
        parseJira( xmlPath );
    }

    public void parseJira( File xml )
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try
        {
            SAXParser parser = factory.newSAXParser();

            parser.parse( xml, this );
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
            issue = new JiraAnnouncement();

            parentElement = "item";
        }
    }

    public void endElement( String namespaceURI, String sName, String qName )
        throws SAXException
    {
        elementValue = elementValue.trim();
        
        if ( qName.equals( "item" ) )
        {
            issues.add( issue );

            parentElement = "";
        }
        else if ( qName.equals( "title" ) && parentElement.equals( "item" ) )
        {
            issue.setTitle( elementValue );
        }
        else if ( qName.equals( "key" ) )
        {
            issue.setKey( elementValue );
        }
        else if ( qName.equals( "link" ) && parentElement.equals( "item" ) )
        {
            issue.setLink( elementValue );
        }
        else if ( qName.equals( "summary" ) )
        {
            issue.setSummary( elementValue );
        }
        else if ( qName.equals( "type" ) )
        {
            issue.setType( elementValue );
        }
        else if ( qName.equals( "status" ) )
        {
            issue.setStatus( elementValue );
        }
        else if ( qName.equals( "resolution" ) )
        {
            issue.setResolution( elementValue );
        }
        else if ( qName.equals( "assignee" ) )
        {
            issue.setAssignee( elementValue );
        }
        else if ( qName.equals( "reporter" ) )
        {
            issue.setReporter( elementValue );
        }
        else if ( qName.equals( "fixVersion" ) )
        {
            issue.setFixVersion( elementValue );
        }
        else if ( qName.equals( "comment" ) )
        {
            issue.addComment( elementValue );
        }
        
        elementValue = "";
    }

    public void characters( char[] buff, int offset, int len )
        throws SAXException
    {
        String str = new String( buff, offset, len );

        elementValue += str;
    }

    public List getIssues()
    {
        return this.issues;
    }

    public List getReleases( List issues )
    {
        // A Map of releases keyed by fixVersion
        Map releasesMap = new HashMap();

        // Loop through all issues looking for fixVersions
        for ( int i = 0; i < issues.size(); i++ )
        {
            JiraAnnouncement issue = (JiraAnnouncement) issues.get( i );
            // Do NOT create a release for issues that lack a fixVersion
            if ( issue.getFixVersion() != null )
            {
                // Try to get a matching Release from the map
                Release release = (Release) releasesMap.get( issue.getFixVersion() );
                if ( release == null )
                {
                    // Add a new Release to the Map if it wasn't there
                    release = new Release();
                    release.setVersion( issue.getFixVersion() );
                    releasesMap.put( issue.getFixVersion(), release );
                }

                // Add this issue as an Action to this release
                Action action = createAction( issue );
                release.addAction( action );
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
    private Action createAction( JiraAnnouncement issue )
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
