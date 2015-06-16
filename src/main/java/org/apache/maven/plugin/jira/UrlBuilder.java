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

/**
 * Builder for a URL which build up of host part, a context part and 0 or more parameters.
 *
 * @author ton.swieb@finalist.com
 * @version $Id$
 * @since 2.8
 */
public class UrlBuilder
{
    private static final String AMPERSAND = "&";

    private static final String QUESTION_MARK = "?";

    private StringBuilder query = new StringBuilder();

    public UrlBuilder( String url, String context )
    {
        query.append( url ).append( "/" ).append( context );
    }

    public UrlBuilder addParameter( String key, String value )
    {
        if ( key != null && value != null )
        {
            if ( query.toString().contains( QUESTION_MARK ) )
            {
                query.append( AMPERSAND );
            }
            else
            {
                query.append( QUESTION_MARK );
            }
            query.append( key ).append( "=" ).append( value );
        }
        return this;
    }

    public UrlBuilder addParameter( String key, int value )
    {
        addParameter( key, String.valueOf( value ) );
        return this;
    }

    public String build()
    {
        return query.toString();
    }
}
