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
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ReportTestSuite
    extends DefaultHandler
{
    private List testCases;
    private int numberOfErrors;
    private int numberOfFailures;
    private int numberOfTests;
    private String name;
    private String fullClassName;
    private String packageName;
    private float timeElapsed;
    private NumberFormat numberFormat;
    private StringBuffer currentElement;
    String currentName;
    ReportTestCase testCase;

    public ReportTestSuite(  )
    {
    }

    public ReportTestSuite( String xmlPath )
    {
        numberFormat = NumberFormat.getInstance();

        SAXParserFactory factory = SAXParserFactory.newInstance(  );
 
        try
        {
            SAXParser saxParser = factory.newSAXParser(  );

            saxParser.parse( new File( xmlPath ),
                             this );
        } catch ( Throwable t )
        {
            // TODO should this break the build?
            Exception e = new Exception( "Failure to parse file at: " + new File( xmlPath ), t );
            
            e.printStackTrace(  );
        }
    }

    public void startElement( String namespaceURI, String sName, String qName, Attributes attrs )
                      throws SAXException
    {
        try
        {
            if ( qName.equals( "testsuite" ) )
            {
                numberOfErrors = Integer.parseInt( attrs.getValue( "errors" ) );
    
                numberOfFailures = Integer.parseInt( attrs.getValue( "failures" ) );
    
                numberOfTests = Integer.parseInt( attrs.getValue( "tests" ) );
    
                Number time = numberFormat.parse( attrs.getValue( "time" ) );
                
                timeElapsed = time.floatValue();

                //check if group attribute is existing
                if( attrs.getValue( "group" ) != null && !attrs.getValue( "group" ).equals( "" ) )
                {
                    packageName = attrs.getValue( "group" );

                    name = attrs.getValue( "name" );

                    fullClassName = packageName + "." + name;
                }
                else
                {
                    fullClassName = attrs.getValue( "name" );

                    name = fullClassName.substring( fullClassName.lastIndexOf( "." ) + 1,
                                            fullClassName.length(  ) );

                    int lastDotPosition = fullClassName.lastIndexOf( "." );
                    if ( lastDotPosition < 0 )
                    {
                        /* no package name */
                        packageName = "";
                    }
                    else
                    {
                        packageName = fullClassName.substring( 0, lastDotPosition );
                    }
                }

                testCases = new ArrayList(  );
            } else if ( qName.equals( "testcase" ) )
            {
                currentElement = new StringBuffer(  );
    
                testCase = new ReportTestCase(  );

                testCase.setFullClassName( fullClassName );

                testCase.setName( attrs.getValue( "name" ) );

                testCase.setClassName( name );
    
                Number time = numberFormat.parse( attrs.getValue( "time" ) );
                
                testCase.setTime( time.floatValue() );
    
                testCase.setFullName( packageName + "." + name + "." + testCase.getName(  ) );
            } else if ( qName.equals( "failure" ) )
            {
                HashMap failure = new HashMap(  );
    
                testCase.setFailure( failure );
    
                failure.put( "message",
                             attrs.getValue( "message" ) );
    
                failure.put( "type",
                             attrs.getValue( "type" ) );
            } else if ( qName.equals( "error" ) )
            {
                HashMap error = new HashMap(  );
    
                testCase.setFailure( error );

                error.put( "message",
                           attrs.getValue( "message" ) );
    
                error.put( "type",
                           attrs.getValue( "type" ) );
            }
        }
        catch (Exception e)
        {
            throw new SAXException(e);
        }
    }

    public void endElement( String namespaceURI, String sName, String qName )
                    throws SAXException
    {
        if ( qName.equals( "testcase" ) )
        {
            testCases.add( testCase );
        } else if ( qName.equals( "failure" ) )
        {
            HashMap failure = testCase.getFailure(  );

            failure.put( "detail",
                         parseCause( currentElement.toString() ) );
        } else if ( qName.equals( "error" ) )
        {
            HashMap error = testCase.getFailure(  );

            error.put( "detail",
                       parseCause( currentElement.toString() ) );
        }
    }

    public void characters( char[] buf, int offset, int len )
                    throws SAXException
    {
        String s = new String( buf, offset, len );

        if ( ! s.trim(  ).equals( "" ) )
        {
            currentElement.append( s );
        }
    }

    public List getTestCases(  )
    {
        return this.testCases;
    }

    public void setTestCases( List TestCases )
    {
        this.testCases = TestCases;
    }

    public int getNumberOfErrors(  )
    {
        return numberOfErrors;
    }

    public void setNumberOfErrors( int numberOfErrors )
    {
        this.numberOfErrors = numberOfErrors;
    }

    public int getNumberOfFailures(  )
    {
        return numberOfFailures;
    }

    public void setNumberOfFailures( int numberOfFailures )
    {
        this.numberOfFailures = numberOfFailures;
    }

    public int getNumberOfTests(  )
    {
        return numberOfTests;
    }

    public void setNumberOfTests( int numberOfTests )
    {
        this.numberOfTests = numberOfTests;
    }

    public String getName(  )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getFName(  )
    {
        return name;
    }

    public void setFName( String name )
    {
        this.name = name;
    }

    public String getPackageName(  )
    {
        return packageName;
    }

    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    }

    public float getTimeElapsed(  )
    {
        return this.timeElapsed;
    }

    public void setTimeElapsed( float timeElapsed )
    {
        this.timeElapsed = timeElapsed;
    }

    private List parseCause( String detail )
    {
        String fullName = testCase.getFullName(  );
        String name = fullName.substring( fullName.lastIndexOf( "." ) + 1 );
        List parsedDetail = parseCause( detail, name );
        return parsedDetail;
    }

    private List parseCause( String detail, String compareTo )
    {
        StringTokenizer stringTokenizer = new StringTokenizer( detail, "\n" );
        List parsedDetail = new ArrayList( stringTokenizer.countTokens() );

        while ( stringTokenizer.hasMoreTokens() )
        {
            String lineString = stringTokenizer.nextToken().trim();
            parsedDetail.add( lineString );
            if ( lineString.indexOf( compareTo ) >= 0 )
            {
                break;
            }
        }

        return parsedDetail;
    }

}
