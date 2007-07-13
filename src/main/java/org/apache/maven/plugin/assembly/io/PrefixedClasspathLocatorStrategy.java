package org.apache.maven.plugin.assembly.io;

import org.apache.maven.shared.io.location.ClasspathResourceLocatorStrategy;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.logging.MessageHolder;

public class PrefixedClasspathLocatorStrategy
    extends ClasspathResourceLocatorStrategy
{

    private final String prefix;

    public PrefixedClasspathLocatorStrategy( String prefix )
    {
        this.prefix = formatPrefix( prefix );
    }

    private String formatPrefix( String prefix )
    {
        if ( !prefix.startsWith( "/" ) )
        {
            prefix = "/" + prefix;
        }

        if ( !prefix.endsWith( "/" ) )
        {
            prefix += "/";
        }

        return prefix;
    }

    public Location resolve( String locationSpecification, MessageHolder messageHolder )
    {
        String spec = formatLocation( locationSpecification );

        return super.resolve( spec, messageHolder );
    }

    private String formatLocation( String location )
    {
        if ( location.startsWith( "/" ) )
        {
            location = location.substring( 1 );
        }

        return prefix + location;
    }

}
