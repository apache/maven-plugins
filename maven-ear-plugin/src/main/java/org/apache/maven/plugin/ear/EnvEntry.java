package org.apache.maven.plugin.ear;

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

import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The representation of a env-entry entry within an
 * application.xml file.
 *
 * @author Jim Brownfield based on code by <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
class EnvEntry
{

    static final String ENV_ENTRY = "env-entry";

    static final String DESCRIPTION = "description";

    static final String ENV_ENTRY_NAME = "env-entry-name";

    static final String ENV_ENTRY_TYPE = "env-entry-type";

    static final String ENV_ENTRY_VALUE = "env-entry-value";

    private final String description;

    private final String name;

    private final String type;

    private final String value;

    public EnvEntry( String description, String name, String type, String value )
    {
        if ( isNullOrEmpty( name ) )
        {
            throw new IllegalArgumentException( ENV_ENTRY_NAME + " in " + ENV_ENTRY + " element cannot be null." );
        }
        else if ( isNullOrEmpty( type ) && isNullOrEmpty( value ) )
        {
            throw new IllegalArgumentException(
                ENV_ENTRY_TYPE + " in " + ENV_ENTRY + " element cannot be null if no " + ENV_ENTRY_VALUE +
                    " was specified." );

        }

        this.description = description;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }

    /**
     * Appends the <tt>XML</tt> representation of this env-entry.
     *
     * @param writer the writer to use
     */
    public void appendEnvEntry( XMLWriter writer )
    {
        writer.startElement( ENV_ENTRY );

        // description
        if ( getDescription() != null )
        {
            doWriteElement( writer, DESCRIPTION, getDescription() );
        }

        // env entry name
        doWriteElement( writer, ENV_ENTRY_NAME, getName() );

        // env entry type
        if ( getType() != null )
        {
            doWriteElement( writer, ENV_ENTRY_TYPE, getType() );
        }

        // env entry value
        if ( getValue() != null )
        {
            doWriteElement( writer, ENV_ENTRY_VALUE, getValue() );
        }

        // end of env-entry
        writer.endElement();
    }


    private void doWriteElement( XMLWriter writer, String element, String text )
    {
        writer.startElement( element );
        writer.writeText( text );
        writer.endElement();
    }

    private boolean isNullOrEmpty( String s )
    {
        return s == null || s.trim().isEmpty();
    }

    public String toString()
    {
        return "env-entry [name=" + getName() + ", type=" + getType() + ", value=" + getValue() + "]";
    }


}
