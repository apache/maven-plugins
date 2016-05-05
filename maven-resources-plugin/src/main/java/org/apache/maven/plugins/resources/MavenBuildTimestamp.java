package org.apache.maven.plugins.resources;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

/**
 * This class is duplicated from maven-model-builder from maven core. (See MRESOURCES-99).
 */
public class MavenBuildTimestamp
{
    /**
     * ISO 8601-compliant timestamp for machine readability
     */
    public static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * The property name.
     */
    public static final String BUILD_TIMESTAMP_FORMAT_PROPERTY = "maven.build.timestamp.format";

    /**
     * The default time zone {@code Etc/UTC}.
     */
    public static final TimeZone DEFAULT_BUILD_TIME_ZONE = TimeZone.getTimeZone( "Etc/UTC" );

    private String formattedTimestamp;

    /**
     * Create an instance.
     */
    public MavenBuildTimestamp()
    {
        this( new Date() );
    }

    /**
     * @param time The time to use.
     */
    public MavenBuildTimestamp( Date time )
    {
        this( time, DEFAULT_BUILD_TIMESTAMP_FORMAT );
    }

    /**
     * @param time The time to use.
     * @param properties the properties which can be define. can be {@code null}
     */
    public MavenBuildTimestamp( Date time, Properties properties )
    {
        this( time, properties != null ? properties.getProperty( BUILD_TIMESTAMP_FORMAT_PROPERTY ) : null );
    }

    /**
     * @param time The time to use.
     * @param timestampFormat The format for {@link SimpleDateFormat}.
     */
    public MavenBuildTimestamp( Date time, String timestampFormat )
    {
        SimpleDateFormat dateFormat;

        if ( timestampFormat == null )
        {
            dateFormat = new SimpleDateFormat( DEFAULT_BUILD_TIMESTAMP_FORMAT );
        }
        else
        {
            dateFormat = new SimpleDateFormat( timestampFormat );
        }

        dateFormat.setCalendar( new GregorianCalendar() );
        dateFormat.setTimeZone( DEFAULT_BUILD_TIME_ZONE );

        if ( time == null )
        {
            formattedTimestamp = dateFormat.format( new Date() );
        }
        else
        {
            formattedTimestamp = dateFormat.format( time );
        }

    }

    /**
     * @return The formatted time stamp.
     */
    public String formattedTimestamp()
    {
        return formattedTimestamp;
    }
}
