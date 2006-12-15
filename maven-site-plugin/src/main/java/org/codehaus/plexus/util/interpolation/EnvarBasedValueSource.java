package org.codehaus.plexus.util.interpolation;

import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.IOException;
import java.util.Properties;

public class EnvarBasedValueSource
    implements ValueSource
{
    
    private Properties envars;
    private final boolean caseSensitive;

    /**
     * Create a new value source for interpolation based on shell environment variables. In this
     * case, envar keys ARE CASE SENSITIVE.
     * 
     * @throws IOException
     */
    public EnvarBasedValueSource() throws IOException
    {
        this( true );
    }

    /**
     * Create a new value source for interpolation based on shell environment variables.
     * 
     * @param caseSensitive Whether the environment variable key should be treated in a 
     *                      case-sensitive manner for lookups
     * @throws IOException
     */
    public EnvarBasedValueSource( boolean caseSensitive ) throws IOException
    {
        this.caseSensitive = caseSensitive;
        
        envars = CommandLineUtils.getSystemEnvVars( caseSensitive );
    }

    public Object getValue( String expression )
    {
        String expr = expression;
        
        if ( expr.startsWith( "env." ) )
        {
            expr = expr.substring( "env.".length() );
        }
        
        if ( !caseSensitive )
        {
            expr = expr.toUpperCase();
        }
        
        return envars.getProperty( expr );
    }
}
