package org.apache.maven.plugins.pdf;

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

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Simple bean to allow date interpolation in the document descriptor, i.e.
 * <pre>
 * ${year}  = 2009
 * ${date}  = 2009-05-17
 * </pre>
 * @author ltheussl
 * @version $Id$
 */
public class DateBean
{
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final SimpleDateFormat YEAR = new SimpleDateFormat( "yyyy", Locale.US );
    private static final SimpleDateFormat MONTH = new SimpleDateFormat( "MM", Locale.US );
    private static final SimpleDateFormat DAY = new SimpleDateFormat( "dd", Locale.US );
    private static final SimpleDateFormat HOUR = new SimpleDateFormat( "HH", Locale.US );
    private static final SimpleDateFormat MINUTE = new SimpleDateFormat( "mm", Locale.US );
    private static final SimpleDateFormat SECOND = new SimpleDateFormat( "ss", Locale.US );
    private static final SimpleDateFormat MILLI_SECOND = new SimpleDateFormat( "SSS", Locale.US );
    private static final SimpleDateFormat DATE = new SimpleDateFormat( "yyyy-MM-dd", Locale.US );
    private static final SimpleDateFormat TIME = new SimpleDateFormat( "HH:mm:ss\'Z\'", Locale.US );
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat( "yyyy-MM-dd\'T\'HH:mm:ss\'Z\'", Locale.US );

    static
    {
        YEAR.setTimeZone( UTC_TIME_ZONE );
        MONTH.setTimeZone( UTC_TIME_ZONE );
        DAY.setTimeZone( UTC_TIME_ZONE );
        HOUR.setTimeZone( UTC_TIME_ZONE );
        MINUTE.setTimeZone( UTC_TIME_ZONE );
        SECOND.setTimeZone( UTC_TIME_ZONE );
        MILLI_SECOND.setTimeZone( UTC_TIME_ZONE );
        DATE.setTimeZone( UTC_TIME_ZONE );
        TIME.setTimeZone( UTC_TIME_ZONE );
        DATE_TIME.setTimeZone( UTC_TIME_ZONE );
    }

    private Date date;

    /**
     * Construct a new DateBean for the current Date.
     */
    public DateBean()
    {
        this( new Date() );
    }

    /**
     * Construct a new DateBean with a given Date.
     *
     * @param date the date to set.
     */
    public DateBean( final Date date )
    {
        this.date = date;
    }

    /**
     * Set the Date of this bean.
     *
     * @param date the date to set.
     */
    public void setDate( final Date date )
    {
        this.date = date;
    }


    /**
     * @return the year in format "yyyy".
     */
    public String getYear()
    {
        synchronized ( this )
        {
            return YEAR.format( date );
        }
    }

    /**
     * @return the month in format "MM".
     */
    public String getMonth()
    {
        synchronized ( this )
        {
            return MONTH.format( date );
        }
    }

    /**
     * @return the day in format "dd".
     */
    public String getDay()
    {
        synchronized ( this )
        {
            return DAY.format( date );
        }
    }

    /**
     * @return the hour in format "HH".
     */
    public String getHour()
    {
        synchronized ( this )
        {
            return HOUR.format( date );
        }
    }

    /**
     * @return the minute in format "mm".
     */
    public String getMinute()
    {
        synchronized ( this )
        {
            return MINUTE.format( date );
        }
    }

    /**
     * @return the second in format "ss".
     */
    public String getSecond()
    {
        synchronized ( this )
        {
            return SECOND.format( date );
        }
    }

    /**
     * @return the millisecond in format "SSS".
     */
    public String getMillisecond()
    {
        synchronized ( this )
        {
            return MILLI_SECOND.format( date );
        }
    }

    /**
     * @return the date using the ISO 8601 format, i.e. <code>yyyy-MM-dd</code>.
     */
    public String getDate()
    {
        synchronized ( this )
        {
            return DATE.format( date );
        }
    }

    /**
     * @return the time using the ISO 8601 format and UTC time zone, i.e. <code>HH:mm:ss'Z'</code>.
     */
    public String getTime()
    {
        synchronized ( this )
        {
            return TIME.format( date );
        }
    }

    /**
     * @return the datetime using the ISO 8601 format, i.e. <code>yyyy-MM-dd'T'HH:mm:ss'Z'</code>.
     */
    public String getDateTime()
    {
        synchronized ( this )
        {
            return DATE_TIME.format( date );
        }
    }
}
