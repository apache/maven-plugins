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

import junit.framework.*;

import java.util.HashMap;

/**
 *
 * @author Jontri
 */
public class ReportTestCaseTest
    extends TestCase
{
    ReportTestCase tCase;

    public ReportTestCaseTest( String testName )
    {
        super( testName );
    }

    protected void setUp(  )
                  throws Exception
    {
        tCase = new ReportTestCase(  );
    }

    protected void tearDown(  )
                     throws Exception
    {
    }

    public static Test suite(  )
    {
        TestSuite suite = new TestSuite( ReportTestCaseTest.class );

        return suite;
    }

    public void testSetName(  )
    {
        tCase.setName( "Test Case Name" );

        assertEquals( "Test Case Name",
                      tCase.getName(  ) );
    }

    public void testSetTime(  )
    {
        tCase.setTime( .06f );

        assertTrue( .06f == tCase.getTime(  ) );
    }

    public void testSetFailure(  )
    {
        HashMap hMap = new HashMap(  );

        tCase.setFailure( hMap );

        assertEquals( hMap,
                      tCase.getFailure(  ) );
    }

    public void testSetFullName(  )
    {
        tCase.setFullName( "Test Case Full Name" );

        assertEquals( "Test Case Full Name",
                      tCase.getFullName(  ) );
    }
}
