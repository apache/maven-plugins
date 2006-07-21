package org.apache.maven.plugin.assembly.utils;

import java.util.Properties;

import junit.framework.TestCase;


public class PropertiesInterpolationValueSourceTest
    extends TestCase
{
    
    public void testShouldRetrievePropertyValueForKey()
    {
        Properties props = new Properties();
        props.setProperty( "key", "value" );
        
        assertEquals( "value", new PropertiesInterpolationValueSource( props ).getValue( "key" ) );
    }
    
    public void testShouldRetrievePropertyValueForKeyWithDot()
    {
        Properties props = new Properties();
        props.setProperty( "key.with.dot", "value" );
        
        assertEquals( "value", new PropertiesInterpolationValueSource( props ).getValue( "key.with.dot" ) );
    }

    public void testShouldRetrieveNullValueForMissingKey()
    {
        Properties props = new Properties();
        
        assertNull( new PropertiesInterpolationValueSource( props ).getValue( "key" ) );
    }

}
