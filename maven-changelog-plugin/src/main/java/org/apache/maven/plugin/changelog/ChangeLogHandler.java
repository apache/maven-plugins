package org.apache.maven.plugin.changelog;

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

import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.command.changelog.ChangeLogSet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

/**
 * Change log generated xml parser.  SAXParser listener for processing a previously generated xml into several
 * change log sets.
 */
public class ChangeLogHandler
    extends DefaultHandler
{
    private Collection changeSets;

    private String bufData = "";

    private ChangeFile bufFile;

    private ChangeSet bufEntry;

    private LinkedList bufEntries;

    private ChangeLogSet bufSet;

    private String currentPattern;

    /**
     * contructor
     *
     * @param changeSets collection object to store all change sets found within the xml document
     */
    public ChangeLogHandler( Collection changeSets )
    {
        this.changeSets = changeSets;
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[],int,int)
     */
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        bufData += new String( ch, start, length );
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(String,String,String)
     */
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( "changeset".equals( qName ) )
        {
            changeSets.add( bufSet );
        }

        if ( "changelog-entry".equals( qName ) )
        {
            bufEntries.add( bufEntry );
        }

        if ( "file".equals( qName ) )
        {
            bufEntry.addFile( bufFile );
        }
        else if ( "date".equals( qName ) )
        {
            try
            {
                long ms = 0;
                if ( bufEntry.getDate() != null )
                {
                    ms = bufEntry.getDate().getTime();
                }
                bufEntry.setDate( new Date( ms + new SimpleDateFormat( currentPattern ).parse( bufData ).getTime() ) );
            }
            catch ( ParseException e )
            {
                throw new SAXException( e );
            }
        }
        else if ( "time".equals( qName ) )
        {
            try
            {
                long ms = 0;
                if ( bufEntry.getDate() != null )
                {
                    ms = bufEntry.getDate().getTime();
                }
                bufEntry.setDate( new Date( ms + new SimpleDateFormat( currentPattern ).parse( bufData ).getTime() + TimeZone.getDefault().getRawOffset()) );
            }
            catch ( ParseException e )
            {
                throw new SAXException( e );
            }
        }
        else if ( "author".equals( qName ) )
        {
            bufEntry.setAuthor( bufData );
        }
        else if ( "msg".equals( qName ) )
        {
            bufEntry.setComment( bufData );
        }

        if ( "revision".equals( qName ) )
        {
            bufFile.setRevision( bufData );
        }
        else if ( "name".equals( qName ) )
        {
            bufFile.setName( bufData.replaceFirst( " \\(from [^:]+:\\d+\\)", "" ) );
        }
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#startElement(String,String,String,Attributes)
     */
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        bufData = "";

        if ( "file".equals( qName ) )
        {
            bufFile = new ChangeFile( "" );
        }
        else if ( "changelog-entry".equals( qName ) )
        {
            bufEntry = new ChangeSet();
        }
        else if ( "date".equals( qName ) )
        {
            currentPattern = attributes.getValue( "pattern" );
            if ( currentPattern == null )
            {
                currentPattern = "yyyy-MM-dd";
            }
        }
        else if ( "time".equals( qName ) )
        {
            currentPattern = attributes.getValue( "pattern" );
            if ( currentPattern == null )
            {
                currentPattern = "HH:mm:ss";
            }
        }
        else if ( "changeset".equals( qName ) )
        {
            bufEntries = new LinkedList();

            currentPattern = attributes.getValue( "datePattern" );
            if ( currentPattern == null )
            {
                currentPattern = "yyyy-MM-dd";
            }

            SimpleDateFormat formatter = new SimpleDateFormat( currentPattern );

            String start = attributes.getValue( "start" );

            String end = attributes.getValue( "end" );

            Date startDate = null;

            Date endDate = null;

            if ( start != null )
            {
                try
                {
                    startDate = formatter.parse( start );
                }
                catch ( ParseException e )
                {
                    throw new SAXException( "Can't parse start date '" + start + "'.", e );
                }
            }

            if ( end != null )
            {
                try
                {
                    endDate = formatter.parse( end );
                }
                catch ( ParseException e )
                {
                    throw new SAXException( "Can't parse end date '" + end + "'.", e );
                }
            }

            bufSet = new ChangeLogSet( bufEntries, startDate, endDate );
        }
    }
}
