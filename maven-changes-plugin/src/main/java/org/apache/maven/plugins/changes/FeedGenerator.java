package org.apache.maven.plugins.changes;

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

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import java.io.IOException;
import java.io.Writer;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.util.HtmlTools;

import org.apache.maven.plugins.changes.model.Release;

/**
 * @author ltheussl
 */
public class FeedGenerator
{
    private final ResourceBundle rbundle;

    private final SyndFeed feed;

    private String link;

    private String title;

    private String author;

    private DateFormat dateFormat;

    /**
     * Initialize feedGenerator for a given locale.
     *
     * @param locale a locale for i18n.
     */
    public FeedGenerator( final Locale locale )
    {
        this.feed = new SyndFeedImpl();
        this.rbundle = ResourceBundle.getBundle( "changes-report", locale, this.getClass().getClassLoader() );
    }

    /**
     * The author of the feed.
     *
     * @return the author.
     */
    public String getAuthor()
    {
        return author;
    }

    /**
     * Set the author of the feed.
     *
     * @param author not null.
     */
    public void setAuthor( final String author )
    {
        this.author = author.trim(); // this also assures that author is not null.
    }

    /**
     * The title of the feed.
     *
     * @return the title.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the title of the feed.
     *
     * @param title not null.
     */
    public void setTitle( final String title )
    {
        this.title = title.trim(); // this also assures that title is not null.
    }

    /**
     * The DateFormat.
     *
     * @return may be null.
     */
    public DateFormat getDateFormat()
    {
        return dateFormat;
    }

    /**
     * Set the date format. This should match the date format used for the release dates in changes.xml.
     *
     * @param dateFormat may be null.
     */
    public void setDateFormat( final DateFormat dateFormat )
    {
        this.dateFormat = dateFormat;
    }

    /**
     * The main link of the feed.
     *
     * @return the link.
     */
    public String getLink()
    {
        return link;
    }

    /**
     * Set the main link of the feed.
     *
     * @param link not null.
     */
    public void setLink( final String link )
    {
        this.link = link.trim(); // this also assures that link is not null.
    }

    /**
     * Determine if a given feed type is supported. The currently supported values are:
     * <code>"rss_0.9", "rss_0.91N" (RSS 0.91 Netscape), "rss_0.91U" (RSS 0.91 Userland),
     * "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3", "atom_1.0"</code>.
     *
     * @param type the feed type to check. May be null.
     * @return true if if the given type is supported by the rome library, false otherwise.
     */
    public boolean isSupportedFeedType( final String type )
    {
        return getSupportedFeedTypes().contains( type );
    }

    /**
     * A List of supported feed types.
     *
     * @return a List of supported feed types.
     * @see #isSupportedFeedType(java.lang.String)
     */
    @SuppressWarnings( "unchecked" )
    public List<String> getSupportedFeedTypes()
    {
        return feed.getSupportedFeedTypes();
    }

    /**
     * Extract a feed and export it to a Writer.
     *
     * @param releases the List of Releases. Only the last release is used in the feed.
     * @param feedType The type of the feed to generate. See {@link #isSupportedFeedType(java.lang.String)} for
     *            supported values.
     * @param writer a Writer. Note that this is not flushed nor closed upon exit.
     * @throws IOException if an error occurs during export.
     */
    public void export( final List<Release> releases, final String feedType, final Writer writer )
        throws IOException
    {
        feed.setFeedType( feedType );
        feed.setTitle( title );
        feed.setAuthor( author );
        feed.setPublishedDate( new Date() );
        feed.setLink( link );
        feed.setDescription( rbundle.getString( "report.changes.text.rssfeed.description" ) );
        feed.setLanguage( rbundle.getLocale().getLanguage() );
        // feed.setCopyright( );
        // feed.setEncoding();
        feed.setEntries( getEntries( releases ) );

        try
        {
            new SyndFeedOutput().output( feed, writer );
        }
        catch ( FeedException ex )
        {
            IOException ioex = new IOException( ex.getMessage() );
            ioex.initCause( ex );
            throw ioex;
        }
    }

    private List<SyndEntry> getEntries( final List<Release> releases )
    {
        final List<SyndEntry> entries = new ArrayList<SyndEntry>( 1 );

        if ( releases.size() > 0 )
        {
            final Release release = releases.get( 0 ); // TODO: is this guaranteed to be the latest?

            final SyndEntry entry = new SyndEntryImpl();
            entry.setTitle( release.getVersion() );
            entry.setLink( link + "#" + HtmlTools.encodeId( release.getVersion() ) );
            entry.setDescription( getSyndContent( release ) );
            entry.setPublishedDate( getDate( release.getDateRelease(), dateFormat ) );

            entries.add( entry );
        }

        return entries;
    }

    private static SyndContent getSyndContent( final Release release )
    {
        final SyndContent syndContent = new SyndContentImpl();
        syndContent.setType( "text/html" );

        final StringBuilder sb = new StringBuilder( 512 );

        final String description = release.getDescription();

        if ( description != null && description.trim().length() > 0 )
        {
            sb.append( "<p>" ).append( description ).append( "</p>" );
        }

        // TODO: localize?
        sb.append( "<p>Version " ).append( release.getVersion() ).append( " is available with " );
        sb.append( release.getActions().size() ).append( " fixed issues.</p>" );

        syndContent.setValue( sb.toString() );

        return syndContent;
    }

    private static Date getDate( final String dateRelease, final DateFormat dateFormat )
    {
        if ( dateFormat == null )
        {
            return new Date();
        }

        try
        {
            return dateFormat.parse( dateRelease );
        }
        catch ( ParseException ex )
        {
            return new Date();
        }
    }
}