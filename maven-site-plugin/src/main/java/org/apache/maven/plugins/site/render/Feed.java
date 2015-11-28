package org.apache.maven.plugins.site.render;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Rss feed.
 * 
 * @author Petar
 */
public class Feed
{
    private final String feedType;

    private final MavenProject mavenProject;

    public Feed( final MavenProject mavenProject, final String feedType )
    {
        this.feedType = feedType;
        this.mavenProject = mavenProject;
    }

    /**
     * Generates the feed of the given feedType inside the specified targetDir.
     * 
     * @param context
     * @param targetDir
     * @throws IOException
     */
    public void generate( final SiteRenderingContext context,
                          final File targetDir )
        throws IOException
    {
        final File outputDir = new File( targetDir, "feeds" );

        final SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType( feedType );
        feed.setTitle( context.getDefaultWindowTitle() );
        feed.setLink( this.mavenProject.getUrl() );
        feed.setDescription( this.mavenProject.getDescription() );

        final List<SyndEntry> entries = new ArrayList<SyndEntry>();

        final List<Menu> menus = context.getDecoration().getMenus();
        if ( menus != null )
        {
            for ( final Menu menu : menus )
            {
                for ( final MenuItem mi : menu.getItems() )
                {
                    entries.addAll( createEntries( mi ) );
                }
            }
        }

        feed.setEntries( entries );

        // Safety
        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        final Writer writer =
            new FileWriter( new File( outputDir, mavenProject.getArtifactId()
                + ".xml" ) );
        final SyndFeedOutput output = new SyndFeedOutput();
        try
        {
            output.output( feed, writer );
        }
        catch ( final FeedException e )
        {
            e.printStackTrace();
        }
        finally
        {
            writer.close();
        }

    }

    private Collection<SyndEntry> createEntries( final MenuItem menu )
    {
        final List<SyndEntry> entries = new ArrayList<SyndEntry>();

        final SyndEntry entry = new SyndEntryImpl();

        entry.setTitle( menu.getName() );
        entry.setLink( this.mavenProject.getUrl() + "/" + menu.getHref() );
        entry.setAuthors( getAuthors() );
        entry.setContributors( getContributors() );

        entry.setPublishedDate( new Date() );

        final SyndContentImpl description = new SyndContentImpl();
        description.setType( "text/plain" );
        description.setValue( menu.getAlt() );
        entry.setDescription( description );

        entries.add( entry );

        if ( menu.getItems() != null )
        {
            for ( final MenuItem mi : menu.getItems() )
            {
                entries.addAll( createEntries( mi ) );
            }
        }
        return entries;
    }

    private List<SyndPerson> getAuthors()
    {
        final List<SyndPerson> authors = new ArrayList<SyndPerson>();
        if ( this.mavenProject.getDevelopers() != null )
        {
            SyndPerson author;
            for ( final Developer developer : this.mavenProject.getDevelopers() )
            {
                author = new SyndPersonImpl();
                author.setEmail( developer.getEmail() );
                author.setName( developer.getName() );
                author.setUri( developer.getUrl() );
                authors.add( author );
            }
        }
        return authors;
    }

    private List<SyndPerson> getContributors()
    {
        final List<SyndPerson> contributors = new ArrayList<SyndPerson>();
        if ( this.mavenProject.getContributors() != null )
        {
            SyndPerson author;
            for ( final Contributor contributor : this.mavenProject.getContributors() )
            {
                author = new SyndPersonImpl();
                author.setEmail( contributor.getEmail() );
                author.setName( contributor.getName() );
                author.setUri( contributor.getUrl() );
                contributors.add( author );
            }
        }
        return contributors;
    }
}
