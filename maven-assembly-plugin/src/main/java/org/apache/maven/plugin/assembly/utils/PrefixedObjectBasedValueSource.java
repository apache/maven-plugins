package org.apache.maven.plugin.assembly.utils;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.interpolation.ValueSource;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

public class PrefixedObjectBasedValueSource
    implements ValueSource
{

    private String prefix;
    private final Object root;
    private final Logger logger;

    public PrefixedObjectBasedValueSource( String prefix, Object root )
    {
        this.prefix = prefix;
        this.root = root;
        logger = null;
    }

    public PrefixedObjectBasedValueSource( String prefix, Object root, Logger logger )
    {
        this.prefix = prefix;
        this.root = root;
        this.logger = logger;
    }

    public Object getValue( String expression )
    {
        if ( ( expression == null ) || !expression.startsWith( prefix ) )
        {
            return null;
        }

        String realExpr = expression.substring( prefix.length() );
        if ( realExpr.startsWith( "." ) )
        {
            realExpr = realExpr.substring( 1 );
        }

        Object value = null;
        try
        {
            value = ReflectionValueExtractor.evaluate( realExpr, root, false );
        }
        catch ( Exception e )
        {
            if ( ( logger != null ) && logger.isDebugEnabled() )
            {
                logger.debug( "Failed to extract \'" + realExpr + "\' from: " + root + " (full expression was: \'" + expression + "\').", e );
            }
        }

        return value;
    }

}
