package org.apache.maven.plugin.eclipse;

import java.util.Enumeration;
import java.util.Properties;

import org.codehaus.plexus.util.xml.XMLWriter;

/**
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 *
 */
public class BuildCommand
{
    private String name;

    private Properties arguments;

    public BuildCommand( String name, Properties arguments )
    {
        this.name = name;
        this.arguments = arguments;
    }

    public BuildCommand( String name, String argName, String argValue )
    {
        this.name=name;
        arguments=new Properties();
        arguments.setProperty( argName, argValue );
    }

    public String getName()
    {
        return name;
    }

    public Properties getProperties()
    {
        return arguments;
    }

    public void print( XMLWriter writer )
    {
        writer.startElement( "buildCommand" );
        writer.startElement( "name" );
        writer.writeText( name );
        writer.endElement();

        if ( arguments != null && !arguments.isEmpty() )
        {
            writer.startElement( "arguments" );

            for ( Enumeration n = arguments.propertyNames(); n.hasMoreElements(); )
            {
                String key = (String) n.nextElement();

                // TODO: not sure about location of dictionary - it's usually just 1 argument.
                writer.startElement( "dictionary" );

                writer.startElement( "key" );
                writer.writeText( key );
                writer.endElement();

                writer.startElement( "value" );
                writer.writeText( arguments.getProperty( key ) );
                writer.endElement();

                writer.endElement();
            }

            writer.endElement();
        }

        writer.endElement();
    }
}
