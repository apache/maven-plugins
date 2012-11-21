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

import org.apache.maven.plugin.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Builder for a JIRA query using the JIRA query language.
 * Only a limited set of JQL is supported.
 *
 * @author ton.swieb@finalist.com
 * @version $Id$
 * @since 2.8
 */
public class JqlQueryBuilder
    implements JiraQueryBuilder
{
    private String filter = "";
    private boolean urlEncode = true;

    /**
     * Log for debug output.
     */
    private Log log;

    private StringBuilder orderBy = new StringBuilder();

    private StringBuilder query = new StringBuilder();

    public JqlQueryBuilder( Log log )
    {
        this.log = log;
    }

    public String build()
    {
        try
        {
            String jqlQuery;
            // If the user has defined a filter - use that
            if ( ( this.filter != null ) && ( this.filter.length() > 0 ) )
            {
                jqlQuery = filter;
            }
            else
            {
                jqlQuery = query.toString() + orderBy.toString();
            }

            if ( urlEncode )
            {
                getLog().debug( "Encoding JQL query " + jqlQuery );
                String encodedQuery = URLEncoder.encode( jqlQuery, "UTF-8" );
                getLog().debug( "Encoded JQL query " + encodedQuery );
                return encodedQuery;
            }
            else
            {
                return jqlQuery;
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            getLog().error( "Unable to encode JQL query with UTF-8", e );
            throw new RuntimeException( e );
        }
    }

    public JiraQueryBuilder components( String components )
    {
        addCommaSeparatedValues( "component", components );
        return this;
    }

    public JiraQueryBuilder filter( String filter )
    {
        this.filter = filter;
        return this;
    }

    /**
     * When both {@link #fixVersion(String)} and {@link #fixVersionIds(String)} are used then you will probably
     * end up with a JQL query that is valid, but returns nothing. Unless they both only reference the same fixVersion
     *
     * @param fixVersion
     * @return
     */
    public JiraQueryBuilder fixVersion( String fixVersion )
    {
        addSingleValue( "fixVersion", fixVersion );
        return this;
    }

    /**
     * When both {@link #fixVersion(String)} and {@link #fixVersionIds(String)} are used then you will probably
     * end up with a JQL query that is valid, but returns nothing. Unless they both only reference the same fixVersion
     *
     * @param fixVersionIds
     * @return
     */
    public JiraQueryBuilder fixVersionIds( String fixVersionIds )
    {
        addCommaSeparatedValues( "fixVersion", fixVersionIds );
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public JiraQueryBuilder priorityIds( String priorityIds )
    {
        addCommaSeparatedValues( "priority", priorityIds );
        return this;
    }

    public JiraQueryBuilder project( String project )
    {
        addSingleValue( "project", project );
        return this;
    }

    public JiraQueryBuilder resolutionIds( String resolutionIds )
    {
        addCommaSeparatedValues( "resolution", resolutionIds );
        return this;
    }

    public JiraQueryBuilder sortColumnNames( String sortColumnNames )
    {
        if ( sortColumnNames != null )
        {
            orderBy.append( " ORDER BY " );

            String[] sortColumnNamesArray = sortColumnNames.split( "," );

            for ( int i = 0; i < sortColumnNamesArray.length - 1; i++ )
            {
                addSingleSortColumn( sortColumnNamesArray[i] );
                orderBy.append( ", " );
            }
            addSingleSortColumn( sortColumnNamesArray[sortColumnNamesArray.length - 1] );
        }
        return this;
    }

    public JiraQueryBuilder statusIds( String statusIds )
    {
        addCommaSeparatedValues( "status", statusIds );
        return this;
    }


    public JiraQueryBuilder typeIds( String typeIds )
    {
        addCommaSeparatedValues( "type", typeIds );
        return this;
    }

    public JiraQueryBuilder urlEncode( boolean doEncoding )
    {
        urlEncode = doEncoding;
        return this;
    }

    public boolean urlEncode()
    {
        return urlEncode;
    }

    /* --------------------------------------------------------------------- */
    /* Private methods                                                       */
    /* --------------------------------------------------------------------- */

    private void addCommaSeparatedValues( String key, String values )
    {
        if ( values != null )
        {
            if ( query.length() > 0 )
            {
                query.append( " AND " );
            }

            query.append( key + " in (" );

            String[] valuesArr = values.split( "," );

            for ( int i = 0; i < ( valuesArr.length - 1 ); i++ )
            {
                trimAndQuoteValue( valuesArr[i] );
                query.append( ", " );
            }
            trimAndQuoteValue( valuesArr[valuesArr.length - 1] );
            query.append( ")" );
        }
    }

    private void addSingleSortColumn( String name )
    {
        boolean descending = false;
        name = name.trim().toLowerCase( Locale.ENGLISH );
        if ( name.endsWith( "desc" ) )
        {
            descending = true;
            name = name.substring( 0, name.length() - 4 ).trim();
        }
        else if ( name.endsWith( "asc" ) )
        {
            descending = false;
            name = name.substring( 0, name.length() - 3 ).trim();
        }
        // Strip any spaces from the column name, or it will trip up JIRA's JQL parser
        name = name.replaceAll( " ", "" );
        orderBy.append( name );
        orderBy.append( descending ? " DESC" : " ASC" );
    }

    private void addSingleValue( String key, String value )
    {
        if ( value != null )
        {
            if ( query.length() > 0 )
            {
                query.append( " AND " );
            }
            query.append( key ).append( " = " );
            trimAndQuoteValue( value );
        }
    }

    private void trimAndQuoteValue( String value )
    {
        String trimmedValue = value.trim();
        if ( trimmedValue.contains( " " ) || trimmedValue.contains( "." ) )
        {
            query.append( "\"" ).append( trimmedValue ).append( "\"" );
        }
        else
        {
            query.append( trimmedValue );
        }
    }
}
