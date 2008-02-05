package org.apache.maven.plugin.changes;

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

import org.apache.maven.plugin.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * XML Parser for changes.xml files.
 *
 * @version $Id$
 */
public class ChangesXML
    extends DefaultHandler
{
    private Action action;

    private List actionList;

    private Release release;

    private StringBuffer currentElement = new StringBuffer( 1024 );

    private String currentName;

    private List releaseList;

    private String author;

    private String authorEmail;

    private String title;

    public ChangesXML( File xmlPath, Log log )
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try
        {
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse( xmlPath, this );
        }
        catch ( Throwable t )
        {
            log.error( "An error occured when parsing the changes.xml file:", t );
        }
    }

    public void setAuthor( String author )
    {
        this.author = author;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthorEmail( String authorEmail )
    {
        this.authorEmail = authorEmail;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setReleaseList( List releaseList )
    {
        this.releaseList = releaseList;
    }

    public List getReleaseList()
    {
        return releaseList;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void characters( char[] buf, int offset, int len )
        throws SAXException
    {
        currentElement.append( buf, offset, len );
    }

    public void endElement( String namespaceURI, String sName, String qName )
        throws SAXException
    {
        if ( qName.equals( "title" ) )
        {
            this.title = currentElement.toString().trim();
        }
        else if ( qName.equals( "author" ) )
        {
            this.title = currentElement.toString().trim();
        }
        else if ( qName.equals( "action" ) )
        {
            action.setAction( currentElement.toString().trim() );

            actionList.add( action );
        }
        else if ( qName.equals( "release" ) )
        {
            release.setAction( actionList );

            releaseList.add( release );
        }

        currentElement.setLength( 0 );
    }

    public void startElement( String namespaceURI, String sName, String qName, Attributes attrs )
        throws SAXException
    {
        if ( qName.equals( "title" ) )
        {
            this.title = "";
        }
        else if ( qName.equals( "author" ) )
        {
            this.authorEmail = attrs.getValue( "email" );

            this.author = "";
        }
        else if ( qName.equals( "body" ) )
        {
            releaseList = new ArrayList();
        }
        else if ( qName.equals( "release" ) )
        {
            release = new Release();

            release.setDateRelease( attrs.getValue( "date" ) );

            release.setVersion( attrs.getValue( "version" ) );

            release.setDescription( attrs.getValue( "description" ) );

            actionList = new ArrayList();
        }
        else if ( qName.equals( "action" ) )
        {
            action = new Action();

            action.setDev( attrs.getValue( "dev" ) );

            action.setDueTo( attrs.getValue( "due-to" ) );

            action.setDueToEmail( attrs.getValue( "due-to-email" ) );

            action.setType( attrs.getValue( "type" ) );

            action.setIssue( attrs.getValue( "issue" ) );
        }

        currentName = qName;
    }
}
