package org.apache.maven.plugin.pmd;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.ReportListener;
import net.sourceforge.pmd.stat.Metric;

import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handle events from PMD, converting them into Doxia events.
 *
 * @author Brett Porter
 * @version $Id: PmdReportListener.java,v 1.1.1.1 2005/02/17 07:16:22 brett Exp $
 */
public class PmdReportListener
    implements ReportListener
{
    private Sink sink;

    private String sourceDirectory;

    private String currentFilename;

    private boolean fileInitialized;

    private ResourceBundle bundle;

    private String xrefLocation;

    private List violations = new ArrayList();

    // The number of erroneous files
    private int fileCount = 0;

    //private List metrics = new ArrayList();

    public PmdReportListener( Sink sink, String sourceDirectory, ResourceBundle bundle )
    {
        this.sink = sink;
        this.sourceDirectory = sourceDirectory;
        this.bundle = bundle;
    }

    private String getTitle()
    {
        return bundle.getString( "report.pmd.title" );
    }

    public void ruleViolationAdded( IRuleViolation ruleViolation )
    {
        if ( !fileInitialized )
        {
            sink.section2();
            sink.sectionTitle2();
            sink.text( currentFilename );
            sink.sectionTitle2_();

            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.pmd.column.violation" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.pmd.column.line" ) );
            sink.tableHeaderCell_();
            sink.tableRow_();

            fileInitialized = true;
        }
        violations.add( ruleViolation );
    }

    // When dealing with multiple rulesets, the violations will get out of order
    // wrt their source line number.  We re-sort them before writing them to the report.
    private void processViolations()
    {
        fileCount++;
        Collections.sort( violations, new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (IRuleViolation) o1 ).getBeginLine() -
                    ( (IRuleViolation) o2 ).getBeginLine();
            }
        } );

        for ( Iterator it = violations.iterator(); it.hasNext(); )
        {
            IRuleViolation ruleViolation = (IRuleViolation) it.next();

            sink.tableRow();
            sink.tableCell();
            sink.text( ruleViolation.getDescription() );
            sink.tableCell_();
            sink.tableCell();

            int beginLine = ruleViolation.getBeginLine();
            outputLineLink( beginLine );
            int endLine = ruleViolation.getEndLine();
            if ( endLine != beginLine )
            {
                sink.text( " - " );
                outputLineLink( endLine );
            }

            sink.tableCell_();
            sink.tableRow_();
        }
        violations.clear();
    }

    private void outputLineLink( int line )
    {
        if ( xrefLocation != null )
        {
            sink.link( xrefLocation + "/" + currentFilename.replaceAll( "\\.java$", ".html" ) + "#" + line );
        }
        sink.text( String.valueOf( line ) );
        if ( xrefLocation != null )
        {
            sink.link_();
        }
    }

    public void metricAdded( Metric metric )
    {
//        if ( metric.getCount() != 0 )
//        {
//            // Skip metrics which have no data
//            metrics.add( metric );
//        }
    }

    public void beginDocument()
    {
        sink.head();
        sink.title();
        sink.text( getTitle() );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( getTitle() );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( bundle.getString( "report.pmd.pmdlink" ) + " " );
        sink.link( "http://pmd.sourceforge.net/" );
        sink.text( "PMD" );
        sink.link_();
        sink.text( " " + PMD.VERSION + "." );
        sink.paragraph_();

        // TODO overall summary

        sink.section1_();
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.pmd.files" ) );
        sink.sectionTitle1_();

        // TODO files summary
    }

    public void beginFile( File file )
    {
        currentFilename = StringUtils.substring( file.getAbsolutePath(), sourceDirectory.length() + 1 );
        currentFilename = StringUtils.replace( currentFilename, "\\", "/" );
        fileInitialized = false;
    }

    public void endFile( File file )
    {
        if ( fileInitialized )
        {
            processViolations();
            sink.table_();
            sink.section2_();
        }
    }

    /*
    private void processMetrics()
    {
        if ( metrics.size() == 0 )
        {
            return;
        }

        sink.section1();
        sink.sectionTitle1();
        sink.text( "Metrics" );
        sink.sectionTitle1_();

        sink.table();
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( "Name" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Count" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "High" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Low" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Average" );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( Iterator iter = metrics.iterator(); iter.hasNext(); )
        {
            Metric met = (Metric) iter.next();
            sink.tableRow();
            sink.tableCell();
            sink.text( met.getMetricName() );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( met.getCount() ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( met.getHighValue() ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( met.getLowValue() ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( met.getAverage() ) );
            sink.tableCell_();
            sink.tableRow_();
        }
        sink.table_();
        sink.section1_();
    }
    */

    public void endDocument()
    {
        if ( fileCount == 0 )
        {
            sink.text( "PMD found no problems in your source code." );
        }

        sink.section1_();

        // The Metrics report useless with the current PMD metrics impl.
        // For instance, run the coupling ruleset and you will get a boatload
        // of excessive imports metrics, none of which is really any use.
        // TODO Determine if we are going to just ignore metrics.

        // processMetrics();

        sink.body_();

        sink.flush();

        sink.close();
    }

    public String getXrefLocation()
    {
        return xrefLocation;
    }

    public void setXrefLocation( String xrefLocation )
    {
        this.xrefLocation = xrefLocation;
    }
}
