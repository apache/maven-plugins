package org.apache.maven.plugins.surefire.report;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Locale;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

public class SurefireReportParser
{
    private NumberFormat numberFormat = NumberFormat.getInstance();

    private File reportsDirectory;

    private List testSuites = new ArrayList();
    
    private Locale locale;

    public SurefireReportParser()
    {
    }

    public SurefireReportParser( File reportsDirectory, Locale locale )
    {
        setReportsDirectory(reportsDirectory);
        
        setLocale(locale);
    }

    public List parseXMLReportFiles()
    {
        ReportTestSuite testSuite;

        if ( !reportsDirectory.exists() )
        {
            return testSuites;
        }

        String[] xmlReportFiles = getIncludedFiles( reportsDirectory, "*.xml", "*.txt" );

        for ( int index = 0; index < xmlReportFiles.length; index++ )
        {
            testSuite = new ReportTestSuite( reportsDirectory + "/" + xmlReportFiles[index] );

            testSuites.add( testSuite );
        }

        return testSuites;
    }

    protected String parseTestSuiteName( String lineString )
    {
        return lineString.substring( lineString.lastIndexOf( "." ) + 1, lineString.length() );
    }

    protected String parseTestSuitePackageName( String lineString )
    {
        return lineString.substring( lineString.indexOf( ":" ) + 2, lineString.lastIndexOf( "." ) );
    }

    protected String parseTestCaseName( String lineString )
    {
        return lineString.substring( 0, lineString.indexOf( "(" ) );
    }

    public Map getSummary( List suites )
    {
        Map totalSummary = new HashMap();

        ListIterator iter = suites.listIterator();

        int totalNumberOfTests = 0;

        int totalNumberOfErrors = 0;

        int totalNumberOfFailures = 0;

        String totalPercentage = "";

        float totalElapsedTime = 0.0f;

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            totalNumberOfTests += suite.getNumberOfTests();

            totalNumberOfErrors += suite.getNumberOfErrors();

            totalNumberOfFailures += suite.getNumberOfFailures();

            totalElapsedTime += suite.getTimeElapsed();
        }

        totalPercentage = computePercentage( totalNumberOfTests, totalNumberOfErrors, totalNumberOfFailures );

        totalSummary.put( "totalTests", Integer.toString( totalNumberOfTests ) );

        totalSummary.put( "totalErrors", Integer.toString( totalNumberOfErrors ) );

        totalSummary.put( "totalFailures", Integer.toString( totalNumberOfFailures ) );

        totalSummary.put( "totalElapsedTime", numberFormat.format( totalElapsedTime ) );

        totalSummary.put( "totalPercentage", totalPercentage );

        return totalSummary;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public File getReportsDirectory()
    {
        return this.reportsDirectory;
    }
    
    public void setLocale( Locale locale )
    {
        this.locale = locale;
        setNumberFormat( NumberFormat.getInstance( locale ) );
    }

    public Locale getLocale()
    {
        return this.locale;
    }
    
    public void setNumberFormat( NumberFormat numberFormat )
    {
        this.numberFormat = numberFormat;
    }
    
    public NumberFormat getNumberFormat()
    {
    	return this.numberFormat;
    }

    public HashMap getSuitesGroupByPackage( List testSuitesList )
    {
        ListIterator iter = testSuitesList.listIterator();

        HashMap suitePackage = new HashMap();

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            List suiteList = new ArrayList();

            if ( (List) suitePackage.get( suite.getPackageName() ) != null )
            {
                suiteList = (List) suitePackage.get( suite.getPackageName() );
            }

            suiteList.add( suite );

            suitePackage.put( suite.getPackageName(), suiteList );
        }

        return suitePackage;
    }

    public String computePercentage( int tests, int errors, int failures )
    {
        if ( tests == 0 )
        {
            return numberFormat.format( 0.00f );
        }

        float percentage = ( (float) ( tests - errors - failures ) / (float) tests ) * 100;

        return numberFormat.format( percentage );
    }

    public List getFailureDetails( List testSuitesList )
    {
        ListIterator iter = testSuitesList.listIterator();

        List failureDetailList = new ArrayList();

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            List testCaseList = suite.getTestCases();
            
            if ( testCaseList == null )
            {
                continue;
            }

            ListIterator caseIter = testCaseList.listIterator();

            while ( caseIter.hasNext() )
            {
                ReportTestCase tCase = (ReportTestCase) caseIter.next();

                if ( tCase.getFailure() != null )
                {
                    failureDetailList.add( tCase );
                }
            }
        }

        return failureDetailList;
    }

    private String[] getIncludedFiles( File directory, String includes, String excludes )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( directory );

        scanner.setIncludes( StringUtils.split( includes, "," ) );

        scanner.setExcludes( StringUtils.split( excludes, "," ) );

        scanner.scan();

        String[] filesToFormat = scanner.getIncludedFiles();

        return filesToFormat;
    }
}
