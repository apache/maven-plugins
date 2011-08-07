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

package org.apache.maven.plugin.eclipse;

import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Represents a LinkedResources section in the <code>.project</code> file.
 * 
 * @author <a href="mailto:ashoknanw@gmail.com">Ashokkumar Sankaran</a>
 */
public class LinkedResource
{
    /** Resource name */
    private String name;

    /** Type */
    private String type;

    /** Resource location */
    private String location;

    /** Resource localtionURI */
    private String locationURI;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    public String getLocationURI()
    {
        return locationURI;
    }

    public void setLocationURI( String locationURI )
    {
        this.locationURI = locationURI;
    }

    /**
     * Default constructor
     */
    public LinkedResource()
    {
        super();
    }

    /**
     * Creates a LinkedResource from a DOM subtree
     * <p>
     * The subtree must represent a &lt;linkedResources&gt; section from an Eclipse .project file
     * 
     * @param node DOM node
     */
    public LinkedResource( Xpp3Dom node )
    {
        Xpp3Dom nameNode = node.getChild( "name" );

        if ( nameNode == null )
        {
            throw new IllegalArgumentException( "No name node." );
        }

        name = nameNode.getValue();

        Xpp3Dom typeNode = node.getChild( "type" );

        if ( typeNode == null )
        {
            throw new IllegalArgumentException( "No type node." );
        }

        type = typeNode.getValue();

        Xpp3Dom locationNode = node.getChild( "location" );
        Xpp3Dom locationURINode = node.getChild( "locationURI" );

        if ( locationNode == null && locationURINode == null )
        {
            throw new IllegalArgumentException( "No location or locationURI node." );
        }
        else if ( locationNode != null && locationURINode != null )
        {
            throw new IllegalArgumentException( "Both location and locationURI nodes are set." );
        }

        location = locationNode.getValue();
    }

    public void print( XMLWriter writer )
    {
        writer.startElement( "link" );

        writer.startElement( "name" );
        writer.writeText( name );
        writer.endElement(); // name

        writer.startElement( "type" );
        writer.writeText( type );
        writer.endElement(); // type

        if ( location != null )
        {
            writer.startElement( "location" );
            writer.writeText( location );
            writer.endElement(); // location
        }
        else if ( locationURI != null )
        {
            writer.startElement( "locationURI" );
            writer.writeText( locationURI );
            writer.endElement(); // locationURI
        }
        writer.endElement();// link
    }

    public boolean equals( Object obj )
    {
        if ( obj instanceof LinkedResource )
        {
            LinkedResource b = (LinkedResource) obj;

            return name.equals( b.name ) && ( type == null ? b.type == null : type.equals( b.type ) )
                && ( location == null ? b.location == null : location.equals( b.location ) )
                && ( locationURI == null ? b.locationURI == null : locationURI.equals( b.locationURI ) );
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return name.hashCode() + ( type == null ? 0 : 13 * type.hashCode() )
            + ( location == null ? 0 : 17 * location.hashCode() )
            + ( locationURI == null ? 0 : 19 * locationURI.hashCode() );
    }
}
