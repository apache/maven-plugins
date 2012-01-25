package org.apache.maven.plugin.ear;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Stephane Nicoll
 */
public class EnvEntryTest
{

    public static final String DESCRIPTION = "description";

    public static final String NAME = "name";

    public static final String TYPE = Integer.class.getName();

    public static final String VALUE = "34";

    @Test
    public void createComplete()
    {
        final EnvEntry envEntry = new EnvEntry( DESCRIPTION, NAME, TYPE, VALUE );
        assertEnvEntry( envEntry, DESCRIPTION, NAME, TYPE, VALUE );
    }

    @Test
    public void createWithoutTypeButValue()
    {
        final EnvEntry envEntry = new EnvEntry( null, NAME, null, VALUE );
        assertEnvEntry( envEntry, null, NAME, null, VALUE );
    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithoutName()
    {
        new EnvEntry( DESCRIPTION, null, TYPE, VALUE );

    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithEmptyName()
    {
        new EnvEntry( DESCRIPTION, "", TYPE, VALUE );
    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithNullTypeAndNoValue()
    {
        new EnvEntry( DESCRIPTION, NAME, null, null );

    }

    @Test( expected = IllegalArgumentException.class )
    public void createWithEmptyTypeAndNoValue()
    {
        new EnvEntry( DESCRIPTION, NAME, "", null );

    }

    private void assertEnvEntry( EnvEntry actual, String description, String name, String type, String value )
    {
        assertNotNull( "Env entry could not be null", actual );
        assertNotNull( "ToString could not be null", actual.toString() );
        assertEquals( "Wrong env entry description for [" + actual + "]", description, actual.getDescription() );
        assertEquals( "Wrong env entry name for [" + actual + "]", name, actual.getName() );
        assertEquals( "Wrong env entry type for [" + actual + "]", type, actual.getType() );
        assertEquals( "Wrong env entry value for [" + actual + "]", value, actual.getValue() );

    }
}
