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
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * Write a plexus configuration to a stream
 * Note: This class was originally copied from plexus-container-default.  It is duplicated here
 * to maintain compatibility with both Maven 2.x and Maven 3.x.
 *
 */
public class AntrunXmlPlexusConfigurationWriter
{

    public void write( PlexusConfiguration configuration, Writer writer )
        throws IOException
    {
        int depth = 0;

        PrettyPrintXMLWriter xmlWriter = new PrettyPrintXMLWriter( writer );
        write( configuration, xmlWriter, depth );
    }

    private void write( PlexusConfiguration c, XMLWriter w, int depth )
        throws IOException
    {
        int count = c.getChildCount();

        if ( count == 0 )
        {
            writeTag( c, w, depth );
        }
        else
        {
            w.startElement( c.getName() );
            writeAttributes( c, w );

            for ( int i = 0; i < count; i++ )
            {
                PlexusConfiguration child = c.getChild( i );

                write( child, w, depth + 1 );
            }
            
            w.endElement();
        }
    }

    private void writeTag( PlexusConfiguration c, XMLWriter w, int depth )
        throws IOException
    {
        w.startElement( c.getName() );
        
        writeAttributes( c, w );
        
        String value = c.getValue( null );
        if ( value != null )
        {
            w.writeText( value );
        }

        w.endElement();
    }

    private void writeAttributes( PlexusConfiguration c, XMLWriter w )
        throws IOException
    {
        String[] names = c.getAttributeNames();

        for ( int i = 0; i < names.length; i++ )
        {
            w.addAttribute( names[i], c.getAttribute( names[i], null ) );
        }
    }

}

