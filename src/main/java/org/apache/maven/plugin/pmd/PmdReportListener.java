package org.apache.maven.plugin.pmd;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import net.sourceforge.pmd.ReportListener;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.stat.Metric;

import org.apache.maven.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handle events from PMD, converting them into Doxia events.
 *
 * @author Brett Porter
 * @version $Id$
 */
public class PmdReportListener
    implements ReportListener
{
    private Sink sink;

    private String currentFilename;

    private ResourceBundle bundle;

    private PmdFileInfo fileInfo;

    private List<RuleViolation> violations = new ArrayList<RuleViolation>();

    private boolean aggregate;

    // The number of erroneous files
    private int fileCount = 0;

    private Map<File, PmdFileInfo> files;

//    private List<Metric> metrics = new ArrayList<Metric>();

    public PmdReportListener( Sink sink, ResourceBundle bundle, boolean aggregate )
    {
        this.sink = sink;
        this.bundle = bundle;
        this.aggregate = aggregate;
    }

    private String getTitle()
    {
        return bundle.getString( "report.pmd.title" );
    }

    /** {@inheritDoc} */
    public void ruleViolationAdded( RuleViolation ruleViolation )
    {
        violations.add( ruleViolation );
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }

    private void startFileSection(String currentFilename, PmdFileInfo fileInfo) {
        sink.section2();
        sink.sectionTitle2();

        // prepare the filename
        this.currentFilename = StringUtils.substring( currentFilename,
                                          fileInfo.getSourceDirectory().getAbsolutePath().length() + 1 );
        this.currentFilename = StringUtils.replace( this.currentFilename, "\\", "/" );

        String title = this.currentFilename;
        if (aggregate)
        {
            title = fileInfo.getProject().getName() + " - " + currentFilename;
        }
        sink.text( title );
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
    }

    private void endFileSection() {
        sink.table_();
        sink.section2_();
    }

    private void processSingleRuleViolation(RuleViolation ruleViolation) {
        sink.tableRow();
        sink.tableCell();
        sink.text( ruleViolation.getDescription() );
        sink.tableCell_();
        sink.tableCell();

        int beginLine = ruleViolation.getBeginLine();
        outputLineLink( beginLine );
        int endLine = ruleViolation.getEndLine();
        if (endLine != beginLine) {
            sink.text( " - " );
            outputLineLink( endLine );
        }

        sink.tableCell_();
        sink.tableRow_();
    }

    // PMD might run the analysis multi-threaded, so the violations might be reported
    // out of order. We sort them here by filename and line number before writing them to
    // the report.
    private void processViolations()
    {
        fileCount = files.size();
        Collections.sort( violations, new Comparator<RuleViolation>() {
            /** {@inheritDoc} */
            public int compare(RuleViolation o1, RuleViolation o2) {
                int filenames = o1.getFilename().compareTo( o2.getFilename() );
                if (filenames == 0) {
                    return o1.getBeginLine() - o2.getBeginLine();
                }
                else {
                    return filenames;
                }
            }
        } );

        Map<String, PmdFileInfo> fileLookup = new HashMap<String, PmdFileInfo>( fileCount );
        for ( Map.Entry<File, PmdFileInfo> entry : files.entrySet() )
        {
            fileLookup.put( entry.getKey().getAbsolutePath(), entry.getValue() );
        }

        boolean fileSectionStarted = false;
        String previousFilename = null;
        for ( RuleViolation ruleViolation : violations )
        {
            String currentFn = ruleViolation.getFilename();
            if ( !currentFn.equalsIgnoreCase( previousFilename ) && fileSectionStarted )
            {
                endFileSection();
                fileSectionStarted = false;
            }
            if ( !fileSectionStarted )
            {
                fileInfo = fileLookup.get( currentFn );
                startFileSection( currentFn, fileInfo );
                fileSectionStarted = true;
            }

            processSingleRuleViolation( ruleViolation );

            previousFilename = currentFn;
        }

        if ( fileSectionStarted )
        {
            endFileSection();
        }
    }

    private void outputLineLink( int line )
    {
        String xrefLocation = fileInfo.getXrefLocation();
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

    /** {@inheritDoc} */
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
        sink.text( " " + AbstractPmdReport.getPmdVersion() + "." );
        sink.paragraph_();

        sink.section1_();

        // TODO overall summary

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.pmd.files" ) );
        sink.sectionTitle1_();

        // TODO files summary
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

        for ( Metric met : metrics )
        {
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
        processViolations();

        if ( fileCount == 0 )
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.pmd.noProblems" ) );
            sink.paragraph_();
        }

        sink.section1_();

        // The Metrics report useless with the current PMD metrics impl.
        // For instance, run the coupling ruleset and you will get a boatload
        // of excessive imports metrics, none of which is really any use.
        // TODO Determine if we are going to just ignore metrics.

//        processMetrics();

        sink.body_();

        sink.flush();

        sink.close();
    }

    public void setFiles(Map<File, PmdFileInfo> files) {
        this.files = files;
    }
}