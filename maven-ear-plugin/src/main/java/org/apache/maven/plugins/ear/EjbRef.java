package org.apache.maven.plugins.ear;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Representation of {@code ejb-ref} element in {@code application.xml} file.
 * 
 * @author Karl Heinz Marbaise
 * @since 2.10
 */
public class EjbRef
{
    static final String DESCRIPTION = "description";

    static final String EJB_REF = "ejb-ref";

    static final String EJB_NAME = "ejb-ref-name";

    static final String EJB_TYPE = "ejb-ref-type";

    static final String EJB_LOOKUP_NAME = "lookup-name";

    private final String description;

    private String name;

    private String type;

    private String lookupName;

    /**
     * @param description The ejb-ref description.
     * @param name The ejb-ref-name.
     * @param type The ejb-ref-type
     * @param lookupName The lookupname.
     */
    public EjbRef( String description, String name, String type, String lookupName )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            throw new IllegalArgumentException( EJB_NAME + " in " + EJB_REF + " element cannot be null." );
        }
        else if ( StringUtils.isEmpty( type ) && StringUtils.isEmpty( lookupName ) )
        {
            throw new IllegalArgumentException( EJB_TYPE + " in " + EJB_REF + " element cannot be null if no "
                + EJB_LOOKUP_NAME + " was specified." );

        }

        this.description = description;
        this.name = name;
        this.type = type;
        this.lookupName = lookupName;

    }

    /**
     * Appends the <tt>XML</tt> representation of this env-entry.
     * 
     * @param writer the writer to use
     */
    public void appendEjbRefEntry( XMLWriter writer )
    {
        writer.startElement( EJB_REF );

        // description
        if ( getDescription() != null )
        {
            doWriteElement( writer, DESCRIPTION, getDescription() );
        }

        // ejb name
        doWriteElement( writer, EJB_NAME, getName() );

        // ejb-type
        if ( getType() != null )
        {
            doWriteElement( writer, EJB_TYPE, getType() );
        }

        // lookup-name
        if ( getLookupName() != null )
        {
            doWriteElement( writer, EJB_LOOKUP_NAME, getLookupName() );
        }

        // end of ejb-ref
        writer.endElement();
    }

    private void doWriteElement( XMLWriter writer, String element, String text )
    {
        writer.startElement( element );
        writer.writeText( text );
        writer.endElement();
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name {@link #name}
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * @return {@link #type}
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type {@link #type}
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * @return {@link #lookupName}
     */
    public String getLookupName()
    {
        return lookupName;
    }

    /**
     * @param lookupName {@link #lookupName}
     */
    public void setLookupName( String lookupName )
    {
        this.lookupName = lookupName;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }
}
