package org.codehaus.plexus.util.interpolation;

import java.util.Map;


public class MapBasedValueSource
    implements ValueSource
{
    
    private final Map values;

    public MapBasedValueSource( Map values )
    {
        this.values = values;
    }

    public Object getValue( String expression )
    {
        return values.get( expression );
    }

}
