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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.command.changelog.ChangeLogSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generate a developer activity report.
 *
 * @version $Id$
 * @goal dev-activity
 */
public class DeveloperActivityReport
    extends ChangeLogReport
{
    /**
     * Used to hold data while creating the report
     */
    private HashMap commits;

    private HashMap files;

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.dev-activity.description" );
    }

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.dev-activity.name" );
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "dev-activity";
    }

    /** {@inheritDoc} */
    protected void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sink.head();
        sink.title();
        sink.text( bundle.getString( "report.dev-activity.header" ) );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();

        sink.sectionTitle1();
        sink.text( bundle.getString( "report.dev-activity.mainTitle" ) );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( "No sources found to create a report." );
        sink.paragraph_();

        sink.section1_();

        sink.body_();
        sink.flush();
        sink.close();
    }

    /** {@inheritDoc} */
    protected void doGenerateReport( List changeLogSets, ResourceBundle bundle, Sink sink )
    {
        sink.head();
        sink.title();
        sink.text( bundle.getString( "report.dev-activity.header" ) );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.dev-activity.mainTitle" ) );
        sink.sectionTitle1_();

        if ( developers.isEmpty() )
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.dev-activity.noDevelopers" ) );
            sink.paragraph_();
        }
        else
        {
            for ( Iterator sets = changeLogSets.iterator(); sets.hasNext(); )
            {
                ChangeLogSet set = (ChangeLogSet) sets.next();
                doChangedSets( set, bundle, sink );
            }
        }

        sink.section1_();
        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * generates a section of the report referring to a changeset
     *
     * @param set    the current ChangeSet to generate this section of the report
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    private void doChangedSets( ChangeLogSet set, ResourceBundle bundle, Sink sink )
    {
        sink.section2();

        doChangeSetTitle( set, bundle, sink );

        doSummary( set, bundle, sink );

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.dev-activity.developer" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.TotalCommits" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.dev-activity.filesChanged" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        doDeveloperRows( set, sink );

        sink.table_();

        sink.section2_();
    }

    /**
     * generates the report section table of the developers
     *
     * @param set  change log set generate the developer activity
     * @param sink the report formatting tool
     */
    private void doDeveloperRows( ChangeLogSet set, Sink sink )
    {
        initDeveloperDetails( set );

        for( Iterator i = commits.keySet().iterator(); i.hasNext(); )
        {
            String author = (String) i.next();

            LinkedList devCommits = (LinkedList) commits.get( author );
            HashMap devFiles = (HashMap) files.get( author );

            sink.tableRow();
            sink.tableCell();

            sinkAuthorDetails( sink, author );

            sink.tableCell_();

            sink.tableCell();
            sink.text( "" + devCommits.size() );
            sink.tableCell_();

            sink.tableCell();
            sink.text( "" + devFiles.size() );
            sink.tableCell_();

            sink.tableRow_();
        }
    }

    /**
     * counts the number of commits and files changed for each developer
     *
     * @param set the change log set to generate the developer details from
     */
    private void initDeveloperDetails( ChangeLogSet set )
    {
        commits = new HashMap();

        files = new HashMap();

        countDevCommits( set.getChangeSets() );

        countDevFiles( set.getChangeSets() );
    }

    /**
     * counts the number of commits of each developer
     *
     * @param entries the change log entries used to search and count developer commits
     */
    private void countDevCommits( Collection entries )
    {
        for ( Iterator i = entries.iterator(); i.hasNext(); )
        {
            ChangeSet entry = (ChangeSet) i.next();

            String developer = entry.getAuthor();

            LinkedList list;

            if ( commits.containsKey( developer ) )
            {
                list = (LinkedList) commits.get( developer );
            }
            else
            {
                list = new LinkedList();
            }

            list.add( entry );

            commits.put( developer, list );
        }
    }

    /**
     * counts the number of files changed by each developer
     *
     * @param entries the change log entries used to search and count file changes
     */
    private void countDevFiles( Collection entries )
    {
        for ( Iterator i2 = entries.iterator(); i2.hasNext(); )
        {
            ChangeSet entry = (ChangeSet) i2.next();

            String developer = entry.getAuthor();

            HashMap filesMap;

            if ( files.containsKey( developer ) )
            {
                filesMap = (HashMap) files.get( developer );
            }
            else
            {
                filesMap = new HashMap();
            }

            for ( Iterator i3 = entry.getFiles().iterator(); i3.hasNext(); )
            {
                ChangeFile file = (ChangeFile) i3.next();

                filesMap.put( file.getName(), file );
            }

            files.put( developer, filesMap );
        }
    }
}
