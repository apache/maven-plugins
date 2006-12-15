package org.codehaus.plexus.util.interpolation;

import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;


public class ObjectBasedValueSource
    implements ValueSource
{
    
    private final Object root;

    public ObjectBasedValueSource( Object root )
    {
        this.root = root;
    }

    public Object getValue( String expression )
    {
        try
        {
            return ReflectionValueExtractor.evaluate( expression, root, false );
        }
        catch ( Exception e )
        {
            return null;
        }
    }

}
