package org.apache.maven.plugin.antrun;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

