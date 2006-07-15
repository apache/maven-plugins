package org.apache.maven.changes;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.logging.Log;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ChangesXML
    extends DefaultHandler
{
    Action action;

    List actionList;

    Release release;

    String currentElement;

    String currentName;

    private List releaseList;

    private String author;

    private String authorEmail;

    private String title;

    public ChangesXML( String xmlPath, Log log )
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try
        {
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse( new File( xmlPath ), this );
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
        String s = new String( buf, offset, len );

        if ( !s.trim().equals( "" ) )
        {
            currentElement = currentElement + s.trim() + "\n";
        }
    }

    public void endElement( String namespaceURI, String sName, String qName )
        throws SAXException
    {
        if ( qName.equals( "title" ) )
        {
            this.title = currentElement;
        }
        else if ( qName.equals( "author" ) )
        {
            this.title = currentElement;
        }
        else if ( qName.equals( "action" ) )
        {
            action.setAction( currentElement.trim() );

            actionList.add( action );
        }
        else if ( qName.equals( "release" ) )
        {
            release.setAction( actionList );

            releaseList.add( release );
        }

        currentElement = "";
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

            release.setDescription( attrs.getValue( "desc" ) );

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
