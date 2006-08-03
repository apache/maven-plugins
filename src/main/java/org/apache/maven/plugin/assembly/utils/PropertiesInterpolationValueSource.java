package org.apache.maven.plugin.assembly.utils;

import org.codehaus.plexus.util.interpolation.ValueSource;

import java.util.Properties;


public class PropertiesInterpolationValueSource
    implements ValueSource
{
    
    private final Properties properties;

    public PropertiesInterpolationValueSource( Properties properties )
    {
        this.properties = properties;
    }

    public Object getValue( String key )
    {
        return properties.getProperty( key );
    }

}
