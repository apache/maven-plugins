package org.codehaus.mojo.surefire;

/*
 * Copyright 2001-2005 The Codehaus.
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

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class SurefireReportGenerator
{
    private SurefireReportParser report;

    private List testSuites;

    private boolean showSuccess;

    public SurefireReportGenerator( File reportsDirectory, Locale locale, boolean showSuccess )
    {
        report = new SurefireReportParser( reportsDirectory, locale );

        this.showSuccess = showSuccess;
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink )
        throws MavenReportException
    {
        testSuites = report.parseXMLReportFiles();

        sink.head();

        sink.text( bundle.getString( "report.surefire.description" ) );

        sink.head_();

        sink.body();

        constructSummarySection( bundle, sink );

        Map suitePackages = report.getSuitesGroupByPackage( testSuites );
        if ( !suitePackages.isEmpty() )
        {
            constructPackagesSection( bundle, sink, suitePackages );
        }

        if ( !testSuites.isEmpty() )
        {
            constructTestCasesSection( bundle, sink );
        }

        List failureList = report.getFailureDetails( testSuites );
        if ( !failureList.isEmpty() )
        {
            constructFailureDetails( sink, bundle, failureList );
        }

        sinkLineBreak( sink );

        sink.body_();

        sink.flush();

        sink.close();
    }

    private void constructSummarySection( ResourceBundle bundle, Sink sink )
    {
        Map summary = report.getSummary( testSuites );

        sink.sectionTitle1();

        sinkAnchor( sink, "Summary" );

        sink.text( bundle.getString( "report.surefire.label.summary" ) );

        sink.sectionTitle1_();

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        sink.tableRow();

        sinkCell( sink, (String) summary.get( "totalTests" ) );

        sinkCell( sink, (String) summary.get( "totalErrors" ) );

        sinkCell( sink, (String) summary.get( "totalFailures" ) );

        sinkCell( sink, summary.get( "totalPercentage" ) + "%" );

        sinkCell( sink, (String) summary.get( "totalElapsedTime" ) );

        sink.tableRow_();

        sink.table_();

        sink.lineBreak();

        sink.rawText( bundle.getString( "report.surefire.text.note1" ) );

        sinkLineBreak( sink );
    }

    private void constructPackagesSection( ResourceBundle bundle, Sink sink, Map suitePackages )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.sectionTitle1();

        sinkAnchor( sink, "Package_List" );

        sink.text( bundle.getString( "report.surefire.label.packagelist" ) );

        sink.sectionTitle1_();

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.package" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        Iterator packIter = suitePackages.keySet().iterator();

        while ( packIter.hasNext() )
        {
            sink.tableRow();

            String packageName = (String) packIter.next();

            List testSuiteList = (List) suitePackages.get( packageName );

            Map packageSummary = report.getSummary( testSuiteList );

            sinkCellLink( sink, packageName, "#" + packageName );

            sinkCell( sink, (String) packageSummary.get( "totalTests" ) );

            sinkCell( sink, (String) packageSummary.get( "totalErrors" ) );

            sinkCell( sink, (String) packageSummary.get( "totalFailures" ) );

            sinkCell( sink, packageSummary.get( "totalPercentage" ) + "%" );

            sinkCell( sink, (String) packageSummary.get( "totalElapsedTime" ) );

            sink.tableRow_();
        }

        sink.table_();

        sink.lineBreak();

        sink.rawText( bundle.getString( "report.surefire.text.note2" ) );

        packIter = suitePackages.keySet().iterator();

        while ( packIter.hasNext() )
        {
            String packageName = (String) packIter.next();

            List testSuiteList = (List) suitePackages.get( packageName );

            Iterator suiteIterator = testSuiteList.iterator();

            sink.sectionTitle2();

            sinkAnchor( sink, packageName );

            sink.text( packageName );

            sink.sectionTitle2_();

            sink.table();

            sink.tableRow();

            sinkHeader( sink, "" );

            sinkHeader( sink, bundle.getString( "report.surefire.label.class" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

            sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

            sink.tableRow_();

            while ( suiteIterator.hasNext() )
            {
                ReportTestSuite suite = (ReportTestSuite) suiteIterator.next();

                if ( showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0 )
                {

                    sink.tableRow();

                    sink.tableCell();

                    sink.link( "#" + suite.getPackageName() + suite.getName() );

                    if ( suite.getNumberOfErrors() > 0 )
                    {
                        sinkIcon( "error", sink );
                    }
                    else if ( suite.getNumberOfFailures() > 0 )
                    {
                        sinkIcon( "junit.framework", sink );
                    }
                    else
                    {
                        sinkIcon( "success", sink );
                    }

                    sink.link_();

                    sink.tableCell_();

                    sinkCellLink( sink, suite.getName(), "#" + suite.getPackageName() + suite.getName() );

                    sinkCell( sink, Integer.toString( suite.getNumberOfTests() ) );

                    sinkCell( sink, Integer.toString( suite.getNumberOfErrors() ) );

                    sinkCell( sink, Integer.toString( suite.getNumberOfFailures() ) );

                    String percentage = report.computePercentage( suite.getNumberOfTests(), suite.getNumberOfErrors(),
                                                                  suite.getNumberOfFailures() );
                    sinkCell( sink, percentage + "%" );

                    sinkCell( sink, numberFormat.format( suite.getTimeElapsed() ) );

                    sink.tableRow_();
                }
            }

            sink.table_();
        }

        sinkLineBreak( sink );
    }

    private void constructTestCasesSection( ResourceBundle bundle, Sink sink )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.sectionTitle1();

        sinkAnchor( sink, "Test_Cases" );

        sink.text( bundle.getString( "report.surefire.label.testcases" ) );

        sink.sectionTitle1_();

        constructHotLinks( sink, bundle );

        ListIterator suiteIterator = testSuites.listIterator();

        while ( suiteIterator.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) suiteIterator.next();

            List testCases = suite.getTestCases();

            if ( testCases != null )
            {
                ListIterator caseIterator = testCases.listIterator();

                sink.sectionTitle2();

                sinkAnchor( sink, suite.getPackageName() + suite.getName() );

                sink.text( suite.getName() );

                sink.sectionTitle2_();

                sink.table();

                while ( caseIterator.hasNext() )
                {
                    ReportTestCase testCase = (ReportTestCase) caseIterator.next();

                    if ( testCase.getFailure() != null || showSuccess )
                    {
                        sink.tableRow();

                        sink.tableCell();

                        Map failure = testCase.getFailure();

                        if ( failure != null )
                        {
                            sink.link( "#" + testCase.getFullName() );

                            sinkIcon( (String) failure.get( "type" ), sink );

                            sink.link_();
                        }
                        else
                        {
                            sinkIcon( "success", sink );
                        }

                        sink.tableCell_();

                        if ( failure != null )
                        {
                            sink.tableCell();

                            sinkLink( sink, testCase.getName(), "#" + testCase.getFullName() );

                            sink.tableCell_();
                        }
                        else
                        {
                            sinkCell( sink, testCase.getName() );
                        }

                        sinkCell( sink, numberFormat.format( testCase.getTime() ) );

                        sink.tableRow_();
                    }
                }

                sink.table_();
            }
        }

        sinkLineBreak( sink );
    }

    private void constructFailureDetails( Sink sink, ResourceBundle bundle, List failureList )
    {
        Iterator failIter = failureList.iterator();

        if ( failIter != null )
        {
            sink.sectionTitle1();

            sinkAnchor( sink, "Failure_Details" );

            sink.text( bundle.getString( "report.surefire.label.failuredetails" ) );

            sink.sectionTitle1_();

            constructHotLinks( sink, bundle );

            sinkLineBreak( sink );

            sink.table();

            while ( failIter.hasNext() )
            {
                ReportTestCase tCase = (ReportTestCase) failIter.next();

                Map failure = tCase.getFailure();

                sink.tableRow();

                sink.tableCell();

                String type = (String) failure.get( "type" );
                sinkIcon( type, sink );

                sink.tableCell_();

                sinkCellAnchor( sink, tCase.getName(), tCase.getFullName() );

                sink.tableRow_();

                String message = (String) failure.get( "message" );

                sink.tableRow();

                sinkCell( sink, "" );

                StringBuffer sb = new StringBuffer();
                sb.append( type );

                if ( message != null )
                {
                    sb.append( ": " );
                    sb.append( message );
                }

                sinkCell( sink, sb.toString() );

                sink.tableRow_();

                if ( !type.startsWith( "junit.framework" ) )
                {
                    List detail = (List) failure.get( "detail" );
                    if ( detail != null )
                    {
                        Iterator it = detail.iterator();
                        sink.tableRow();
                        sinkCell( sink, "" );
                        sink.tableCell();
                        sink.verbatim( true );
                        boolean firstLine = true;
                        while ( it.hasNext() )
                        {
                            if ( firstLine )
                            {
                                firstLine = false;
                            }
                            else
                            {
                                sink.text( "    " );
                            }
                            sink.text( it.next().toString() + "\n" );
                        }
                        sink.verbatim_();
                        sink.tableCell_();
                        sink.tableRow_();
                    }
                }

            }

            sink.table_();
        }

        sinkLineBreak( sink );
    }

    private void constructHotLinks( Sink sink, ResourceBundle bundle )
    {
        if ( !testSuites.isEmpty() )
        {
            sink.section2();

            sink.rawText( "[" );
            sinkLink( sink, bundle.getString( "report.surefire.label.summary" ), "#Summary" );
            sink.rawText( "]" );

            sink.rawText( "[" );
            sinkLink( sink, bundle.getString( "report.surefire.label.packagelist" ), "#Package_List" );
            sink.rawText( "]" );

            sink.rawText( "[" );
            sinkLink( sink, bundle.getString( "report.surefire.label.testcases" ), "#Test_Cases" );
            sink.rawText( "]" );
            sink.section2_();
        }
    }

    private void sinkLineBreak( Sink sink )
    {
        sink.table();
        sink.tableRow();
        sink.tableRow_();
        sink.tableRow();
        sink.tableRow_();
        sink.table_();
    }

    private void sinkIcon( String type, Sink sink )
    {
        sink.figure();

        if ( type.startsWith( "junit.framework" ) )
        {
            sink.figureGraphics( "images/icon_warning_sml.gif" );
        }
        else if ( type.startsWith( "success" ) )
        {
            sink.figureGraphics( "images/icon_success_sml.gif" );
        }
        else
        {
            sink.figureGraphics( "images/icon_error_sml.gif" );
        }

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();
        sink.text( header );
        sink.tableHeaderCell_();
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );
        sink.text( text );
        sink.link_();
    }

    private void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();
        sinkLink( sink, text, link );
        sink.tableCell_();
    }

    private void sinkCellAnchor( Sink sink, String text, String anchor )
    {
        sink.tableCell();
        sinkAnchor( sink, anchor );
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkAnchor( Sink sink, String anchor )
    {
        sink.anchor( anchor );
        sink.anchor_();
    }
}
