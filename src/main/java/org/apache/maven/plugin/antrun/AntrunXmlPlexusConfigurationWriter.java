package org.apache.maven.plugin.antrun;

import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Write a plexus configuration to a stream
 * Note: this is a copy of a class from plexus-container-default.  It is duplicated here
 * to maintain compatibility with both Maven 2.x and Maven 3.x.
 *
 */
public class AntrunXmlPlexusConfigurationWriter
{

    public void write( OutputStream outputStream, PlexusConfiguration configuration )
        throws IOException
    {
        write( new OutputStreamWriter( outputStream, "UTF-8" ), configuration );
    }

    public void write( Writer writer, PlexusConfiguration configuration )
        throws IOException
    {
        int depth = 0;

        display( configuration, writer, depth );
    }

    private void display( PlexusConfiguration c, Writer w, int depth )
        throws IOException
    {
        int count = c.getChildCount();

        if ( count == 0 )
        {
            displayTag( c, w, depth );
        }
        else
        {
            indent( depth, w );
            w.write( '<' );
            w.write( c.getName() );

            attributes( c, w );

            w.write( '>' );
            w.write( '\n' );

            for ( int i = 0; i < count; i++ )
            {
                PlexusConfiguration child = c.getChild( i );

                display( child, w, depth + 1 );
            }

            indent( depth, w );
            w.write( '<' );
            w.write( '/' );
            w.write( c.getName() );
            w.write( '>' );
            w.write( '\n' );
        }
    }

    private void displayTag( PlexusConfiguration c, Writer w, int depth )
        throws IOException
    {
        String value = c.getValue( null );

        if ( value != null )
        {
            indent( depth, w );
            w.write( '<' );
            w.write( c.getName() );

            attributes( c, w );

            w.write( '>' );
            w.write( c.getValue( null ) );
            w.write( '<' );
            w.write( '/' );
            w.write( c.getName() );
            w.write( '>' );
            w.write( '\n' );
        }
        else
        {
            indent( depth, w );
            w.write( '<' );
            w.write( c.getName() );

            attributes( c, w );

            w.write( '/' );
            w.write( '>' );
            w.write( "\n" );
        }
    }

    private void attributes( PlexusConfiguration c, Writer w )
        throws IOException
    {
        String[] names = c.getAttributeNames();

        for ( int i = 0; i < names.length; i++ )
        {
            w.write( ' ' );
            w.write( names[i] );
            w.write( '=' );
            w.write( '"' );
            w.write( c.getAttribute( names[i], null ) );
            w.write( '"' );
        }
    }

    private void indent( int depth, Writer w )
        throws IOException
    {
        for ( int i = 0; i < depth; i++ )
        {
            w.write( ' ' );
        }
    }

}

