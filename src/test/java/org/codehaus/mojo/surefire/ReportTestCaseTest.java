/*
 * ReportTestCaseTest.java
 * JUnit based test
 *
 * Created on August 10, 2005, 10:29 AM
 */
package org.codehaus.mojo.surefire;

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
