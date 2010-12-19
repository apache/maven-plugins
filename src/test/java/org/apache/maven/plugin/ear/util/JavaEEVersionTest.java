package org.apache.maven.plugin.ear.util;

import junit.framework.TestCase;

/**
 * @author Stephane Nicoll
 */
public class JavaEEVersionTest
    extends TestCase
{

    public void testGtSameVersion()
    {
        assertFalse( JavaEEVersion.Five.gt( JavaEEVersion.Five ) );
    }

    public void testGtNextVersion()
    {
        assertFalse( JavaEEVersion.Five.gt( JavaEEVersion.Six ) );
    }

    public void testGtPreviousVersion()
    {
        assertTrue( JavaEEVersion.Five.gt( JavaEEVersion.OneDotFour ) );
    }

    public void testGeSameVersion()
    {
        assertTrue( JavaEEVersion.Five.ge( JavaEEVersion.Five ) );
    }

    public void testGePreviousVersion()
    {
        assertTrue( JavaEEVersion.Five.ge( JavaEEVersion.OneDotFour ) );
    }

    public void testGeNextVersion()
    {
        assertFalse( JavaEEVersion.Five.ge( JavaEEVersion.Six ) );
    }

    public void testLtSameVersion()
    {
        assertFalse( JavaEEVersion.Five.lt( JavaEEVersion.Five ) );
    }

    public void testLtPreviousVersion()
    {
        assertFalse( JavaEEVersion.Five.lt( JavaEEVersion.OneDotFour ) );
    }

    public void testLtNextVersion()
    {
        assertTrue( JavaEEVersion.Five.lt( JavaEEVersion.Six ) );
    }

    public void testLeSameVersion()
    {
        assertTrue( JavaEEVersion.Five.le( JavaEEVersion.Five ) );
    }

    public void testLePreviousVersion()
    {
        assertFalse( JavaEEVersion.Five.le( JavaEEVersion.OneDotFour ) );
    }

    public void testLeNextVersion()
    {
        assertTrue( JavaEEVersion.Five.le( JavaEEVersion.Six ) );
    }

    public void testEqSameVersion()
    {
        assertTrue( JavaEEVersion.Five.eq( JavaEEVersion.Five ) );
    }

    public void testEqAnotherVersion()
    {
        assertFalse( JavaEEVersion.Five.eq( JavaEEVersion.OneDotThree ) );
    }

    public void testGetVersion()
    {
        assertEquals( "5", JavaEEVersion.Five.getVersion() );
    }

    public void testGetJavaEEVersionValid()
    {
        try
        {
            assertEquals( JavaEEVersion.Six, JavaEEVersion.getJavaEEVersion( "6" ) );
        }
        catch ( InvalidJavaEEVersion invalidJavaEEVersion )
        {
            fail( "No exception should have been thrown but got [" + invalidJavaEEVersion.getMessage() + "]" );
        }
    }

    public void testGetJavaEEVersionInvalid()
    {
        try
        {
            JavaEEVersion.getJavaEEVersion( "2.4" );
            fail( "Should have failed to get an invalid version." );
        }
        catch ( InvalidJavaEEVersion e )
        {
            //OK
        }
    }

    public void testGetJavaEEVersionNull()
    {
        try
        {
            JavaEEVersion.getJavaEEVersion( null );
            fail( "Should have failed to get a 'null' version." );
        }
        catch ( InvalidJavaEEVersion e )
        {
            fail( "Should have failed with an illegal argument exception instead." );
        }
        catch ( IllegalArgumentException e )
        {
            // OK
        }

    }


}
