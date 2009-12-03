package org.apache.maven.plugin.trac;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A Trac Ticket.
 *
 * @author Noriko Kinugasa
 * @version $Id$
 */
public class TracTicket
{
    private String id;

    private String link;

    private Date timeCreated;

    private Date timeChanged;

    private String type;

    private String summary;

    private String status;

    private String resolution;

    private String milestone;

    private String owner;

    private String priority;

    private String reporter;

    private String component;

    public String getComponent()
    {
        return component;
    }

    public void setComponent( String component )
    {
        this.component = component;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority( String priority )
    {
        this.priority = priority;
    }

    public String getReporter()
    {
        return reporter;
    }

    public void setReporter( String reporter )
    {
        this.reporter = reporter;
    }

    public TracTicket()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getMilestone()
    {
        return milestone;
    }

    public void setMilestone( String milestone )
    {
        this.milestone = milestone;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner( String owner )
    {
        this.owner = owner;
    }

    public String getResolution()
    {
        return resolution;
    }

    public void setResolution( String resolution )
    {
        this.resolution = resolution;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary( String summary )
    {
        this.summary = summary;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public Date getTimeChanged()
    {
        return timeChanged;
    }

    public void setTimeChanged( String timeChanged )
    {
        this.timeChanged = parseDate( timeChanged );
    }

    public Date getTimeCreated()
    {
        return timeCreated;
    }

    public void setTimeCreated( String timeCreated )
    {
        this.timeCreated = parseDate( timeCreated );
    }

    private Date parseDate( String timeCreated )
        throws RuntimeException
    {
        try
        {
            long millis = Long.parseLong( timeCreated );
            Calendar cld = Calendar.getInstance();
            cld.setTimeInMillis( millis * 1000L );
            return cld.getTime();
        }
        catch ( NumberFormatException e )
        {
            SimpleDateFormat format = new SimpleDateFormat( "EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH );
            try
            {
                return format.parse( timeCreated );
            }
            catch ( ParseException e1 )
            {
                throw new RuntimeException( "Failed to parse date '" + timeCreated + "' as a date.", e1 );
            }
        }
    }

    public String getLink()
    {
        return link;
    }

    public void setLink( String link )
    {
        this.link = link;
    }

}
