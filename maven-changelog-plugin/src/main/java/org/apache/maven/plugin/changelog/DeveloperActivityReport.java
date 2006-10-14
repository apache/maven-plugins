package org.apache.maven.plugin.changelog;

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

import org.apache.maven.model.Developer;
import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.command.changelog.ChangeLogSet;
import org.codehaus.doxia.sink.Sink;

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
 * @goal dev-activity
 */
public class DeveloperActivityReport
    extends ChangeLogReport
{
    /**
     * List of developers to be shown on the report.
     *
     * @parameter expression="${project.developers}"
     */
    private List developers;

    /**
     * Used to hold data while creating the report
     */
    private HashMap commits;

    private HashMap files;

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return "Generated developer activity report from SCM.";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "Developer Activity";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dev-activity";
    }

    /**
     * generates an empty report in case there are no sources to generate a report with
     *
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
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

    /**
     * method that generates the report for this mojo.
     *
     * @param changeLogSets changed sets to generate the report from
     * @param bundle        the resource bundle to retrieve report phrases from
     * @param sink          the report formatting tool
     */
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

        if( developers.isEmpty() ) {
            sink.paragraph();
            sink.text( bundle.getString( "report.dev-activity.noDevelopers" ) );
            sink.paragraph_();
        }
        else {
            for ( Iterator sets = changeLogSets.iterator(); sets.hasNext(); )
            {
                ChangeLogSet set = (ChangeLogSet) sets.next();
                doChangedSets( set, bundle, sink );
            }
        }

        sink.section1_();
        sink.body_();
        sink.flush();
        sink.table_();
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
        sink.sectionTitle2();
        if ( set.getStartDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeUnknown" ) );
        }
        else if ( set.getEndDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeSince" ) );
        }
        else
        {
            sink.text( " " + set.getStartDate() + " " + bundle.getString( "report.To" ) + " " + set.getEndDate() );
            sink.sectionTitle2_();
        }
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
     * generates the report summary section of the report
     *
     * @param set    changed set to generate the report from
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    private void doSummary( ChangeLogSet set, ResourceBundle bundle, Sink sink )
    {
        sink.paragraph();

        sink.text( bundle.getString( "report.dev-activity.range" ) );
        sink.text( ": " + set.getStartDate() + " " + bundle.getString( "report.To" ) + " " + set.getEndDate() );

        sink.text( ", " + bundle.getString( "report.TotalCommits" ) );
        sink.text( ":" + set.getChangeSets().size() );

        sink.text( ", " + bundle.getString( "report.dev-activity.filesChanged" ) );
        sink.text( ":" + countFilesChanged( set.getChangeSets() ) );

        sink.paragraph_();
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

        //for( Iterator i=commits.keySet().iterator(); i.hasNext(); )
        for ( Iterator i = developers.iterator(); i.hasNext(); )
        {
            Developer developer = (Developer) i.next();

            String name = developer.getName();

            String id = developer.getId();

            LinkedList devCommits;
            HashMap devFiles;

            if ( !commits.containsKey( name ) )
            {
                if ( !commits.containsKey( id ) )
                {
                    continue;
                }
                else
                {
                    devCommits = (LinkedList) commits.get( id );

                    devFiles = (HashMap) files.get( id );
                }
            }
            else
            {
                devCommits = (LinkedList) commits.get( name );

                devFiles = (HashMap) files.get( name );
            }

            sink.tableRow();

            sink.tableCell();
            sink.link( "team-list.html#" + developer.getId() );
            sink.text( name );
            sink.link_();
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
